package com.example.engine

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.ui.graphics.toArgb
import com.example.model.EffectArchetype
import com.example.model.NeonAssetSpec
import com.example.model.NeonEffectId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class RenderParams(
    val asset: NeonAssetSpec,
    val overlayVfx: NeonAssetSpec? = null,
    val timeSec: Float = 0f,
    val cornerRadiusFraction: Float = 0.14f,
    val bevelDepthScale: Float = 1.0f,
    val glowIntensity: Float = 1.0f,
    val particleDensity: Float = 1.0f,
    val clearTransparentBackground: Boolean = true
)

/**
 * Deterministic 3D PBR Neon Frame & Volumetric VFX Engine.
 * Supports direct Canvas rendering AND native 16K tiled rendering without upscaling.
 */
object Neon3DRenderer {
    const val LOOP_DURATION_SEC = 15f
    private const val TWO_PI = (2.0 * PI).toFloat()

    fun renderToCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        params: RenderParams
    ) {
        if (width <= 2f || height <= 2f) return

        if (params.clearTransparentBackground) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        val normalizedTime = ((params.timeSec % LOOP_DURATION_SEC) + LOOP_DURATION_SEC) % LOOP_DURATION_SEC
        val phase = normalizedTime / LOOP_DURATION_SEC
        val theta = phase * TWO_PI

        if (params.asset.isFrameAsset) {
            render3DNeonFrame(canvas, width, height, params.asset, theta, phase, params)
            params.overlayVfx?.let { vfx ->
                if (!vfx.isFrameAsset) {
                    renderVfxOverlay(canvas, width, height, vfx, theta, phase, params.particleDensity, params.glowIntensity)
                }
            }
        } else {
            renderVfxOverlay(canvas, width, height, params.asset, theta, phase, params.particleDensity, params.glowIntensity)
        }
    }

    /**
     * Renders a single tile of a native 16K master canvas (e.g., 4x4 grid of 3840x2160 = 15360x8640)
     * with exact sub-pixel vector geometry at native 16K coordinates.
     */
    fun renderMasterTileToCanvas(
        tileCanvas: Canvas,
        tileWidth: Int,
        tileHeight: Int,
        tileCol: Int,
        tileRow: Int,
        gridCols: Int,
        gridRows: Int,
        params: RenderParams
    ) {
        val fullWidth = (tileWidth * gridCols).toFloat()
        val fullHeight = (tileHeight * gridRows).toFloat()
        if (params.clearTransparentBackground) {
            tileCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }
        tileCanvas.save()
        tileCanvas.translate(-(tileCol * tileWidth).toFloat(), -(tileRow * tileHeight).toFloat())
        renderToCanvas(
            canvas = tileCanvas,
            width = fullWidth,
            height = fullHeight,
            params = params.copy(clearTransparentBackground = false)
        )
        tileCanvas.restore()
    }

    private fun render3DNeonFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        spec: NeonAssetSpec,
        theta: Float,
        phase: Float,
        params: RenderParams
    ) {
        val minDim = min(width, height)
        val marginX = width * 0.135f
        val marginY = height * 0.135f
        val frameRect = RectF(marginX, marginY, width - marginX, height - marginY)
        val cornerRadius = (min(frameRect.width(), frameRect.height()) * params.cornerRadiusFraction)
            .coerceIn(minDim * 0.04f, minDim * 0.28f)

        val tubeThickness = (minDim * 0.023f) * params.bevelDepthScale.coerceIn(0.6f, 1.8f)
        val framePath = Path().apply { addRoundRect(frameRect, cornerRadius, cornerRadius, Path.Direction.CW) }

        val primaryInt = spec.primaryColor.toArgb()
        val secondaryInt = spec.secondaryColor.toArgb()
        val coreInt = spec.coreColor.toArgb()
        val chassisInt = spec.chassisTint.toArgb()

        val pulse = computePulseFactor(spec.archetype, spec.id, theta, phase)
        val glowScale = params.glowIntensity.coerceIn(0.3f, 1.8f)
        val effectiveGlow = (0.72f + 0.48f * pulse) * glowScale

        // LAYER 5 (Underlay) — VOLUMETRIC ATMOSPHERIC GLOW & SCATTERING
        val wideBlur = max(4f, tubeThickness * 2.2f)
        val wideGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = tubeThickness * 2.4f
            color = adjustAlpha(secondaryInt, (0.28f * effectiveGlow).coerceIn(0f, 0.72f))
            maskFilter = BlurMaskFilter(wideBlur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(framePath, wideGlowPaint)

        val midBlur = max(3f, tubeThickness * 1.35f)
        val midGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = tubeThickness * 1.6f
            color = adjustAlpha(primaryInt, (0.52f * effectiveGlow).coerceIn(0f, 0.90f))
            maskFilter = BlurMaskFilter(midBlur, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(framePath, midGlowPaint)

        // LAYER 1 — CORE 3D METALLIC & BEVELED CHASSIS STRUCTURE
        val depthOffset = tubeThickness * 0.22f
        val extrudedRect = RectF(
            frameRect.left + depthOffset,
            frameRect.top + depthOffset,
            frameRect.right + depthOffset,
            frameRect.bottom + depthOffset
        )
        val extrudedPath = Path().apply { addRoundRect(extrudedRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        val chassisBackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = tubeThickness * 1.75f
            color = adjustAlpha(Color.BLACK, 0.72f)
        }
        canvas.drawPath(extrudedPath, chassisBackPaint)

        val metallicShader = LinearGradient(
            frameRect.left, frameRect.top,
            frameRect.right, frameRect.bottom,
            intArrayOf(
                blendColors(chassisInt, Color.WHITE, if (spec.archetype == EffectArchetype.FRAME_LIQUID_CHROME) 0.65f else 0.28f),
                chassisInt,
                blendColors(chassisInt, primaryInt, 0.22f),
                blendColors(chassisInt, Color.BLACK, 0.55f),
                blendColors(chassisInt, secondaryInt, 0.34f)
            ),
            floatArrayOf(0f, 0.25f, 0.52f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        val chassisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = tubeThickness * 1.65f
            shader = metallicShader
        }
        canvas.drawPath(framePath, chassisPaint)

        // LAYER 3 — OUTER TRANSPARENT GLASS CHANNEL & SPECIALIZED SHADERS
        val glassBevelWidth = tubeThickness * 1.32f
        val glassSweepPos = 0.5f + 0.45f * sin(theta)
        val glassShader: Shader = when (spec.archetype) {
            EffectArchetype.FRAME_PRISM_HOLOGRAPHIC -> {
                val rotOffset = phase
                SweepGradient(
                    frameRect.centerX(),
                    frameRect.centerY(),
                    intArrayOf(
                        adjustAlpha(Color.rgb(0, 245, 212), 0.72f),
                        adjustAlpha(Color.rgb(58, 134, 255), 0.72f),
                        adjustAlpha(Color.rgb(255, 78, 205), 0.72f),
                        adjustAlpha(Color.rgb(255, 190, 11), 0.72f),
                        adjustAlpha(Color.rgb(0, 245, 212), 0.72f)
                    ),
                    floatArrayOf(
                        0f,
                        (0.25f + 0.08f * sin(theta + rotOffset)).coerceIn(0.05f, 0.45f),
                        0.5f,
                        (0.75f + 0.08f * cos(theta)).coerceIn(0.55f, 0.95f),
                        1f
                    )
                )
            }
            EffectArchetype.FRAME_AURORA_VOLUMETRIC -> {
                LinearGradient(
                    frameRect.left,
                    frameRect.top + frameRect.height() * (0.3f + 0.2f * sin(theta)),
                    frameRect.right,
                    frameRect.bottom - frameRect.height() * (0.3f + 0.2f * cos(theta)),
                    intArrayOf(
                        adjustAlpha(primaryInt, 0.52f),
                        adjustAlpha(secondaryInt, 0.48f),
                        adjustAlpha(Color.rgb(157, 78, 221), 0.45f),
                        adjustAlpha(primaryInt, 0.52f)
                    ),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            else -> {
                LinearGradient(
                    frameRect.left,
                    frameRect.top + frameRect.height() * (glassSweepPos - 0.35f),
                    frameRect.right,
                    frameRect.top + frameRect.height() * (glassSweepPos + 0.35f),
                    intArrayOf(
                        adjustAlpha(primaryInt, 0.22f),
                        adjustAlpha(Color.WHITE, 0.48f * (0.6f + 0.4f * pulse)),
                        adjustAlpha(primaryInt, 0.18f),
                        adjustAlpha(secondaryInt, 0.34f)
                    ),
                    floatArrayOf(0f, 0.45f, 0.65f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        }
        val glassRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = glassBevelWidth
            shader = glassShader
        }
        canvas.drawPath(framePath, glassRimPaint)

        // Inner & outer beveled glass Fresnel edge highlight lines
        val outerInset = -tubeThickness * 0.62f
        val innerInset = tubeThickness * 0.62f
        val outerRimRect = RectF(frameRect).apply { inset(outerInset, outerInset) }
        val innerRimRect = RectF(frameRect).apply { inset(innerInset, innerInset) }

        val bevelRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, tubeThickness * 0.11f)
            color = adjustAlpha(Color.WHITE, 0.38f + 0.24f * pulse)
        }
        canvas.drawRoundRect(outerRimRect, cornerRadius - outerInset, cornerRadius - outerInset, bevelRimPaint)

        val innerReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, tubeThickness * 0.14f)
            color = adjustAlpha(primaryInt, 0.58f * (0.7f + 0.3f * pulse))
        }
        canvas.drawRoundRect(innerRimRect, max(4f, cornerRadius - innerInset), max(4f, cornerRadius - innerInset), innerReflectionPaint)

        // LAYER 2 — NEON LIGHT TUBE & TRAVELING HIGHLIGHTS
        val tubeBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = tubeThickness * 0.68f
            color = adjustAlpha(primaryInt, (0.78f + 0.22f * pulse).coerceIn(0f, 1f))
            maskFilter = BlurMaskFilter(max(1.5f, tubeThickness * 0.25f), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(framePath, tubeBasePaint)

        renderTravelingHighlight(canvas, framePath, spec, theta, tubeThickness, effectiveGlow)

        // LAYER 4 — RAZOR-SHARP EMISSIVE CORE
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, tubeThickness * 0.26f)
            color = adjustAlpha(coreInt, (0.65f + 0.35f * pulse).coerceIn(0f, 1f))
        }
        canvas.drawPath(framePath, corePaint)

        // LAYER 6 — MICRO DETAILS, CIRCUIT NODES, PLASMA ARCS & DETERMINISTIC PARTICLES
        renderMicroDetailsAndPlasma(canvas, framePath, spec, theta, phase, tubeThickness, params.particleDensity, effectiveGlow)

        // PRO-50 SPECIAL 3D ARCHITECTURAL GEOMETRY (Double Chamber, Aerospace HUD Brackets, Cyber Wings)
        NeonProVfxShaders.renderProFrameExtraGeometry(
            canvas = canvas,
            frameRect = frameRect,
            cornerRadius = cornerRadius,
            tubeThickness = tubeThickness,
            spec = spec,
            theta = theta,
            glowScale = effectiveGlow
        )
    }

    private fun computePulseFactor(archetype: EffectArchetype, id: NeonEffectId, theta: Float, phase: Float): Float {
        return when {
            archetype == EffectArchetype.FRAME_BREATHING -> 0.28f + 0.72f * (0.5f - 0.5f * cos(theta * 3f))
            archetype == EffectArchetype.FRAME_HEARTBEAT -> {
                val beatPhase = (phase * 5f) % 1f
                val lub = expPulse(beatPhase, center = 0.14f, width = 0.055f)
                val dub = 0.68f * expPulse(beatPhase, center = 0.31f, width = 0.06f)
                (0.22f + 0.78f * max(lub, dub)).coerceIn(0f, 1f)
            }
            id == NeonEffectId.ELECTRIC_PULSE -> {
                val p = (phase * 5f) % 1f
                val spike = expPulse(p, 0.18f, 0.04f) + 0.6f * expPulse(p, 0.52f, 0.03f)
                (0.42f + 0.58f * spike).coerceIn(0f, 1f)
            }
            archetype in setOf(
                EffectArchetype.FRAME_GLASS_BEVEL_3D,
                EffectArchetype.FRAME_LIQUID_CHROME,
                EffectArchetype.FRAME_CINEMATIC_GLOW,
                EffectArchetype.FRAME_AURORA_VOLUMETRIC
            ) -> 0.55f + 0.45f * (0.5f + 0.5f * sin(theta * 2f))
            else -> 0.65f + 0.35f * (0.5f + 0.5f * sin(theta * 3f))
        }
    }

    private fun expPulse(x: Float, center: Float, width: Float): Float {
        val d = (x - center) / width
        return kotlin.math.exp((-d * d).toDouble()).toFloat()
    }

    private fun renderTravelingHighlight(
        canvas: Canvas,
        framePath: Path,
        spec: NeonAssetSpec,
        theta: Float,
        tubeThickness: Float,
        glowScale: Float
    ) {
        if (spec.archetype == EffectArchetype.FRAME_BREATHING || spec.archetype == EffectArchetype.FRAME_HEARTBEAT) return

        val pm = PathMeasure(framePath, true)
        val totalLen = pm.length
        if (totalLen <= 0f) return

        val primaryInt = spec.primaryColor.toArgb()
        val coreInt = spec.coreColor.toArgb()

        val headFraction = when (spec.archetype) {
            EffectArchetype.FRAME_LIGHT_SCAN, EffectArchetype.FRAME_CYBER_CIRCUIT, EffectArchetype.FRAME_ENERGY_FLOW ->
                ((theta / TWO_PI) * 3f) % 1f
            else -> ((theta / TWO_PI) * 2f) % 1f
        }

        val packets = if (spec.archetype == EffectArchetype.FRAME_LIGHT_SCAN) 1 else 2
        val segmentArc = if (spec.archetype == EffectArchetype.FRAME_LIGHT_SCAN) totalLen * 0.16f else totalLen * 0.24f

        for (p in 0 until packets) {
            val offsetFrac = (headFraction + p * 0.5f) % 1f
            val endDist = offsetFrac * totalLen
            val startDist = endDist - segmentArc

            val segmentPath = Path()
            if (startDist >= 0f) {
                pm.getSegment(startDist, endDist, segmentPath, true)
            } else {
                pm.getSegment(totalLen + startDist, totalLen, segmentPath, true)
                pm.getSegment(0f, endDist, segmentPath, true)
            }

            val scanGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeWidth = tubeThickness * 1.45f
                color = adjustAlpha(primaryInt, (0.68f * glowScale).coerceIn(0f, 0.95f))
                maskFilter = BlurMaskFilter(max(2f, tubeThickness * 1.1f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(segmentPath, scanGlowPaint)

            val pos = FloatArray(2)
            pm.getPosTan(endDist, pos, null)
            val flareRadius = tubeThickness * if (spec.archetype == EffectArchetype.FRAME_LIGHT_SCAN) 2.5f else 1.8f
            val flarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    pos[0], pos[1], flareRadius,
                    intArrayOf(coreInt, adjustAlpha(primaryInt, 0.7f), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.38f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(pos[0], pos[1], flareRadius, flarePaint)
        }
    }

    private fun renderMicroDetailsAndPlasma(
        canvas: Canvas,
        framePath: Path,
        spec: NeonAssetSpec,
        theta: Float,
        phase: Float,
        tubeThickness: Float,
        particleDensity: Float,
        glowScale: Float
    ) {
        val pm = PathMeasure(framePath, true)
        val totalLen = pm.length
        if (totalLen <= 0f) return

        val pos = FloatArray(2)
        val tan = FloatArray(2)
        val primaryInt = spec.primaryColor.toArgb()
        val secondaryInt = spec.secondaryColor.toArgb()
        val coreInt = spec.coreColor.toArgb()

        // Cyber circuit etched nodes along beveled rail
        if (spec.archetype == EffectArchetype.FRAME_CYBER_CIRCUIT) {
            val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, tubeThickness * 0.10f)
            }
            for (i in 0 until 16) {
                val s = i / 16f
                pm.getPosTan(s * totalLen, pos, tan)
                val nx = -tan[1]
                val ny = tan[0]
                val side = if (i % 2 == 0) 1f else -1f
                val pulseHere = (0.5f + 0.5f * sin(theta * 3f - s * TWO_PI * 2f)).pow(2f)
                val endX = pos[0] + nx * side * tubeThickness * 0.72f
                val endY = pos[1] + ny * side * tubeThickness * 0.72f
                tracePaint.color = adjustAlpha(secondaryInt, 0.35f + 0.55f * pulseHere)
                canvas.drawLine(pos[0], pos[1], endX, endY, tracePaint)
                nodePaint.color = adjustAlpha(coreInt, 0.45f + 0.55f * pulseHere)
                canvas.drawCircle(endX, endY, max(1.5f, tubeThickness * 0.14f), nodePaint)
            }
        }

        // Faceted Diamond Crystal glints & Molten Copper micro-sparks
        if (spec.archetype == EffectArchetype.FRAME_DIAMOND_CRYSTAL_3D) {
            val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            for (i in 0 until 20) {
                val s = (i / 20f + phase * 0.5f) % 1f
                pm.getPosTan(s * totalLen, pos, tan)
                val sparkle = (0.5f + 0.5f * sin(theta * 4f + i * 1.3f)).pow(3f)
                if (sparkle > 0.25f) {
                    val arm = tubeThickness * (0.45f + 0.75f * sparkle)
                    glintPaint.strokeWidth = max(1f, tubeThickness * 0.08f)
                    glintPaint.color = adjustAlpha(if (i % 2 == 0) Color.WHITE else primaryInt, 0.78f * sparkle)
                    canvas.drawLine(pos[0] - arm, pos[1], pos[0] + arm, pos[1], glintPaint)
                    canvas.drawLine(pos[0], pos[1] - arm, pos[0], pos[1] + arm, glintPaint)
                }
            }
        }

        // Plasma arcs for plasma / electric / prism / molten copper models
        val hasElectricArcs = spec.archetype in setOf(
            EffectArchetype.FRAME_PLASMA_ELECTRIC,
            EffectArchetype.FRAME_PRISM_HOLOGRAPHIC,
            EffectArchetype.FRAME_MOLTEN_COPPER_3D
        )
        if (hasElectricArcs) {
            val arcPath = Path()
            val samples = 140
            val amp = tubeThickness * when (spec.id) {
                NeonEffectId.ELECTRIC_PULSE -> 0.52f * (0.4f + 0.6f * computePulseFactor(spec.archetype, spec.id, theta, phase))
                NeonEffectId.ICE_WHITE_PLASMA, NeonEffectId.VFX_07_NEON_GLASS_FRAME -> 0.34f
                else -> 0.28f
            }
            for (i in 0..samples) {
                val frac = i.toFloat() / samples
                val d = frac * totalLen
                pm.getPosTan(d, pos, tan)
                val nx = -tan[1]
                val ny = tan[0]
                val w1 = sin(frac * TWO_PI * 18f + theta * 4f)
                val w2 = 0.55f * sin(frac * TWO_PI * 37f - theta * 7f)
                val w3 = 0.30f * cos(frac * TWO_PI * 61f + theta * 11f)
                val jitter = amp * (w1 + w2 + w3) * 0.55f
                val px = pos[0] + nx * jitter
                val py = pos[1] + ny * jitter
                if (i == 0) arcPath.moveTo(px, py) else arcPath.lineTo(px, py)
            }
            arcPath.close()

            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, tubeThickness * 0.14f)
                color = adjustAlpha(coreInt, (0.68f * glowScale).coerceIn(0f, 0.95f))
                maskFilter = BlurMaskFilter(max(1f, tubeThickness * 0.18f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(arcPath, arcPaint)
        }

        // Deterministic orbiting & drifting micro-particles near frame wall
        val count = (28 * particleDensity).toInt().coerceIn(8, 72)
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until count) {
            val seed = (i * 0.6180339887f) % 1f
            val speedHarmonic = 1 + (i % 2)
            val s = (seed + phase * speedHarmonic) % 1f
            pm.getPosTan(s * totalLen, pos, tan)
            val nx = -tan[1]
            val ny = tan[0]
            val radialOsc = sin(theta * (2 + i % 3) + seed * TWO_PI)
            val offset = tubeThickness * (0.9f + 2.1f * ((i * 37 % 100) / 100f)) * radialOsc
            val px = pos[0] + nx * offset
            val py = pos[1] + ny * offset
            val pRadius = max(1.2f, tubeThickness * (0.10f + 0.16f * ((i * 19 % 100) / 100f)))
            val alpha = (0.35f + 0.55f * (0.5f + 0.5f * cos(theta * 3f + seed * TWO_PI))) * glowScale.coerceAtMost(1.2f)

            particlePaint.color = adjustAlpha(if (i % 3 == 0) coreInt else primaryInt, alpha.coerceIn(0f, 0.92f))
            canvas.drawCircle(px, py, pRadius, particlePaint)
        }

        // Micro-scratches catching light on metallic bevel
        val scratchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(0.8f, tubeThickness * 0.05f)
        }
        for (i in 0 until 14) {
            val s = ((i * 0.754877f) % 1f)
            pm.getPosTan(s * totalLen, pos, tan)
            val nx = -tan[1]
            val ny = tan[0]
            val glint = (0.5f + 0.5f * sin(theta * 2f + s * TWO_PI)).pow(3f)
            if (glint > 0.15f) {
                scratchPaint.color = adjustAlpha(Color.WHITE, 0.35f * glint)
                val len = tubeThickness * 0.45f
                canvas.drawLine(
                    pos[0] + nx * tubeThickness * 0.4f - tan[0] * len * 0.5f,
                    pos[1] + ny * tubeThickness * 0.4f - tan[1] * len * 0.5f,
                    pos[0] + nx * tubeThickness * 0.4f + tan[0] * len * 0.5f,
                    pos[1] + ny * tubeThickness * 0.4f + tan[1] * len * 0.5f,
                    scratchPaint
                )
            }
        }
    }

    private fun renderVfxOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        vfx: NeonAssetSpec,
        theta: Float,
        phase: Float,
        density: Float,
        glowIntensity: Float
    ) {
        val minDim = min(width, height)
        val primary = vfx.primaryColor.toArgb()
        val secondary = vfx.secondaryColor.toArgb()
        val core = vfx.coreColor.toArgb()
        val glow = glowIntensity.coerceIn(0.4f, 1.8f)

        if (NeonProVfxShaders.renderProVfx(canvas, width, height, vfx, theta, phase, density, glowIntensity)) {
            return
        }

        when (vfx.archetype) {
            EffectArchetype.VFX_GOLDEN_PARTICLES -> {
                val isFire = vfx.id == NeonEffectId.VFX_09_FIRE_EMBERS
                val baseGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.96f, minDim * 0.65f,
                        intArrayOf(
                            adjustAlpha(secondary, (0.55f * glow).coerceIn(0f, 0.85f)),
                            adjustAlpha(primary, (0.22f * glow).coerceIn(0f, 0.6f)),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, baseGlowPaint)

                val count = (75 * density).toInt().coerceIn(20, 140)
                val pPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val h1 = hashFloat(i * 13 + 7)
                    val h2 = hashFloat(i * 29 + 3)
                    val speedMul = 1 + (i % 2)
                    val yProg = (h1 + phase * speedMul) % 1f
                    val y = height * (1.05f - yProg * 1.15f)
                    val xDrift = sin(theta * (1 + i % 2) + h2 * TWO_PI) * width * 0.04f
                    val x = width * (0.04f + 0.92f * h2) + xDrift
                    val fade = sin(yProg * PI.toFloat()).coerceIn(0f, 1f)
                    val r = minDim * (0.0035f + 0.014f * (hashFloat(i * 41) * hashFloat(i * 41)))

                    if (isFire && i % 2 == 0) {
                        pPaint.shader = null
                        pPaint.color = adjustAlpha(if (i % 4 == 0) core else primary, fade * 0.9f)
                        pPaint.strokeWidth = max(1.5f, r * 0.7f)
                        pPaint.strokeCap = Paint.Cap.ROUND
                        canvas.drawLine(x, y, x - xDrift * 0.35f, y + r * 4.5f, pPaint)
                    } else {
                        pPaint.shader = RadialGradient(
                            x, y, r * 2.2f,
                            intArrayOf(
                                adjustAlpha(core, fade * 0.95f),
                                adjustAlpha(primary, fade * 0.55f),
                                Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.45f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        canvas.drawCircle(x, y, r * 2.2f, pPaint)
                    }
                }
            }

            EffectArchetype.VFX_HEX_BOKEH_RAYS -> {
                val rayShift = 0.08f * sin(theta)
                val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, width * (0.75f + rayShift), height * 0.85f,
                        intArrayOf(
                            adjustAlpha(primary, 0.34f * glow.coerceAtMost(1.3f)),
                            adjustAlpha(secondary, 0.14f),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.42f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, rayPaint)

                val count = (46 * density).toInt().coerceIn(14, 90)
                val bPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (i in 0 until count) {
                    val hx = hashFloat(i * 19 + 5)
                    val hy = hashFloat(i * 31 + 11)
                    val hr = hashFloat(i * 47 + 2)
                    val x = width * hx + cos(theta + hx * TWO_PI) * width * 0.05f
                    val y = height * hy + sin(theta + hy * TWO_PI) * height * 0.05f
                    val radius = minDim * (0.018f + 0.075f * hr)
                    val breathe = 0.35f + 0.65f * (0.5f + 0.5f * sin(theta * 2f + hr * TWO_PI))
                    val alpha = (0.14f + 0.24f * (1f - hr)) * breathe * glow.coerceAtMost(1.3f)

                    if (i % 3 == 0) {
                        val hexPath = createPolygonPath(x, y, radius, 6, (i * 0.4f))
                        bPaint.color = adjustAlpha(primary, alpha.coerceIn(0f, 0.65f))
                        canvas.drawPath(hexPath, bPaint)
                        ringPaint.strokeWidth = max(1f, radius * 0.07f)
                        ringPaint.color = adjustAlpha(core, (alpha * 1.25f).coerceIn(0f, 0.55f))
                        canvas.drawPath(hexPath, ringPaint)
                    } else {
                        bPaint.color = adjustAlpha(if (i % 5 == 0) core else primary, alpha.coerceIn(0f, 0.65f))
                        canvas.drawCircle(x, y, radius, bPaint)
                        ringPaint.strokeWidth = max(1f, radius * 0.08f)
                        ringPaint.color = adjustAlpha(core, (alpha * 1.35f).coerceIn(0f, 0.55f))
                        canvas.drawCircle(x, y, radius * 0.94f, ringPaint)
                    }
                }
            }

            EffectArchetype.VFX_RING_BOKEH_FOG -> {
                val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.5f, minDim * 0.65f,
                        intArrayOf(adjustAlpha(secondary, 0.24f), Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width, height, mistPaint)

                val count = (44 * density).toInt().coerceIn(14, 90)
                val bPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (i in 0 until count) {
                    val hx = hashFloat(i * 19 + 5)
                    val hy = hashFloat(i * 31 + 11)
                    val hr = hashFloat(i * 47 + 2)
                    val x = width * hx + cos(theta + hx * TWO_PI) * width * 0.05f
                    val y = height * hy + sin(theta + hy * TWO_PI) * height * 0.05f
                    val radius = minDim * (0.018f + 0.082f * hr)
                    val breathe = 0.35f + 0.65f * (0.5f + 0.5f * sin(theta * 2f + hr * TWO_PI))
                    val alpha = (0.12f + 0.22f * (1f - hr)) * breathe * glow.coerceAtMost(1.3f)

                    bPaint.color = adjustAlpha(primary, (alpha * 0.65f).coerceIn(0f, 0.50f))
                    canvas.drawCircle(x, y, radius, bPaint)

                    // Concentric optical aberration rings (VFX_06 reference)
                    ringPaint.strokeWidth = max(1f, radius * 0.09f)
                    ringPaint.color = adjustAlpha(core, (alpha * 1.4f).coerceIn(0f, 0.60f))
                    canvas.drawCircle(x, y, radius * 0.92f, ringPaint)
                    ringPaint.strokeWidth = max(0.8f, radius * 0.06f)
                    ringPaint.color = adjustAlpha(primary, (alpha * 1.1f).coerceIn(0f, 0.45f))
                    canvas.drawCircle(x, y, radius * 0.64f, ringPaint)
                }
            }

            EffectArchetype.VFX_DARK_DUST_MIST -> {
                val mistRadius = minDim * (0.48f + 0.04f * sin(theta))
                val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        width * 0.5f, height * 0.5f, mistRadius,
                        intArrayOf(adjustAlpha(secondary, 0.24f), Color.TRANSPARENT),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(width * 0.5f, height * 0.5f, mistRadius, mistPaint)

                val count = (64 * density).toInt().coerceIn(20, 120)
                val dPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val hx = hashFloat(i * 17 + 1)
                    val hy = hashFloat(i * 43 + 9)
                    val hs = hashFloat(i * 53 + 4)
                    val x = width * (0.08f + 0.84f * hx) + sin(theta + hy * TWO_PI) * width * 0.04f
                    val y = height * (0.08f + 0.84f * hy) + cos(theta + hx * TWO_PI) * height * 0.04f
                    val isSharp = i % 3 == 0
                    val r = if (isSharp) minDim * 0.004f else minDim * (0.012f + 0.032f * hs)
                    val alpha = if (isSharp) 0.72f else (0.12f + 0.16f * (1f - hs))
                    dPaint.color = adjustAlpha(if (isSharp) core else primary, alpha)
                    dPaint.maskFilter = if (!isSharp) BlurMaskFilter(max(1f, r * 0.35f), BlurMaskFilter.Blur.NORMAL) else null
                    canvas.drawCircle(x, y, r, dPaint)
                }
            }

            EffectArchetype.VFX_SMOKE_EMBERS, EffectArchetype.VFX_STORM_EMBERS_RAYS -> {
                val isStorm = vfx.archetype == EffectArchetype.VFX_STORM_EMBERS_RAYS
                if (isStorm) {
                    val shaftShift = 0.06f * sin(theta)
                    val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            width * (0.85f + shaftShift), 0f,
                            width * 0.2f, height * 0.85f,
                            intArrayOf(
                                adjustAlpha(Color.rgb(180, 205, 235), 0.24f * glow.coerceAtMost(1.3f)),
                                adjustAlpha(secondary, 0.08f),
                                Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.45f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, width, height, rayPaint)
                }

                val smokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until 16) {
                    val hx = hashFloat(i * 23 + 3)
                    val cx = width * hx + sin(theta + hx * TWO_PI) * width * 0.08f
                    val cyBase = if (isStorm) 0.35f + 0.45f * hashFloat(i * 11) else 0.74f + 0.20f * hashFloat(i * 11)
                    val cy = height * cyBase + cos(theta * 2f + hx * TWO_PI) * height * 0.04f
                    val sr = minDim * (0.22f + 0.14f * hashFloat(i * 37))
                    smokePaint.shader = RadialGradient(
                        cx, cy, sr,
                        intArrayOf(
                            adjustAlpha(if (isStorm) Color.rgb(95, 115, 138) else Color.rgb(125, 130, 140), 0.24f),
                            adjustAlpha(primary, 0.08f * glow.coerceAtMost(1.3f)),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(cx, cy, sr, smokePaint)
                }

                val count = (52 * density).toInt().coerceIn(16, 100)
                val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val hx = hashFloat(i * 29 + 1)
                    val hy = hashFloat(i * 61 + 7)
                    val yProg = (hy + phase * (1 + i % 2)) % 1f
                    val x = width * hx + sin(theta * 2f + hy * TWO_PI) * width * 0.06f
                    val y = height * (1.02f - yProg * 1.08f)
                    val fade = sin(yProg * PI.toFloat()).coerceIn(0f, 1f)
                    val r = minDim * (0.004f + 0.008f * hashFloat(i * 13))
                    emberPaint.color = adjustAlpha(if (i % 4 == 0) core else primary, fade * 0.92f)
                    emberPaint.maskFilter = BlurMaskFilter(max(1f, r * 0.8f), BlurMaskFilter.Blur.NORMAL)
                    canvas.drawOval(RectF(x - r * 0.8f, y - r * 1.5f, x + r * 0.8f, y + r * 1.5f), emberPaint)
                }
            }

            EffectArchetype.VFX_GOLDEN_SILK -> {
                val silkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                for (w in 0 until 6) {
                    val wavePath = Path()
                    val baseY = height * (0.22f + 0.11f * w)
                    val amp = height * (0.06f + 0.02f * (w % 3))
                    for (step in 0..48) {
                        val u = step / 48f
                        val x = u * width
                        val y = baseY + sin(u * TWO_PI * 1.5f + theta + w * 0.9f) * amp +
                            cos(u * TWO_PI * 2.5f - theta * 2f) * amp * 0.4f
                        if (step == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                    }
                    silkPaint.strokeWidth = minDim * (0.045f + 0.015f * (w % 2))
                    silkPaint.color = adjustAlpha(if (w % 2 == 0) primary else secondary, 0.16f * glow.coerceAtMost(1.3f))
                    silkPaint.maskFilter = BlurMaskFilter(minDim * 0.03f, BlurMaskFilter.Blur.NORMAL)
                    canvas.drawPath(wavePath, silkPaint)
                }

                val count = (45 * density).toInt().coerceIn(14, 90)
                val dustPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val hx = hashFloat(i * 19)
                    val hy = hashFloat(i * 37)
                    val x = width * hx + cos(theta + hy * TWO_PI) * width * 0.03f
                    val y = height * hy + sin(theta + hx * TWO_PI) * height * 0.03f
                    val r = minDim * (0.0025f + 0.006f * hashFloat(i * 53))
                    dustPaint.color = adjustAlpha(core, 0.65f * (0.5f + 0.5f * sin(theta * 2f + hx * TWO_PI)))
                    canvas.drawCircle(x, y, r, dustPaint)
                }
            }

            EffectArchetype.VFX_RISING_SMOKE_HALF_FADE -> {
                // Slow, silky 3D volumetric smoke plumes rising from bottom edge (1.06h) up to ~0.46h and fading smoothly to 0 alpha
                val count = (26 * density).toInt().coerceIn(14, 48)
                val plumePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val hx = hashFloat(i * 19 + 3)
                    val hPhase = hashFloat(i * 37 + 11)
                    val prog = (hPhase + phase) % 1f
                    // y moves smoothly from bottom (1.08h) to just below half-screen (0.46h)
                    val cy = height * (1.08f - prog * 0.62f)
                    val cx = width * (0.06f + 0.88f * hx) + sin(theta + hx * TWO_PI + prog * 3.1f) * width * 0.065f
                    val radius = minDim * (0.14f + 0.20f * prog) * (0.75f + 0.45f * hashFloat(i * 53))
                    // Smooth fade in at bottom, peak around prog=0.28, complete dissolve to 0 before prog=0.95 (half screen)
                    val fadeIn = (prog / 0.18f).coerceIn(0f, 1f)
                    val fadeOut = (1f - ((prog - 0.30f) / 0.68f).coerceIn(0f, 1f)).pow(1.6f)
                    val alpha = 0.22f * fadeIn * fadeOut * glow.coerceAtMost(1.35f)
                    if (alpha > 0.004f) {
                        val plumeTint = if (i % 3 == 0) core else if (i % 2 == 0) primary else secondary
                        plumePaint.shader = RadialGradient(
                            cx, cy, radius,
                            intArrayOf(
                                adjustAlpha(plumeTint, alpha),
                                adjustAlpha(secondary, alpha * 0.45f),
                                Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.52f, 1f),
                            Shader.TileMode.CLAMP
                        )
                        canvas.drawCircle(cx, cy, radius, plumePaint)
                    }
                }
                // Subtle rising wisp ribbons confined to lower half
                val wispPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                for (w in 0 until 5) {
                    val path = Path()
                    val baseX = width * (0.12f + 0.19f * w)
                    for (s in 0..24) {
                        val u = s / 24f // 0 = bottom, 1 = mid screen
                        val y = height * (1.02f - u * 0.52f)
                        val x = baseX + sin(u * PI.toFloat() * 2.2f + theta + w) * width * 0.05f
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    wispPaint.strokeWidth = minDim * 0.04f
                    wispPaint.shader = LinearGradient(
                        0f, height, 0f, height * 0.48f,
                        intArrayOf(
                            adjustAlpha(primary, 0.14f * glow.coerceAtMost(1.2f)),
                            adjustAlpha(secondary, 0.06f),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    wispPaint.maskFilter = BlurMaskFilter(minDim * 0.035f, BlurMaskFilter.Blur.NORMAL)
                    canvas.drawPath(path, wispPaint)
                }
            }

            EffectArchetype.VFX_GLITTER_SHIMMER_FADE -> {
                // Shimmering 4-point diamond star sparkles & soft micro-bokeh drifting and fading cleanly to 0 alpha
                val count = (72 * density).toInt().coerceIn(24, 130)
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val hx = hashFloat(i * 17 + 5)
                    val hy = hashFloat(i * 41 + 9)
                    val lifeOffset = hashFloat(i * 67 + 3)
                    val life = (lifeOffset + phase * (1 + i % 2)) % 1f
                    // Smooth bell-curve fade so every particle smoothly appears and disappears
                    val fade = sin(life * PI.toFloat()).pow(1.4f).coerceIn(0f, 1f)
                    val twinkle = (0.45f + 0.55f * (0.5f + 0.5f * sin(theta * 4f + i * 2.1f)))
                    val alpha = fade * twinkle * glow.coerceAtMost(1.3f)

                    val x = width * (0.06f + 0.88f * hx) + sin(theta + hy * TWO_PI) * width * 0.035f
                    val y = height * (0.08f + 0.84f * hy) - life * height * 0.06f
                    val baseR = minDim * (0.004f + 0.015f * hashFloat(i * 23))

                    dotPaint.shader = RadialGradient(
                        x, y, baseR * 2.4f,
                        intArrayOf(
                            adjustAlpha(core, (alpha * 0.95f).coerceIn(0f, 1f)),
                            adjustAlpha(if (i % 2 == 0) primary else secondary, (alpha * 0.45f).coerceIn(0f, 1f)),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.4f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(x, y, baseR * 2.4f, dotPaint)

                    if (i % 3 == 0 && alpha > 0.18f) {
                        val arm = baseR * 2.8f
                        starPaint.strokeWidth = max(1f, baseR * 0.36f)
                        starPaint.color = adjustAlpha(core, (alpha * 0.88f).coerceIn(0f, 0.95f))
                        canvas.drawLine(x - arm, y, x + arm, y, starPaint)
                        canvas.drawLine(x, y - arm, x, y + arm, starPaint)
                    }
                }
            }

            EffectArchetype.VFX_GOD_RAYS_MIST -> {
                // Volumetric diagonal 3D god-ray beams + soft floating mist & dust
                val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (r in 0 until 5) {
                    val shift = sin(theta + r * 0.8f) * width * 0.05f
                    val startX = width * (0.05f + 0.20f * r) + shift
                    val rayPath = Path().apply {
                        moveTo(startX, 0f)
                        lineTo(startX + width * 0.13f, 0f)
                        lineTo(startX + width * 0.42f, height)
                        lineTo(startX + width * 0.14f, height)
                        close()
                    }
                    rayPaint.shader = LinearGradient(
                        startX, 0f, startX + width * 0.28f, height * 0.92f,
                        intArrayOf(
                            adjustAlpha(if (r % 2 == 0) primary else core, 0.16f * glow.coerceAtMost(1.25f)),
                            adjustAlpha(secondary, 0.08f * glow.coerceAtMost(1.25f)),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawPath(rayPath, rayPaint)
                }

                // Drifting illuminated dust motes inside the shafts
                val count = (48 * density).toInt().coerceIn(16, 90)
                val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until count) {
                    val hx = hashFloat(i * 31 + 7)
                    val hy = hashFloat(i * 47 + 13)
                    val x = width * (0.08f + 0.84f * hx) + cos(theta + hy * TWO_PI) * width * 0.04f
                    val y = height * (0.08f + 0.84f * hy) + sin(theta + hx * TWO_PI) * height * 0.04f
                    val rad = minDim * (0.003f + 0.008f * hashFloat(i * 19))
                    val alpha = 0.25f + 0.55f * (0.5f + 0.5f * sin(theta * 2f + hx * TWO_PI))
                    mPaint.color = adjustAlpha(if (i % 3 == 0) core else primary, alpha.coerceIn(0f, 0.9f))
                    canvas.drawCircle(x, y, rad, mPaint)
                }
            }

            EffectArchetype.VFX_CYBER_ORBITAL_ARCS -> {
                // 3D perspective orbital light rings + plasma arc filaments in raw alpha
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                }
                val cx = width * 0.5f
                val cy = height * 0.5f
                for (ring in 0 until 3) {
                    canvas.save()
                    val rotDeg = (ring * 52f) + (phase * 360f * if (ring % 2 == 0) 1f else -1f)
                    canvas.rotate(rotDeg, cx, cy)
                    val rx = minDim * (0.28f + 0.07f * ring)
                    val ry = rx * (0.34f + 0.08f * sin(theta + ring))
                    val oval = RectF(cx - rx, cy - ry, cx + rx, cy + ry)

                    ringPaint.strokeWidth = max(2f, minDim * 0.012f)
                    ringPaint.color = adjustAlpha(if (ring % 2 == 0) primary else secondary, 0.52f * glow.coerceAtMost(1.3f))
                    ringPaint.maskFilter = BlurMaskFilter(minDim * 0.012f, BlurMaskFilter.Blur.NORMAL)
                    canvas.drawArc(oval, 0f, 240f, false, ringPaint)

                    ringPaint.strokeWidth = max(1f, minDim * 0.004f)
                    ringPaint.color = adjustAlpha(core, 0.85f)
                    ringPaint.maskFilter = null
                    canvas.drawArc(oval, 20f, 180f, false, ringPaint)
                    canvas.restore()
                }
            }

            else -> Unit
        }
    }

    private fun createPolygonPath(cx: Float, cy: Float, radius: Float, sides: Int, angleOffset: Float): Path {
        val path = Path()
        for (i in 0 until sides) {
            val angle = angleOffset + (i.toFloat() / sides) * TWO_PI
            val px = cx + cos(angle) * radius
            val py = cy + sin(angle) * radius
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
    }

    private fun hashFloat(seed: Int): Float {
        var n = seed * 374761393 + 668265263
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE.toFloat()
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        val a = (Color.alpha(c1) * (1f - t) + Color.alpha(c2) * t).toInt()
        val r = (Color.red(c1) * (1f - t) + Color.red(c2) * t).toInt()
        val g = (Color.green(c1) * (1f - t) + Color.green(c2) * t).toInt()
        val b = (Color.blue(c1) * (1f - t) + Color.blue(c2) * t).toInt()
        return Color.argb(a, r, g, b)
    }
}

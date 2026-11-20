package com.example.engine

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import com.example.model.EffectArchetype
import com.example.model.NeonAssetSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Procedural 3D shaders for the 50 Famous Pro Effects (PRO-01 to PRO-50).
 * Generates 100% pure-alpha RGBA geometry & volumetric shaders (no opaque backdrops).
 */
internal object NeonProVfxShaders {
    private const val TWO_PI = (2.0 * PI).toFloat()

    fun renderProFrameExtraGeometry(
        canvas: Canvas,
        frameRect: RectF,
        cornerRadius: Float,
        tubeThickness: Float,
        spec: NeonAssetSpec,
        theta: Float,
        glowScale: Float
    ) {
        val primary = spec.primaryColor.toArgb()
        val secondary = spec.secondaryColor.toArgb()
        val core = spec.coreColor.toArgb()

        when (spec.archetype) {
            EffectArchetype.FRAME_DOUBLE_CHAMBER_LUXURY_3D -> {
                val outerInset = -tubeThickness * 1.15f
                val outerRect = RectF(frameRect).apply { inset(outerInset, outerInset) }
                val outerR = cornerRadius - outerInset
                val dualPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(1.5f, tubeThickness * 0.24f)
                    shader = LinearGradient(
                        outerRect.left, outerRect.top, outerRect.right, outerRect.bottom,
                        intArrayOf(
                            adjustAlpha(core, 0.88f),
                            adjustAlpha(primary, 0.72f),
                            adjustAlpha(secondary, 0.55f),
                            adjustAlpha(core, 0.88f)
                        ),
                        floatArrayOf(0f, 0.35f, 0.7f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(outerRect, outerR, outerR, dualPaint)

                val innerInset = tubeThickness * 1.05f
                val innerRect = RectF(frameRect).apply { inset(innerInset, innerInset) }
                val innerR = max(4f, cornerRadius - innerInset)
                val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(1f, tubeThickness * 0.14f)
                    color = adjustAlpha(secondary, (0.68f * glowScale).coerceIn(0f, 0.9f))
                }
                canvas.drawRoundRect(innerRect, innerR, innerR, innerPaint)
            }

            EffectArchetype.FRAME_LASER_GRID_HUD_3D -> {
                val bracketLen = min(frameRect.width(), frameRect.height()) * 0.11f
                val pad = tubeThickness * 1.25f
                val r = RectF(frameRect).apply { inset(-pad, -pad) }
                val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.SQUARE
                    strokeWidth = max(2f, tubeThickness * 0.28f)
                    color = adjustAlpha(primary, (0.85f * glowScale).coerceIn(0f, 0.96f))
                }
                // 4 Corner aerospace brackets
                val corners = listOf(
                    floatArrayOf(r.left, r.top, 1f, 1f),
                    floatArrayOf(r.right, r.top, -1f, 1f),
                    floatArrayOf(r.right, r.bottom, -1f, -1f),
                    floatArrayOf(r.left, r.bottom, 1f, -1f)
                )
                corners.forEach { (cx, cy, sx, sy) ->
                    canvas.drawLine(cx, cy, cx + sx * bracketLen, cy, bracketPaint)
                    canvas.drawLine(cx, cy, cx, cy + sy * bracketLen, bracketPaint)
                }
                // Subtle HUD ticks on side rails
                val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = max(1f, tubeThickness * 0.1f)
                    color = adjustAlpha(secondary, 0.65f)
                }
                for (i in 0 until 6) {
                    val y = frameRect.top + frameRect.height() * (0.28f + 0.088f * i)
                    val tickW = tubeThickness * if (i % 2 == 0) 0.9f else 0.45f
                    canvas.drawLine(r.left - tickW, y, r.left, y, tickPaint)
                    canvas.drawLine(r.right, y, r.right + tickW, y, tickPaint)
                }
            }

            EffectArchetype.FRAME_CYBER_WING_EMBLEM_3D -> {
                val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = max(1.8f, tubeThickness * 0.30f)
                }
                val glowWingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = max(3f, tubeThickness * 0.65f)
                    color = adjustAlpha(primary, 0.34f * glowScale.coerceAtMost(1.3f))
                    maskFilter = BlurMaskFilter(max(2f, tubeThickness * 0.6f), BlurMaskFilter.Blur.NORMAL)
                }
                val pulse = 0.7f + 0.3f * sin(theta * 2f)
                for (feather in 0 until 3) {
                    val spread = tubeThickness * (1.2f + feather * 0.85f)
                    val wingH = frameRect.height() * (0.20f - feather * 0.045f)
                    val cy = frameRect.centerY() - frameRect.height() * 0.04f + feather * tubeThickness * 1.1f
                    // Left & Right aerodynamic wing arcs
                    val leftWing = Path().apply {
                        moveTo(frameRect.left - tubeThickness * 0.8f, cy - wingH * 0.25f)
                        quadTo(frameRect.left - spread * 2.2f, cy - wingH * 0.75f, frameRect.left - spread * 2.7f, cy - wingH)
                    }
                    val rightWing = Path().apply {
                        moveTo(frameRect.right + tubeThickness * 0.8f, cy - wingH * 0.25f)
                        quadTo(frameRect.right + spread * 2.2f, cy - wingH * 0.75f, frameRect.right + spread * 2.7f, cy - wingH)
                    }
                    wingPaint.color = adjustAlpha(if (feather == 0) core else primary, (0.88f - feather * 0.18f) * pulse)
                    canvas.drawPath(leftWing, glowWingPaint)
                    canvas.drawPath(rightWing, glowWingPaint)
                    canvas.drawPath(leftWing, wingPaint)
                    canvas.drawPath(rightWing, wingPaint)
                }
            }

            else -> Unit
        }
    }

    fun renderProVfx(
        canvas: Canvas,
        width: Float,
        height: Float,
        vfx: NeonAssetSpec,
        theta: Float,
        phase: Float,
        density: Float,
        glowIntensity: Float
    ): Boolean {
        val minDim = min(width, height)
        val primary = vfx.primaryColor.toArgb()
        val secondary = vfx.secondaryColor.toArgb()
        val core = vfx.coreColor.toArgb()
        val glow = glowIntensity.coerceIn(0.4f, 1.8f)

        when (vfx.archetype) {
            EffectArchetype.VFX_STARFIELD_HYPERSPACE_3D -> {
                val cx = width * 0.5f
                val cy = height * 0.5f
                val count = (76 * density).toInt().coerceIn(28, 130)
                val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
                for (i in 0 until count) {
                    val angle = hashFloat(i * 19 + 3) * TWO_PI
                    val zProg = (hashFloat(i * 41 + 9) + phase * (1 + i % 2)) % 1f
                    val dist = zProg.pow(1.6f) * minDim * 0.62f
                    val tailDist = max(4f, dist * 0.74f)
                    val x1 = cx + cos(angle) * tailDist
                    val y1 = cy + sin(angle) * tailDist
                    val x2 = cx + cos(angle) * dist
                    val y2 = cy + sin(angle) * dist
                    val fade = sin(zProg * PI.toFloat()).coerceIn(0f, 1f) * glow.coerceAtMost(1.3f)
                    streakPaint.strokeWidth = max(1f, minDim * (0.0022f + 0.0045f * zProg))
                    streakPaint.color = adjustAlpha(if (i % 3 == 0) core else primary, fade.coerceIn(0f, 0.95f))
                    canvas.drawLine(x1, y1, x2, y2, streakPaint)
                }
                return true
            }

            EffectArchetype.VFX_SACRED_HUD_GEOMETRY_3D -> {
                val cx = width * 0.5f
                val cy = height * 0.5f
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (r in 0 until 4) {
                    val rad = minDim * (0.14f + 0.085f * r)
                    val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
                    val rot = phase * 360f * if (r % 2 == 0) 1f else -1f
                    paint.strokeWidth = max(1.2f, minDim * (0.005f - r * 0.0007f))
                    paint.color = adjustAlpha(if (r % 2 == 0) primary else secondary, 0.78f * glow.coerceAtMost(1.25f))
                    canvas.save()
                    canvas.rotate(rot + r * 28f, cx, cy)
                    canvas.drawArc(oval, 0f, 115f, false, paint)
                    canvas.drawArc(oval, 180f, 115f, false, paint)
                    // Precision calibration ticks on middle ring
                    if (r == 2) {
                        paint.color = adjustAlpha(core, 0.65f)
                        for (t in 0 until 12) {
                            val a = (t / 12f) * TWO_PI
                            canvas.drawLine(
                                cx + cos(a) * rad * 0.94f,
                                cy + sin(a) * rad * 0.94f,
                                cx + cos(a) * rad * 1.05f,
                                cy + sin(a) * rad * 1.05f,
                                paint
                            )
                        }
                    }
                    canvas.restore()
                }
                return true
            }

            EffectArchetype.VFX_PETALS_LEAVES_SAKURA_3D -> {
                val count = (42 * density).toInt().coerceIn(16, 80)
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (i in 0 until count) {
                    val hx = hashFloat(i * 23 + 7)
                    val hLife = hashFloat(i * 53 + 11)
                    val prog = (hLife + phase) % 1f
                    val y = height * (-0.06f + prog * 1.12f)
                    val x = width * (0.06f + 0.88f * hx) + sin(theta * 2f + hx * TWO_PI + prog * 4f) * width * 0.07f
                    val sz = minDim * (0.018f + 0.024f * hashFloat(i * 37))
                    val rotDeg = (prog * 360f * (if (i % 2 == 0) 1f else -1f)) + i * 31f
                    val fade = sin(prog * PI.toFloat()).coerceIn(0f, 1f)

                    canvas.save()
                    canvas.translate(x, y)
                    canvas.rotate(rotDeg)
                    canvas.scale(1f, 0.55f + 0.45f * cos(theta * 3f + i))
                    val petalPath = Path().apply {
                        moveTo(0f, -sz)
                        cubicTo(sz * 0.85f, -sz * 0.4f, sz * 0.75f, sz * 0.55f, 0f, sz)
                        cubicTo(-sz * 0.75f, sz * 0.55f, -sz * 0.85f, -sz * 0.4f, 0f, -sz)
                        close()
                    }
                    fillPaint.color = adjustAlpha(if (i % 2 == 0) primary else secondary, 0.72f * fade)
                    canvas.drawPath(petalPath, fillPaint)
                    rimPaint.strokeWidth = max(1f, sz * 0.12f)
                    rimPaint.color = adjustAlpha(core, 0.82f * fade)
                    canvas.drawPath(petalPath, rimPaint)
                    canvas.restore()
                }
                return true
            }

            EffectArchetype.VFX_ANAMORPHIC_LENS_FLARE_3D -> {
                val cx = width * (0.5f + 0.14f * sin(theta))
                val cy = height * (0.48f + 0.04f * cos(theta * 2f))
                val breathe = 0.65f + 0.35f * sin(theta * 2f)
                val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val bladeW = width * 0.44f
                val bladeH = minDim * 0.032f * breathe
                bladePaint.shader = LinearGradient(
                    cx - bladeW, cy, cx + bladeW, cy,
                    intArrayOf(Color.TRANSPARENT, adjustAlpha(primary, 0.75f), core, adjustAlpha(primary, 0.75f), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.28f, 0.5f, 0.72f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawOval(RectF(cx - bladeW, cy - bladeH, cx + bladeW, cy + bladeH), bladePaint)

                // Optical lens ghost discs
                val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (g in 1..5) {
                    val t = (g - 3) * 0.22f
                    val gx = width * 0.5f + (cx - width * 0.5f) * t * 2.2f
                    val gy = height * 0.5f + (cy - height * 0.5f) * t * 2.2f
                    val gr = minDim * (0.025f + 0.018f * g)
                    ghostPaint.strokeWidth = max(1.2f, gr * 0.12f)
                    ghostPaint.color = adjustAlpha(if (g % 2 == 0) secondary else primary, 0.42f * glow.coerceAtMost(1.3f))
                    canvas.drawCircle(gx, gy, gr, ghostPaint)
                }
                return true
            }

            EffectArchetype.VFX_DIGITAL_MATRIX_RUNES_3D -> {
                val cols = (20 * density).toInt().coerceIn(12, 34)
                val colPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
                val headPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (c in 0 until cols) {
                    val hx = 0.06f + 0.88f * (c.toFloat() / cols.toFloat())
                    val x = width * hx
                    val prog = (hashFloat(c * 29 + 7) + phase * (1 + c % 2)) % 1f
                    val headY = height * (0.05f + 0.90f * prog)
                    val tailLen = height * (0.14f + 0.16f * hashFloat(c * 43))
                    val tailY = max(height * 0.04f, headY - tailLen)
                    val fade = sin(prog * PI.toFloat()).coerceIn(0f, 1f)
                    val tint = if (c % 2 == 0) primary else secondary
                    colPaint.strokeWidth = max(1.5f, minDim * 0.0055f)
                    colPaint.shader = LinearGradient(
                        x, tailY, x, headY,
                        intArrayOf(Color.TRANSPARENT, adjustAlpha(tint, 0.65f * fade), adjustAlpha(core, 0.92f * fade)),
                        floatArrayOf(0f, 0.65f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawLine(x, tailY, x, headY, colPaint)
                    headPaint.color = adjustAlpha(core, 0.92f * fade)
                    canvas.drawCircle(x, headY, max(2f, minDim * 0.006f), headPaint)
                }
                return true
            }

            EffectArchetype.VFX_LIGHTNING_TESLA_WEB_3D -> {
                val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = max(1.6f, minDim * 0.0055f)
                }
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = max(4f, minDim * 0.018f)
                    maskFilter = BlurMaskFilter(max(2f, minDim * 0.015f), BlurMaskFilter.Blur.NORMAL)
                }
                for (b in 0 until 5) {
                    val pulsePhase = (phase * 3f + hashFloat(b * 31)) % 1f
                    val intensity = sin(pulsePhase * PI.toFloat()).pow(2f)
                    if (intensity < 0.08f) continue
                    val path = Path()
                    val sx = width * (0.15f + 0.17f * b)
                    val sy = height * 0.10f
                    path.moveTo(sx, sy)
                    for (seg in 1..10) {
                        val u = seg / 10f
                        val jx = (hashFloat(seg * 17 + b * 53 + (phase * 12).toInt()) - 0.5f) * width * 0.12f
                        path.lineTo(sx + jx, sy + u * height * 0.80f)
                    }
                    glowPaint.color = adjustAlpha(if (b % 2 == 0) primary else secondary, 0.65f * intensity * glow.coerceAtMost(1.3f))
                    boltPaint.color = adjustAlpha(core, 0.95f * intensity)
                    canvas.drawPath(path, glowPaint)
                    canvas.drawPath(path, boltPaint)
                }
                return true
            }

            EffectArchetype.VFX_LIQUID_AURORA_NEBULA_3D -> {
                val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                for (band in 0 until 6) {
                    val path = Path()
                    val baseY = height * (0.20f + 0.11f * band)
                    val amp = height * 0.09f
                    for (s in 0..36) {
                        val u = s / 36f
                        val x = width * (0.04f + 0.92f * u)
                        val y = baseY + sin(u * TWO_PI * 1.3f + theta + band * 0.8f) * amp
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    ribbonPaint.strokeWidth = minDim * (0.055f + 0.015f * (band % 2))
                    ribbonPaint.color = adjustAlpha(if (band % 2 == 0) primary else secondary, 0.20f * glow.coerceAtMost(1.3f))
                    ribbonPaint.maskFilter = BlurMaskFilter(minDim * 0.032f, BlurMaskFilter.Blur.NORMAL)
                    canvas.drawPath(path, ribbonPaint)
                }
                return true
            }

            EffectArchetype.VFX_SNOW_CRYSTAL_BLIZZARD_3D -> {
                val count = (44 * density).toInt().coerceIn(18, 84)
                val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
                for (i in 0 until count) {
                    val hx = hashFloat(i * 19 + 5)
                    val prog = (hashFloat(i * 37 + 9) + phase) % 1f
                    val y = height * (0.04f + prog * 0.92f)
                    val x = width * (0.06f + 0.88f * hx) + sin(theta + hx * TWO_PI) * width * 0.04f
                    val r = minDim * (0.010f + 0.018f * hashFloat(i * 61))
                    val fade = sin(prog * PI.toFloat()).coerceIn(0f, 1f)
                    armPaint.strokeWidth = max(1f, r * 0.16f)
                    armPaint.color = adjustAlpha(if (i % 3 == 0) core else primary, 0.85f * fade)
                    for (arm in 0 until 3) {
                        val a = (arm / 3f) * PI.toFloat() + phase * TWO_PI
                        val dx = cos(a) * r
                        val dy = sin(a) * r
                        canvas.drawLine(x - dx, y - dy, x + dx, y + dy, armPaint)
                    }
                }
                return true
            }

            EffectArchetype.VFX_PRISM_SHARDS_CONFETTI_3D -> {
                val count = (46 * density).toInt().coerceIn(18, 90)
                val shardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (i in 0 until count) {
                    val hx = hashFloat(i * 23 + 11)
                    val prog = (hashFloat(i * 47 + 3) + phase) % 1f
                    val x = width * (0.06f + 0.88f * hx) + sin(theta * 2f + i) * width * 0.04f
                    val y = height * (0.05f + prog * 0.90f)
                    val sz = minDim * (0.014f + 0.022f * hashFloat(i * 17))
                    val fade = sin(prog * PI.toFloat()).coerceIn(0f, 1f)
                    canvas.save()
                    canvas.translate(x, y)
                    canvas.rotate(prog * 360f + i * 40f)
                    canvas.scale(1f, 0.45f + 0.55f * absCos(theta * 3f + i))
                    val poly = Path().apply {
                        moveTo(0f, -sz)
                        lineTo(sz * 0.72f, 0f)
                        lineTo(0f, sz)
                        lineTo(-sz * 0.72f, 0f)
                        close()
                    }
                    shardPaint.color = adjustAlpha(if (i % 2 == 0) primary else secondary, 0.76f * fade)
                    edgePaint.strokeWidth = max(1f, sz * 0.12f)
                    edgePaint.color = adjustAlpha(core, 0.92f * fade)
                    canvas.drawPath(poly, shardPaint)
                    canvas.drawPath(poly, edgePaint)
                    canvas.restore()
                }
                return true
            }

            EffectArchetype.VFX_CAUSTIC_WATER_RIPPLES_3D -> {
                val cx = width * 0.5f
                val cy = height * 0.5f
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
                for (w in 0 until 5) {
                    val waveProg = (phase + w / 5f) % 1f
                    val rx = minDim * (0.08f + 0.36f * waveProg)
                    val ry = rx * 0.62f
                    val fade = (1f - waveProg).pow(1.3f) * sin(waveProg * PI.toFloat()).coerceIn(0f, 1f)
                    ringPaint.strokeWidth = max(1.5f, minDim * 0.010f * (1f - waveProg * 0.5f))
                    ringPaint.color = adjustAlpha(if (w % 2 == 0) primary else secondary, 0.75f * fade * glow.coerceAtMost(1.3f))
                    canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), ringPaint)
                }
                return true
            }

            else -> return false
        }
    }

    private fun absCos(x: Float): Float = kotlin.math.abs(cos(x))

    private fun hashFloat(seed: Int): Float {
        var n = seed * 374761393 + 668265263
        n = (n xor (n ushr 13)) * 1274126177
        return ((n xor (n ushr 16)) and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE.toFloat()
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

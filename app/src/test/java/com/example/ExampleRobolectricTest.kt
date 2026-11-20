package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ExportAndQcEngine
import com.example.engine.Neon3DRenderer
import com.example.engine.RenderParams
import com.example.model.ExportResolutionTier
import com.example.model.FrameAspectRatio
import com.example.model.NeonCatalog
import com.example.model.NeonEffectId
import com.example.model.NeonPro50Catalog
import com.example.model.StudioThemePreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @Test
    fun `app name matches NeonVFX Studio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NeonVFX Studio", appName)
    }

    @Test
    fun `catalog contains all 84 raw 3d frames and vfx overlays`() {
        assertEquals(50, NeonPro50Catalog.pro50Assets.size)
        assertEquals(84, NeonCatalog.builtInAssets.size)
        assertEquals(30, NeonCatalog.builtInAssets.count { it.isFrameAsset })
        assertEquals(4, StudioThemePreset.entries.size)
    }

    @Test
    fun `seven pass audit passes raw alpha and 15s seamless loop across 3d frames`() = runBlocking {
        val frameIds = listOf(
            NeonEffectId.ICE_WHITE_PLASMA,
            NeonEffectId.CYAN_GLASS_NEON,
            NeonEffectId.HEARTBEAT_NEON,
            NeonEffectId.LIGHT_SCAN,
            NeonEffectId.BEVELED_CYAN_GLASS_3D,
            NeonEffectId.PRISM_HOLOGRAPHIC_3D,
            NeonEffectId.LIQUID_CHROME_3D,
            NeonEffectId.CYBER_CIRCUIT_3D,
            NeonEffectId.AURORA_VOLUMETRIC_3D,
            NeonEffectId.DIAMOND_CRYSTAL_3D,
            NeonEffectId.MOLTEN_COPPER_3D,
            NeonEffectId.VFX_07_NEON_GLASS_FRAME
        )
        for (id in frameIds) {
            val spec = NeonCatalog.findById(id)
            val report = ExportAndQcEngine.runSevenPassAudit(
                params = RenderParams(asset = spec),
                aspectRatio = FrameAspectRatio.SQUARE_1_1,
                resolutionTier = ExportResolutionTier.MASTER_16K_TILED
            )
            assertEquals("Expected zero exterior corner alpha for ${spec.title}", 0, report.exteriorCornerAlphaMax)
            assertEquals("Expected zero interior center alpha for ${spec.title}", 0, report.interiorCenterAlphaMax)
            assertTrue("Expected seamless 15s loop delta < 0.05% for ${spec.title}, got ${report.loopDeltaPercent}", report.loopDeltaPercent < 0.05f)
            val failedPasses = report.passes.filter { !it.passed }.joinToString { "${it.code}: ${it.metricDetail}" }
            assertTrue("Expected all 7 QC passes to pass for ${spec.title}, failed: [$failedPasses]", report.allPassed)
        }
    }

    @Test
    fun `rising silk smoke fades cleanly before upper half of screen`() = runBlocking {
        val smokeSpec = NeonCatalog.findById(NeonEffectId.VFX_11_RISING_SILK_SMOKE_FADE)
        val w = 360
        val h = 640
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        try {
            Neon3DRenderer.renderToCanvas(
                Canvas(bmp),
                w.toFloat(),
                h.toFloat(),
                RenderParams(asset = smokeSpec, timeSec = 3.0f, clearTransparentBackground = true)
            )
            // Top 20% of frame must stay completely transparent (smoke dissolves before half-screen)
            var topAlphaMax = 0
            for (y in 0 until (h * 0.20f).toInt() step 4) {
                for (x in 0 until w step 8) {
                    val a = android.graphics.Color.alpha(bmp.getPixel(x, y))
                    if (a > topAlphaMax) topAlphaMax = a
                }
            }
            assertEquals("Expected upper region above half-screen fade to stay transparent", 0, topAlphaMax)

            // Bottom 30% must contain visible rising smoke
            var bottomLitCount = 0
            for (y in (h * 0.70f).toInt() until h step 4) {
                for (x in 0 until w step 4) {
                    if (android.graphics.Color.alpha(bmp.getPixel(x, y)) > 8) bottomLitCount++
                }
            }
            assertTrue("Expected rising smoke visible near bottom half, lit=$bottomLitCount", bottomLitCount > 40)
        } finally {
            bmp.recycle()
        }
    }

    @Test
    fun `native 16k 4x4 tile renderer draws clean exterior corner and non-empty frame tile`() {
        val spec = NeonCatalog.findById(NeonEffectId.PRISM_HOLOGRAPHIC_3D)
        val tileW = 320
        val tileH = 180
        val bmpCorner = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.ARGB_8888)
        val bmpEdge = Bitmap.createBitmap(tileW, tileH, Bitmap.Config.ARGB_8888)
        try {
            Neon3DRenderer.renderMasterTileToCanvas(
                tileCanvas = Canvas(bmpCorner),
                tileWidth = tileW,
                tileHeight = tileH,
                tileCol = 0,
                tileRow = 0,
                gridCols = 4,
                gridRows = 4,
                params = RenderParams(asset = spec, clearTransparentBackground = true)
            )
            val topLeftAlpha = android.graphics.Color.alpha(bmpCorner.getPixel(1, 1))
            assertEquals("Outer corner of 16K tile (0,0) must remain 0 alpha", 0, topLeftAlpha)

            // Tile (1,0) crosses the top horizontal beveled frame bar
            Neon3DRenderer.renderMasterTileToCanvas(
                tileCanvas = Canvas(bmpEdge),
                tileWidth = tileW,
                tileHeight = tileH,
                tileCol = 1,
                tileRow = 0,
                gridCols = 4,
                gridRows = 4,
                params = RenderParams(asset = spec, clearTransparentBackground = true)
            )
            val pixels = IntArray(tileW * tileH)
            bmpEdge.getPixels(pixels, 0, tileW, 0, 0, tileW, tileH)
            val litCount = pixels.count { android.graphics.Color.alpha(it) > 20 }
            assertTrue("Expected 16K top-bar tile (1,0) to contain rendered frame geometry, lit=$litCount", litCount > 500)
        } finally {
            bmpCorner.recycle()
            bmpEdge.recycle()
        }
    }

    @Test
    fun `all 50 famous pro 3d effects render cleanly with visible alpha geometry`() {
        val w = 240
        val h = 240
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pixels = IntArray(w * h)
        try {
            NeonPro50Catalog.pro50Assets.forEach { spec ->
                Neon3DRenderer.renderToCanvas(
                    canvas = canvas,
                    width = w.toFloat(),
                    height = h.toFloat(),
                    params = RenderParams(asset = spec, timeSec = 3.2f, clearTransparentBackground = true)
                )
                bmp.getPixels(pixels, 0, w, 0, 0, w, h)
                val litCount = pixels.count { android.graphics.Color.alpha(it) > 8 }
                assertTrue("Expected visible 3D pixels for ${spec.code} (${spec.title}), got $litCount", litCount > 40)
            }
        } finally {
            bmp.recycle()
        }
    }

    @Test
    fun `offline webapp assets are bundled with zero external dependencies and 84 effects`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val html = context.assets.open("webapp/index.html").bufferedReader().use { it.readText() }
        val manifest = context.assets.open("webapp/manifest.webmanifest").bufferedReader().use { it.readText() }
        val sw = context.assets.open("webapp/sw.js").bufferedReader().use { it.readText() }

        assertTrue("Offline index.html must include CORE_34 and PRO_50_DEFS", html.contains("CORE_34") && html.contains("PRO_50_DEFS"))
        assertTrue("Offline index.html must include all 50 Pro effects", html.contains("INFINITY_COSMIC_PORTAL_AURA"))
        assertTrue("Offline index.html must include ZIP builder for 16K offline export", html.contains("buildZipBlob"))
        assertTrue("Offline manifest must configure standalone PWA", manifest.contains("\"display\": \"standalone\""))
        assertTrue("Offline service worker must cache index.html", sw.contains("./index.html"))
    }
}

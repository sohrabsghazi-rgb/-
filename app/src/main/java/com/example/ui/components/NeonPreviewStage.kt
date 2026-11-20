package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.engine.ExportAndQcEngine
import com.example.engine.Neon3DRenderer
import com.example.engine.RenderParams
import com.example.model.FrameAspectRatio
import com.example.model.NeonAssetSpec
import com.example.model.PreviewBackdrop
import com.example.ui.StudioUiState
import com.example.ui.theme.EmeraldIon
import com.example.ui.theme.LocalStudioTheme

@Composable
fun NeonPreviewStage(
    studioState: StudioUiState,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalStudioTheme.current
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(activeTheme.backgroundVoid)
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(22.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val aspect = studioState.aspectRatio
        val canvasModifier = when (aspect) {
            FrameAspectRatio.SQUARE_1_1 -> Modifier
                .widthIn(max = 360.dp)
                .heightIn(max = 360.dp)
                .aspectRatio(1f)
            FrameAspectRatio.LANDSCAPE_16_9 -> Modifier
                .widthIn(max = 520.dp)
                .aspectRatio(16f / 9f)
            FrameAspectRatio.PORTRAIT_9_16 -> Modifier
                .heightIn(max = 380.dp)
                .aspectRatio(9f / 16f)
        }

        Box(
            modifier = canvasModifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
        ) {
            // 1. Live Alpha Audit Backdrop Layer (Checkerboard, Black, White, Photo/Color tests)
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (studioState.backdrop) {
                    PreviewBackdrop.CHECKERBOARD_ALPHA -> {
                        val tile = 14.dp.toPx()
                        val cols = (size.width / tile).toInt() + 1
                        val rows = (size.height / tile).toInt() + 1
                        val cLight = Color(0xFF283140)
                        val cDark = Color(0xFF19202B)
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                drawRect(
                                    color = if ((r + c) % 2 == 0) cLight else cDark,
                                    topLeft = Offset(c * tile, r * tile),
                                    size = Size(tile, tile)
                                )
                            }
                        }
                    }
                    PreviewBackdrop.PURE_BLACK -> drawRect(Color.Black)
                    PreviewBackdrop.PURE_WHITE -> drawRect(Color.White)
                    PreviewBackdrop.DARK_PHOTO -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF0A111D), Color(0xFF18283E), Color(0xFF0A0E17))
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0x44326B9E), Color.Transparent),
                                center = Offset(size.width * 0.35f, size.height * 0.35f),
                                radius = size.minDimension * 0.7f
                            )
                        )
                    }
                    PreviewBackdrop.BRIGHT_PHOTO -> {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF72B5E8), Color(0xFFDDECF7), Color(0xFF9AC2DC))
                            )
                        )
                    }
                    PreviewBackdrop.COLORED_INDIGO -> {
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1D0D3D), Color(0xFF3E186E), Color(0xFF132A4A))
                            )
                        )
                    }
                }
            }

            // 2. Optional User-Placed Image Layer (clipped inside frame or behind raw VFX)
            val placedBmp = studioState.placedImageBitmap
            if (!studioState.rawAlphaOnlyMode && placedBmp != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { composeCanvas ->
                        ExportAndQcEngine.drawPlacedImageToCanvas(
                            canvas = composeCanvas.nativeCanvas,
                            width = size.width,
                            height = size.height,
                            bitmap = placedBmp,
                            placementMode = studioState.imagePlacementMode,
                            cornerRadiusFraction = studioState.cornerRadiusFraction,
                            scale = studioState.imageScale,
                            offsetXFraction = studioState.imageOffsetX,
                            offsetYFraction = studioState.imageOffsetY,
                            opacity = studioState.imageOpacity
                        )
                    }
                }
            }

            // 3. Real-time 3D PBR Raw Neon Frame & Volumetric VFX Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { composeCanvas ->
                    Neon3DRenderer.renderToCanvas(
                        canvas = composeCanvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        params = studioState.toRenderParams().copy(clearTransparentBackground = false)
                    )
                }
            }

            // Corner Badges: Aspect Ratio + Raw Alpha / Placed Image + Resolution Tier
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${aspect.id} · ${studioState.resolutionTier.badge}",
                        style = MaterialTheme.typography.labelSmall,
                        color = activeTheme.primaryColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (studioState.rawAlphaOnlyMode || placedBmp == null) "RAW ALPHA" else "FRAMED IMG",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (studioState.rawAlphaOnlyMode || placedBmp == null) EmeraldIon else activeTheme.secondaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun NeonThumbnailCanvas(
    spec: NeonAssetSpec,
    timeSec: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF070B14))
        drawIntoCanvas { composeCanvas ->
            Neon3DRenderer.renderToCanvas(
                canvas = composeCanvas.nativeCanvas,
                width = size.width,
                height = size.height,
                params = RenderParams(
                    asset = spec,
                    timeSec = timeSec,
                    clearTransparentBackground = false
                )
            )
        }
    }
}

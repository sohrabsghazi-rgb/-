package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.Neon3DRenderer
import com.example.model.ExportResolutionTier
import com.example.model.FrameAspectRatio
import com.example.model.ImagePlacementMode
import com.example.model.NeonCatalog
import com.example.model.NeonModelCategory
import com.example.model.PreviewBackdrop
import com.example.ui.NeonStudioViewModel
import com.example.ui.QcUiState
import com.example.ui.StudioUiState
import com.example.ui.components.NeonPreviewStage
import com.example.ui.theme.EmeraldIon
import com.example.ui.theme.LocalStudioTheme
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PlatinumText
import com.example.ui.theme.VazirmatnFontFamily
import kotlin.math.roundToInt

@Composable
fun ViewportScreen(
    studioState: StudioUiState,
    qcState: QcUiState,
    viewModel: NeonStudioViewModel,
    onOpenCollection: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenStudioMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPlacedImageFromUri(context, uri)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 760.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActiveAssetHeader(
                        studioState = studioState,
                        qcState = qcState,
                        onOpenCollection = onOpenCollection,
                        onOpenExport = onOpenExport,
                        onOpenStudioMenu = onOpenStudioMenu
                    )
                    NeonPreviewStage(
                        studioState = studioState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                    )
                    TimelineTransportCard(studioState, viewModel)
                }

                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ImagePlacementMenuCard(
                        studioState = studioState,
                        viewModel = viewModel,
                        onPickImage = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    OrientationAndBackdropCard(studioState, viewModel)
                    VfxLayerSelectorCard(studioState, viewModel)
                    PbrMaterialControlsCard(studioState, viewModel)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActiveAssetHeader(
                    studioState = studioState,
                    qcState = qcState,
                    onOpenCollection = onOpenCollection,
                    onOpenExport = onOpenExport,
                    onOpenStudioMenu = onOpenStudioMenu
                )
                NeonPreviewStage(
                    studioState = studioState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
                TimelineTransportCard(studioState, viewModel)
                ImagePlacementMenuCard(
                    studioState = studioState,
                    viewModel = viewModel,
                    onPickImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                OrientationAndBackdropCard(studioState, viewModel)
                VfxLayerSelectorCard(studioState, viewModel)
                PbrMaterialControlsCard(studioState, viewModel)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActiveAssetHeader(
    studioState: StudioUiState,
    qcState: QcUiState,
    onOpenCollection: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenStudioMenu: () -> Unit
) {
    val activeTheme = LocalStudioTheme.current
    val spec = studioState.selectedAsset
    val allPassed = (qcState as? QcUiState.Ready)?.report?.allPassed == true

    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(spec.primaryColor.copy(alpha = 0.18f))
                            .border(1.dp, spec.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = spec.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = spec.primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = spec.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = PlatinumText
                        )
                        Text(
                            text = spec.persianTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            color = activeTheme.primaryColor
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = if (allPassed) EmeraldIon.copy(alpha = 0.14f) else activeTheme.deckSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .testTag("qc_badge_button")
                            .clickable(onClick = onOpenExport)
                            .border(
                                1.dp,
                                if (allPassed) EmeraldIon.copy(alpha = 0.55f) else activeTheme.borderStroke,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Quality Control Status",
                                tint = if (allPassed) EmeraldIon else MutedSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (allPassed) "7/7 QC" else "AUDIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (allPassed) EmeraldIon else PlatinumText
                            )
                        }
                    }

                    Surface(
                        color = activeTheme.deckSurface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .testTag("switch_model_button")
                            .clickable(onClick = onOpenCollection)
                            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Switch 3D Frame Model",
                                tint = activeTheme.secondaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "MODELS",
                                style = MaterialTheme.typography.labelSmall,
                                color = PlatinumText
                            )
                        }
                    }

                    Surface(
                        color = activeTheme.primaryColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .testTag("open_power_menu_button")
                            .clickable(onClick = onOpenStudioMenu)
                            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Open Pro Studio Menu",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = spec.persianDescription,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                color = MutedSlate
            )
        }
    }
}

@Composable
private fun ImagePlacementMenuCard(
    studioState: StudioUiState,
    viewModel: NeonStudioViewModel,
    onPickImage: () -> Unit
) {
    val activeTheme = LocalStudioTheme.current
    val hasImage = studioState.placedImageBitmap != null

    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FRAME IMAGE PLACEMENT & RAW MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = activeTheme.primaryColor
                    )
                    Text(
                        text = "منوی افزودن فایل تصویر برای جاگذاری در قاب (یا خروجی خام)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                        color = PlatinumText
                    )
                }
                if (hasImage) {
                    IconButton(
                        onClick = viewModel::clearPlacedImage,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("clear_placed_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Remove placed image",
                            tint = MutedSlate
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPickImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = activeTheme.backgroundVoid
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_frame_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Add or replace image inside frame"
                    )
                    Text(
                        text = if (hasImage) " جایگزینی فایل تصویر" else " افزودن فایل تصویر",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = VazirmatnFontFamily),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حالت خام ۱۰۰٪ شفاف (Raw Alpha)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                        color = PlatinumText
                    )
                    Text(
                        text = studioState.placedImageName ?: "بدون تصویر پس‌زمینه — افکت کاملاً خام است",
                        style = MaterialTheme.typography.labelSmall,
                        color = activeTheme.secondaryColor
                    )
                }
                Switch(
                    checked = studioState.rawAlphaOnlyMode,
                    onCheckedChange = viewModel::setRawAlphaOnlyMode,
                    modifier = Modifier.testTag("toggle_raw_alpha_mode")
                )
            }

            if (hasImage && !studioState.rawAlphaOnlyMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImagePlacementMode.entries.forEach { mode ->
                        FilterChip(
                            selected = studioState.imagePlacementMode == mode,
                            onClick = { viewModel.updateImagePlacementMode(mode) },
                            label = {
                                Text(
                                    mode.persianLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = VazirmatnFontFamily)
                                )
                            },
                            modifier = Modifier.testTag("placement_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                PbrSliderRow(
                    label = "Image Zoom (مقیاس تصویر)",
                    valueText = "${"%.2f".format(studioState.imageScale)}x",
                    value = studioState.imageScale,
                    range = 0.5f..2.2f,
                    onValueChange = viewModel::updateImageScale,
                    tag = "slider_image_scale"
                )

                PbrSliderRow(
                    label = "Image Opacity (شفافیت تصویر)",
                    valueText = "${(studioState.imageOpacity * 100).roundToInt()}%",
                    value = studioState.imageOpacity,
                    range = 0.2f..1.0f,
                    onValueChange = viewModel::updateImageOpacity,
                    tag = "slider_image_opacity"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ترکیب تصویر در فایل خروجی (به‌جای خروجی خام)",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = VazirmatnFontFamily),
                        color = MutedSlate,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = studioState.bakePlacedImageInExport,
                        onCheckedChange = viewModel::setBakePlacedImageInExport,
                        modifier = Modifier.testTag("toggle_bake_image_export")
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineTransportCard(
    studioState: StudioUiState,
    viewModel: NeonStudioViewModel
) {
    val activeTheme = LocalStudioTheme.current
    val frameNumber = ((studioState.currentTimeSec / Neon3DRenderer.LOOP_DURATION_SEC) * 900f)
        .roundToInt()
        .coerceIn(0, 899)

    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = viewModel::togglePlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (studioState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (studioState.isPlaying) "Pause 15s loop" else "Play 15s loop",
                            tint = activeTheme.primaryColor
                        )
                    }
                    Column {
                        Text(
                            text = "${"%.2f".format(studioState.currentTimeSec)}s / 15.00s",
                            style = MaterialTheme.typography.titleMedium,
                            color = PlatinumText
                        )
                        Text(
                            text = "FRAME ${"%03d".format(frameNumber)} / 900 · 60 FPS SEAMLESS LOOP",
                            style = MaterialTheme.typography.labelSmall,
                            color = activeTheme.primaryColor
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.5f, 1.0f, 2.0f).forEach { speed ->
                        val selected = studioState.playbackSpeed == speed
                        Surface(
                            color = if (selected) activeTheme.primaryColor.copy(alpha = 0.18f) else activeTheme.deckSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .testTag("speed_${speed}x")
                                .clickable { viewModel.setPlaybackSpeed(speed) }
                                .border(
                                    1.dp,
                                    if (selected) activeTheme.primaryColor else activeTheme.borderStroke,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = "${speed}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) activeTheme.primaryColor else MutedSlate,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Slider(
                value = studioState.currentTimeSec,
                onValueChange = viewModel::scrubToTime,
                valueRange = 0f..Neon3DRenderer.LOOP_DURATION_SEC,
                colors = SliderDefaults.colors(
                    thumbColor = activeTheme.primaryColor,
                    activeTrackColor = activeTheme.primaryColor,
                    inactiveTrackColor = activeTheme.deckSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timeline_scrubber")
            )
        }
    }
}

@Composable
private fun OrientationAndBackdropCard(
    studioState: StudioUiState,
    viewModel: NeonStudioViewModel
) {
    val activeTheme = LocalStudioTheme.current
    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ORIENTATION & RESOLUTION TIER (FULLHD / 4K / 16K)",
                style = MaterialTheme.typography.labelSmall,
                color = activeTheme.primaryColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrameAspectRatio.entries.forEach { ratio ->
                    val selected = studioState.aspectRatio == ratio
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setAspectRatio(ratio) },
                        label = {
                            Column {
                                Text(ratio.label, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "${ratio.widthForTier(studioState.resolutionTier)}×${ratio.heightForTier(studioState.resolutionTier)}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeTheme.primaryColor.copy(alpha = 0.16f),
                            selectedLabelColor = activeTheme.primaryColor,
                            containerColor = activeTheme.deckSurface,
                            labelColor = MutedSlate
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("aspect_${ratio.id}")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportResolutionTier.entries.forEach { tier ->
                    val selected = studioState.resolutionTier == tier
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setResolutionTier(tier) },
                        label = { Text(tier.label, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("vp_tier_${tier.name.lowercase()}")
                    )
                }
            }

            Text(
                text = "ALPHA TRANSPARENCY AUDITOR (RAW CHECK)",
                style = MaterialTheme.typography.labelSmall,
                color = activeTheme.secondaryColor
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewBackdrop.entries.forEach { bg ->
                    val selected = studioState.backdrop == bg
                    Surface(
                        color = if (selected) activeTheme.primaryColor.copy(alpha = 0.16f) else activeTheme.deckSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("backdrop_${bg.name.lowercase()}")
                            .clickable { viewModel.setBackdrop(bg) }
                            .border(
                                1.dp,
                                if (selected) activeTheme.primaryColor else activeTheme.borderStroke,
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(bg.swatchColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Text(
                                text = bg.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) PlatinumText else MutedSlate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VfxLayerSelectorCard(
    studioState: StudioUiState,
    viewModel: NeonStudioViewModel
) {
    val activeTheme = LocalStudioTheme.current
    val vfxList = NeonCatalog.allAssets.filter { !it.isFrameAsset }

    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = "Atmospheric VFX Compositor",
                    tint = activeTheme.secondaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "VFX ATMOSPHERIC COMPOSITOR OVERLAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTheme.secondaryColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = studioState.overlayVfx == null,
                    onClick = { viewModel.setOverlayVfx(null) },
                    label = { Text("Pure Raw Frame") },
                    modifier = Modifier.testTag("vfx_layer_none")
                )
                vfxList.forEach { vfx ->
                    val selected = studioState.overlayVfx?.key == vfx.key
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setOverlayVfx(if (selected) null else vfx) },
                        label = { Text("${vfx.code} · ${vfx.title}") },
                        enabled = studioState.selectedAsset.isFrameAsset,
                        modifier = Modifier.testTag("vfx_layer_${vfx.id.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun PbrMaterialControlsCard(
    studioState: StudioUiState,
    viewModel: NeonStudioViewModel
) {
    val activeTheme = LocalStudioTheme.current
    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "PBR Material & Lighting Parameters",
                    tint = activeTheme.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "PBR GEOMETRY, LIGHTING & PARTICLES",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTheme.primaryColor
                )
            }

            PbrSliderRow(
                label = "Corner Bevel Radius",
                valueText = "${(studioState.cornerRadiusFraction * 100).roundToInt()}%",
                value = studioState.cornerRadiusFraction,
                range = 0.05f..0.24f,
                onValueChange = viewModel::updateCornerRadius,
                tag = "slider_corner_radius"
            )
            PbrSliderRow(
                label = "3D Bevel Thickness",
                valueText = "${"%.2f".format(studioState.bevelDepthScale)}x",
                value = studioState.bevelDepthScale,
                range = 0.65f..1.65f,
                onValueChange = viewModel::updateBevelDepth,
                tag = "slider_bevel_depth"
            )
            PbrSliderRow(
                label = "Volumetric Glow Falloff",
                valueText = "${"%.2f".format(studioState.glowIntensity)}x",
                value = studioState.glowIntensity,
                range = 0.45f..1.65f,
                onValueChange = viewModel::updateGlowIntensity,
                tag = "slider_glow_intensity"
            )
            PbrSliderRow(
                label = "Deterministic Particles",
                valueText = "${(studioState.particleDensity * 100).roundToInt()}%",
                value = studioState.particleDensity,
                range = 0.3f..1.8f,
                onValueChange = viewModel::updateParticleDensity,
                tag = "slider_particle_density"
            )
        }
    }
}

@Composable
private fun PbrSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    val activeTheme = LocalStudioTheme.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PlatinumText)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = activeTheme.primaryColor)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = activeTheme.secondaryColor,
                activeTrackColor = activeTheme.primaryColor,
                inactiveTrackColor = activeTheme.deckSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

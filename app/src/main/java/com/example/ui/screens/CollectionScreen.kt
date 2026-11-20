package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.NeonAssetSpec
import com.example.model.NeonCatalog
import com.example.model.NeonModelCategory
import com.example.ui.DriveUiState
import com.example.ui.NeonStudioViewModel
import com.example.ui.StudioUiState
import com.example.ui.components.NeonThumbnailCanvas
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldIon
import com.example.ui.theme.LocalStudioTheme
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PlatinumText
import com.example.ui.theme.VazirmatnFontFamily

@Composable
fun CollectionScreen(
    studioState: StudioUiState,
    driveState: DriveUiState,
    viewModel: NeonStudioViewModel,
    onLoadedToViewport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalStudioTheme.current
    val context = LocalContext.current
    val activity = context as? Activity
    val driveAssets by viewModel.driveAssets.collectAsStateWithLifecycle()
    val allAssets = remember(driveAssets) { NeonCatalog.builtInAssets + driveAssets }

    var selectedCategory by remember { mutableStateOf<NeonModelCategory?>(null) }
    val filteredAssets = remember(selectedCategory, allAssets) {
        if (selectedCategory == null) allAssets
        else allAssets.filter { it.category == selectedCategory }
    }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDriveAuthorizationIntentResult(context, result.data)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "NEON_FRAME_COLLECTION (${allAssets.size} RAW 3D ASSETS)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PlatinumText
                )
                Text(
                    text = "30 Pro 3D Frames + 54 Raw VFX, Smoke, Sparkles & Famous Pro Ideas · 100% Raw Alpha · FullHD / 4K / 16K",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )

                // Drive Pack Sync Banner inside Collection
                Card(
                    colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudSync,
                                contentDescription = "Sync Drive Pack",
                                tint = activeTheme.primaryColor
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive Pack AI Analyzer",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PlatinumText
                                )
                                Text(
                                    text = "آنالیز تمام افکت‌های پوشه گوگل درایو و تبدیل به سه‌بعدی خام FullHD / 16K",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                                    color = MutedSlate
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!driveState.isConnected) {
                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            viewModel.connectGoogleDrive(activity) { pendingIntent ->
                                                driveAuthLauncher.launch(
                                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                                )
                                            }
                                        }
                                    },
                                    enabled = !driveState.isConnecting,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activeTheme.primaryColor,
                                        contentColor = activeTheme.backgroundVoid
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("collection_connect_drive_button")
                                ) {
                                    Text(
                                        text = if (driveState.isConnecting) "Connecting..." else "Connect Google Drive & Sync Pack",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = viewModel::syncAndAnalyzeDrivePack,
                                    enabled = !driveState.isAnalyzingPack,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activeTheme.primaryColor,
                                        contentColor = activeTheme.backgroundVoid
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("collection_analyze_drive_button")
                                ) {
                                    if (driveState.isAnalyzingPack) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = activeTheme.backgroundVoid,
                                            strokeWidth = 2.dp
                                        )
                                        Text("  ${driveState.analysisProgressLabel ?: "Analyzing..."}")
                                    } else {
                                        Text(
                                            text = "Analyze & Import Drive Pack (${driveState.folderFiles.size} items)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        driveState.errorMessage?.let { err ->
                            Text(text = err, style = MaterialTheme.typography.labelSmall, color = CrimsonAlert)
                        }
                        driveState.statusMessage?.let { msg ->
                            Text(text = msg, style = MaterialTheme.typography.labelSmall, color = EmeraldIon)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${allAssets.size})") },
                        modifier = Modifier.testTag("filter_all")
                    )
                    NeonModelCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    when (cat) {
                                        NeonModelCategory.FAMOUS_50_PRO -> "50 Famous Pro 3D"
                                        NeonModelCategory.VISUAL_MODEL -> "01–19 3D Frames"
                                        NeonModelCategory.ANIMATION_BEHAVIOR -> "07–12 Anim"
                                        NeonModelCategory.REFERENCE_VFX -> "Drive Ref VFX"
                                        NeonModelCategory.STUDIO_PRO_IDEAS -> "Studio Pro Ideas"
                                        NeonModelCategory.DRIVE_PACK_AI -> "Drive AI (${driveAssets.size})"
                                    }
                                )
                            },
                            modifier = Modifier.testTag("filter_${cat.name.lowercase()}")
                        )
                    }
                }
            }
        }

        items(filteredAssets, key = { it.key }) { spec ->
            AssetCatalogCard(
                spec = spec,
                isActive = studioState.selectedAsset.key == spec.key,
                isLayeredVfx = studioState.overlayVfx?.key == spec.key,
                liveTimeSec = studioState.currentTimeSec,
                canLayerAsVfx = !spec.isFrameAsset && studioState.selectedAsset.isFrameAsset,
                onLoadInStudio = {
                    viewModel.selectAsset(spec)
                    onLoadedToViewport()
                },
                onToggleLayer = {
                    if (studioState.overlayVfx?.key == spec.key) {
                        viewModel.setOverlayVfx(null)
                    } else {
                        viewModel.setOverlayVfx(spec)
                        onLoadedToViewport()
                    }
                }
            )
        }
    }
}

@Composable
private fun AssetCatalogCard(
    spec: NeonAssetSpec,
    isActive: Boolean,
    isLayeredVfx: Boolean,
    liveTimeSec: Float,
    canLayerAsVfx: Boolean,
    onLoadInStudio: () -> Unit,
    onToggleLayer: () -> Unit
) {
    val activeTheme = LocalStudioTheme.current
    Card(
        colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("catalog_card_${spec.key.lowercase()}")
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) activeTheme.primaryColor else activeTheme.borderStroke,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onLoadInStudio)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 104.dp, height = 104.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, spec.primaryColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            ) {
                NeonThumbnailCanvas(
                    spec = spec,
                    timeSec = liveTimeSec,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = spec.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = spec.primaryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PlatinumText,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Active Asset",
                            tint = EmeraldIon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = spec.persianTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                    color = activeTheme.primaryColor
                )
                Text(
                    text = spec.persianDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                    color = MutedSlate,
                    maxLines = 2
                )
                Text(
                    text = "Raw Alpha · ${spec.key}",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTheme.secondaryColor
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = activeTheme.primaryColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .testTag("load_asset_${spec.key.lowercase()}")
                            .clickable(onClick = onLoadInStudio)
                            .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = "Open in Studio Viewport",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Open in Studio",
                                style = MaterialTheme.typography.labelSmall,
                                color = PlatinumText
                            )
                        }
                    }

                    if (canLayerAsVfx) {
                        OutlinedButton(
                            onClick = onToggleLayer,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("layer_vfx_${spec.key.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = "Composite as VFX Layer",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isLayeredVfx) " Remove Layer" else " Layer VFX",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

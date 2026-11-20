package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ExportResolutionTier
import com.example.ui.DriveUiState
import com.example.ui.ExportUiState
import com.example.ui.NeonStudioViewModel
import com.example.ui.QcUiState
import com.example.ui.StudioUiState
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldIon
import com.example.ui.theme.LocalStudioTheme
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PlatinumText
import com.example.ui.theme.VazirmatnFontFamily

@Composable
fun QualityExportScreen(
    studioState: StudioUiState,
    qcState: QcUiState,
    exportState: ExportUiState,
    driveState: DriveUiState,
    viewModel: NeonStudioViewModel,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalStudioTheme.current
    val context = LocalContext.current
    val activity = context as? Activity
    val apkDeliveryStatus by viewModel.apkDeliveryStatus.collectAsStateWithLifecycle()

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onDriveAuthorizationIntentResult(context, result.data)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Primary Local Gallery & Device Folder Delivery Builder (FullHD / 4K / 16K Master Tiled)
        Card(
            colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "FULLHD / 4K / 16K GALLERY & FOLDER EXPORT",
                    style = MaterialTheme.typography.titleMedium,
                    color = PlatinumText
                )
                Text(
                    text = "ذخیره مستقیم خروجی خام ۱۰۰٪ شفاف (ARGB_8888) در گالری گوشی (Pictures/NeonVFX_Studio) و پوشه فایل‌های دستگاه با کاشی‌های ۱۶K و توالی ۱۵ ثانیه‌ای.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                    color = MutedSlate
                )

                Text(
                    text = "OUTPUT RESOLUTION TIER",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTheme.primaryColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportResolutionTier.entries.forEach { tier ->
                        FilterChip(
                            selected = studioState.resolutionTier == tier,
                            onClick = { viewModel.setResolutionTier(tier) },
                            label = { Text(tier.label) },
                            modifier = Modifier.testTag("export_tier_${tier.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "PNG RGBA SEQUENCE FRAMES (15s LOOP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeTheme.secondaryColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60, 120).forEach { count ->
                        FilterChip(
                            selected = studioState.sequenceFrameCount == count,
                            onClick = { viewModel.setSequenceFrameCount(count) },
                            label = { Text("$count frames") },
                            modifier = Modifier.testTag("seq_count_$count")
                        )
                    }
                }

                Button(
                    onClick = { viewModel.exportCollectionPackage(context) },
                    enabled = exportState !is ExportUiState.Exporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = activeTheme.backgroundVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_package_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Save FullHD and 16K Package to Gallery and Device Folder"
                    )
                    Text(
                        text = "  Save ${studioState.resolutionTier.label} to Gallery & Folder",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (exportState) {
                    is ExportUiState.Idle -> Unit
                    is ExportUiState.Exporting -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { exportState.progress },
                                color = activeTheme.primaryColor,
                                trackColor = activeTheme.deckSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = exportState.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = activeTheme.primaryColor
                            )
                        }
                    }

                    is ExportUiState.Error -> {
                        Text(
                            text = "Export failed: ${exportState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonAlert
                        )
                    }

                    is ExportUiState.Success -> {
                        val res = exportState.result
                        Surface(
                            color = EmeraldIon.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EmeraldIon.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Verified (${res.exportedFrameCount} Seq Frames${if (res.exportedTileCount > 0) " + ${res.exportedTileCount}×16K Tiles" else ""})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldIon
                                )
                                Text(
                                    text = "${res.outputResolution} · ${if (res.isRawPureAlpha) "RAW PURE ALPHA" else "FRAMED IMAGE"}${if (res.savedToGallery) " · Saved to Pictures/NeonVFX_Studio" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PlatinumText
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ShareArtifactButton(
                                        label = "RGBA PNG",
                                        icon = Icons.Rounded.Share,
                                        onClick = { shareFileUri(context, res.previewPngUri, "image/png", "Share RGBA Neon Frame") },
                                        modifier = Modifier.weight(1f),
                                        tag = "share_rgba_png"
                                    )
                                    ShareArtifactButton(
                                        label = "Contact Sheet",
                                        icon = Icons.Rounded.GridView,
                                        onClick = { shareFileUri(context, res.contactSheetUri, "image/jpeg", "Share Contact Sheet") },
                                        modifier = Modifier.weight(1f),
                                        tag = "share_contact_sheet"
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ShareArtifactButton(
                                        label = "Manifest JSON",
                                        icon = Icons.Rounded.Description,
                                        onClick = { shareFileUri(context, res.manifestUri, "application/json", "Share TECHNICAL_MANIFEST.json") },
                                        modifier = Modifier.weight(1f),
                                        tag = "share_manifest"
                                    )
                                    ShareArtifactButton(
                                        label = "README_FA.txt",
                                        icon = Icons.Rounded.Description,
                                        onClick = { shareFileUri(context, res.readmeFaUri, "text/plain", "Share README_FA.txt") },
                                        modifier = Modifier.weight(1f),
                                        tag = "share_readme_fa"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1B. Direct Phone APK Downloads, Share Sheet & GitHub Release Link Card
        Card(
            colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeTheme.secondaryColor.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DIRECT APK DOWNLOAD & GITHUB RELEASE",
                    style = MaterialTheme.typography.titleMedium,
                    color = PlatinumText
                )
                Text(
                    text = "ذخیره مستقیم فایل نصبی APK در پوشه Downloads گوشی، ارسال مستقیم در پیام‌رسان‌ها، خروجی یکپارچه ZIP و لینک آماده انتشار در مخزن گیت‌هاب شما.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                    color = MutedSlate
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveApkDirectlyToPhoneDownloads(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.secondaryColor,
                            contentColor = activeTheme.backgroundVoid
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_apk_downloads_button")
                    ) {
                        Icon(Icons.Rounded.SaveAlt, contentDescription = "Save APK to Downloads", modifier = Modifier.size(18.dp))
                        Text(" Save APK", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.shareApkFileDirectly(context) { uri ->
                                shareFileUri(context, uri, "application/vnd.android.package-archive", "Share NeonVFX Studio APK")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_apk_direct_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share APK Directly", modifier = Modifier.size(18.dp))
                        Text(" Share APK", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = {
                        viewModel.exportZipPackageDirectly(context) { uri ->
                            shareFileUri(context, uri, "application/zip", "Share Full 16K Package (.ZIP)")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_zip_bundle_button")
                ) {
                    Icon(Icons.Rounded.FolderZip, contentDescription = "Export and Share ZIP Bundle", modifier = Modifier.size(18.dp))
                    Text("  Save & Share Complete .ZIP Bundle", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        openUrlInBrowser(context, "https://github.com/sohrabsghazi-rgb/-/releases/latest")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_github_releases_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open GitHub Releases Page", modifier = Modifier.size(16.dp))
                    Text("  GitHub Releases (sohrabsghazi-rgb/-)", style = MaterialTheme.typography.labelLarge)
                }
                apkDeliveryStatus?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.startsWith("Error:")) CrimsonAlert else EmeraldIon
                    )
                }
            }
        }

        // 2. Google Drive Pack Sync & Optional APK/Folder Delivery Card
        Card(
            colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (driveState.isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
                        contentDescription = "Google Drive Cloud Delivery",
                        tint = if (driveState.isConnected) EmeraldIon else activeTheme.primaryColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GOOGLE DRIVE PACK SYNC & APK UPLOAD",
                            style = MaterialTheme.typography.titleMedium,
                            color = PlatinumText
                        )
                        Text(
                            text = "همگام‌سازی پوشه درایو پک و آپلود اختیاری فایل APK یا پکیج رندرشده",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                            color = activeTheme.primaryColor
                        )
                    }
                }

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
                            .height(50.dp)
                            .testTag("export_connect_drive_button")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = "Connect Google Drive")
                        Text(
                            text = if (driveState.isConnecting) "  Authorizing Drive..." else "  Connect Google Drive (OAuth)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.uploadFullApkToDrive(context) },
                        enabled = !driveState.isUploadingApk && !driveState.isUploadingPackage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.secondaryColor,
                            contentColor = activeTheme.backgroundVoid
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("upload_full_apk_drive_button")
                    ) {
                        Icon(Icons.Rounded.Android, contentDescription = "Upload Full APK to Google Drive")
                        Text(
                            text = "  Upload Full APK to Google Drive",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.uploadExportedPackageToDrive(context) },
                        enabled = !driveState.isUploadingPackage && !driveState.isUploadingApk,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.primaryColor,
                            contentColor = activeTheme.backgroundVoid
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("upload_package_drive_button")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = "Upload Rendered Package to Drive")
                        Text(
                            text = "  Upload ${studioState.resolutionTier.label} Assets to Drive",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = viewModel::syncAndAnalyzeDrivePack,
                        enabled = !driveState.isAnalyzingPack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_sync_drive_pack_button")
                    ) {
                        Text("Sync & Gemini-Analyze Drive Pack Folder")
                    }
                }

                if (driveState.isUploadingApk || driveState.isUploadingPackage || driveState.isAnalyzingPack) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = activeTheme.primaryColor)
                        Text(
                            text = driveState.uploadProgressLabel
                                ?: driveState.analysisProgressLabel
                                ?: "Working with Google Drive...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = activeTheme.primaryColor
                        )
                    }
                }

                driveState.errorMessage?.let { err ->
                    Text(text = err, style = MaterialTheme.typography.bodyMedium, color = CrimsonAlert)
                }
                driveState.statusMessage?.let { msg ->
                    Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = EmeraldIon)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    driveState.uploadedApkUrl?.let { link ->
                        OutlinedButton(
                            onClick = { openUrlInBrowser(context, link) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_uploaded_apk_link")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open APK in Drive", modifier = Modifier.size(16.dp))
                            Text(" Open APK in Drive", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    driveState.uploadedFolderUrl?.let { link ->
                        OutlinedButton(
                            onClick = { openUrlInBrowser(context, link) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_uploaded_folder_link")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open Assets Folder in Drive", modifier = Modifier.size(16.dp))
                            Text(" Open Drive Folder", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // 3. 7-Pass Quality Control Audit
        Card(
            colors = CardDefaults.cardColors(containerColor = activeTheme.panelSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, activeTheme.borderStroke, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "7-Pass Quality Control",
                            tint = EmeraldIon
                        )
                        Column {
                            Text(
                                text = "7-PASS QUALITY CONTROL AUDIT",
                                style = MaterialTheme.typography.titleMedium,
                                color = PlatinumText
                            )
                            Text(
                                text = "${studioState.selectedAsset.key} · ${studioState.aspectRatio.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = activeTheme.primaryColor
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::runSevenPassAudit,
                        modifier = Modifier.testTag("rerun_qc_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Re-run QC Audit",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(" Re-Audit", style = MaterialTheme.typography.labelLarge)
                    }
                }

                when (qcState) {
                    is QcUiState.Idle, is QcUiState.Auditing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = activeTheme.primaryColor)
                            Text(
                                text = "Sampling ARGB_8888 alpha borders, center transparency, and t=0 vs t=15s loop delta...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    }

                    is QcUiState.Error -> {
                        Text(
                            text = "Audit error: ${qcState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonAlert
                        )
                    }

                    is QcUiState.Ready -> {
                        val report = qcState.report
                        report.passes.forEach { pass ->
                            Surface(
                                color = activeTheme.deckSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (pass.passed) EmeraldIon.copy(alpha = 0.35f) else CrimsonAlert.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (pass.passed) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                        contentDescription = if (pass.passed) "Passed" else "Failed",
                                        tint = if (pass.passed) EmeraldIon else CrimsonAlert,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${pass.code} — ${pass.title}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = PlatinumText
                                            )
                                            Text(
                                                text = if (pass.passed) "PASS" else "WARN",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (pass.passed) EmeraldIon else CrimsonAlert
                                            )
                                        }
                                        Text(
                                            text = pass.metricDetail,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedSlate
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ShareArtifactButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .testTag(tag)
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Text(" $label", style = MaterialTheme.typography.labelLarge)
    }
}

private fun shareFileUri(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun openUrlInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

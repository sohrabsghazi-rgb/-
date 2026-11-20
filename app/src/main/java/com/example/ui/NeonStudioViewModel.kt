package com.example.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAspectRatioOption
import com.example.ai.AiImageModelOption
import com.example.ai.AiImageSizeOption
import com.example.ai.AiReasoningMode
import com.example.ai.GeminiStudioRepository
import com.example.drive.DriveAuthOutcome
import com.example.drive.DriveFileItem
import com.example.drive.GoogleDriveRepository
import com.example.engine.ExportAndQcEngine
import com.example.engine.ExportPackageResult
import com.example.engine.Neon3DRenderer
import com.example.engine.RenderParams
import com.example.model.ExportResolutionTier
import com.example.model.FrameAspectRatio
import com.example.model.ImagePlacementMode
import com.example.model.NeonAssetSpec
import com.example.model.NeonCatalog
import com.example.model.NeonEffectId
import com.example.model.PreviewBackdrop
import com.example.model.QcAuditReport
import com.example.model.StudioThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface QcUiState {
    data object Idle : QcUiState
    data object Auditing : QcUiState
    data class Ready(val report: QcAuditReport) : QcUiState
    data class Error(val message: String) : QcUiState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(val progress: Float, val status: String) : ExportUiState
    data class Success(val result: ExportPackageResult) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

sealed interface AiImageUiState {
    data object Idle : AiImageUiState
    data object Loading : AiImageUiState
    data class Success(val bitmap: Bitmap, val modelUsed: String) : AiImageUiState
    data class Error(val message: String) : AiImageUiState
}

sealed interface AiVisionUiState {
    data object Idle : AiVisionUiState
    data object Loading : AiVisionUiState
    data class Success(val response: String, val mode: AiReasoningMode) : AiVisionUiState
    data class Error(val message: String) : AiVisionUiState
}

data class DriveUiState(
    val accessToken: String? = null,
    val isConnecting: Boolean = false,
    val folderFiles: List<DriveFileItem> = emptyList(),
    val isAnalyzingPack: Boolean = false,
    val analysisProgressLabel: String? = null,
    val isUploadingPackage: Boolean = false,
    val isUploadingApk: Boolean = false,
    val uploadProgressLabel: String? = null,
    val uploadedFolderUrl: String? = null,
    val uploadedApkUrl: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
) {
    val isConnected: Boolean get() = !accessToken.isNullOrBlank()
}

data class StudioUiState(
    val selectedAsset: NeonAssetSpec = NeonCatalog.findById(NeonEffectId.CYAN_GLASS_NEON),
    val overlayVfx: NeonAssetSpec? = null,
    val aspectRatio: FrameAspectRatio = FrameAspectRatio.PORTRAIT_9_16,
    val resolutionTier: ExportResolutionTier = ExportResolutionTier.FULL_HD,
    val backdrop: PreviewBackdrop = PreviewBackdrop.CHECKERBOARD_ALPHA,
    val themePreset: StudioThemePreset = StudioThemePreset.OBSIDIAN_CYBER_CYAN,
    val isPlaying: Boolean = true,
    val playbackSpeed: Float = 1.0f,
    val currentTimeSec: Float = 0f,
    val cornerRadiusFraction: Float = 0.14f,
    val bevelDepthScale: Float = 1.0f,
    val glowIntensity: Float = 1.05f,
    val particleDensity: Float = 1.0f,
    val sequenceFrameCount: Int = 30,
    val rawAlphaOnlyMode: Boolean = true,
    val placedImageBitmap: Bitmap? = null,
    val placedImageName: String? = null,
    val imagePlacementMode: ImagePlacementMode = ImagePlacementMode.INSIDE_FRAME_CROP,
    val imageScale: Float = 1.0f,
    val imageOffsetX: Float = 0f,
    val imageOffsetY: Float = 0f,
    val imageOpacity: Float = 1.0f,
    val bakePlacedImageInExport: Boolean = false
) {
    fun toRenderParams(): RenderParams = RenderParams(
        asset = selectedAsset,
        overlayVfx = overlayVfx,
        timeSec = currentTimeSec,
        cornerRadiusFraction = cornerRadiusFraction,
        bevelDepthScale = bevelDepthScale,
        glowIntensity = glowIntensity,
        particleDensity = particleDensity,
        clearTransparentBackground = true
    )
}

class NeonStudioViewModel(
    private val aiRepository: GeminiStudioRepository = GeminiStudioRepository(),
    private val driveRepository: GoogleDriveRepository = GoogleDriveRepository()
) : ViewModel() {

    private val _studioState = MutableStateFlow(StudioUiState())
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    private val _driveAssets = MutableStateFlow<List<NeonAssetSpec>>(emptyList())
    val driveAssets: StateFlow<List<NeonAssetSpec>> = _driveAssets.asStateFlow()

    private val _qcState = MutableStateFlow<QcUiState>(QcUiState.Idle)
    val qcState: StateFlow<QcUiState> = _qcState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _aiImageState = MutableStateFlow<AiImageUiState>(AiImageUiState.Idle)
    val aiImageState: StateFlow<AiImageUiState> = _aiImageState.asStateFlow()

    private val _aiVisionState = MutableStateFlow<AiVisionUiState>(AiVisionUiState.Idle)
    val aiVisionState: StateFlow<AiVisionUiState> = _aiVisionState.asStateFlow()

    private val _driveState = MutableStateFlow(DriveUiState())
    val driveState: StateFlow<DriveUiState> = _driveState.asStateFlow()

    private val _apkDeliveryStatus = MutableStateFlow<String?>(null)
    val apkDeliveryStatus: StateFlow<String?> = _apkDeliveryStatus.asStateFlow()

    val allCatalogAssets: List<NeonAssetSpec>
        get() = NeonCatalog.builtInAssets + _driveAssets.value

    init {
        startPlaybackClock()
        runSevenPassAudit()
    }

    private fun startPlaybackClock() {
        viewModelScope.launch {
            var lastNanos = System.nanoTime()
            while (isActive) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastNanos = now
                _studioState.update { state ->
                    if (!state.isPlaying) {
                        state
                    } else {
                        val nextTime = (state.currentTimeSec + dt * state.playbackSpeed) % Neon3DRenderer.LOOP_DURATION_SEC
                        state.copy(currentTimeSec = nextTime)
                    }
                }
            }
        }
    }

    fun selectAsset(spec: NeonAssetSpec) {
        _studioState.update { state ->
            if (spec.isFrameAsset) {
                state.copy(selectedAsset = spec)
            } else {
                state.copy(selectedAsset = spec, overlayVfx = null)
            }
        }
        runSevenPassAudit()
    }

    fun setOverlayVfx(vfx: NeonAssetSpec?) {
        _studioState.update { it.copy(overlayVfx = vfx) }
        runSevenPassAudit()
    }

    fun setAspectRatio(ratio: FrameAspectRatio) {
        _studioState.update { it.copy(aspectRatio = ratio) }
        runSevenPassAudit()
    }

    fun setResolutionTier(tier: ExportResolutionTier) {
        _studioState.update { it.copy(resolutionTier = tier) }
        runSevenPassAudit()
    }

    fun setBackdrop(backdrop: PreviewBackdrop) {
        _studioState.update { it.copy(backdrop = backdrop) }
    }

    fun setThemePreset(preset: StudioThemePreset) {
        _studioState.update { it.copy(themePreset = preset) }
    }

    fun setRawAlphaOnlyMode(rawOnly: Boolean) {
        _studioState.update {
            it.copy(
                rawAlphaOnlyMode = rawOnly,
                backdrop = if (rawOnly) PreviewBackdrop.CHECKERBOARD_ALPHA else it.backdrop
            )
        }
    }

    fun setPlacedImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bmp = loadBitmapFromUri(context, uri, maxDim = 2048) ?: return@launch
            val displayName = resolveDisplayName(context, uri) ?: "placed_image.png"
            _studioState.update {
                it.copy(
                    placedImageBitmap = bmp,
                    placedImageName = displayName,
                    rawAlphaOnlyMode = false,
                    imageScale = 1.0f,
                    imageOffsetX = 0f,
                    imageOffsetY = 0f
                )
            }
        }
    }

    fun clearPlacedImage() {
        _studioState.update {
            it.copy(
                placedImageBitmap = null,
                placedImageName = null,
                rawAlphaOnlyMode = true,
                bakePlacedImageInExport = false
            )
        }
    }

    fun updateImagePlacementMode(mode: ImagePlacementMode) {
        _studioState.update { it.copy(imagePlacementMode = mode, rawAlphaOnlyMode = false) }
    }

    fun updateImageScale(scale: Float) {
        _studioState.update { it.copy(imageScale = scale) }
    }

    fun updateImageOffset(dx: Float, dy: Float) {
        _studioState.update { it.copy(imageOffsetX = dx, imageOffsetY = dy) }
    }

    fun updateImageOpacity(opacity: Float) {
        _studioState.update { it.copy(imageOpacity = opacity) }
    }

    fun setBakePlacedImageInExport(bake: Boolean) {
        _studioState.update { it.copy(bakePlacedImageInExport = bake) }
    }

    fun togglePlayPause() {
        _studioState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _studioState.update { it.copy(playbackSpeed = speed) }
    }

    fun scrubToTime(timeSec: Float) {
        _studioState.update {
            it.copy(
                isPlaying = false,
                currentTimeSec = timeSec.coerceIn(0f, Neon3DRenderer.LOOP_DURATION_SEC)
            )
        }
    }

    fun updateCornerRadius(value: Float) {
        _studioState.update { it.copy(cornerRadiusFraction = value) }
    }

    fun updateBevelDepth(value: Float) {
        _studioState.update { it.copy(bevelDepthScale = value) }
    }

    fun updateGlowIntensity(value: Float) {
        _studioState.update { it.copy(glowIntensity = value) }
    }

    fun updateParticleDensity(value: Float) {
        _studioState.update { it.copy(particleDensity = value) }
    }

    fun setSequenceFrameCount(count: Int) {
        _studioState.update { it.copy(sequenceFrameCount = count) }
    }

    fun runSevenPassAudit() {
        val snapshot = _studioState.value
        viewModelScope.launch {
            _qcState.value = QcUiState.Auditing
            try {
                val report = ExportAndQcEngine.runSevenPassAudit(
                    params = snapshot.toRenderParams(),
                    aspectRatio = snapshot.aspectRatio,
                    resolutionTier = snapshot.resolutionTier
                )
                _qcState.value = QcUiState.Ready(report)
            } catch (e: Exception) {
                _qcState.value = QcUiState.Error(e.localizedMessage ?: "QC audit failed.")
            }
        }
    }

    fun exportCollectionPackage(context: Context) {
        val snapshot = _studioState.value
        val currentReport = (_qcState.value as? QcUiState.Ready)?.report
        val catalogSnapshot = allCatalogAssets
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting(0.02f, "Initializing ${snapshot.resolutionTier.label} renderer...")
            try {
                val result = ExportAndQcEngine.exportCollectionPackage(
                    context = context.applicationContext,
                    params = snapshot.toRenderParams(),
                    aspectRatio = snapshot.aspectRatio,
                    resolutionTier = snapshot.resolutionTier,
                    sequenceFrameCount = snapshot.sequenceFrameCount,
                    auditReport = currentReport,
                    allCatalogAssets = catalogSnapshot,
                    placedImageBitmap = snapshot.placedImageBitmap,
                    imagePlacementMode = snapshot.imagePlacementMode,
                    imageScale = snapshot.imageScale,
                    imageOffsetX = snapshot.imageOffsetX,
                    imageOffsetY = snapshot.imageOffsetY,
                    imageOpacity = snapshot.imageOpacity,
                    bakePlacedImageInExport = snapshot.bakePlacedImageInExport && !snapshot.rawAlphaOnlyMode,
                    onProgress = { progress, label ->
                        _exportState.value = ExportUiState.Exporting(progress, label)
                    }
                )
                _exportState.value = ExportUiState.Success(result)
            } catch (e: Exception) {
                _exportState.value = ExportUiState.Error(e.localizedMessage ?: "Export failed.")
            }
        }
    }

    fun connectGoogleDrive(
        activity: Activity,
        onResolutionRequired: (PendingIntent) -> Unit
    ) {
        viewModelScope.launch {
            _driveState.update { it.copy(isConnecting = true, errorMessage = null, statusMessage = "Authorizing Google Drive...") }
            when (val outcome = driveRepository.authorizeDrive(activity)) {
                is DriveAuthOutcome.AccessToken -> {
                    _driveState.update {
                        it.copy(
                            accessToken = outcome.token,
                            isConnecting = false,
                            statusMessage = "Google Drive connected. Ready to sync Drive Pack & upload."
                        )
                    }
                    refreshDrivePackList(outcome.token)
                }
                is DriveAuthOutcome.ResolutionRequired -> {
                    _driveState.update { it.copy(isConnecting = false, statusMessage = "Waiting for Google Drive consent...") }
                    onResolutionRequired(outcome.pendingIntent)
                }
                is DriveAuthOutcome.Error -> {
                    _driveState.update {
                        it.copy(isConnecting = false, errorMessage = outcome.message, statusMessage = null)
                    }
                }
            }
        }
    }

    fun onDriveAuthorizationIntentResult(context: Context, data: Intent?) {
        val res = driveRepository.extractAccessTokenFromIntent(context, data)
        res.fold(
            onSuccess = { token ->
                _driveState.update {
                    it.copy(
                        accessToken = token,
                        isConnecting = false,
                        errorMessage = null,
                        statusMessage = "Google Drive connected."
                    )
                }
                refreshDrivePackList(token)
            },
            onFailure = { err ->
                _driveState.update {
                    it.copy(
                        isConnecting = false,
                        errorMessage = err.localizedMessage ?: "Google Drive consent was cancelled."
                    )
                }
            }
        )
    }

    fun refreshDrivePackList(explicitToken: String? = null) {
        val token = explicitToken ?: _driveState.value.accessToken ?: return
        viewModelScope.launch {
            _driveState.update { it.copy(errorMessage = null, statusMessage = "Listing files in Drive Pack folder...") }
            val listRes = driveRepository.listDriveFolderFiles(token)
            listRes.fold(
                onSuccess = { files ->
                    _driveState.update {
                        it.copy(
                            folderFiles = files,
                            statusMessage = "Found ${files.size} items in Drive Pack (${files.count { f -> f.isSupportedForAiAnalysis }} AI-analyzable)."
                        )
                    }
                },
                onFailure = { err ->
                    _driveState.update {
                        it.copy(errorMessage = err.localizedMessage ?: "Unable to read Drive folder.")
                    }
                }
            )
        }
    }

    fun syncAndAnalyzeDrivePack() {
        val token = _driveState.value.accessToken ?: return
        viewModelScope.launch {
            _driveState.update { it.copy(isAnalyzingPack = true, errorMessage = null, analysisProgressLabel = "Fetching Drive Pack index...") }
            val filesResult = driveRepository.listDriveFolderFiles(token)
            val files = filesResult.getOrElse { err ->
                _driveState.update { it.copy(isAnalyzingPack = false, errorMessage = err.localizedMessage ?: "Failed to list Drive folder.") }
                return@launch
            }
            _driveState.update { it.copy(folderFiles = files) }
            val candidates = files.filter { it.isSupportedForAiAnalysis }
            if (candidates.isEmpty()) {
                _driveState.update {
                    it.copy(
                        isAnalyzingPack = false,
                        analysisProgressLabel = null,
                        statusMessage = "No analyzable image/video/JSON files found in folder."
                    )
                }
                return@launch
            }

            val analyzedSpecs = mutableListOf<NeonAssetSpec>()
            candidates.forEachIndexed { idx, item ->
                _driveState.update {
                    it.copy(analysisProgressLabel = "Gemini analyzing ${idx + 1}/${candidates.size}: ${item.name}...")
                }
                val bytesRes = driveRepository.downloadDriveFileBytes(token, item.id)
                val bytes = bytesRes.getOrNull()
                if (bytes != null) {
                    val aiRes = aiRepository.analyzeDrivePackAsset(
                        driveFileId = item.id,
                        fileName = item.name,
                        mimeType = item.mimeType,
                        bytes = bytes,
                        index = idx
                    )
                    aiRes.getOrNull()?.let { analyzedSpecs += it }
                }
            }

            if (analyzedSpecs.isNotEmpty()) {
                _driveAssets.value = analyzedSpecs
                _studioState.update { it.copy(selectedAsset = analyzedSpecs.first()) }
                runSevenPassAudit()
            }
            _driveState.update {
                it.copy(
                    isAnalyzingPack = false,
                    analysisProgressLabel = null,
                    statusMessage = "Synced & analyzed ${analyzedSpecs.size} Drive Pack effects into 3D FullHD/16K engine!"
                )
            }
        }
    }

    fun uploadExportedPackageToDrive(context: Context) {
        val token = _driveState.value.accessToken ?: return
        val existingExport = (_exportState.value as? ExportUiState.Success)?.result
        viewModelScope.launch {
            _driveState.update {
                it.copy(isUploadingPackage = true, errorMessage = null, uploadProgressLabel = "Preparing FullHD/16K delivery files...")
            }
            val packageResult = if (existingExport != null) {
                existingExport
            } else {
                val snapshot = _studioState.value
                val currentReport = (_qcState.value as? QcUiState.Ready)?.report
                try {
                    val built = ExportAndQcEngine.exportCollectionPackage(
                        context = context.applicationContext,
                        params = snapshot.toRenderParams(),
                        aspectRatio = snapshot.aspectRatio,
                        resolutionTier = snapshot.resolutionTier,
                        sequenceFrameCount = snapshot.sequenceFrameCount,
                        auditReport = currentReport,
                        allCatalogAssets = allCatalogAssets,
                        placedImageBitmap = snapshot.placedImageBitmap,
                        imagePlacementMode = snapshot.imagePlacementMode,
                        imageScale = snapshot.imageScale,
                        imageOffsetX = snapshot.imageOffsetX,
                        imageOffsetY = snapshot.imageOffsetY,
                        imageOpacity = snapshot.imageOpacity,
                        bakePlacedImageInExport = snapshot.bakePlacedImageInExport && !snapshot.rawAlphaOnlyMode,
                        onProgress = { _, label ->
                            _driveState.update { d -> d.copy(uploadProgressLabel = label) }
                        }
                    )
                    _exportState.value = ExportUiState.Success(built)
                    built
                } catch (e: Exception) {
                    _driveState.update {
                        it.copy(isUploadingPackage = false, uploadProgressLabel = null, errorMessage = e.localizedMessage)
                    }
                    return@launch
                }
            }

            val folderName = "NeonVFX_3D_${_studioState.value.selectedAsset.code}_${_studioState.value.resolutionTier.name}"
            _driveState.update { it.copy(uploadProgressLabel = "Creating Google Drive folder '$folderName'...") }
            val createdFolder = driveRepository.createDriveFolder(token, folderName).getOrElse { err ->
                _driveState.update {
                    it.copy(isUploadingPackage = false, uploadProgressLabel = null, errorMessage = err.localizedMessage)
                }
                return@launch
            }

            val filesToUpload = packageResult.allGeneratedFiles
            var uploadedCount = 0
            filesToUpload.forEachIndexed { idx, (file, mime) ->
                _driveState.update {
                    it.copy(uploadProgressLabel = "Uploading ${idx + 1}/${filesToUpload.size}: ${file.name}")
                }
                val upRes = driveRepository.uploadFileMultipart(
                    accessToken = token,
                    file = file,
                    mimeType = mime,
                    parentFolderId = createdFolder.id
                )
                if (upRes.isSuccess) uploadedCount++
            }

            _driveState.update {
                it.copy(
                    isUploadingPackage = false,
                    uploadProgressLabel = null,
                    uploadedFolderUrl = createdFolder.webViewLink,
                    statusMessage = "Uploaded $uploadedCount/${filesToUpload.size} rendered files to Google Drive!"
                )
            }
        }
    }

    fun uploadFullApkToDrive(context: Context) {
        val token = _driveState.value.accessToken ?: return
        val apkFile = driveRepository.getInstalledApkFile(context)
        if (apkFile == null) {
            _driveState.update { it.copy(errorMessage = "Installed APK sourceDir is unavailable on this device.") }
            return
        }
        viewModelScope.launch {
            _driveState.update {
                it.copy(
                    isUploadingApk = true,
                    errorMessage = null,
                    uploadProgressLabel = "Uploading NeonVFX_Studio_Full.apk (${apkFile.length() / (1024 * 1024)} MB)..."
                )
            }
            val folderRes = driveRepository.createDriveFolder(token, "NeonVFX_Studio_APK_Release")
            val parentId = folderRes.getOrNull()?.id
            val uploadRes = driveRepository.uploadFileMultipart(
                accessToken = token,
                file = apkFile,
                mimeType = "application/vnd.android.package-archive",
                parentFolderId = parentId,
                uploadFileName = "NeonVFX_Studio_Full_16K_3D.apk"
            )
            uploadRes.fold(
                onSuccess = { uploaded ->
                    _driveState.update {
                        it.copy(
                            isUploadingApk = false,
                            uploadProgressLabel = null,
                            uploadedApkUrl = uploaded.webViewLink,
                            statusMessage = "Full APK uploaded to Google Drive: ${uploaded.name}"
                        )
                    }
                },
                onFailure = { err ->
                    _driveState.update {
                        it.copy(
                            isUploadingApk = false,
                            uploadProgressLabel = null,
                            errorMessage = err.localizedMessage ?: "APK upload failed."
                        )
                    }
                }
            )
        }
    }

    fun saveApkDirectlyToPhoneDownloads(context: Context) {
        viewModelScope.launch {
            _apkDeliveryStatus.value = "Saving APK to Phone Downloads..."
            val res = ExportAndQcEngine.saveApkToPhoneDownloads(context.applicationContext)
            _apkDeliveryStatus.value = res.fold(
                onSuccess = { msg -> msg },
                onFailure = { err -> "Error: ${err.localizedMessage ?: "Failed to save APK"}" }
            )
        }
    }

    fun shareApkFileDirectly(context: Context, onUriReady: (Uri) -> Unit) {
        viewModelScope.launch {
            _apkDeliveryStatus.value = "Preparing APK for direct sharing..."
            val res = ExportAndQcEngine.prepareApkShareUri(context.applicationContext)
            res.fold(
                onSuccess = { uri ->
                    _apkDeliveryStatus.value = "APK ready to share via Telegram, WhatsApp, or Files."
                    onUriReady(uri)
                },
                onFailure = { err ->
                    _apkDeliveryStatus.value = "Error: ${err.localizedMessage ?: "Unable to prepare APK"}"
                }
            )
        }
    }

    fun exportZipPackageDirectly(context: Context, onUriReady: (Uri) -> Unit) {
        viewModelScope.launch {
            val currentExport = (_exportState.value as? ExportUiState.Success)?.result
            if (currentExport == null) {
                _apkDeliveryStatus.value = "Building FullHD/16K package before zipping..."
                val snapshot = _studioState.value
                val currentReport = (_qcState.value as? QcUiState.Ready)?.report
                val built = try {
                    ExportAndQcEngine.exportCollectionPackage(
                        context = context.applicationContext,
                        params = snapshot.toRenderParams(),
                        aspectRatio = snapshot.aspectRatio,
                        resolutionTier = snapshot.resolutionTier,
                        sequenceFrameCount = snapshot.sequenceFrameCount,
                        auditReport = currentReport,
                        allCatalogAssets = allCatalogAssets,
                        placedImageBitmap = snapshot.placedImageBitmap,
                        imagePlacementMode = snapshot.imagePlacementMode,
                        imageScale = snapshot.imageScale,
                        imageOffsetX = snapshot.imageOffsetX,
                        imageOffsetY = snapshot.imageOffsetY,
                        imageOpacity = snapshot.imageOpacity,
                        bakePlacedImageInExport = snapshot.bakePlacedImageInExport && !snapshot.rawAlphaOnlyMode,
                        onProgress = { progress, status ->
                            _exportState.value = ExportUiState.Exporting(progress, status)
                        }
                    ).also { _exportState.value = ExportUiState.Success(it) }
                } catch (e: Exception) {
                    _apkDeliveryStatus.value = "Error: ${e.localizedMessage ?: "Package render failed"}"
                    return@launch
                }
                val zipRes = ExportAndQcEngine.createZipPackageAndSaveToDownloads(context.applicationContext, built)
                zipRes.fold(
                    onSuccess = { (uri, note) ->
                        _apkDeliveryStatus.value = "Saved ZIP ($note)"
                        onUriReady(uri)
                    },
                    onFailure = { err ->
                        _apkDeliveryStatus.value = "Error: ${err.localizedMessage ?: "ZIP failed"}"
                    }
                )
            } else {
                _apkDeliveryStatus.value = "Compressing package to ZIP..."
                val zipRes = ExportAndQcEngine.createZipPackageAndSaveToDownloads(context.applicationContext, currentExport)
                zipRes.fold(
                    onSuccess = { (uri, note) ->
                        _apkDeliveryStatus.value = "Saved ZIP ($note)"
                        onUriReady(uri)
                    },
                    onFailure = { err ->
                        _apkDeliveryStatus.value = "Error: ${err.localizedMessage ?: "ZIP failed"}"
                    }
                )
            }
        }
    }

    fun generateAiConcept(
        prompt: String,
        referenceBitmap: Bitmap?,
        modelOption: AiImageModelOption,
        aspectRatio: AiAspectRatioOption,
        imageSize: AiImageSizeOption
    ) {
        if (prompt.isBlank()) {
            _aiImageState.value = AiImageUiState.Error("Please enter a prompt describing the 3D neon frame or VFX effect.")
            return
        }
        viewModelScope.launch {
            _aiImageState.value = AiImageUiState.Loading
            val res = aiRepository.generateOrEditConceptImage(
                prompt = prompt,
                referenceBitmap = referenceBitmap,
                modelOption = modelOption,
                aspectRatio = aspectRatio,
                imageSize = imageSize
            )
            _aiImageState.value = res.fold(
                onSuccess = { bmp -> AiImageUiState.Success(bmp, modelOption.label) },
                onFailure = { err -> AiImageUiState.Error(err.message ?: "Image generation failed.") }
            )
        }
    }

    fun analyzeWithGemini(
        context: Context,
        prompt: String,
        reasoningMode: AiReasoningMode,
        imageBitmap: Bitmap?,
        videoUri: Uri?,
        includeLiveViewportSnapshot: Boolean
    ) {
        viewModelScope.launch {
            _aiVisionState.value = AiVisionUiState.Loading
            val effectiveBitmap = if (includeLiveViewportSnapshot && imageBitmap == null) {
                renderSnapshotBitmap(_studioState.value)
            } else {
                imageBitmap
            }
            val finalPrompt = buildString {
                append(prompt.ifBlank { "Analyze the 3D neon frame geometry, PBR glass/metal materials, volumetric glow, and suggest exact parameter improvements." })
                append("\n\nActive Studio Context: ${_studioState.value.selectedAsset.title} (${_studioState.value.selectedAsset.persianTitle}), Aspect: ${_studioState.value.aspectRatio.label}, Tier: ${_studioState.value.resolutionTier.label}, Glow: ${_studioState.value.glowIntensity}.")
            }
            val res = aiRepository.analyzeVisualOrDirectorPrompt(
                context = context.applicationContext,
                prompt = finalPrompt,
                reasoningMode = reasoningMode,
                imageBitmap = effectiveBitmap,
                videoUri = videoUri
            )
            _aiVisionState.value = res.fold(
                onSuccess = { text -> AiVisionUiState.Success(text, reasoningMode) },
                onFailure = { err -> AiVisionUiState.Error(err.message ?: "Analysis failed.") }
            )
        }
    }

    private suspend fun renderSnapshotBitmap(state: StudioUiState): Bitmap = withContext(Dispatchers.Default) {
        val w = 720
        val h = (720f / state.aspectRatio.ratio).toInt().coerceIn(360, 1280)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(android.graphics.Color.rgb(8, 12, 22))
        if (!state.rawAlphaOnlyMode && state.placedImageBitmap != null) {
            ExportAndQcEngine.drawPlacedImageToCanvas(
                canvas = canvas,
                width = w.toFloat(),
                height = h.toFloat(),
                bitmap = state.placedImageBitmap,
                placementMode = state.imagePlacementMode,
                cornerRadiusFraction = state.cornerRadiusFraction,
                scale = state.imageScale,
                offsetXFraction = state.imageOffsetX,
                offsetYFraction = state.imageOffsetY,
                opacity = state.imageOpacity
            )
        }
        Neon3DRenderer.renderToCanvas(
            canvas,
            w.toFloat(),
            h.toFloat(),
            state.toRenderParams().copy(clearTransparentBackground = false)
        )
        bmp
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    if (w > maxDim || h > maxDim) {
                        val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
                        decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
                    }
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

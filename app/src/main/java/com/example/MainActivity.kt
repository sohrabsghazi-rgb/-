package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ExportResolutionTier
import com.example.model.StudioThemePreset
import com.example.ui.DriveUiState
import com.example.ui.NeonStudioViewModel
import com.example.ui.StudioUiState
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.QualityExportScreen
import com.example.ui.screens.ViewportScreen
import com.example.ui.theme.LocalStudioTheme
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NeonVfxStudioTheme
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlatinumText
import com.example.ui.theme.VazirmatnFontFamily

enum class StudioDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    VIEWPORT("Viewport", Icons.Rounded.MovieFilter, Icons.Outlined.MovieFilter, "nav_viewport"),
    COLLECTION("Collection", Icons.Rounded.Collections, Icons.Outlined.Collections, "nav_collection"),
    QC_EXPORT("Drive & 16K", Icons.Rounded.Verified, Icons.Outlined.Verified, "nav_qc_export"),
    AI_STUDIO("Gemini AI", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai_studio")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val studioViewModel: NeonStudioViewModel = viewModel()
            val studioState by studioViewModel.studioState.collectAsStateWithLifecycle()

            NeonVfxStudioTheme(preset = studioState.themePreset) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = studioState.themePreset.backgroundVoid
                ) {
                    NeonVfxStudioApp(
                        studioState = studioState,
                        viewModel = studioViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonVfxStudioApp(
    studioState: StudioUiState,
    viewModel: NeonStudioViewModel = viewModel()
) {
    val activeTheme = LocalStudioTheme.current
    val context = LocalContext.current
    var activeTab by rememberSaveable { mutableStateOf(StudioDestination.VIEWPORT) }
    var showPowerMenu by remember { mutableStateOf(false) }

    val qcState by viewModel.qcState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val aiImageState by viewModel.aiImageState.collectAsStateWithLifecycle()
    val aiVisionState by viewModel.aiVisionState.collectAsStateWithLifecycle()
    val driveState by viewModel.driveState.collectAsStateWithLifecycle()

    val quickImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPlacedImageFromUri(context, uri)
            activeTab = StudioDestination.VIEWPORT
            showPowerMenu = false
        }
    }

    if (activeTab != StudioDestination.VIEWPORT) {
        BackHandler { activeTab = StudioDestination.VIEWPORT }
    }

    if (showPowerMenu) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPowerMenu = false },
            sheetState = sheetState,
            containerColor = activeTheme.panelSurface
        ) {
            ProStudioPowerMenuContent(
                studioState = studioState,
                driveState = driveState,
                viewModel = viewModel,
                onPickImage = {
                    quickImagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onNavigateToDriveExport = {
                    showPowerMenu = false
                    activeTab = StudioDestination.QC_EXPORT
                },
                onDismiss = { showPowerMenu = false }
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activeTheme.backgroundVoid)
            ) {
                NavigationRail(
                    containerColor = activeTheme.panelSurface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .navigationBarsPadding()
                ) {
                    IconButton(
                        onClick = { showPowerMenu = true },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .testTag("rail_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Open Pro Studio Menu",
                            tint = activeTheme.primaryColor
                        )
                    }
                    StudioDestination.entries.forEach { dest ->
                        val selected = activeTab == dest
                        NavigationRailItem(
                            selected = selected,
                            onClick = { activeTab = dest },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = activeTheme.primaryColor,
                                selectedTextColor = activeTheme.primaryColor,
                                indicatorColor = activeTheme.deckSurface,
                                unselectedIconColor = MutedSlate,
                                unselectedTextColor = MutedSlate
                            ),
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .navigationBarsPadding()
                ) {
                    StudioCommandTopBar(
                        studioState = studioState,
                        onOpenMenu = { showPowerMenu = true },
                        onPickImage = {
                            quickImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        DestinationContent(
                            activeTab = activeTab,
                            studioState = studioState,
                            qcState = qcState,
                            exportState = exportState,
                            driveState = driveState,
                            aiImageState = aiImageState,
                            aiVisionState = aiVisionState,
                            viewModel = viewModel,
                            onNavigate = { activeTab = it },
                            onOpenStudioMenu = { showPowerMenu = true }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = activeTheme.backgroundVoid,
                contentWindowInsets = WindowInsets.statusBars,
                topBar = {
                    StudioCommandTopBar(
                        studioState = studioState,
                        onOpenMenu = { showPowerMenu = true },
                        onPickImage = {
                            quickImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = activeTheme.panelSurface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        StudioDestination.entries.forEach { dest ->
                            val selected = activeTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = activeTheme.primaryColor,
                                    selectedTextColor = activeTheme.primaryColor,
                                    indicatorColor = activeTheme.deckSurface,
                                    unselectedIconColor = MutedSlate,
                                    unselectedTextColor = MutedSlate
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    DestinationContent(
                        activeTab = activeTab,
                        studioState = studioState,
                        qcState = qcState,
                        exportState = exportState,
                        driveState = driveState,
                        aiImageState = aiImageState,
                        aiVisionState = aiVisionState,
                        viewModel = viewModel,
                        onNavigate = { activeTab = it },
                        onOpenStudioMenu = { showPowerMenu = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioCommandTopBar(
    studioState: StudioUiState,
    onOpenMenu: () -> Unit,
    onPickImage: () -> Unit
) {
    val activeTheme = LocalStudioTheme.current
    Surface(
        color = activeTheme.panelSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, activeTheme.borderStroke.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = activeTheme.deckSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("studio_menu_button")
                        .clickable(onClick = onOpenMenu)
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Open Studio Theme & Power Menu",
                            tint = activeTheme.primaryColor
                        )
                    }
                }
                Column {
                    Text(
                        text = "NEONVFX 3D STUDIO",
                        style = MaterialTheme.typography.titleMedium,
                        color = PlatinumText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${studioState.themePreset.label} · ${studioState.resolutionTier.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = activeTheme.primaryColor
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = activeTheme.primaryColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("topbar_add_image_button")
                        .clickable(onClick = onPickImage)
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Add Image for Frame Placement",
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (studioState.placedImageBitmap != null) "تغییر عکس" else "جاگذاری عکس",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = VazirmatnFontFamily),
                            color = PlatinumText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProStudioPowerMenuContent(
    studioState: StudioUiState,
    driveState: DriveUiState,
    viewModel: NeonStudioViewModel,
    onPickImage: () -> Unit,
    onNavigateToDriveExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeTheme = LocalStudioTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PRO STUDIO COMMAND MENU · منوی قدرتمند و تم",
            style = MaterialTheme.typography.titleMedium,
            color = activeTheme.primaryColor,
            fontWeight = FontWeight.Bold
        )

        // Theme Switcher
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.ColorLens, contentDescription = "Studio Themes", tint = activeTheme.secondaryColor)
            Text(
                text = "انتخاب تم استودیویی (4 Pro Themes)",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                color = PlatinumText
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StudioThemePreset.entries.forEach { preset ->
                val selected = studioState.themePreset == preset
                Surface(
                    color = if (selected) preset.primaryColor.copy(alpha = 0.2f) else activeTheme.deckSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .testTag("theme_preset_${preset.name.lowercase()}")
                        .clickable { viewModel.setThemePreset(preset) }
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) preset.primaryColor else activeTheme.borderStroke,
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(preset.primaryColor, preset.secondaryColor))
                                )
                        )
                        Column {
                            Text(preset.label, style = MaterialTheme.typography.labelLarge, color = PlatinumText)
                            Text(
                                preset.persianLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                                color = MutedSlate
                            )
                        }
                    }
                }
            }
        }

        // Image Placement Quick Action
        Button(
            onClick = onPickImage,
            colors = ButtonDefaults.buttonColors(
                containerColor = activeTheme.primaryColor,
                contentColor = activeTheme.backgroundVoid
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("menu_add_image_button")
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Pick Image to Place Inside Frame")
            Text(
                text = "  افزودن / جایگزینی فایل تصویر داخل قاب",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = VazirmatnFontFamily),
                fontWeight = FontWeight.Bold
            )
        }

        // Raw Pure Alpha Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "خام بودن تمام افکت‌ها (Raw Pure Alpha)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
                    color = PlatinumText
                )
                Text(
                    text = "مرکز و پس‌زمینه کاملاً شفاف برای استفاده روی هر تصویر/ویدیو",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
                    color = MutedSlate
                )
            }
            Switch(
                checked = studioState.rawAlphaOnlyMode,
                onCheckedChange = viewModel::setRawAlphaOnlyMode
            )
        }

        // Resolution Selector
        Text(
            text = "سطح رندر (FullHD / 4K / 16K Master)",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
            color = PlatinumText
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
                    label = { Text(tier.label) }
                )
            }
        }

        // Quick Drive / APK Delivery Jump
        OutlinedButton(
            onClick = onNavigateToDriveExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("menu_open_drive_apk_button")
        ) {
            Icon(Icons.Rounded.Android, contentDescription = "Drive and APK Upload", tint = activeTheme.secondaryColor)
            Text(
                text = if (driveState.isConnected) "  آپلود APK کامل و پکیج ۱۶K به درایو (متصل)" else "  اتصال به گوگل درایو و آپلود نسخه کامل APK",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = VazirmatnFontFamily)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DestinationContent(
    activeTab: StudioDestination,
    studioState: StudioUiState,
    qcState: com.example.ui.QcUiState,
    exportState: com.example.ui.ExportUiState,
    driveState: DriveUiState,
    aiImageState: com.example.ui.AiImageUiState,
    aiVisionState: com.example.ui.AiVisionUiState,
    viewModel: NeonStudioViewModel,
    onNavigate: (StudioDestination) -> Unit,
    onOpenStudioMenu: () -> Unit
) {
    when (activeTab) {
        StudioDestination.VIEWPORT -> ViewportScreen(
            studioState = studioState,
            qcState = qcState,
            viewModel = viewModel,
            onOpenCollection = { onNavigate(StudioDestination.COLLECTION) },
            onOpenExport = { onNavigate(StudioDestination.QC_EXPORT) },
            onOpenStudioMenu = onOpenStudioMenu
        )
        StudioDestination.COLLECTION -> CollectionScreen(
            studioState = studioState,
            driveState = driveState,
            viewModel = viewModel,
            onLoadedToViewport = { onNavigate(StudioDestination.VIEWPORT) }
        )
        StudioDestination.QC_EXPORT -> QualityExportScreen(
            studioState = studioState,
            qcState = qcState,
            exportState = exportState,
            driveState = driveState,
            viewModel = viewModel
        )
        StudioDestination.AI_STUDIO -> AiStudioScreen(
            aiImageState = aiImageState,
            aiVisionState = aiVisionState,
            viewModel = viewModel
        )
    }
}

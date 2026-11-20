package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiAspectRatioOption
import com.example.ai.AiImageModelOption
import com.example.ai.AiImageSizeOption
import com.example.ai.AiReasoningMode
import com.example.ui.AiImageUiState
import com.example.ui.AiVisionUiState
import com.example.ui.NeonStudioViewModel
import com.example.ui.theme.CarbonPanel
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DeckSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlasmaViolet
import com.example.ui.theme.PlatinumText
import kotlinx.coroutines.launch

@Composable
fun AiStudioScreen(
    aiImageState: AiImageUiState,
    aiVisionState: AiVisionUiState,
    viewModel: NeonStudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Image Gen/Edit state
    var imagePrompt by remember {
        mutableStateOf("Photorealistic 3D beveled cyan glass neon rounded rectangular frame with volumetric plasma glow and champagne gold particles on pure black studio void")
    }
    var selectedImageModel by remember { mutableStateOf(AiImageModelOption.NANO_BANANA) }
    var selectedAspectRatio by remember { mutableStateOf(AiAspectRatioOption.RATIO_16_9) }
    var selectedImageSize by remember { mutableStateOf(AiImageSizeOption.SIZE_1K) }
    var editReferenceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Multimodal Vision / Video / Reasoning state
    var visionPrompt by remember {
        mutableStateOf("Analyze the 3D neon geometry, glass refraction, volumetric glow falloff, and particle dynamics. Recommend shader and lighting improvements.")
    }
    var selectedReasoningMode by remember { mutableStateOf(AiReasoningMode.STANDARD) }
    var pickedVisionBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pickedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var includeLiveSnapshot by remember { mutableStateOf(true) }

    val editPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                editReferenceBitmap = viewModel.loadBitmapFromUri(context, uri)
            }
        }
    }

    val visionPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                pickedVisionBitmap = viewModel.loadBitmapFromUri(context, uri)
                pickedVideoUri = null
            }
        }
    }

    val visionVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedVideoUri = uri
            pickedVisionBitmap = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Gemini Concept Image Generation & Editing
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonPanel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
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
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Gemini Image Studio",
                        tint = ElectricCyan
                    )
                    Column {
                        Text(
                            text = "AI 3D CONCEPT GENERATOR & EDITOR",
                            style = MaterialTheme.typography.titleMedium,
                            color = PlatinumText
                        )
                        Text(
                            text = "Powered by Firebase AI Logic · Zero API Key Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }

                OutlinedTextField(
                    value = imagePrompt,
                    onValueChange = { imagePrompt = it },
                    label = { Text("3D Neon / VFX Prompt or Edit Instruction") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_image_prompt_input")
                )

                Text("MODEL ENGINE", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiImageModelOption.entries.forEach { option ->
                        FilterChip(
                            selected = selectedImageModel == option,
                            onClick = { selectedImageModel = option },
                            label = { Text(option.label) },
                            modifier = Modifier.testTag("ai_model_${option.name.lowercase()}")
                        )
                    }
                }

                Text("ASPECT RATIO", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiAspectRatioOption.entries.forEach { ar ->
                        FilterChip(
                            selected = selectedAspectRatio == ar,
                            onClick = { selectedAspectRatio = ar },
                            label = { Text(ar.label) },
                            modifier = Modifier.testTag("ai_ar_${ar.label}")
                        )
                    }
                }

                Text("RESOLUTION TIER", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiImageSizeOption.entries.forEach { sz ->
                        FilterChip(
                            selected = selectedImageSize == sz,
                            onClick = { selectedImageSize = sz },
                            label = { Text(sz.label) },
                            modifier = Modifier.testTag("ai_size_${sz.label}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            editPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_edit_reference_button")
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Pick reference photo", modifier = Modifier.size(16.dp))
                        Text(
                            text = if (editReferenceBitmap != null) " Reference Loaded" else " Pick Photo to Edit",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (editReferenceBitmap != null) {
                        IconButton(
                            onClick = { editReferenceBitmap = null },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("clear_edit_reference_button")
                        ) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear reference photo", tint = MutedSlate)
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.generateAiConcept(
                            prompt = imagePrompt,
                            referenceBitmap = editReferenceBitmap,
                            modelOption = selectedImageModel,
                            aspectRatio = selectedAspectRatio,
                            imageSize = selectedImageSize
                        )
                    },
                    enabled = aiImageState !is AiImageUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianVoid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_ai_image_button")
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = "Generate Concept Image")
                    Text(
                        text = if (editReferenceBitmap != null) "  Edit Image with Gemini" else "  Generate Concept Art",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (aiImageState) {
                    is AiImageUiState.Idle -> Unit
                    is AiImageUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ElectricCyan)
                            Text("Synthesizing image via ${selectedImageModel.label} (30–60s)...", style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                        }
                    }
                    is AiImageUiState.Error -> {
                        Text(
                            text = aiImageState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonAlert
                        )
                    }
                    is AiImageUiState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Generated via ${aiImageState.modelUsed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                            Image(
                                bitmap = aiImageState.bitmap.asImageBitmap(),
                                contentDescription = "Gemini Generated 3D Neon Concept",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Multimodal Image/Video Analyzer & CGI Technical Director
        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonPanel),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
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
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = "Multimodal Vision & Video Director",
                        tint = PlasmaViolet
                    )
                    Column {
                        Text(
                            text = "MULTIMODAL VISION, VIDEO & CGI DIRECTOR",
                            style = MaterialTheme.typography.titleMedium,
                            color = PlatinumText
                        )
                        Text(
                            text = "Analyze photos, video clips (<18MB), or live 3D viewport renders",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiReasoningMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedReasoningMode == mode,
                            onClick = { selectedReasoningMode = mode },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (mode == AiReasoningMode.LOW_LATENCY) Icons.Rounded.Bolt else Icons.Rounded.Psychology,
                                    contentDescription = mode.label,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("reasoning_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            visionPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_vision_photo_button")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = "Analyze Photo", modifier = Modifier.size(16.dp))
                        Text(
                            text = if (pickedVisionBitmap != null) " Photo Ready" else " Analyze Photo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            visionVideoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_vision_video_button")
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = "Analyze Video", modifier = Modifier.size(16.dp))
                        Text(
                            text = if (pickedVideoUri != null) " Video Ready" else " Analyze Video",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Include Live 3D Viewport Snapshot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlatinumText
                    )
                    Switch(
                        checked = includeLiveSnapshot,
                        onCheckedChange = { includeLiveSnapshot = it },
                        modifier = Modifier.testTag("toggle_live_snapshot")
                    )
                }

                OutlinedTextField(
                    value = visionPrompt,
                    onValueChange = { visionPrompt = it },
                    label = { Text("Analysis Prompt or CGI Technical Question") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_vision_prompt_input")
                )

                Button(
                    onClick = {
                        viewModel.analyzeWithGemini(
                            context = context,
                            prompt = visionPrompt,
                            reasoningMode = selectedReasoningMode,
                            imageBitmap = pickedVisionBitmap,
                            videoUri = pickedVideoUri,
                            includeLiveViewportSnapshot = includeLiveSnapshot
                        )
                    },
                    enabled = aiVisionState !is AiVisionUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = PlasmaViolet, contentColor = PlatinumText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("run_ai_analysis_button")
                ) {
                    Text(
                        text = "Run ${selectedReasoningMode.label}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (aiVisionState) {
                    is AiVisionUiState.Idle -> Unit
                    is AiVisionUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PlasmaViolet)
                            Text("Running ${selectedReasoningMode.badge}...", style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                        }
                    }
                    is AiVisionUiState.Error -> {
                        Text(
                            text = aiVisionState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonAlert
                        )
                    }
                    is AiVisionUiState.Success -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "MODEL: ${aiVisionState.mode.badge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneGold
                                )
                                Text(
                                    text = aiVisionState.response,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PlatinumText
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

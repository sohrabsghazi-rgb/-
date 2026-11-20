package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.model.EffectArchetype
import com.example.model.NeonAssetSpec
import com.example.model.NeonEffectId
import com.example.model.NeonModelCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AiAspectRatioOption(val label: String, val sdkValue: AspectRatio) {
    RATIO_1_1("1:1", AspectRatio.SQUARE_1x1),
    RATIO_3_4("3:4", AspectRatio.PORTRAIT_3x4),
    RATIO_4_3("4:3", AspectRatio.LANDSCAPE_4x3),
    RATIO_9_16("9:16", AspectRatio.PORTRAIT_9x16),
    RATIO_16_9("16:9", AspectRatio.LANDSCAPE_16x9)
}

enum class AiImageSizeOption(val label: String, val sdkValue: ImageSize) {
    SIZE_1K("1K", ImageSize.SIZE_1K),
    SIZE_2K("2K", ImageSize.SIZE_2K),
    SIZE_4K("4K", ImageSize.SIZE_4K)
}

enum class AiImageModelOption(val modelId: String, val label: String) {
    NANO_BANANA("gemini-nano-banana-2.1", "Nano Banana 2.1 (Fast)"),
    PRO_IMAGE("gemini-3-pro-image", "Studio Pro Image")
}

enum class AiReasoningMode(val modelId: String, val label: String, val badge: String) {
    STANDARD("gemini-3.8-flash", "Standard Vision", "gemini-3.8-flash"),
    HIGH_REASONING("gemini-4-argon", "Deep CGI Reasoning", "gemini-4-argon"),
    LOW_LATENCY("gemini-3.5-flash-lite", "Low-Latency Flash", "gemini-3.5-flash-lite")
}

class GeminiStudioRepository {
    companion object {
        private const val TAG = "GeminiStudioRepo"
        private const val MAX_INLINE_BYTES = 18 * 1024 * 1024 // Keep under 20MB inline limit
    }

    private fun createImageModel(
        modelOption: AiImageModelOption,
        aspectRatio: AiAspectRatioOption,
        imageSize: AiImageSizeOption
    ): GenerativeModel {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio.sdkValue)
                .setImageSize(imageSize.sdkValue)
                .build()
        }
        return Firebase.ai.generativeModel(
            modelName = modelOption.modelId,
            generationConfig = config
        )
    }

    suspend fun generateOrEditConceptImage(
        prompt: String,
        referenceBitmap: Bitmap?,
        modelOption: AiImageModelOption,
        aspectRatio: AiAspectRatioOption,
        imageSize: AiImageSizeOption
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val model = createImageModel(modelOption, aspectRatio, imageSize)
            val bitmap = withTimeout(60_000L) {
                val response = if (referenceBitmap != null) {
                    model.generateContent(
                        content {
                            image(referenceBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    model.generateContent(prompt)
                }
                response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()?.image
            }
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Gemini did not return an image. Check prompt or project billing plan."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("Image generation timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Image generation failed."))
        }
    }

    suspend fun analyzeVisualOrDirectorPrompt(
        context: Context,
        prompt: String,
        reasoningMode: AiReasoningMode,
        imageBitmap: Bitmap? = null,
        videoUri: Uri? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = Firebase.ai.generativeModel(modelName = reasoningMode.modelId)
            val responseText = withTimeout(60_000L) {
                val inputContent = content {
                    if (imageBitmap != null) {
                        image(imageBitmap)
                    }
                    if (videoUri != null) {
                        val mime = context.contentResolver.getType(videoUri) ?: "video/mp4"
                        val bytes = context.contentResolver.openInputStream(videoUri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("Unable to read selected video.")
                        if (bytes.size > MAX_INLINE_BYTES) {
                            throw IllegalArgumentException("Video exceeds 18MB inline limit (${bytes.size / (1024 * 1024)}MB). Choose a shorter clip.")
                        }
                        inlineData(bytes, mime)
                    }
                    text(prompt)
                }
                model.generateContent(inputContent).text
            }
            if (!responseText.isNullOrBlank()) {
                Result.success(responseText)
            } else {
                Result.failure(IllegalStateException("Model returned an empty response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Analysis timed out", e)
            Result.failure(Exception("Analysis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI analysis failed."))
        }
    }

    suspend fun analyzeDrivePackAsset(
        driveFileId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        index: Int
    ): Result<NeonAssetSpec> = withContext(Dispatchers.IO) {
        try {
            val archetypeNames = EffectArchetype.entries.map { it.name }
            val schema = Schema.obj(
                mapOf(
                    "title" to Schema.string("Short English title of the 3D VFX / neon frame effect"),
                    "persianTitle" to Schema.string("Short natural Persian title"),
                    "persianDescription" to Schema.string("Detailed Persian technical description of geometry, material, lighting and 3D motion"),
                    "archetype" to Schema.enumeration(archetypeNames, "Closest procedural 3D archetype"),
                    "primaryHex" to Schema.string("Main luminous 6-char hex color like #00E5FF"),
                    "secondaryHex" to Schema.string("Secondary accent 6-char hex color like #7C3AED"),
                    "coreHex" to Schema.string("Hot core highlight 6-char hex color like #FFFFFF"),
                    "chassisHex" to Schema.string("Dark metallic/glass base 6-char hex color like #0A1628"),
                    "animationSummary" to Schema.string("Concise English summary of 15s seamless animation"),
                    "loopHarmonics" to Schema.integer("Integer loop cycles in 15s between 1 and 5")
                )
            )
            val config = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
            val model = Firebase.ai.generativeModel(
                modelName = AiReasoningMode.STANDARD.modelId,
                generationConfig = config
            )
            val prompt = """
                Analyze this effect asset from the user's Google Drive VFX Pack ("$fileName", mime: $mimeType).
                Classify its 3D geometry, glass/metal/plasma lighting, volumetric atmosphere, and motion profile so our procedural 16K 3D renderer recreates it as a pure raw transparent-alpha asset.
            """.trimIndent()

            val rawJson = withTimeout(60_000L) {
                val input = content {
                    if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                        inlineData(bytes, mimeType)
                    } else {
                        text("File content preview:\n" + bytes.decodeToString().take(4000))
                    }
                    text(prompt)
                }
                model.generateContent(input).text
            } ?: throw IllegalStateException("Gemini returned empty structured JSON.")

            val obj = JSONObject(rawJson)
            val archetypeStr = obj.optString("archetype", EffectArchetype.FRAME_GLASS_BEVEL_3D.name)
            val archetype = EffectArchetype.entries.firstOrNull { it.name == archetypeStr }
                ?: EffectArchetype.FRAME_GLASS_BEVEL_3D

            val spec = NeonAssetSpec(
                key = "DRIVE_${index}_${driveFileId.take(8)}",
                id = NeonEffectId.DRIVE_CUSTOM,
                archetype = archetype,
                code = "DRV-${"%02d".format(index + 1)}",
                title = obj.optString("title", fileName.substringBeforeLast('.')),
                persianTitle = obj.optString("persianTitle", "افکت درایو ${index + 1}"),
                category = NeonModelCategory.DRIVE_PACK_AI,
                primaryColor = parseHexColor(obj.optString("primaryHex"), Color(0xFF00E5FF)),
                secondaryColor = parseHexColor(obj.optString("secondaryHex"), Color(0xFF7C3AED)),
                coreColor = parseHexColor(obj.optString("coreHex"), Color(0xFFFFFFFF)),
                chassisTint = parseHexColor(obj.optString("chassisHex"), Color(0xFF0B182B)),
                animationSummary = obj.optString("animationSummary", "Synchronized 15s 3D Drive Pack procedural effect"),
                persianDescription = obj.optString("persianDescription", "افکت استخراج و آنالیزشده از درایو با رندر خام ۱۶K سه‌بعدی."),
                materialLayers = listOf(
                    "Drive Source: $fileName",
                    "3D ${archetype.name} Geometry",
                    "PBR Refraction & Specular",
                    "Raw Straight Alpha Output"
                ),
                loopHarmonics = obj.optInt("loopHarmonics", 2).coerceIn(1, 5),
                driveFileId = driveFileId,
                driveSourceName = fileName
            )
            Result.success(spec)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Drive asset analysis timed out for $fileName"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("AI analysis failed for $fileName: ${e.localizedMessage}"))
        }
    }

    private fun parseHexColor(raw: String, fallback: Color): Color {
        return try {
            val cleaned = raw.trim().let { if (it.startsWith("#")) it else "#$it" }
            Color(android.graphics.Color.parseColor(cleaned))
        } catch (_: Exception) {
            fallback
        }
    }
}

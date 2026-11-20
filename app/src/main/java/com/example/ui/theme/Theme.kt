package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.example.model.StudioThemePreset

val LocalStudioTheme = staticCompositionLocalOf { StudioThemePreset.OBSIDIAN_CYBER_CYAN }

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NeonVfxStudioTheme(
    preset: StudioThemePreset = StudioThemePreset.OBSIDIAN_CYBER_CYAN,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = preset.primaryColor,
        onPrimary = preset.backgroundVoid,
        primaryContainer = preset.primaryColor.copy(alpha = 0.24f),
        onPrimaryContainer = IceSilver,
        secondary = preset.secondaryColor,
        onSecondary = preset.backgroundVoid,
        secondaryContainer = preset.deckSurface,
        onSecondaryContainer = preset.secondaryColor,
        tertiary = preset.tertiaryColor,
        onTertiary = PlatinumText,
        background = preset.backgroundVoid,
        onBackground = PlatinumText,
        surface = preset.panelSurface,
        onSurface = PlatinumText,
        surfaceVariant = preset.deckSurface,
        onSurfaceVariant = MutedSlate,
        outline = preset.borderStroke,
        error = CrimsonAlert,
        onError = preset.backgroundVoid
    )

    CompositionLocalProvider(LocalStudioTheme provides preset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content
        )
    }
}

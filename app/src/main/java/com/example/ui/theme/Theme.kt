package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = Color(0xFF002530),
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = CyanContainerLight,
    secondary = EmeraldSignal,
    onSecondary = Color(0xFF00281B),
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldContainerLight,
    tertiary = VioletPulse,
    onTertiary = Color.White,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1E304A),
    error = CoralAlert,
    onError = Color(0xFF3E0011)
)

private val LightColorScheme = lightColorScheme(
    primary = CyanDeep,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = Color(0xFF001F28),
    secondary = EmeraldDeep,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = Color(0xFF002116),
    tertiary = VioletPulse,
    onTertiary = Color.White,
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = DaylightSurface,
    surfaceContainer = DaylightSurfaceHigh,
    surfaceContainerHigh = DaylightSurfaceHighest,
    outline = DaylightOutline,
    outlineVariant = Color(0xFFD5E1EE),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EsimStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

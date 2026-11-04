package com.desego.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DesegoLightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = OceanSoftBg,
    onPrimaryContainer = OceanBlue,
    secondary = SeafoamAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = AmberRating,
    onTertiary = Color.White,
    background = BackgroundSand,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = BackgroundSand,
    onSurfaceVariant = TextMuted,
    outline = BorderLight,
    outlineVariant = BorderSubtle
)

private val DesegoDarkColorScheme = darkColorScheme(
    primary = OceanBlueLight,
    onPrimary = Color.White,
    primaryContainer = NavyDeep,
    onPrimaryContainer = Color.White,
    secondary = SeafoamAccent,
    onSecondary = Color.White,
    tertiary = AmberRating,
    onTertiary = Color.White,
    background = NavyDeep,
    onBackground = Color.White,
    surface = NavyPrimary,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun DesegoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DesegoDarkColorScheme else DesegoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

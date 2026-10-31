package com.desego.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color

// Rappi‑inspired color palette
object DesegoColors {
    // Primary brand colors
    val RappiRed = Color(0xFFFF3B30) // vivid red
    val RappiOrange = Color(0xFFFF9500) // orange accent
    val RappiGreen = Color(0xFF34C759) // success green
    // Supporting neutrals
    val NeutralWhite = Color(0xFFFFFFFF)
    val NeutralGray = Color(0xFFF2F2F2)
    val NeutralDark = Color(0xFF1C1C1E)
}

private val DarkColorScheme = darkColorScheme(
    primary = DesegoColors.RappiRed,
    secondary = DesegoColors.RappiOrange,
    background = DesegoColors.NeutralDark,
    surface = DesegoColors.NeutralDark,
    onPrimary = DesegoColors.NeutralWhite,
    onSecondary = DesegoColors.NeutralWhite,
    onBackground = DesegoColors.NeutralWhite,
    onSurface = DesegoColors.NeutralWhite
)

private val LightColorScheme = lightColorScheme(
    primary = DesegoColors.RappiRed,
    secondary = DesegoColors.RappiOrange,
    background = DesegoColors.NeutralWhite,
    surface = DesegoColors.NeutralWhite,
    onPrimary = DesegoColors.NeutralWhite,
    onSecondary = DesegoColors.NeutralWhite,
    onBackground = DesegoColors.NeutralDark,
    onSurface = DesegoColors.NeutralDark
)

@Composable
fun DesegoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


package com.desego.iu.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DesegoColors {
    val Background = Color(0xFF10110F)
    val Surface = Color(0xFF181916)
    val Surface2 = Color(0xFF22231F)
    val Border = Color(0xFF30312C)
    val Primary = Color(0xFFF4A340)
    val PrimarySoft = Color(0xFF4A3119)
    val Cream = Color(0xFFFFE7C2)
    val Green = Color(0xFF55C983)
    val Red = Color(0xFFFF6B5E)
    val Blue = Color(0xFF7887F2)
    val Purple = Color(0xFFC786E6)
    val Yellow = Color(0xFFF7C948)
    val Text = Color(0xFFF4F3EE)
    val Muted = Color(0xFFA8A89F)
}

@Composable
fun DesegoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DesegoColors.Primary,
            onPrimary = Color(0xFF241506),
            secondary = DesegoColors.Green,
            background = DesegoColors.Background,
            surface = DesegoColors.Surface,
            surfaceContainer = DesegoColors.Surface2,
            onSurface = DesegoColors.Text,
            onBackground = DesegoColors.Text,
            outline = DesegoColors.Border,
        ),
        content = content,
    )
}

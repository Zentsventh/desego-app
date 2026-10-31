package com.desego.iu.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.desego.iu.theme.DesegoUi

@Composable
fun BlueScreen(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesegoUi.Blue)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCircle(
                color = Color.White.copy(alpha = .055f),
                radius = size.width * .44f,
                center = Offset(size.width * .93f, size.height * .05f)
            )

            drawCircle(
                color = DesegoUi.Cyan.copy(alpha = .07f),
                radius = size.width * .33f,
                center = Offset(size.width * .02f, size.height * .88f)
            )
        }
        content()
    }
}

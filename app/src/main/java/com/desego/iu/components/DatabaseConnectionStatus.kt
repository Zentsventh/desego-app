package com.desego.iu.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.desego.BuildConfig
import com.desego.data.DatabaseApi

@Composable
fun DatabaseConnectionStatus() {
    if (!BuildConfig.DEBUG) return
    var attempt by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(true) }
    var connected by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        checking = true
        connected = DatabaseApi.checkConnection()
        checking = false
    }
    Text(
        text = when {
            checking -> "Comprobando conexión…"
            connected -> "Conexión disponible"
            else -> "Servidor local no disponible"
        },
        color = Color.White.copy(alpha = .8f),
        fontSize = 12.sp
    )
    TextButton(enabled = !checking, onClick = { attempt++ }) {
        Text("Comprobar conexión", color = Color.White, fontSize = 12.sp)
    }
}

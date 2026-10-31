package com.desego.iu.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.desego.data.BackendClient

/** The staff app uses the same protected, responsive Inertia workspace and Laravel session. */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    var browser by remember { mutableStateOf<WebView?>(null) }
    val base = remember { BackendClient.baseUrl() }
    DisposableEffect(Unit) { onDispose { browser?.destroy() } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DESEGO · Equipo", fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8F3)),
                actions = {
                    TextButton(onClick = { browser?.loadUrl(base + "admin/seccion/salon") }) {
                        Text("Salón 3D", fontSize = 12.sp)
                    }
                    TextButton(onClick = { browser?.loadUrl(base + "admin/seccion/cocina") }) {
                        Text("Cocina", fontSize = 12.sp)
                    }
                    IconButton(onClick = { browser?.reload() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    browser = this
                    val staffWebView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(staffWebView, false)
                        BackendClient.webCookies().forEach { setCookie(base, it) }
                        flush()
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val allowed = Uri.parse(base)
                            if (request.url.host == allowed.host && request.url.port == allowed.port && request.url.scheme == allowed.scheme) return false
                            if (request.hasGesture() && request.url.scheme in listOf("https", "tel", "mailto")) runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                            }
                            return true
                        }
                    }
                    loadUrl(base + "admin")
                }
            }
        )
    }
}

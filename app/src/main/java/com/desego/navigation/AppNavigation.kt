package com.desego.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.desego.data.DemoAuthStore
import com.desego.data.DemoUser
import com.desego.data.NativeGoogleSignIn
import com.desego.iu.login.LoginScreen
import com.desego.iu.login.RegistroScreen
import com.desego.iu.store.Storefront
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation(initialTableToken: String? = null) {
    val context = LocalContext.current
    val auth = remember { DemoAuthStore(context) }
    var user by remember { mutableStateOf(auth.cachedUser()) }
    var authVisible by rememberSaveable { mutableStateOf(false) }
    var accountTab by rememberSaveable { mutableStateOf(false) }
    var registering by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        busy = true
        val restored = withContext(Dispatchers.IO) { runCatching { auth.session() } }
        restored.onSuccess { user = it }
        busy = false
    }
    val logout: () -> Unit = {
        scope.launch { withContext(Dispatchers.IO) { auth.logout() }; runCatching { NativeGoogleSignIn.signOut(context) }; user = null; accountTab = false; authVisible = false; error = null }
        Unit
    }
    val googleLogin: () -> Unit = {
        busy = true; error = null
        scope.launch {
            runCatching {
                val token = NativeGoogleSignIn.token(context)
                withContext(Dispatchers.IO) { auth.loginGoogle(token) }
            }.onSuccess { user = it; registering = false; authVisible = false; accountTab = true }
                .onFailure { error = it.message ?: "No se pudo ingresar con Google." }
            busy = false
        }
        Unit
    }
    BackHandler(authVisible && !busy) { authVisible = false; registering = false; error = null }
    if (!authVisible) {
        Storefront(user = user, onLogout = logout, initialTableToken = initialTableToken,
            onRequireLogin = { authVisible = true; error = null }, initialAccount = accountTab)
        return
    }
    Box(Modifier.fillMaxSize()) {
    when {
        registering -> RegistroScreen(
            busy = busy, errorMessage = error,
            onGoogleLogin = googleLogin,
            onNavigateToLogin = { registering = false; error = null },
            onRegisterSuccess = { name, email, password, phone ->
                busy = true; error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { auth.register(name, email, password, phone) } }
                    result.onSuccess { user = it; registering = false; authVisible = false; accountTab = true }
                        .onFailure { error = it.message ?: "No se pudo crear la cuenta." }
                    busy = false
                }
            }
        )
        else -> LoginScreen(
            busy = busy, errorMessage = error,
            onGoogleLogin = googleLogin,
            onNavigateToRegister = { registering = true; error = null },
            onLogin = { email, password, rememberMe ->
                busy = true; error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { auth.login(email, password) } }
                    val account = result.getOrNull()
                    if (account == null) error = result.exceptionOrNull()?.message ?: "Correo o contraseña incorrectos"
                    else { auth.remember(account, rememberMe); user = account; authVisible = false; accountTab = true }
                    busy = false
                }
            }
        )
    }
    TextButton(onClick = { authVisible = false; registering = false; error = null }, enabled = !busy,
        modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) { Text("Volver a la tienda", color = Color.White) }
    }
}

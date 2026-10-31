package com.desego.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.desego.data.DemoAuthStore
import com.desego.data.DemoRole
import com.desego.data.DemoUser
import com.desego.iu.dashboard.DashboardScreen
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
    val userSaver = remember { Saver<DemoUser?, List<String>>(
        save = { it?.let { u -> listOf(u.email, u.name, u.role.name) } ?: emptyList() },
        restore = { if (it.size == 3) DemoUser(it[0], it[1], DemoRole.valueOf(it[2])) else null }
    ) }
    var user by rememberSaveable(stateSaver = userSaver) { mutableStateOf<DemoUser?>(null) }
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
        scope.launch { withContext(Dispatchers.IO) { auth.logout() }; user = null; error = null }
        Unit
    }
    BackHandler(registering && !busy) { registering = false; error = null }
    when {
        user?.role == DemoRole.STAFF -> DashboardScreen(onLogout = logout)
        user != null -> Storefront(user = user!!, onLogout = logout, initialTableToken = initialTableToken)
        registering -> RegistroScreen(
            busy = busy, errorMessage = error,
            onNavigateToLogin = { registering = false; error = null },
            onRegisterSuccess = { name, email, password ->
                busy = true; error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { auth.register(name, email, password) } }
                    result.onSuccess { user = it; registering = false }
                        .onFailure { error = it.message ?: "No se pudo crear la cuenta." }
                    busy = false
                }
            }
        )
        else -> LoginScreen(
            busy = busy, errorMessage = error,
            onNavigateToRegister = { registering = true; error = null },
            onLogin = { email, password, rememberMe ->
                busy = true; error = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { auth.login(email, password) } }
                    val account = result.getOrNull()
                    if (account == null) error = result.exceptionOrNull()?.message ?: "Correo o contraseña incorrectos"
                    else { auth.remember(account, rememberMe); user = account }
                    busy = false
                }
            }
        )
    }
}

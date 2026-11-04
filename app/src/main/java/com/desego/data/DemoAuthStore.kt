package com.desego.data

import android.content.Context
import org.json.JSONObject

enum class DemoRole { CUSTOMER }
data class DemoUser(val email: String, val name: String, val role: DemoRole)

/** Account information is always verified by Laravel. No local demo credentials are accepted. */
class DemoAuthStore(private val context: Context) {
    init { BackendClient.configure(context) }
    fun cachedUser(): DemoUser? {
        if (!BackendClient.isRemembered()) return null
        return OfflineStore(context).use { it.read("session", "user") }?.let { runCatching { decode(it) }.getOrNull() }
    }
    fun session(): DemoUser? {
        if (!BackendClient.isRemembered()) return null
        return try {
            val result = BackendClient.request("api/mobile/session")
            val json = result.optJSONObject("user")
            if (json == null) {
                clearLocalSession()
                null
            } else { cache(json); decode(json) }
        } catch (error: Exception) {
            if (error is BackendException && error.status in listOf(401, 403, 419)) {
                clearLocalSession()
                return null
            }

            cachedUser()
        }
    }
    private fun clearLocalSession() {
        val scope = OfflineStore(context).use { it.read("session", "user")?.optString("email")?.lowercase() }
        BackendClient.clear()
        OfflineStore(context).use { database ->
            database.clearScope("session")
            scope?.let { database.clearScope(it) }
        }
    }
    fun logout() {
        synchronized(BackendClient) {
            val scope = OfflineStore(context).use { it.read("session", "user")?.optString("email")?.lowercase() }
            runCatching { BackendClient.request("api/mobile/logout", "POST") }
            BackendClient.clear()
            OfflineStore(context).use { database ->
                database.clearScope("session")
                scope?.let { database.clearScope(it) }
            }
        }
    }
    fun login(email: String, password: String, remember: Boolean): DemoUser {
        BackendClient.remember(remember)
        val result = BackendClient.request("api/mobile/login", "POST", JSONObject().put("email",email.trim()).put("password",password).put("remember", remember))
        BackendClient.remember(remember)
        cache(result.getJSONObject("user"))
        return decode(result.getJSONObject("user"))
    }
    fun register(name: String, email: String, password: String, phone: String = ""): DemoUser {
        BackendClient.remember(true)
        val result = BackendClient.request("api/mobile/register", "POST", JSONObject().put("name",name.trim()).put("email",email.trim()).put("phone",phone.trim()).put("password",password).put("password_confirmation",password).put("remember", true))
        BackendClient.remember(true)
        cache(result.getJSONObject("user"))
        return decode(result.getJSONObject("user"))
    }
    fun loginGoogle(idToken: String): DemoUser {
        BackendClient.remember(true)
        val result = BackendClient.request("api/mobile/google/login", "POST", JSONObject().put("id_token", idToken).put("remember", true))
        BackendClient.remember(true)
        cache(result.getJSONObject("user"))
        return decode(result.getJSONObject("user"))
    }
    private fun cache(json: JSONObject) {
        decode(json)
        OfflineStore(context).use { it.write("session", "user", JSONObject().put("email", json.getString("email")).put("name", json.getString("name")).put("role_code", "customer")) }
    }
    private fun decode(json: JSONObject): DemoUser {
        check(json.optString("role_code") == "customer") {
            "La aplicación está disponible únicamente para clientes."
        }

        return DemoUser(json.getString("email"), json.getString("name"), DemoRole.CUSTOMER)
    }
}

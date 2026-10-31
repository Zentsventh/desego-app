package com.desego.data

import android.content.Context
import org.json.JSONObject

enum class DemoRole { CUSTOMER, STAFF }
data class DemoUser(val email: String, val name: String, val role: DemoRole)

/** Account information is always verified by Laravel. No local demo credentials are accepted. */
class DemoAuthStore(context: Context) {
    init { BackendClient.configure(context) }
    fun session(): DemoUser? {
        if (!BackendClient.isRemembered()) return null
        val result = BackendClient.request("api/mobile/session")
        return result.optJSONObject("user")?.let(::decode)
    }
    fun remember(user: DemoUser, enabled: Boolean) { BackendClient.remember(enabled) }
    fun logout() {
        runCatching { BackendClient.request("api/mobile/logout", "POST") }
        BackendClient.clear()
    }
    fun login(email: String, password: String): DemoUser {
        val result = BackendClient.request("api/mobile/login", "POST", JSONObject().put("email",email.trim()).put("password",password).put("remember",true))
        return decode(result.getJSONObject("user"))
    }
    fun register(name: String, email: String, password: String): DemoUser {
        val result = BackendClient.request("api/mobile/register", "POST", JSONObject().put("name",name.trim()).put("email",email.trim()).put("password",password).put("password_confirmation",password))
        BackendClient.remember(true)
        return decode(result.getJSONObject("user"))
    }
    private fun decode(json: JSONObject) = DemoUser(json.getString("email"),json.getString("name"),
        if (json.optString("role_code")=="customer") DemoRole.CUSTOMER else DemoRole.STAFF)
}

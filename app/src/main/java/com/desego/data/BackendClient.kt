package com.desego.data

import android.content.Context
import com.desego.BuildConfig
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** Shared Laravel session. Passwords are never stored and mutations are never retried silently. */
object BackendClient {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    private var prefs: android.content.SharedPreferences? = null
    private var activeBase: String? = null
    private var csrf: String? = null

    @Synchronized
    fun configure(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("DesegoLaravelSession", Context.MODE_PRIVATE)
        if (prefs!!.getBoolean("remember", false)) {
            val saved = runCatching { JSONArray(prefs!!.getString("cookies", "[]")) }.getOrDefault(JSONArray())
            for (i in 0 until saved.length()) {
                val entry = saved.getJSONObject(i)
                val cookie = HttpCookie(entry.getString("name"), entry.getString("value"))
                cookie.domain = entry.optString("domain")
                cookie.path = entry.optString("path", "/")
                cookie.secure = entry.optBoolean("secure")
                cookies.cookieStore.add(URI(entry.getString("origin")), cookie)
            }
        }
    }

    @Synchronized
    fun remember(enabled: Boolean) {
        prefs?.edit()?.putBoolean("remember", enabled)?.apply()
        if (enabled) persistCookies() else prefs?.edit()?.remove("cookies")?.apply()
    }

    @Synchronized
    fun clear() {
        csrf = null
        cookies.cookieStore.removeAll()
        prefs?.edit()?.clear()?.apply()
    }

    private fun persistCookies() {
        if (prefs?.getBoolean("remember", false) != true) return
        val array = JSONArray()
        cookies.cookieStore.cookies.filterNot { it.hasExpired() }.forEach { cookie ->
            array.put(JSONObject().put("name", cookie.name).put("value", cookie.value)
                .put("domain", cookie.domain ?: URI(baseUrl()).host).put("path", cookie.path ?: "/")
                .put("secure", cookie.secure).put("origin", baseUrl()))
        }
        prefs?.edit()?.putString("cookies", array.toString())?.apply()
    }

    fun isRemembered() = prefs?.getBoolean("remember", false) == true

    @Synchronized
    fun baseUrl(): String {
        activeBase?.let { return it }
        val configured = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        check(configured.isNotEmpty()) { "Configura la dirección del restaurante para esta versión." }
        val candidates = if (BuildConfig.DEBUG && URI(configured).host in listOf("127.0.0.1", "10.0.2.2", "localhost"))
            listOf(configured, "http://10.0.2.2:8000", "http://127.0.0.1:8000").distinct() else listOf(configured)
        for (base in candidates) {
            val connection = URL("$base/health.php").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 2500; connection.readTimeout = 2500
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode == 200) {
                    val payload = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    if (payload.optString("service") == "desego" && payload.optString("database") == "connected") {
                        activeBase = "$base/"; return "$base/"
                    }
                }
            } catch (_: Exception) {
                // Only health checks can try another development address.
            } finally { connection.disconnect() }
        }
        error("No podemos conectar con DESEGO. Revisa la conexión y vuelve a intentar.")
    }

    @Synchronized
    fun request(path: String, method: String = "GET", body: JSONObject? = null, headers: Map<String, String> = emptyMap()): JSONObject {
        if (method != "GET" && csrf.isNullOrBlank()) request("api/mobile/session")
        val url = URL(baseUrl() + path.trimStart('/'))
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000; connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            cookies.get(url.toURI(), emptyMap()).forEach { (key, values) -> connection.setRequestProperty(key, values.joinToString("; ")) }
            if (method != "GET") {
                connection.setRequestProperty("X-CSRF-TOKEN", csrf)
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                connection.outputStream.use { it.write((body ?: JSONObject()).toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            cookies.put(url.toURI(), connection.headerFields.filterKeys { it != null }.mapKeys { it.key!! })
            persistCookies()
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val result = runCatching { JSONObject(text) }.getOrElse { error("El restaurante no pudo responder. Inténtalo de nuevo.") }
            result.optString("csrfToken").takeIf { it.isNotBlank() }?.let { csrf = it }
            if (code !in 200..299 || result.optBoolean("ok", true).not()) {
                if (code == 419) { csrf = null; error("Tu sesión de seguridad venció. Vuelve a intentar.") }
                val errors = result.optJSONObject("errors")
                val first = errors?.keys()?.asSequence()?.firstOrNull()?.let { errors.optJSONArray(it)?.optString(0) }
                error(first ?: result.optString("message", "No se pudo completar la solicitud."))
            }
            return result
        } finally { connection.disconnect() }
    }

    @Synchronized
    fun webCookies(): List<String> = cookies.cookieStore.cookies.filterNot { it.hasExpired() }.map { "${it.name}=${it.value}; Path=/; SameSite=Lax" + (if(it.isHttpOnly) "; HttpOnly" else "") + (if(it.secure) "; Secure" else "") }
}

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

class BackendException(val status: Int, message: String) : java.io.IOException(message)

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
            val saved = runCatching { JSONArray(SessionCipher.decrypt(prefs!!.getString("sealedCookies", "")!!)) }.getOrDefault(JSONArray())
            for (i in 0 until saved.length()) {
                val entry = saved.getJSONObject(i)
                val cookie = HttpCookie(entry.getString("name"), entry.getString("value"))
                cookie.domain = entry.optString("domain")
                cookie.path = entry.optString("path", "/")
                cookie.secure = entry.optBoolean("secure")
                cookie.isHttpOnly = entry.optBoolean("httpOnly")
                val expires = entry.optLong("expires", 0)
                if (expires == 0L || expires > System.currentTimeMillis()) {
                    if (expires > 0) cookie.maxAge = ((expires - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                    cookies.cookieStore.add(URI(entry.getString("origin")), cookie)
                }
            }
        }
    }

    @Synchronized
    fun remember(enabled: Boolean) {
        prefs?.edit()?.putBoolean("remember", enabled)?.apply()
        if (enabled) {
            persistCookies()
        } else {
            prefs?.edit()?.remove("sealedCookies")?.remove("cookies")?.apply()
        }
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
                .put("secure", cookie.secure).put("httpOnly", cookie.isHttpOnly).put("origin", baseUrl())
                .put("expires", if (cookie.maxAge < 0) 0 else System.currentTimeMillis() + (cookie.maxAge - ((System.currentTimeMillis() - cookieCreationTime(cookie)) / 1000)).coerceAtLeast(0) * 1000))
        }
        prefs?.edit()?.putString("sealedCookies", SessionCipher.encrypt(array.toString()))?.remove("cookies")?.apply()
    }

    private val cookieTimes = java.util.WeakHashMap<HttpCookie, Long>()
    private fun cookieCreationTime(cookie: HttpCookie): Long = cookieTimes.getOrPut(cookie) { System.currentTimeMillis() }

    fun isRemembered() = prefs?.getBoolean("remember", false) == true

    @Synchronized
    fun baseUrl(): String {
        activeBase?.let { return it }
        val configured = BuildConfig.API_BASE_URL.trim().trimEnd('/')
        check(configured.isNotEmpty()) { "Configura la dirección del restaurante para esta versión." }
        val localServers = BuildConfig.API_LOCAL_URLS.split('|').filter { it.isNotBlank() }.map { it.trimEnd('/') }
        val emulator = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.PRODUCT.contains("sdk") || android.os.Build.MODEL.contains("Emulator")
        val candidates = if (BuildConfig.DEBUG && URI(configured).host in listOf("127.0.0.1", "10.0.2.2", "localhost")) {
            (if (emulator) listOf(configured) + localServers else localServers + configured)
                .plus(listOf("http://10.0.2.2:8000", "http://10.0.3.2:8000", "http://127.0.0.1:8000")).distinct()
        } else listOf(configured)
        var lastFailure = "sin respuesta"
        for (base in candidates) {
            val connection = URL("$base/health.php").openConnection() as HttpURLConnection
            try {
                val remote = !base.startsWith("http://10.") && !base.startsWith("http://127.") && !base.startsWith("http://192.168.")
                connection.connectTimeout = if (remote) 10_000 else 2_500; connection.readTimeout = if (remote) 10_000 else 2_500
                connection.setRequestProperty("Accept", "application/json")
                val status = connection.responseCode
                if (status != 200) lastFailure = "HTTP $status en $base/health.php"
                if (status == 200) {
                    val payload = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    if (payload.optString("service") == "desego" && payload.optString("database") == "connected") {
                        activeBase = "$base/"; return "$base/"
                    }
                    lastFailure = "el servidor respondió pero la base de datos no está conectada"
                }
            } catch (error: Exception) {
                lastFailure = error.javaClass.simpleName + (error.message?.let { ": $it" } ?: "")
            } finally { connection.disconnect() }
        }
        error(if (BuildConfig.DEBUG && candidates.any { it.contains("10.0.2.2") || it.contains("127.0.0.1") }) "No se pudo conectar al servidor local ($lastFailure). Conecta el teléfono y la computadora a la misma red Wi-Fi y comprueba que el servidor esté encendido." else "No se pudo conectar con DESEGO ($lastFailure). Revisa tu conexión a internet e inténtalo de nuevo.")
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
            val result = runCatching { JSONObject(text) }.getOrElse { throw BackendException(code, "El restaurante no pudo responder. Inténtalo de nuevo.") }
            result.optString("csrfToken").takeIf { it.isNotBlank() }?.let { csrf = it }
            if (code !in 200..299 || result.optBoolean("ok", true).not()) {
                if (code == 419) { csrf = null; throw BackendException(code, "Tu sesión de seguridad venció. Vuelve a ingresar.") }
                val errors = result.optJSONObject("errors")
                val first = errors?.keys()?.asSequence()?.firstOrNull()?.let { errors.optJSONArray(it)?.optString(0) }
                throw BackendException(code, first ?: result.optString("message", "No se pudo completar la solicitud."))
            }
            return result
        } catch (error: java.io.IOException) {
            if (error !is BackendException) { activeBase = null; csrf = null }
            throw error
        } finally { connection.disconnect() }
    }

    @Synchronized
    fun webCookies(): List<String> = cookies.cookieStore.cookies.filterNot { it.hasExpired() }.map { "${it.name}=${it.value}; Path=/; SameSite=Lax" + (if(it.isHttpOnly) "; HttpOnly" else "") + (if(it.secure) "; Secure" else "") }
}

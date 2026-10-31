package com.desego.data

import android.content.Context
import android.net.Uri
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class DiningTablePreview(val number: Int, val label: String, val occupied: Boolean, val maintenance: Boolean)
data class DiningOrderLine(val name: String, val quantity: Int)
data class DiningOrder(val id: Long, val number: String, val status: String, val paymentStatus: String,
    val totalCents: Int, val mine: Boolean, val items: List<DiningOrderLine>)
data class DiningRequest(val id: Long, val kind: String, val status: String, val notes: String? = null)
data class DiningSnapshot(val visitStatus: String, val joinCode: String?, val tableLabel: String,
    val orders: List<DiningOrder>, val requests: List<DiningRequest>, val balanceCents: Int,
    val myBalanceCents: Int, val onlinePayment: Boolean, val yapeNumber: String = "999 888 777",
    val plinNumber: String = "999 888 777", val paymentHolder: String = "CEVICHERÍA DESEGO S.A.C.") {
    val isOpen get() = visitStatus == "open"
    val isFinished get() = visitStatus in listOf("paid", "cleaning", "closed")
}

/** Customer side of the table flow. The member token only lives on this device and is sent as `X-Dining-Token`. */
class DiningApi(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("DesegoDining", Context.MODE_PRIVATE)

    init { BackendClient.configure(context) }

    val hasSession get() = !prefs.getString("member", null).isNullOrBlank()
    private fun headers() = mapOf("X-Dining-Token" to prefs.getString("member", "").orEmpty())

    fun leave() { prefs.edit().remove("member").remove("checkoutKey").apply() }

    fun preview(qrToken: String): DiningTablePreview {
        val result = BackendClient.request("api/mobile/dining/tables/${Uri.encode(qrToken)}")
        val table = result.getJSONObject("table")
        return DiningTablePreview(table.getInt("number"), table.getString("label"),
            result.optBoolean("occupied"), result.optBoolean("maintenance"))
    }

    fun join(qrToken: String, name: String, joinCode: String?): DiningSnapshot {
        val body = JSONObject().put("name", name.trim()).put("confirm", true)
        if (!joinCode.isNullOrBlank()) body.put("join_code", joinCode.trim())
        prefs.getString("member", null)?.let { body.put("member_token", it) }
        val result = BackendClient.request("api/mobile/dining/tables/${Uri.encode(qrToken)}/join", "POST", body)
        prefs.edit().putString("member", result.getString("member_token")).apply()
        return parse(result)
    }

    /** Clears the stored member when the server no longer recognises it (visit closed or token invalid). */
    fun state(): DiningSnapshot = try {
        parse(BackendClient.request("api/mobile/dining/session", headers = headers()))
    } catch (error: IllegalStateException) {
        if (error.message.orEmpty().let { "no es válida" in it || "Escanea el QR" in it }) leave()
        throw error
    }

    /** The checkout key is kept until Laravel confirms, so a retry after a timeout can never duplicate the order. */
    fun order(items: Map<Int, Int>, notes: String): DiningSnapshot {
        val key = prefs.getString("checkoutKey", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("checkoutKey", it).apply() }
        val lines = JSONArray()
        items.filterValues { it > 0 }.forEach { (id, quantity) -> lines.put(JSONObject().put("product_id", id).put("quantity", quantity)) }
        val body = JSONObject().put("checkout_key", key).put("items", lines)
        if (notes.isNotBlank()) body.put("notes", notes.trim())
        val result = BackendClient.request("api/mobile/dining/orders", "POST", body, headers())
        prefs.edit().remove("checkoutKey").apply()
        return parse(result)
    }

    fun attention(kind: String, notes: String? = null): DiningSnapshot {
        val body = JSONObject().put("kind", kind)
        if (!notes.isNullOrBlank()) body.put("notes", notes.trim())
        return parse(BackendClient.request("api/mobile/dining/requests", "POST", body, headers()))
    }

    /** Returns the signed Niubiz checkout address. The payment is only recorded once Niubiz confirms it. */
    fun startPayment(scope: String, email: String, phone: String, address: String, city: String, postalCode: String): String {
        val body = JSONObject().put("scope", scope).put("email", email.trim()).put("phone", phone.trim())
            .put("address", address.trim()).put("city", city.trim()).put("postal_code", postalCode.trim())
        return BackendClient.request("api/mobile/dining/payments", "POST", body, headers()).getString("checkout_url")
    }

    private fun parse(result: JSONObject): DiningSnapshot {
        val visit = result.getJSONObject("visit")
        val orders = result.optJSONArray("orders") ?: JSONArray()
        val requests = result.optJSONArray("requests") ?: JSONArray()
        val direct = result.optJSONObject("direct_payment")
        return DiningSnapshot(
            visitStatus = visit.getString("status"),
            joinCode = visit.optString("join_code").takeIf { it.isNotBlank() && it != "null" },
            tableLabel = result.getJSONObject("table").getString("label"),
            orders = (0 until orders.length()).map { index ->
                val order = orders.getJSONObject(index)
                val lines = order.optJSONArray("items") ?: JSONArray()
                DiningOrder(order.getLong("id"), order.optString("order_number"), order.getString("status"),
                    order.optString("payment_status"), cents(order.optDouble("total")), order.optBoolean("is_mine"),
                    (0 until lines.length()).map { DiningOrderLine(lines.getJSONObject(it).getString("product_name"), lines.getJSONObject(it).getInt("quantity")) })
            },
            requests = (0 until requests.length()).map {
                val request = requests.getJSONObject(it)
                DiningRequest(request.getLong("id"), request.getString("kind"), request.getString("status"),
                    request.optString("notes").takeIf { n -> n.isNotBlank() && n != "null" })
            },
            balanceCents = cents(result.optDouble("balance")),
            myBalanceCents = cents(result.optDouble("my_balance")),
            onlinePayment = result.optBoolean("online_payment_available"),
            yapeNumber = direct?.optString("yape_number")?.takeIf { it.isNotBlank() } ?: "999 888 777",
            plinNumber = direct?.optString("plin_number")?.takeIf { it.isNotBlank() } ?: "999 888 777",
            paymentHolder = direct?.optString("holder")?.takeIf { it.isNotBlank() } ?: "CEVICHERÍA DESEGO S.A.C.")
    }

    private fun cents(amount: Double) = if (amount.isNaN()) 0 else Math.round(amount * 100).toInt()

    companion object {
        /** Accepts the full QR address (`.../mesa/{token}` or `desego://mesa/{token}`) or the bare token typed by hand. */
        fun tokenFromScan(raw: String): String? {
            val text = raw.trim()
            val uri = Uri.parse(text)
            if (uri.scheme == "desego" && uri.host == "mesa") {
                return uri.lastPathSegment?.takeIf { it.matches(Regex("[A-Za-z0-9]{32}")) }
            }
            val segments = uri.pathSegments
            val candidate = if (segments.size >= 2 && segments[segments.size - 2] == "mesa") segments.last() else text
            return candidate.takeIf { it.matches(Regex("[A-Za-z0-9]{32}")) }
        }
    }
}

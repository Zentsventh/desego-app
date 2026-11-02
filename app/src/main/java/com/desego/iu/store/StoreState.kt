package com.desego.iu.store

import android.content.Context
import androidx.compose.runtime.*
import com.desego.R
import com.desego.data.BackendClient
import com.desego.data.DemoUser
import com.desego.data.OfflineStore
import com.desego.data.StoreSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** Local cache of server-owned data. A checkout is committed locally only after Laravel confirms it. */
class StoreState(private val context: Context, private val user: DemoUser?) {
    private val scope = user?.email?.lowercase() ?: "guest"
    private val database = OfflineStore(context)
    private val prefs = context.getSharedPreferences("DesegoLiveStore_${user?.email.orEmpty()}", Context.MODE_PRIVATE)
    private val initial = database.read(scope, "state") ?: runCatching { JSONObject(prefs.getString("state", "{}")!!) }.getOrDefault(JSONObject())
    var name by mutableStateOf(initial.optString("name", user?.name ?: "Invitado")); private set
    var phone by mutableStateOf(initial.optString("phone", "")); private set
    var cart by mutableStateOf(readCart(initial.optJSONObject("cart"))); private set
    var favorites by mutableStateOf(initial.optJSONArray("favorites").list { it.getInt(this) }.toSet()); private set
    var addresses by mutableStateOf(emptyList<DeliveryAddress>()); private set
    var orders by mutableStateOf(emptyList<DemoOrder>()); private set
    var reservations by mutableStateOf(emptyList<DemoReservation>()); private set
    var loyaltyPoints by mutableIntStateOf(0); private set
    var notifications by mutableStateOf(initial.optBoolean("notifications", true)); private set
    var coupon by mutableStateOf(initial.optString("coupon", "")); private set
    var busy by mutableStateOf(false); private set
    var loading by mutableStateOf(true); private set
    var connectionError by mutableStateOf<String?>(null); private set
    var pendingChanges by mutableIntStateOf(database.pending(scope).size); private set
    var syncIssue by mutableStateOf<String?>(null); private set
    var sessionExpired by mutableStateOf(false); private set
    var lastSynced by mutableStateOf(database.read("public", "sync")?.optLong("time", 0) ?: 0); private set
    private var checkoutKey = initial.optString("checkoutKey").takeIf { it.isNotBlank() } ?: prefs.getString("checkoutKey", null) ?: UUID.randomUUID().toString()
    private var refreshing = false
    private var closed = false
    val count get() = cart.values.sum()
    init {
        BackendClient.configure(context)
        demoProducts.clear()
        runCatching { (database.read("public", "catalog") ?: prefs.getString("catalog", null)?.let(::JSONObject))?.let(::applyCatalog) }
        if (user != null) {
            runCatching { (database.read(scope, "account") ?: prefs.getString("account", null)?.let(::JSONObject))?.let { applyAccount(it); database.write(scope, "account", it) } }
            database.read("guest", "state")?.let { guest ->
                val guestCart = readCart(guest.optJSONObject("cart"))
                if (cart.isEmpty()) cart = guestCart
                guest.optJSONArray("favorites").list { it.getInt(this) }.forEach { id ->
                    favorites = favorites + id
                    queue("favorite:$id", "api/mobile/favorites/$id", "PUT", JSONObject().put("favorited", true))
                }
                database.clearScope("guest")
            }
        }
        database.pending(scope).filter { it.resource.startsWith("favorite:") }.forEach { mutation ->
            val id = mutation.resource.substringAfter(':').toInt()
            favorites = if (mutation.payload.optBoolean("favorited")) favorites + id else favorites - id
        }
        database.pending(scope).firstOrNull { it.resource == "profile" }?.let {
            name = it.payload.getString("name")
            phone = it.payload.optString("phone")
        }
        save()
        prefs.edit().clear().apply()
    }
    suspend fun refresh() {
        if (refreshing || closed) return
        refreshing = true
        loading = demoProducts.isEmpty()
        try {
            val snapshot = withContext(Dispatchers.IO) { StoreSync.refresh(context, user?.email?.lowercase()) }
            if (closed) return
            applyCatalog(snapshot.getJSONObject("catalog"))
            sessionExpired = user != null && snapshot.optJSONObject("user")?.optString("email")?.lowercase() != scope
            if (!sessionExpired) snapshot.optJSONObject("account")?.let(::applyAccount)
            val pending = database.pending(scope)
            pendingChanges = pending.size
            syncIssue = if (sessionExpired) "Tu sesión venció. Ingresa para sincronizar tus cambios." else pending.firstOrNull { it.error != null }?.error
            pending.filter { it.resource.startsWith("favorite:") }.forEach { mutation ->
                val id = mutation.resource.substringAfter(':').toInt()
                favorites = if (mutation.payload.optBoolean("favorited")) favorites + id else favorites - id
            }
            pending.firstOrNull { it.resource == "profile" }?.let { profile(it.payload.getString("name"), it.payload.optString("phone")) }
            connectionError = null
            lastSynced = System.currentTimeMillis()
            database.write("public", "sync", JSONObject().put("time", lastSynced))
            save()
        } catch (error: Exception) { connectionError = error.message ?: "No se pudo actualizar la carta." }
        finally { loading = false; refreshing = false }
    }
    private suspend fun refreshAccount() {
        val result = withContext(Dispatchers.IO) { BackendClient.request("api/mobile/account") }
        applyAccount(result)
        database.write(scope, "account", result)
    }
    private fun applyCatalog(data: JSONObject) {
        storeDeliveryFeeCents.intValue = Math.round(data.optDouble("delivery_fee", 0.0) * 100).toInt().coerceAtLeast(0)
        val products = data.getJSONArray("products").list { array ->
            val product = array.getJSONObject(this)
            val title = product.getString("name")
            val image = when {
                title.contains("arroz",true) -> R.drawable.food_arroz
                title.contains("chicha",true) -> R.drawable.food_chicha
                title.contains("chocolate",true) -> R.drawable.food_chocolate
                title.contains("frito",true) -> R.drawable.food_pescado
                else -> R.drawable.food_ceviche
            }
            StoreProduct(product.getInt("id"), title, product.getString("category"),
                Math.round(product.getDouble("price")*100).toInt(),image,product.optString("description"),product.optString("allergens"),available=product.optBoolean("can_order"), imageUrl=product.optString("image").takeUnless { it == "null" }.orEmpty())
        }
        demoProducts.clear(); demoProducts.addAll(products)
        cart = cart.filterKeys { id -> products.any { it.id == id } }
    }
    private fun applyAccount(data: JSONObject) {
        loyaltyPoints = data.optJSONObject("loyalty")?.optInt("points_balance", 0) ?: 0
        if (data.has("favorites")) favorites = data.optJSONArray("favorites").list { it.getJSONObject(this).getInt("id") }.toSet()
        data.optJSONObject("customer")?.let { name=it.optString("full_name",user?.name.orEmpty());phone=it.optString("phone","").takeUnless { v->v=="null" }.orEmpty() }
        addresses = data.optJSONArray("addresses").list { array ->
            val a=array.getJSONObject(this)
            DeliveryAddress(a.getLong("id").toString(),a.getString("label"),a.getString("address_line"),a.getString("district"),a.optString("reference").takeUnless { it=="null" }.orEmpty())
        }
        orders = data.optJSONArray("orders").list { array ->
            val o=array.getJSONObject(this);val lines=o.getJSONArray("items");val items=mutableMapOf<Int,Int>();val names=mutableMapOf<Int,String>();val prices=mutableMapOf<Int,Int>()
            for (i in 0 until lines.length()) { val line=lines.getJSONObject(i);val id=line.getInt("product_id");items[id]=line.getInt("quantity");names[id]=line.getString("product_name");prices[id]=Math.round(line.getDouble("unit_price")*100).toInt() }
            DemoOrder(o.optString("order_number"),localDate(o.getString("created_at")),items,Math.round(o.getDouble("total")*100).toInt(),o.getString("order_type"),"Pedido registrado en DESEGO",o.optString("payment_preference","Al recibir"),o.optString("notes").takeUnless { it=="null" }.orEmpty(),o.getString("status"),o.getLong("id"),names,prices,paymentStatus=o.optString("payment_status", "Pendiente"))
        }
        reservations=data.optJSONArray("reservations").list { array ->
            val r=array.getJSONObject(this);val date=localDate(r.getString("reservation_at"));
            DemoReservation(r.getLong("id").toString(),date.substringBefore(' '),date.substringAfter(' '),r.getInt("guest_count"),r.getString("status"))
        }
    }
    fun quantity(id: Int) = cart[id] ?: 0
    fun setQuantity(id: Int, quantity: Int) {
        if (busy || (quantity > 0 && demoProducts.none { it.id==id && it.available })) return
        cart=cart.toMutableMap().apply { if(quantity<=0)remove(id) else put(id,quantity.coerceAtMost(20)) };save()
    }
    fun add(id: Int, quantity: Int=1)=setQuantity(id,quantity(id)+quantity)
    fun favorite(id: Int) {
        favorites=if(id in favorites)favorites-id else favorites+id
        if (user != null) queue("favorite:$id", "api/mobile/favorites/$id", "PUT", JSONObject().put("favorited", id in favorites))
        save()
    }
    fun applyCoupon(value: String) { coupon=value.trim().uppercase(Locale.ROOT);save() }
    fun profile(newName: String,newPhone: String) { name=newName.trim();phone=newPhone.trim();save() }
    suspend fun saveProfile(newName: String,newPhone: String) {
        require(user != null) { "Ingresa a tu cuenta." }
        require(newName.trim().length in 3..100 && newPhone.length <= 30) { "Revisa tu nombre y celular." }
        queue("profile", "api/mobile/profile", "POST", JSONObject().put("name",newName.trim()).put("phone",newPhone.trim()))
        profile(newName,newPhone)
        refresh()
    }
    fun notificationSetting(value: Boolean) { notifications=value;save() }
    suspend fun addAddress(label: String,street: String,district: String,reference: String): DeliveryAddress {
        val result=withContext(Dispatchers.IO) { BackendClient.request("api/mobile/addresses","POST",JSONObject().put("label",label).put("address_line",street).put("district",district).put("reference",reference).put("phone",phone)) }
        refreshAccount();return addresses.first { it.id==result.getLong("id").toString() }
    }
    suspend fun removeAddress(id: String) { withContext(Dispatchers.IO) { BackendClient.request("api/mobile/addresses/$id","DELETE") };refreshAccount() }
    suspend fun order(delivery: Boolean,address: DeliveryAddress?,payment: String,note: String): DemoOrder {
        require(user != null && !sessionExpired) { "Ingresa a tu cuenta para confirmar el pedido." }
        check(!busy) { "Tu pedido se esta procesando." };require(cart.isNotEmpty()) { "Tu carrito esta vacio." }
        require(!delivery||address!=null) { "Selecciona una direccion." };busy=true
        try {
            val snapshot=cart.toMap();save()
            val items=JSONArray(snapshot.map { (id,quantity)->JSONObject().put("product_id",id).put("quantity",quantity) })
            val payload=JSONObject().put("customer_name",name).put("email",user.email).put("phone",phone).put("consent",true)
                .put("order_type",if(delivery)"Delivery" else "Para llevar").put("payment_preference",when {
                    payment.contains("Yape",true)||payment.contains("Plin",true)->"Yape/Plin"
                    payment.contains("Tarjeta",true)->"Tarjeta al recibir"
                    else->"Efectivo"
                }).put("notes",note).put("coupon_code",coupon).put("checkout_key",checkoutKey).put("items",items)
            if(delivery&&address!=null) { payload.put("saved_address_id",address.id.toLong()).put("address_line",address.street).put("district",address.district).put("reference",address.reference) }
            val result=withContext(Dispatchers.IO) { BackendClient.request("api/mobile/checkout","POST",payload) }
            val order=DemoOrder(result.getString("order_number"),java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("es","PE")).format(java.util.Date()),snapshot,
                Math.round(result.getDouble("total")*100).toInt(),if(delivery)"Delivery" else "Para llevar",address?.let{"${it.street}, ${it.district}"}?:"Recojo en DESEGO",payment,note,"Pendiente",result.getLong("order_id"),demoProducts.associate { it.id to it.name },demoProducts.associate { it.id to it.price })
            orders=listOf(order)+orders.filterNot { it.serverId==order.serverId };cart=emptyMap();coupon="";checkoutKey=UUID.randomUUID().toString();save()
            runCatching { refreshAccount() }
            return orders.firstOrNull { it.serverId==order.serverId }?:order
        } finally { busy=false }
    }
    suspend fun cancel(id: String) {
        val order=orders.first { it.id==id }
        withContext(Dispatchers.IO) { BackendClient.request("api/mobile/orders/${order.serverId}/cancel","POST") };refreshAccount()
    }
    fun reorder(order: DemoOrder) { order.items.forEach { (id,qty)->add(id,qty) } }
    suspend fun reserve(date: String,time: String,people: Int) {
        require(user != null && !sessionExpired) { "Ingresa a tu cuenta para reservar." }
        val parsed=SimpleDateFormat("dd/MM/yyyy",Locale.ROOT).parse(date)!!
        val iso=SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(parsed)
        withContext(Dispatchers.IO) { BackendClient.request("api/mobile/reservations","POST",JSONObject().put("customer_name",name).put("phone",phone).put("email",user.email).put("date",iso).put("time",time).put("people",people)) };refreshAccount()
    }
    suspend fun cancelReservation(id: String) { withContext(Dispatchers.IO) { BackendClient.request("api/mobile/reservations/$id/cancel","POST") };refreshAccount() }
    private fun save() {
        database.write(scope, "state", JSONObject().put("checkoutKey",checkoutKey).put("name",name).put("phone",phone)
            .put("cart",JSONObject(cart.mapKeys { it.key.toString() })).put("favorites",JSONArray(favorites.toList())).put("coupon",coupon).put("notifications",notifications))
    }
    private fun queue(resource: String, path: String, method: String, payload: JSONObject) {
        database.enqueue(scope, resource, path, method, payload)
        pendingChanges = database.pending(scope).size
        StoreSync.schedule(context)
    }
    fun close() { closed = true; database.close() }
    private fun readCart(json: JSONObject?): Map<Int,Int> = json?.keys()?.asSequence()?.mapNotNull { key->key.toIntOrNull()?.let { it to json.optInt(key).coerceIn(0,20) } }?.filter { it.second>0 }?.toMap()?:emptyMap()
    private fun localDate(raw: String): String {
        return runCatching {
            val parser=SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ROOT).apply { timeZone=TimeZone.getTimeZone("UTC") }
            SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("es","PE")).apply { timeZone=TimeZone.getTimeZone("America/Lima") }.format(parser.parse(raw)!!)
        }.getOrDefault(raw)
    }
    private fun <T> JSONArray?.list(block: Int.(JSONArray)->T):List<T> = if(this==null)emptyList() else (0 until length()).mapNotNull { runCatching { block(it,this) }.getOrNull() }
}

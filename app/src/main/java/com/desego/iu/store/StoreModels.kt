package com.desego.iu.store

import com.desego.R
import java.text.NumberFormat
import java.util.Locale

data class StoreProduct(val id: Int, val name: String, val category: String, val price: Int,
    val image: Int, val description: String, val allergens: String, val tag: String = "", val available: Boolean = true, val imageUrl: String = "")

val demoProducts = androidx.compose.runtime.mutableStateListOf<StoreProduct>()
var storeDeliveryFeeCents = androidx.compose.runtime.mutableIntStateOf(500)

fun money(cents: Int): String = "S/ " + NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}.format(cents / 100.0)

data class CartTotals(val subtotal: Int, val discount: Int, val delivery: Int) {
    val total get() = subtotal - discount + delivery
}
fun calculateTotals(cart: Map<Int, Int>, delivery: Boolean, coupon: String): CartTotals {
    val subtotal = demoProducts.sumOf { it.price * (cart[it.id] ?: 0).coerceIn(0, 20) }
    return CartTotals(subtotal, 0,
        if (delivery && subtotal > 0) storeDeliveryFeeCents.intValue else 0)
}
data class DeliveryAddress(val id: String, val label: String, val street: String, val district: String, val reference: String, val isDefault: Boolean = false)
data class DemoOrder(val id: String, val date: String, val items: Map<Int, Int>, val total: Int,
    val mode: String, val address: String, val payment: String, val note: String, val status: String = "Pendiente", val serverId: Long = 0, val itemNames: Map<Int,String> = emptyMap(), val itemPrices: Map<Int,Int> = emptyMap(), val paymentStatus: String = "Pendiente")
data class DemoReservation(val id: String, val date: String, val time: String, val people: Int, val status: String = "Solicitada")

data class LoyaltyReward(val id: Int, val code: String, val name: String, val description: String, val type: String, val pointsCost: Int, val discountAmount: Double)
data class LoyaltyRedemption(val id: Int, val code: String, val pointsSpent: Int, val status: String, val rewardName: String, val date: String)
data class LoyaltyTransaction(val id: Int, val type: String, val pointsDelta: Int, val balanceAfter: Int, val description: String, val date: String)
data class CustomerReview(val id: Int, val productId: Int, val productName: String, val rating: Int, val comment: String, val date: String)
data class CustomerStats(val ordersCount: Int = 0, val totalSpent: Double = 0.0, val activeReservationsCount: Int = 0)

package com.desego.iu.store

import com.desego.R
import java.text.NumberFormat
import java.util.Locale

data class StoreProduct(val id: Int, val name: String, val category: String, val price: Int,
    val image: Int, val description: String, val allergens: String, val tag: String = "", val available: Boolean = true)

val demoProducts = androidx.compose.runtime.mutableStateListOf<StoreProduct>()

fun money(cents: Int): String = "S/ " + NumberFormat.getNumberInstance(Locale("es", "PE")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}.format(cents / 100.0)

data class CartTotals(val subtotal: Int, val discount: Int, val delivery: Int) {
    val total get() = subtotal - discount + delivery
}
fun calculateTotals(cart: Map<Int, Int>, delivery: Boolean, coupon: String): CartTotals {
    val subtotal = demoProducts.sumOf { it.price * (cart[it.id] ?: 0).coerceIn(0, 20) }
    return CartTotals(subtotal, 0,
        if (delivery && subtotal > 0) 500 else 0)
}
data class DeliveryAddress(val id: String, val label: String, val street: String, val district: String, val reference: String)
data class DemoOrder(val id: String, val date: String, val items: Map<Int, Int>, val total: Int,
    val mode: String, val address: String, val payment: String, val note: String, val status: String = "Pendiente", val serverId: Long = 0, val itemNames: Map<Int,String> = emptyMap(), val itemPrices: Map<Int,Int> = emptyMap())
data class DemoReservation(val id: String, val date: String, val time: String, val people: Int, val status: String = "Solicitada")

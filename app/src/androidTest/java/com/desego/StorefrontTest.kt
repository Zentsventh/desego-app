package com.desego

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.desego.data.DemoAuthStore
import com.desego.data.DemoRole
import com.desego.data.DemoUser
import com.desego.iu.store.StoreState
import com.desego.iu.store.Storefront
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.desego.navigation.AppNavigation

class StorefrontTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun user() = DemoUser("test-${UUID.randomUUID()}@example.com", "Cliente de prueba", DemoRole.CUSTOMER)

    private fun isolatedContext(prefix: String) = object : ContextWrapper(context) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = super.getSharedPreferences(prefix + name, mode)
    }

    private fun loginRoute(email: String, expected: String) {
        val isolated = isolatedContext("Test_${UUID.randomUUID()}_")
        try {
            compose.setContent { CompositionLocalProvider(LocalContext provides isolated) { AppNavigation() } }
            compose.onNodeWithText("Correo Electrónico").performTextInput(email)
            compose.onNodeWithText("Contraseña").performTextInput("123456")
            compose.onNodeWithText("INICIAR SESIÓN").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(expected).assertExists()
        } finally {
            listOf("DesegoDemoAuth", "DesegoPrefs", "DesegoStore_cliente@desego.com").forEach {
                isolated.getSharedPreferences(it, 0).edit().clear().commit()
            }
        }
    }

    @Test fun customerLoginOpensStorefront() = loginRoute("cliente@desego.com", "Carta")
    @Test fun staffLoginOpensExistingPanel() = loginRoute("admin@desego.com", "Ventas Hoy")

    @Test fun demoAccountsRouteByRoleAndWrongPasswordFails() {
        val auth = DemoAuthStore(context)
        assertEquals(DemoRole.STAFF, auth.login(" ADMIN@DESEGO.COM ", "123456")?.role)
        assertEquals(DemoRole.CUSTOMER, auth.login("cliente@desego.com", "123456")?.role)
        assertNull(auth.login("admin@desego.com", "incorrecta"))
    }

    @Test fun registeredAccountsAreCustomersAndRememberMeControlsSession() {
        val prefix = "Test_${UUID.randomUUID()}_"
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences(prefix + name, mode)
        }
        try {
            val auth = DemoAuthStore(isolated)
            val customer = auth.register("Nuevo cliente", "nuevo@example.com", "clave123")
            assertEquals(DemoRole.CUSTOMER, customer.role)
            assertEquals(customer, auth.login("nuevo@example.com", "clave123"))
            assertNull(auth.login("nuevo@example.com", "error"))
            auth.remember(customer, true)
            assertEquals(customer, DemoAuthStore(isolated).session())
            auth.remember(customer, false)
            assertNull(DemoAuthStore(isolated).session())
            try {
                auth.register("Otro", "admin@desego.com", "clave123")
                fail("Registration must not overwrite the staff account")
            } catch (_: IllegalArgumentException) { }
        } finally {
            listOf("DesegoDemoAuth", "DesegoPrefs").forEach { isolated.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        }
    }

    @Test fun cartFavoritesAddressesAndOrdersPersistAndStayIsolated() {
        val user = user()
        val state = StoreState(context, user)
        try {
            state.add(1, 2); state.favorite(1); state.applyCoupon("DESEGO10")
            val address = state.addAddress("Casa", "Av. de prueba 123", "Lima", "Puerta azul")
            val reloaded = StoreState(context, user)
            assertEquals(2, reloaded.quantity(1)); assertTrue(1 in reloaded.favorites)
            assertEquals(address, reloaded.addresses.first())
            val order = reloaded.order(true, address, "Tarjeta", "Sin ají")
            assertEquals(5082, order.total)
            assertTrue(reloaded.cart.isEmpty())
            assertEquals("Recibido", StoreState(context, user).orders.first().status)
            reloaded.cancel(order.id)
            assertEquals("Cancelado", StoreState(context, user).orders.first().status)
            assertTrue(StoreState(context, user()).cart.isEmpty())
        } finally { context.getSharedPreferences("DesegoStore_${user.email}", 0).edit().clear().commit() }
    }

    @Test fun customerCanCompletePickupCheckoutWithoutAnyPaymentCharge() {
        val user = user()
        try {
            compose.setContent { Storefront(user, {}) }
            compose.onAllNodesWithContentDescription("Ceviche Desego").onFirst().performClick()
            compose.onNodeWithText("Añadir al carrito · S/ 24.90").performClick()
            compose.onNodeWithText("Recojo").performClick()
            compose.onNodeWithText("Continuar · S/ 24.90").performClick()
            compose.onNodeWithText("Celular de contacto").performScrollTo().performTextInput("999999999")
            compose.onNodeWithText("Confirmar pedido de prueba").performClick()
            compose.onNodeWithText("Sin cobro realizado").assertExists()
            compose.onNodeWithText("Contraentrega · pago pendiente. Este pedido no se ha enviado al restaurante.").assertExists()
            assertEquals(1, StoreState(context, user).orders.size)
        } finally { context.getSharedPreferences("DesegoStore_${user.email}", 0).edit().clear().commit() }
    }
}

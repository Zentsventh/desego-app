package com.desego

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.desego.data.OfflineStore
import com.desego.iu.store.Storefront
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class StorefrontTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun firstLaunchHasOfflineCatalogWithoutRequiringLogin() {
        val name = "offline-test-${UUID.randomUUID()}.db"
        try {
            OfflineStore(context, name).use { database ->
                val catalog = database.read("public", "catalog")!!
                assertTrue(catalog.getJSONArray("products").length() > 0)
                assertFalse(catalog.has("csrfToken"))
                assertFalse(catalog.has("customer_profile"))
                assertNull(database.read("another-customer", "account"))
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun outboxSurvivesRestartAndAcknowledgementCannotDeleteANewerEdit() {
        val name = "offline-test-${UUID.randomUUID()}.db"
        try {
            val old = OfflineStore(context, name).use { database ->
                database.enqueue("one@example.test", "favorite:1", "api/mobile/favorites/1", "PUT", JSONObject().put("favorited", true))
                database.pending("one@example.test").single()
            }
            OfflineStore(context, name).use { database ->
                assertEquals(1, database.pending("one@example.test").size)
                assertTrue(database.pending("other@example.test").isEmpty())
                database.enqueue("one@example.test", "favorite:1", "api/mobile/favorites/1", "PUT", JSONObject().put("favorited", false))
                database.acknowledge(old)
                val latest = database.pending("one@example.test").single()
                assertFalse(latest.payload.getBoolean("favorited"))
                database.acknowledge(latest)
                assertTrue(database.pending("one@example.test").isEmpty())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun logoutClearsOnlyTheSelectedAccountsPrivateCacheAndPendingChanges() {
        val name = "offline-test-${UUID.randomUUID()}.db"
        try {
            OfflineStore(context, name).use { database ->
                database.write("one", "account", JSONObject().put("name", "Private"))
                database.write("two", "account", JSONObject().put("name", "Other"))
                database.enqueue("one", "profile", "api/mobile/profile", "POST", JSONObject())
                database.clearScope("one")
                assertNull(database.read("one", "account"))
                assertTrue(database.pending("one").isEmpty())
                assertEquals("Other", database.read("two", "account")!!.getString("name"))
                assertNotNull(database.read("public", "catalog"))
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun guestBrowsesNativeStoreAndOnlyAccountEntryRequestsLogin() {
        var loginRequested = false
        compose.setContent { Storefront(null, {}, onRequireLogin = { loginRequested = true }) }
        compose.onNodeWithText("Ver carta").assertExists()
        assertFalse(loginRequested)
        compose.onNodeWithText("Cuenta").performClick()
        compose.onNodeWithText("Ingresar o crear mi cuenta").performClick()
        compose.runOnIdle { assertTrue(loginRequested) }
    }
}

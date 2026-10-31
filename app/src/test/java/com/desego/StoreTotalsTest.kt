package com.desego

import com.desego.iu.store.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test

class StoreTotalsTest {
    @Before fun catalog() {
        demoProducts.clear()
        demoProducts.add(StoreProduct(1,"Ceviche","Marinos",2490,0,"",""))
        demoProducts.add(StoreProduct(5,"Bebida","Bebidas",800,0,"",""))
    }
    @After fun clear() { demoProducts.clear() }
    @Test fun localEstimatesNeverInventCouponDiscounts() {
        val totals=calculateTotals(mapOf(1 to 2,5 to 1),true,"DESEGO10")
        assertEquals(5780,totals.subtotal)
        assertEquals(0,totals.discount)
        assertEquals(500,totals.delivery)
        assertEquals(6280,totals.total)
    }
    @Test fun emptyCartHasNoDeliveryCharge() {
        assertEquals(0,calculateTotals(emptyMap(),true,"").total)
    }
    @Test fun pickupHasNoFee() {
        assertEquals(2490,calculateTotals(mapOf(1 to 1),false,"").total)
    }
    @Test fun unknownProductsAndNegativeQuantitiesCannotChangeTotals() {
        assertEquals(0,calculateTotals(mapOf(999 to 1,1 to -1),false,"").total)
    }
    @Test fun quantitiesAreBounded() {
        assertEquals(49800,calculateTotals(mapOf(1 to 200),false,"").total)
    }
}

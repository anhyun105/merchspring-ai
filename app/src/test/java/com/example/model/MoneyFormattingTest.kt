package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormattingTest {
    @Test
    fun formatsZeroRupees() {
        assertEquals("₹0.00", 0L.toRupeesString())
    }

    @Test
    fun formatsPaiseAndRupees() {
        assertEquals("₹1.23", 123L.toRupeesString())
        assertEquals("₹123.45", 12345L.toRupeesString())
    }

    @Test
    fun usesIndianDigitGrouping() {
        assertEquals("₹12,34,567.89", 123456789L.toRupeesString())
    }

    @Test
    fun formatsNegativeAmounts() {
        assertEquals("-₹123.45", (-12345L).toRupeesString())
    }
}

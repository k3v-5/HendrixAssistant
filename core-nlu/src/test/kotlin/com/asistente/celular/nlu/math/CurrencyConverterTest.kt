package com.asistente.celular.nlu.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CurrencyConverterTest {

    private val converter = CurrencyConverter()

    @Test
    fun testDirectDollarToPesos() {
        val res = converter.parseAndConvert("100 dólares a pesos")
        assertNotNull(res)
        assertEquals("USD", res!!.fromCurrency)
        assertEquals("MXN", res.toCurrency)
        // 100 * 19.80 = 1980.00
        assertEquals(0, BigDecimal("1980.00").compareTo(res.resultAmount))
    }

    @Test
    fun testConversionPhraseWithPasa() {
        val res = converter.parseAndConvert("pasa 50 euros a dólares")
        assertNotNull(res)
        assertEquals("EUR", res!!.fromCurrency)
        assertEquals("USD", res.toCurrency)
        assertTrue(res.resultAmount > BigDecimal.ZERO)
    }

    @Test
    fun testExchangeRateQuery() {
        val res = converter.parseAndConvert("a cuánto está el dólar")
        assertNotNull(res)
        assertEquals("USD", res!!.fromCurrency)
        assertEquals("MXN", res.toCurrency)
    }
}

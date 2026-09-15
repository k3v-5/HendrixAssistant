package com.asistente.celular.nlu.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UnitConverterTest {

    @Test
    fun testLengthConversionInchesToCm() {
        val res = UnitConverter.parseAndConvert("convierte 10 pulgadas a centímetros")
        assertNotNull(res)
        // 10 in = 25.4 cm
        assertEquals(25.4, res!!.resultAmount, 0.01)
        assertEquals("cm", res.toSymbol)
    }

    @Test
    fun testLengthConversionMilesToKm() {
        val res = UnitConverter.parseAndConvert("cuántos kilómetros son 5 millas")
        assertNotNull(res)
        // 5 mi = 8.04672 km
        assertEquals(8.046, res!!.resultAmount, 0.01)
        assertEquals("km", res.toSymbol)
    }

    @Test
    fun testTemperatureCelsiusToFahrenheit() {
        val res = UnitConverter.parseAndConvert("convierte 30 grados celsius a fahrenheit")
        assertNotNull(res)
        // 30°C = 86°F
        assertEquals(86.0, res!!.resultAmount, 0.01)
        assertEquals("°F", res.toSymbol)
    }

    @Test
    fun testIncompatibleUnitsRejected() {
        val res = UnitConverter.parseAndConvert("convierte 10 metros a kilos")
        assertNull(res)
    }
}

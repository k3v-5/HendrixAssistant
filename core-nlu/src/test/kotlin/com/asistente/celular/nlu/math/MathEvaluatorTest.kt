package com.asistente.celular.nlu.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal

class MathEvaluatorTest {

    @Test
    fun testSimpleAddition() {
        val res = MathEvaluator.evaluate("cuánto es 45 más 80")
        assertNotNull(res)
        assertEquals(0, BigDecimal("125").compareTo(res!!.value))
    }

    @Test
    fun testDivisionAndDecimals() {
        val res = MathEvaluator.evaluate("1500 entre 12")
        assertNotNull(res)
        assertEquals(0, BigDecimal("125").compareTo(res!!.value))
    }

    @Test
    fun testPrecedenceMultiplicationAddition() {
        val res = MathEvaluator.evaluate("calcula 10 más 5 por 4")
        assertNotNull(res)
        // 5 * 4 = 20 + 10 = 30
        assertEquals(0, BigDecimal("30").compareTo(res!!.value))
    }

    @Test
    fun testPercentageDirect() {
        val res = MathEvaluator.evaluate("el 15 por ciento de 800")
        assertNotNull(res)
        assertEquals(0, BigDecimal("120").compareTo(res!!.value))
    }

    @Test
    fun testPowers() {
        val resSquare = MathEvaluator.evaluate("5 al cuadrado")
        assertNotNull(resSquare)
        assertEquals(0, BigDecimal("25").compareTo(resSquare!!.value))

        val resCube = MathEvaluator.evaluate("2 al cubo")
        assertNotNull(resCube)
        assertEquals(0, BigDecimal("8").compareTo(resCube!!.value))
    }
}

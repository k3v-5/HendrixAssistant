package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.math.MathSkill
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private val skill = MathSkill()

    @Test
    fun testArithmeticOperation() = runBlocking {
        val input = "cuánto es 15 más 27"
        val score = skill.score(dummyContext, input)
        assertTrue("Expected confidence >= 0.90 but got ${score.confidence}", score.confidence >= 0.90f)
        assertEquals("math", score.capturedSlots["type"])

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue("Output speech should contain 42, got: ${output.speech}", output.speech.contains("42"))
    }

    @Test
    fun testPercentageCalculation() = runBlocking {
        val input = "cuánto es el 15 por ciento de 200"
        val score = skill.score(dummyContext, input)
        assertTrue("Expected confidence >= 0.90 but got ${score.confidence}", score.confidence >= 0.90f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue("Output speech should contain 30, got: ${output.speech}", output.speech.contains("30"))
    }

    @Test
    fun testCurrencyConversion() = runBlocking {
        val input = "100 dólares a pesos mexicanos"
        val score = skill.score(dummyContext, input)
        assertTrue("Expected confidence >= 0.90 but got ${score.confidence}", score.confidence >= 0.90f)
        assertEquals("currency", score.capturedSlots["type"])

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue("Speech should contain MXN or pesos: ${output.speech}", output.speech.contains("pesos mexicanos") || output.speech.contains("MXN"))
    }

    @Test
    fun testUnitConversion() = runBlocking {
        val input = "5 kilómetros a metros"
        val score = skill.score(dummyContext, input)
        assertTrue("Expected confidence >= 0.90 but got ${score.confidence}", score.confidence >= 0.90f)
        assertEquals("unit", score.capturedSlots["type"])

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue("Speech should contain 5000 or 5,000: ${output.speech}", output.speech.contains("5000") || output.speech.contains("5,000"))
    }

    @Test
    fun testNoMatch() {
        val input = "prende el foco de la sala"
        val score = skill.score(dummyContext, input)
        assertTrue("Should not match, got confidence ${score.confidence}", score.confidence == 0.0f)
    }
}

package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.flashlight.FlashlightController
import com.asistente.celular.skills.flashlight.FlashlightSkill
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashlightSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class MockFlashlightController(
        var available: Boolean = true,
        var torchOn: Boolean = false
    ) : FlashlightController {
        override fun setTorch(enabled: Boolean): Boolean {
            if (!available) return false
            torchOn = enabled
            return true
        }

        override fun toggleTorch(): Boolean {
            if (!available) return false
            torchOn = !torchOn
            return torchOn
        }

        override fun isTorchOn(): Boolean = torchOn

        override fun isAvailable(): Boolean = available
    }

    @Test
    fun testTurnOnFlashlight() = runBlocking {
        val mock = MockFlashlightController(torchOn = false)
        val skill = FlashlightSkill(mock)

        val input = "prende la linterna"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.85f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue(mock.torchOn)
        assertTrue(output.speech.contains("encendida"))
    }

    @Test
    fun testTurnOffFlashlight() = runBlocking {
        val mock = MockFlashlightController(torchOn = true)
        val skill = FlashlightSkill(mock)

        val input = "apaga la linterna"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.85f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertFalse(mock.torchOn)
        assertTrue(output.speech.contains("apagada"))
    }

    @Test
    fun testToggleFlashlight() = runBlocking {
        val mock = MockFlashlightController(torchOn = false)
        val skill = FlashlightSkill(mock)

        val input = "alterna la linterna"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.85f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue(mock.torchOn)
    }

    @Test
    fun testQueryFlashlightState() = runBlocking {
        val mock = MockFlashlightController(torchOn = true)
        val skill = FlashlightSkill(mock)

        val input = "está encendida la linterna"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.85f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("encendida"))
        // El estado no debió modificarse
        assertTrue(mock.torchOn)
    }

    @Test
    fun testSmartBulbCommandDoesNotTriggerFlashlight() {
        val mock = MockFlashlightController()
        val skill = FlashlightSkill(mock)

        val input = "prende el foco de la sala"
        val score = skill.score(dummyContext, input)
        assertEquals(0.0f, score.confidence, 0.001f)
    }
}

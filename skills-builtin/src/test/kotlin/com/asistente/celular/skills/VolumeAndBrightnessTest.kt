package com.asistente.celular.skills

import android.content.Intent
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.BrightnessUiPayload
import com.asistente.celular.skills.system.BrightnessController
import com.asistente.celular.skills.system.SystemSettingsSkill
import com.asistente.celular.skills.system.VolumeSkill
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeAndBrightnessTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("No direct Android context in dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class MockBrightnessController : BrightnessController {
        var currentBrightness: Int = 50
        var hasPermission: Boolean = true
        var lastSetBrightness: Int? = null

        override fun setBrightness(percent: Int): Boolean {
            if (!hasPermission) return false
            currentBrightness = percent.coerceIn(1, 100)
            lastSetBrightness = currentBrightness
            return true
        }

        override fun getBrightness(): Int = currentBrightness

        override fun adjustBrightness(deltaPercent: Int): Int {
            if (!hasPermission) return currentBrightness
            currentBrightness = (currentBrightness + deltaPercent).coerceIn(1, 100)
            lastSetBrightness = currentBrightness
            return currentBrightness
        }

        override fun hasWritePermission(): Boolean = hasPermission

        override fun requestWritePermissionIntent(): Intent = Intent("android.settings.action.MANAGE_WRITE_SETTINGS")
    }

    @Test
    fun testVolumeSkillPatterns() {
        val skill = VolumeSkill()

        val phrasesToMatch = listOf(
            "silencia el celular",
            "modo silencio",
            "silenciar el telefono",
            "sube el volumen",
            "aumenta el volumen",
            "baja el volumen",
            "disminuye el volumen de la musica",
            "pon el volumen al 50",
            "volumen al 80%",
            "sube el volumen de la alarma",
            "baja el volumen de llamada"
        )

        for (phrase in phrasesToMatch) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con patrón de volumen: '$phrase'", score.isMatch)
        }
    }

    @Test
    fun testBrightnessSkillWithPercentage() = runBlocking {
        val mockController = MockBrightnessController()
        val skill = SystemSettingsSkill(mockController)

        val phrase = "pon el brillo al 75%"
        val score = skill.score(dummyContext, phrase)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, phrase, score)
        assertTrue(output.success)
        assertEquals(75, mockController.lastSetBrightness)

        val payload = output.payload as? BrightnessUiPayload
        assertNotNull(payload)
        assertEquals(75, payload?.percent)
        assertTrue(payload?.hasPermission == true)
    }

    @Test
    fun testBrightnessSkillRelativeAdjustments() = runBlocking {
        val mockController = MockBrightnessController()
        mockController.currentBrightness = 50
        val skill = SystemSettingsSkill(mockController)

        // Sube el brillo (+20%)
        val upScore = skill.score(dummyContext, "sube el brillo")
        assertTrue(upScore.isMatch)
        val upOutput = skill.execute(dummyContext, "sube el brillo", upScore)
        assertTrue(upOutput.success)
        assertEquals(70, mockController.currentBrightness)

        // Baja el brillo (-20%)
        val downScore = skill.score(dummyContext, "baja el brillo")
        assertTrue(downScore.isMatch)
        val downOutput = skill.execute(dummyContext, "baja el brillo", downScore)
        assertTrue(downOutput.success)
        assertEquals(50, mockController.currentBrightness)
    }

    @Test
    fun testBrightnessPermissionDeniedPayload() = runBlocking {
        val mockController = MockBrightnessController()
        mockController.hasPermission = false
        val skill = SystemSettingsSkill(mockController)

        val phrase = "brillo al 100%"
        val score = skill.score(dummyContext, phrase)
        val output = skill.execute(dummyContext, phrase, score)

        val payload = output.payload as? BrightnessUiPayload
        assertNotNull(payload)
        assertEquals(false, payload?.hasPermission)
    }

    @Test
    fun testConnectivityPanelsPatterns() {
        val skill = SystemSettingsSkill()

        val connectivityPhrases = listOf(
            "abre el wifi",
            "ajustes de wifi",
            "abrir bluetooth",
            "panel de bluetooth",
            "punto de acceso",
            "zona wifi",
            "abrir modo avion"
        )

        for (phrase in connectivityPhrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con ajuste de conectividad: '$phrase'", score.isMatch)
        }
    }
}

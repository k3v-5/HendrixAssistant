package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.system.DeviceControlSkill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceControlSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private val skill = DeviceControlSkill()

    @Test
    fun testBatteryQuery() {
        val score = skill.score(dummyContext, "cuánta batería me queda")
        assertTrue(score.confidence >= 0.90f)
        assertEquals("battery", score.capturedSlots["action"])
    }

    @Test
    fun testStorageQuery() {
        val score = skill.score(dummyContext, "cuánto espacio libre tengo")
        assertTrue(score.confidence >= 0.90f)
        assertEquals("storage", score.capturedSlots["action"])
    }

    @Test
    fun testDndMode() {
        val score = skill.score(dummyContext, "activa el modo no molestar")
        assertTrue(score.confidence >= 0.90f)
        assertEquals("dnd", score.capturedSlots["action"])
        assertEquals("true", score.capturedSlots["enable"])

        val disableScore = skill.score(dummyContext, "desactiva el modo no molestar")
        assertTrue(disableScore.confidence >= 0.90f)
        assertEquals("dnd", disableScore.capturedSlots["action"])
        assertEquals("false", disableScore.capturedSlots["enable"])
    }

    @Test
    fun testRingerModes() {
        val silentScore = skill.score(dummyContext, "pon el celular en silencio")
        assertTrue(silentScore.confidence >= 0.90f)
        assertEquals("ringer", silentScore.capturedSlots["action"])
        assertEquals("silent", silentScore.capturedSlots["mode"])

        val vibrateScore = skill.score(dummyContext, "activa el modo vibración")
        assertTrue(vibrateScore.confidence >= 0.90f)
        assertEquals("ringer", vibrateScore.capturedSlots["action"])
        assertEquals("vibrate", vibrateScore.capturedSlots["mode"])
    }
}

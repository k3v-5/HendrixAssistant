package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.alarm.AlarmSkill
import com.asistente.celular.skills.applauncher.AppLauncherSkill
import com.asistente.celular.skills.flashlight.FlashlightSkill
import com.asistente.celular.skills.media.MediaControlSkill
import com.asistente.celular.skills.time.CurrentTimeSkill
import com.asistente.celular.skills.timer.TimerSkill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinSkillsTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    @Test
    fun testFlashlightSkillMatches() {
        val skill = FlashlightSkill()
        val score1 = skill.score(dummyContext, "enciende la linterna")
        assertTrue(score1.isMatch)

        val score2 = skill.score(dummyContext, "apaga la linterna")
        assertTrue(score2.isMatch)

        val score3 = skill.score(dummyContext, "linterna on")
        assertTrue(score3.isMatch)
    }

    @Test
    fun testTimerSkillMatches() {
        val skill = TimerSkill()
        val score = skill.score(dummyContext, "pon un temporizador de 5 minutos")
        assertTrue(score.isMatch)
        assertEquals("5 minutos", score.capturedSlots["duration"])
    }

    @Test
    fun testAlarmSkillMatches() {
        val skill = AlarmSkill()
        val score = skill.score(dummyContext, "pon una alarma a las 7 de la manana")
        assertTrue(score.isMatch)
        assertEquals("7 de la manana", score.capturedSlots["time"])
    }

    @Test
    fun testAppLauncherSkillMatches() {
        val skill = AppLauncherSkill()
        val score = skill.score(dummyContext, "abre la app spotify")
        assertTrue(score.isMatch)
        assertEquals("spotify", score.capturedSlots["appName"])
    }

    @Test
    fun testCurrentTimeSkillMatches() {
        val skill = CurrentTimeSkill()
        val score1 = skill.score(dummyContext, "que hora es")
        assertTrue(score1.isMatch)

        val score2 = skill.score(dummyContext, "qué día es hoy")
        assertTrue(score2.isMatch)

        kotlinx.coroutines.runBlocking {
            val output = skill.execute(dummyContext, "Qué día es hoy", score2)
            assertTrue(output.displayText.startsWith("Hoy es"))
        }
    }

    @Test
    fun testMediaControlSkillMatches() {
        val skill = MediaControlSkill()
        val score1 = skill.score(dummyContext, "pausa la musica")
        assertTrue(score1.isMatch)

        val score2 = skill.score(dummyContext, "siguiente cancion")
        assertTrue(score2.isMatch)
    }

    @Test
    fun testHelpSkillMatches() {
        val skill = com.asistente.celular.skills.help.HelpSkill()
        val score1 = skill.score(dummyContext, "que puedes hacer")
        assertTrue(score1.isMatch)

        val score2 = skill.score(dummyContext, "que cosas puedes hacer")
        assertTrue(score2.isMatch)

        val score3 = skill.score(dummyContext, "cuales son tus funciones")
        assertTrue(score3.isMatch)

        val score4 = skill.score(dummyContext, "ayuda")
        assertTrue(score4.isMatch)
    }
}

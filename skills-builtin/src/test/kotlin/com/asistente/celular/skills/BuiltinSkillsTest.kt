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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

        val score2 = skill.score(dummyContext, "ponme un temporizador de 10 minutos")
        assertTrue(score2.isMatch)
        assertEquals("10 minutos", score2.capturedSlots["duration"])

        // Sin duración especificada
        val scoreNoDuration = skill.score(dummyContext, "ponme un temporizador")
        assertTrue(scoreNoDuration.isMatch)
        assertNull(scoreNoDuration.capturedSlots["duration"])

        val scoreOnlyWord = skill.score(dummyContext, "temporizador")
        assertTrue(scoreOnlyWord.isMatch)
    }

    @Test
    fun testAlarmSkillMatches() {
        val skill = AlarmSkill()
        val score = skill.score(dummyContext, "pon una alarma a las 7 de la manana")
        assertTrue(score.isMatch)
        assertEquals("7 de la manana", score.capturedSlots["time"])

        val score2 = skill.score(dummyContext, "Ponme una alarma a las 7 de la noche")
        assertTrue("Ponme una alarma a las 7 de la noche debe coincidir", score2.isMatch)
        assertEquals("7 de la noche", score2.capturedSlots["time"])

        // Sin hora especificada ("Ponme una alarma")
        val scoreNoTime = skill.score(dummyContext, "Ponme una alarma")
        assertTrue("Ponme una alarma sin hora debe coincidir con AlarmSkill", scoreNoTime.isMatch)
        assertNull(scoreNoTime.capturedSlots["time"])

        // Verificar que "Ponme una alarma" o "una alarma" NO se parsea como la 1:00 a.m.
        assertNull(com.asistente.celular.nlu.parser.SpanishDateTimeParser.parseTime("Ponme una alarma"))
        assertNull(com.asistente.celular.nlu.parser.SpanishDateTimeParser.parseTime("una alarma"))
        assertNull(com.asistente.celular.nlu.parser.SpanishDateTimeParser.parseTime("comprar una mesa"))

        val scoreNoTime2 = skill.score(dummyContext, "pon una alarma")
        assertTrue(scoreNoTime2.isMatch)

        val scoreOnlyAlarm = skill.score(dummyContext, "alarma")
        assertTrue(scoreOnlyAlarm.isMatch)
    }

    @Test
    fun testAppLauncherSkillMatches() {
        val skill = AppLauncherSkill()
        val score = skill.score(dummyContext, "abre la app spotify")
        assertTrue(score.isMatch)
        assertEquals("spotify", score.capturedSlots["appName"])

        val scoreAppExplicit = skill.score(dummyContext, "pon la app de spotify")
        assertTrue(scoreAppExplicit.isMatch)
        assertEquals("spotify", scoreAppExplicit.capturedSlots["appName"])

        // "ponme una alarma" NO debe ser interceptado por AppLauncherSkill
        val scoreAlarmCollision = skill.score(dummyContext, "ponme una alarma")
        assertFalse("AppLauncherSkill NO debe capturar 'ponme una alarma'", scoreAlarmCollision.isMatch)

        val scoreTimerCollision = skill.score(dummyContext, "pon un temporizador")
        assertFalse("AppLauncherSkill NO debe capturar 'pon un temporizador'", scoreTimerCollision.isMatch)
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

    @Test
    fun testVolumeSkillMatches() {
        val skill = com.asistente.celular.skills.system.VolumeSkill()
        val scoreRaise = skill.score(dummyContext, "sube el volumen")
        assertTrue(scoreRaise.isMatch)

        val scoreLower = skill.score(dummyContext, "baja el volumen")
        assertTrue(scoreLower.isMatch)

        val scoreLevel = skill.score(dummyContext, "pon el volumen al 50%")
        assertTrue(scoreLevel.isMatch)
        assertEquals("50", scoreLevel.capturedSlots["level"])

        val scoreSilence = skill.score(dummyContext, "silencia el celular")
        assertTrue(scoreSilence.isMatch)

        val scoreMute = skill.score(dummyContext, "modo silencio")
        assertTrue(scoreMute.isMatch)
    }

    @Test
    fun testSystemSettingsSkillMatches() {
        val skill = com.asistente.celular.skills.system.SystemSettingsSkill()
        val scoreWifi = skill.score(dummyContext, "abre los ajustes de wifi")
        assertTrue(scoreWifi.isMatch)

        val scoreBt = skill.score(dummyContext, "configurar bluetooth")
        assertTrue(scoreBt.isMatch)

        val scoreDnd = skill.score(dummyContext, "modo no molestar")
        assertTrue(scoreDnd.isMatch)
    }

    @Test
    fun testPhoneCallSkillMatches() {
        val skill = com.asistente.celular.skills.communication.PhoneCallSkill()
        val scoreName = skill.score(dummyContext, "llama a Juan")
        assertTrue(scoreName.isMatch)
        assertEquals("juan", scoreName.capturedSlots["target"])

        val scoreNum = skill.score(dummyContext, "marcar al 5512345678")
        assertTrue(scoreNum.isMatch)
        assertEquals("5512345678", scoreNum.capturedSlots["target"])

        val scoreCall = skill.score(dummyContext, "haz una llamada a Mama")
        assertTrue(scoreCall.isMatch)
        assertEquals("mama", scoreCall.capturedSlots["target"])
    }

    @Test
    fun testWhatsAppSkillMatches() {
        val skill = com.asistente.celular.skills.communication.WhatsAppSkill()
        val score1 = skill.score(dummyContext, "manda un mensaje por whatsapp a Juan diciendo ya voy en camino")
        assertTrue(score1.isMatch)

        val score2 = skill.score(dummyContext, "escribe a Carlos por whatsapp hola como estas")
        assertTrue(score2.isMatch)
        assertEquals("carlos", score2.capturedSlots["contact"])
        assertEquals("hola como estas", score2.capturedSlots["message"])
    }
}

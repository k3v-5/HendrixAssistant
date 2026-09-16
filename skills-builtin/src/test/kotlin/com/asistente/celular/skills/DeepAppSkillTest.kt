package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.deepapp.DeepAppSkill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepAppSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private val skill = DeepAppSkill()

    @Test
    fun testUberMatching() {
        val score = skill.score(dummyContext, "pide un uber al trabajo")
        assertTrue(score.confidence > 0.90f)
        assertEquals("uber", score.capturedSlots["action"])
        assertEquals("trabajo", score.capturedSlots["destination"])
    }

    @Test
    fun testGoogleMapsNavigation() {
        val score = skill.score(dummyContext, "cómo llegar al museo de antropología")
        assertTrue(score.confidence > 0.90f)
        assertEquals("navigate", score.capturedSlots["action"])
        assertEquals("museo de antropología", score.capturedSlots["destination"])
        assertEquals("google_maps", score.capturedSlots["app"])
    }

    @Test
    fun testWazeNavigation() {
        val score = skill.score(dummyContext, "llévame a casa en waze")
        assertTrue(score.confidence > 0.90f)
        assertEquals("navigate", score.capturedSlots["action"])
        assertEquals("casa", score.capturedSlots["destination"])
        assertEquals("waze", score.capturedSlots["app"])
    }

    @Test
    fun testMercadoLibreShopping() {
        val score = skill.score(dummyContext, "busca audífonos bluetooth en mercadolibre")
        assertTrue(score.confidence > 0.90f)
        assertEquals("shop", score.capturedSlots["action"])
        assertEquals("mercadolibre", score.capturedSlots["platform"])
        assertEquals("audífonos bluetooth", score.capturedSlots["query"])
    }

    @Test
    fun testAmazonShopping() {
        val score = skill.score(dummyContext, "busca funda para pixel en amazon")
        assertTrue(score.confidence > 0.90f)
        assertEquals("shop", score.capturedSlots["action"])
        assertEquals("amazon", score.capturedSlots["platform"])
        assertEquals("funda para pixel", score.capturedSlots["query"])
    }

    @Test
    fun testYouTubeVideo() {
        val score = skill.score(dummyContext, "busca música para programar en youtube")
        assertTrue(score.confidence > 0.90f)
        assertEquals("youtube", score.capturedSlots["action"])
        assertEquals("música para programar", score.capturedSlots["query"])
    }

    @Test
    fun testGmailEmail() {
        val score = skill.score(dummyContext, "manda un correo a soporte@hendrix.com con asunto ayuda")
        assertTrue(score.confidence > 0.90f)
        assertEquals("email", score.capturedSlots["action"])
        assertEquals("soporte@hendrix.com", score.capturedSlots["recipient"])
        assertEquals("ayuda", score.capturedSlots["subject"])
    }
}

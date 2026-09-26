package com.asistente.celular.skills.search

import com.asistente.celular.nlu.search.WebSearchEngine
import com.asistente.celular.nlu.search.WebSearchResponse
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.WebSearchResultItem
import com.asistente.celular.nlu.ui.WebSearchUiPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveWebSearchSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class FakeWebSearchEngine : WebSearchEngine {
        var lastQueryReceived: String? = null
        var shouldSucceed = true

        override suspend fun searchAndSynthesize(
            query: String,
            maxResults: Int
        ): WebSearchResponse {
            lastQueryReceived = query
            return if (shouldSucceed) {
                WebSearchResponse(
                    query = query,
                    spokenSummary = "Nanite es el sistema de geometría micropoligonal virtualizada de Unreal Engine 5.",
                    detailedMarkdown = "### Nanite en UE5\nNanite permite importar mallas de millones de polígonos sin pérdida de rendimiento.",
                    results = listOf(
                        WebSearchResultItem(
                            title = "Nanite Virtualized Geometry - Unreal Engine Docs",
                            snippet = "Nanite is Unreal Engine 5's virtualized micropolygon geometry system.",
                            url = "https://docs.unrealengine.com/5.4/en-US/nanite/",
                            sourceName = "Unreal Engine Docs"
                        )
                    ),
                    isSuccess = true
                )
            } else {
                WebSearchResponse(
                    query = query,
                    spokenSummary = "No pude conectarme a internet para investigar.",
                    detailedMarkdown = "⚠️ Error de red",
                    results = emptyList(),
                    isSuccess = false
                )
            }
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = LiveWebSearchSkill()

        assertTrue(skill.score(dummyContext, "busca en internet cómo compilar shaders en unreal").isMatch)
        assertTrue(skill.score(dummyContext, "investiga sobre la nueva versión de blender").isMatch)
        assertTrue(skill.score(dummyContext, "busca información de nanite en ue5").isMatch)
        assertTrue(skill.score(dummyContext, "documentación sobre audio mixers").isMatch)
        assertTrue(skill.score(dummyContext, "investiga en internet cotizaciones de gpu").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "pon un temporizador de 10 minutos").isMatch)
        assertFalse(skill.score(dummyContext, "enciende la linterna").isMatch)
    }

    @Test
    fun testSearchExecutionFlow() = runBlocking {
        val fakeEngine = FakeWebSearchEngine()
        val skill = LiveWebSearchSkill(fakeEngine)

        val input = "busca en internet cómo funciona nanite en unreal engine"
        val score = skill.score(dummyContext, input)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)

        assertEquals("cómo funciona nanite en unreal engine", fakeEngine.lastQueryReceived)
        assertTrue(output.speech.contains("Nanite"))
        assertTrue(output.speech.contains("Unreal Engine 5"))

        val payload = output.payload as? WebSearchUiPayload
        assertNotNull(payload)
        assertEquals("cómo funciona nanite en unreal engine", payload?.query)
        assertEquals(1, payload?.sources?.size)
        assertEquals("Unreal Engine Docs", payload?.sources?.first()?.sourceName)
    }

    @Test
    fun testSearchExecutionFallbackWhenDisconnected() = runBlocking {
        val skill = LiveWebSearchSkill(webSearchEngine = null)

        val input = "busca en internet blender 4.2"
        val score = skill.score(dummyContext, input)
        val output = skill.execute(dummyContext, input, score)

        assertTrue(output.speech.contains("no está disponible"))
    }
}

package com.asistente.celular.ai

import com.asistente.celular.ai.analyzer.ComplexityAnalyzer
import com.asistente.celular.ai.analyzer.ComplexityLevel
import com.asistente.celular.ai.client.LlmClient
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.ai.router.AiRouterSkill
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRouterTest {

    @Test
    fun testComplexityAnalyzerDetectsComplexQuestions() {
        val complex1 = ComplexityAnalyzer.analyze("explicame como funciona la fotosintesis")
        assertEquals(ComplexityLevel.COMPLEX_AI_REQUIRED, complex1)

        val complex2 = ComplexityAnalyzer.analyze("por que el cielo es azul y el mar es transparente")
        assertEquals(ComplexityLevel.COMPLEX_AI_REQUIRED, complex2)

        val complex3 = ComplexityAnalyzer.analyze("redacta una carta formal de agradecimiento")
        assertEquals(ComplexityLevel.COMPLEX_AI_REQUIRED, complex3)
    }

    @Test
    fun testAiRouterPriorityOnComplexQuestions() {
        val config = LlmConfig(provider = AiProvider.GEMINI, apiKey = "")
        val client = LlmClient { config }
        val aiSkill = AiRouterSkill(client) { config }

        val dummyContext = object : SkillContext {
            override val androidContext: android.content.Context get() = error("Dummy")
            override val isConnectedToInternet: Boolean = false
            override val previousOutput: SkillOutput? = null
        }

        val score = aiSkill.score(dummyContext, "explicame como funciona la relatividad general")
        assertTrue(score.confidence >= 0.9f)
        assertEquals(Specificity.HIGH, score.specificity)
    }

    @Test
    fun testAiRouterOfflineBehavior() = runBlocking {
        val config = LlmConfig(provider = AiProvider.GEMINI, apiKey = "test_key")
        val client = LlmClient { config }
        val aiSkill = AiRouterSkill(client) { config }

        val offlineContext = object : SkillContext {
            override val androidContext: android.content.Context get() = error("Dummy")
            override val isConnectedToInternet: Boolean = false
            override val previousOutput: SkillOutput? = null
        }

        val score = aiSkill.score(offlineContext, "que es la inteligencia artificial")
        val output = aiSkill.execute(offlineContext, "que es la inteligencia artificial", score)

        assertFalse(output.success)
        assertTrue(output.handledByAi)
        assertTrue(output.speech.contains("no tienes conexión"))
    }
}

package com.asistente.celular.skills.security

import com.asistente.celular.ai.audit.AiDestinationType
import com.asistente.celular.ai.audit.AiTrafficRecord
import com.asistente.celular.ai.audit.InMemoryAiTrafficAuditor
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.ui.AiTrafficAuditUiPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPrivacyTrafficSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: com.asistente.celular.nlu.skill.SkillOutput? = null
    }

    @Test
    fun `score should match AI privacy queries with high confidence`() {
        val auditor = InMemoryAiTrafficAuditor()
        val skill = AiPrivacyTrafficSkill(auditor)

        val score1 = skill.score(dummyContext, "auditoría de ia")
        assertTrue(score1.confidence >= 0.9f)

        val score2 = skill.score(dummyContext, "muestra el tráfico de inteligencia artificial")
        assertTrue(score2.confidence >= 0.7f)

        val score3 = skill.score(dummyContext, "limpiar auditoria de ia")
        assertTrue(score3.confidence >= 0.9f)
    }

    @Test
    fun `execute should report correct sovereignty stats and payload`() = runBlocking {
        val auditor = InMemoryAiTrafficAuditor()
        auditor.recordEvent(
            AiTrafficRecord(
                destination = AiDestinationType.LOCAL_NPU_SLM,
                modelName = "gemma-2b",
                promptBytes = 150,
                estimatedPromptTokens = 30,
                responseBytes = 300,
                estimatedResponseTokens = 60,
                latencyMs = 90,
                isFullyLocal = true,
                piiDetectedCount = 2,
                piiScrubbed = true
            )
        )
        auditor.recordEvent(
            AiTrafficRecord(
                destination = AiDestinationType.CLOUD_GEMINI,
                modelName = "gemini-1.5-flash",
                promptBytes = 200,
                estimatedPromptTokens = 40,
                responseBytes = 400,
                estimatedResponseTokens = 80,
                latencyMs = 350,
                isFullyLocal = false,
                piiDetectedCount = 0,
                piiScrubbed = false
            )
        )

        val skill = AiPrivacyTrafficSkill(auditor)
        val score = skill.score(dummyContext, "auditoría de ia")
        val output = skill.execute(dummyContext, "auditoría de ia", score)

        assertNotNull(output.payload)
        assertTrue(output.payload is AiTrafficAuditUiPayload)
        val payload = output.payload as AiTrafficAuditUiPayload
        assertEquals(2L, payload.totalRequests)
        assertEquals(1L, payload.localRequests)
        assertEquals(1L, payload.cloudRequests)
        assertEquals(50.0, payload.localRatioPercentage, 0.01)
        assertEquals(2, payload.piiProtectedCount)
        assertTrue(output.speech.contains("50.0%"))
    }

    @Test
    fun `execute should clear history when requested`() = runBlocking {
        val auditor = InMemoryAiTrafficAuditor()
        auditor.recordEvent(
            AiTrafficRecord(
                destination = AiDestinationType.LOCAL_NPU_SLM,
                modelName = "gemma-2b",
                promptBytes = 100,
                estimatedPromptTokens = 20,
                responseBytes = 200,
                estimatedResponseTokens = 40,
                latencyMs = 80
            )
        )

        val skill = AiPrivacyTrafficSkill(auditor)
        val score = skill.score(dummyContext, "limpiar auditoria de ia")
        val output = skill.execute(dummyContext, "limpiar auditoria de ia", score)

        assertTrue(output.speech.contains("eliminado por completo"))
        assertEquals(0, auditor.observeTraffic().value.size)
        val payload = output.payload as AiTrafficAuditUiPayload
        assertEquals(0L, payload.totalRequests)
    }
}

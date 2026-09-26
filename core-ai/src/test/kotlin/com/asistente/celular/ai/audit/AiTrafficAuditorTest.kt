package com.asistente.celular.ai.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTrafficAuditorTest {

    @Test
    fun `RegexPiiScrubber should mask sensitive data and report count`() {
        val scrubber = RegexPiiScrubber()
        val text = "Mi correo es usuario@ejemplo.com, mi teléfono es +1-555-123-4567 y mi tarjeta es 4111-2222-3333-4444 con API key sk-abcdef1234567890abcdef12345"

        val result = scrubber.inspectAndScrub(text)

        assertEquals(4, result.detectedCount)
        assertTrue(result.detectedTypes.contains("EMAIL"))
        assertTrue(result.detectedTypes.contains("TELEFONO"))
        assertTrue(result.detectedTypes.contains("TARJETA_BANCARIA"))
        assertTrue(result.detectedTypes.contains("API_KEY"))

        assertFalse(result.scrubbedText.contains("usuario@ejemplo.com"))
        assertFalse(result.scrubbedText.contains("4111-2222-3333-4444"))
        assertFalse(result.scrubbedText.contains("+1-555-123-4567"))
        assertFalse(result.scrubbedText.contains("sk-abcdef1234567890abcdef12345"))

        assertTrue(result.scrubbedText.contains("[EMAIL_PROTEGIDO]"))
        assertTrue(result.scrubbedText.contains("[TARJETA_PROTEGIDA]"))
        assertTrue(result.scrubbedText.contains("[TEL_PROTEGIDO]"))
        assertTrue(result.scrubbedText.contains("[API_KEY_PROTEGIDA]"))
    }

    @Test
    fun `InMemoryAiTrafficAuditor should accurately calculate local ratio and metrics`() {
        val auditor = InMemoryAiTrafficAuditor(maxRecords = 10)

        // 1. Registrar 3 peticiones locales
        repeat(3) {
            auditor.recordEvent(
                AiTrafficRecord(
                    destination = AiDestinationType.LOCAL_NPU_SLM,
                    modelName = "gemma-2b-it",
                    promptBytes = 100,
                    estimatedPromptTokens = 25,
                    responseBytes = 200,
                    estimatedResponseTokens = 50,
                    latencyMs = 120,
                    isFullyLocal = true,
                    piiDetectedCount = 1,
                    piiScrubbed = true
                )
            )
        }

        // 2. Registrar 1 petición en la nube
        auditor.recordEvent(
            AiTrafficRecord(
                destination = AiDestinationType.CLOUD_GEMINI,
                modelName = "gemini-1.5-flash",
                promptBytes = 300,
                estimatedPromptTokens = 75,
                responseBytes = 600,
                estimatedResponseTokens = 150,
                latencyMs = 450,
                isFullyLocal = false,
                piiDetectedCount = 0,
                piiScrubbed = false
            )
        )

        val summary = auditor.getSummary().value
        assertEquals(4L, summary.totalRequests)
        assertEquals(3L, summary.localRequests)
        assertEquals(1L, summary.cloudRequests)
        assertEquals(75.0, summary.localRatioPercentage, 0.01)
        assertEquals(600L, summary.totalPromptBytes) // 3*100 + 300
        assertEquals(1200L, summary.totalResponseBytes) // 3*200 + 600
        assertEquals(3, summary.totalPiiEntitiesProtected)

        val records = auditor.observeTraffic().value
        assertEquals(4, records.size)
        // El más reciente debe ser el de la nube
        assertEquals(AiDestinationType.CLOUD_GEMINI, records.first().destination)

        // 3. Probar limpieza de historial
        auditor.clearHistory()
        assertEquals(0, auditor.observeTraffic().value.size)
        assertEquals(0L, auditor.getSummary().value.totalRequests)
    }

    @Test
    fun `InMemoryAiTrafficAuditor should respect maxRecords capacity limit`() {
        val auditor = InMemoryAiTrafficAuditor(maxRecords = 5)
        repeat(10) { index ->
            auditor.recordEvent(
                AiTrafficRecord(
                    destination = AiDestinationType.LOCAL_LAN_OLLAMA,
                    modelName = "llama3:8b",
                    promptBytes = 50,
                    estimatedPromptTokens = 10,
                    responseBytes = 100,
                    estimatedResponseTokens = 20,
                    latencyMs = 80
                )
            )
        }

        val records = auditor.observeTraffic().value
        assertEquals(5, records.size)
    }
}

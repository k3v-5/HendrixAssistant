package com.asistente.celular.ai.audit

import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Destinos posibles para peticiones de Inteligencia Artificial y tareas cognitivas.
 */
enum class AiDestinationType(val displayName: String, val isLocal: Boolean) {
    LOCAL_NPU_SLM("NPU / SLM Local On-Device", true),
    LOCAL_LAN_OLLAMA("Ollama Servidor Local LAN", true),
    CLOUD_GEMINI("Google Gemini Cloud", false),
    CLOUD_OPENAI("OpenAI Cloud", false),
    CLOUD_GROQ("Groq LPU Cloud", false),
    REMOTE_PC_BRIDGE("Puente Hendrix PC", true)
}

/**
 * Registro atómico de auditoría para cada transacción de IA.
 */
data class AiTrafficRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestampEpoch: Long = System.currentTimeMillis(),
    val destination: AiDestinationType,
    val modelName: String,
    val promptBytes: Long,
    val estimatedPromptTokens: Int,
    val responseBytes: Long,
    val estimatedResponseTokens: Int,
    val latencyMs: Long,
    val isFullyLocal: Boolean = destination.isLocal,
    val piiDetectedCount: Int = 0,
    val piiScrubbed: Boolean = false,
    val success: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Resumen consolidado de soberanía y privacidad de datos.
 */
data class AiPrivacyMetricsSummary(
    val totalRequests: Long = 0,
    val localRequests: Long = 0,
    val cloudRequests: Long = 0,
    val localRatioPercentage: Double = 100.0,
    val totalPromptBytes: Long = 0,
    val totalResponseBytes: Long = 0,
    val totalTokensProcessed: Long = 0,
    val totalPiiEntitiesProtected: Int = 0,
    val averageLatencyMs: Long = 0
)

/**
 * Contrato para el auditor de tráfico y privacidad de IA.
 */
interface AiTrafficAuditor {
    fun recordEvent(record: AiTrafficRecord)
    fun observeTraffic(): StateFlow<List<AiTrafficRecord>>
    fun getSummary(): StateFlow<AiPrivacyMetricsSummary>
    fun clearHistory()
}

/**
 * Resultado de inspección y anonimización de datos sensibles (PII).
 */
data class PiiScrubResult(
    val scrubbedText: String,
    val detectedCount: Int,
    val detectedTypes: List<String>
)

/**
 * Contrato para sanitizadores de datos personales antes de enviar prompts.
 */
interface PiiScrubber {
    fun inspectAndScrub(text: String): PiiScrubResult
}

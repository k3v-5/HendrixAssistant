package com.asistente.celular.ai.orchestration

enum class QueryComplexityLevel {
    TRIVIAL_DEVICE_ACTION,
    GENERAL_KNOWLEDGE,
    COMPLEX_REASONING_OR_CODE
}

enum class InferenceLatencyBudget {
    ULTRA_LOW_300MS,
    BALANCED_1S,
    DEEP_UNCONSTRAINED
}

data class ModelExecutionPlan(
    val selectedModelName: String,
    val expectedLatencyMs: Int,
    val useSpeculativeFastPath: Boolean,
    val requiresCloudEscalation: Boolean,
    val complexityLevel: QueryComplexityLevel = QueryComplexityLevel.TRIVIAL_DEVICE_ACTION
)

/**
 * Contrato para el orquestador multi-modelo y enrutamiento especulativo de baja latencia.
 */
interface MultiModelOrchestrator {
    fun planExecution(prompt: String): ModelExecutionPlan
    suspend fun executeSpeculative(prompt: String, onTokenStream: (String) -> Unit): String
    fun getOrchestrationTelemetry(): Map<String, Any>
}

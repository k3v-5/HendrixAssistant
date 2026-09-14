package com.asistente.celular.ai.harness

import com.asistente.celular.ai.model.AiProvider
import kotlinx.serialization.Serializable

/**
 * Nivel o categoría de capacidad de razonamiento de un modelo.
 */
@Serializable
enum class ReasoningTier(val priority: Int, val label: String) {
    FAST(1, "Rápido"),                    // Tareas breves, QA directo, conversación ágil
    BALANCED(2, "Balanceado"),            // Síntesis equilibrada y velocidad moderada
    DEEP_REASONING(3, "Razonamiento"),     // Análisis profundo, código, comparativas técnicas
    THINKING(4, "Pensamiento Lógico")     // Lógica pura paso a paso, acertijos, matemáticas
}

/**
 * Perfil y metadatos de un modelo evaluable por el Harness.
 */
@Serializable
data class ModelProfile(
    val modelId: String,
    val displayName: String,
    val provider: AiProvider,
    val tier: ReasoningTier,
    val description: String,
    val contextWindowTokens: Int = 1_000_000,
    val supportsThinking: Boolean = false
)

/**
 * Registro de modelos disponibles para el Harness.
 * Diseñado con arquitectura abierta a la extensión (OCP).
 */
object ModelRegistry {

    val GEMINI_FLASH_LATEST = ModelProfile(
        modelId = "gemini-flash-latest",
        displayName = "Gemini Flash",
        provider = AiProvider.GEMINI,
        tier = ReasoningTier.FAST,
        description = "Velocidad extrema y bajo consumo para respuestas inmediatas."
    )

    val GEMINI_2_5_FLASH = ModelProfile(
        modelId = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        provider = AiProvider.GEMINI,
        tier = ReasoningTier.BALANCED,
        description = "Próxima generación multimodal de alta velocidad y consistencia."
    )

    val GEMINI_PRO_LATEST = ModelProfile(
        modelId = "gemini-pro-latest",
        displayName = "Gemini Pro",
        provider = AiProvider.GEMINI,
        tier = ReasoningTier.DEEP_REASONING,
        description = "Razonamiento analítico profundo, programación y problemas complejos."
    )

    val GEMINI_THINKING = ModelProfile(
        modelId = "gemini-3.7-flash",
        displayName = "Gemini Thinking",
        provider = AiProvider.GEMINI,
        tier = ReasoningTier.THINKING,
        description = "Cadena de pensamiento explícita para lógica, matemáticas y deducción.",
        supportsThinking = true
    )

    val DEFAULT_GEMINI_POOL = listOf(
        GEMINI_FLASH_LATEST,
        GEMINI_2_5_FLASH,
        GEMINI_PRO_LATEST,
        GEMINI_THINKING
    )

    fun getModelById(id: String): ModelProfile? {
        return DEFAULT_GEMINI_POOL.firstOrNull { it.modelId == id }
    }
}

package com.asistente.celular.ai.harness

import java.text.Normalizer
import java.util.Locale

/**
 * Decisión emitida por el enrutador del Harness.
 */
data class RoutingDecision(
    val selectedModel: ModelProfile,
    val reason: String,
    val fallbackModel: ModelProfile,
    val confidence: Float = 1.0f
)

/**
 * Contrato de enrutador de modelos (Strategy Pattern).
 * Permite cambiar la estrategia de enrutamiento (heurística, por embeddings, o por meta-prompt).
 */
interface ModelRouter {
    fun route(prompt: String, availableModels: List<ModelProfile>): RoutingDecision
}

/**
 * Enrutador heurístico de alta velocidad (0 ms de sobrecarga) para la familia Google Gemini.
 * Analiza la naturaleza semántica de la consulta y elige el modelo más idóneo.
 */
class GeminiHeuristicRouter : ModelRouter {

    private val thinkingTriggers = listOf(
        "paso a paso", "acertijo", "demuestra", "demostracion", "deduccion",
        "logica", "problema de logica", "resuelve paso a paso", "ecuacion",
        "calculo complejo", "rompecabezas", "demuestrame"
    )

    private val deepReasoningTriggers = listOf(
        "compara", "comparativa", "diferencia entre", "analiza", "analisis",
        "arquitectura", "codigo", "programa en", "refactoriza", "algoritmo",
        "escribe un ensayo", "ensayo", "pros y contras", "ventajas y desventajas",
        "disena un sistema", "estructura de datos", "filosofia de"
    )

    override fun route(prompt: String, availableModels: List<ModelProfile>): RoutingDecision {
        val normalized = normalize(prompt)
        val flashModel = availableModels.firstOrNull { it.tier == ReasoningTier.FAST }
            ?: ModelRegistry.GEMINI_FLASH_LATEST

        // 1. Criterio de Pensamiento Lógico / Cadena de deducción
        for (trigger in thinkingTriggers) {
            if (normalized.contains(trigger)) {
                val thinkingModel = availableModels.firstOrNull { it.supportsThinking }
                    ?: availableModels.firstOrNull { it.tier == ReasoningTier.DEEP_REASONING }
                    ?: flashModel

                return RoutingDecision(
                    selectedModel = thinkingModel,
                    reason = "Requiere deducción y desglose lógico paso a paso ('$trigger').",
                    fallbackModel = flashModel
                )
            }
        }

        // 2. Criterio de Razonamiento Profundo / Código / Comparativas
        for (trigger in deepReasoningTriggers) {
            if (normalized.contains(trigger)) {
                val proModel = availableModels.firstOrNull { it.tier == ReasoningTier.DEEP_REASONING }
                    ?: flashModel

                return RoutingDecision(
                    selectedModel = proModel,
                    reason = "Consulta analítica o de desarrollo técnico detectada ('$trigger').",
                    fallbackModel = flashModel
                )
            }
        }

        // 3. Criterio por longitud y complejidad sintáctica
        val words = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.size >= 25) {
            val proModel = availableModels.firstOrNull { it.tier == ReasoningTier.DEEP_REASONING }
                ?: flashModel
            return RoutingDecision(
                selectedModel = proModel,
                reason = "Consulta extensa con múltiples instrucciones (${words.size} palabras).",
                fallbackModel = flashModel
            )
        }

        // 4. Criterio por defecto: Modelo Flash de respuesta rápida y bajo consumo
        return RoutingDecision(
            selectedModel = flashModel,
            reason = "Pregunta directa optimizada para mínima latencia con Gemini Flash.",
            fallbackModel = flashModel
        )
    }

    private fun normalize(text: String): String {
        return Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .trim()
    }
}

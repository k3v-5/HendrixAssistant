package com.asistente.celular.nlu.evaluator

import com.asistente.celular.nlu.skill.InteractionPlan
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Evento de interacción registrado en la conversación.
 */
data class InteractionEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userInput: String,
    val skillName: String,
    val output: SkillOutput,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estado general del evaluador del asistente.
 */
data class AssistantState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isSpeaking: Boolean = false,
    val currentPrompt: String? = null,
    val history: List<InteractionEntry> = emptyList(),
    val error: String? = null
)

/**
 * Orquestador principal de evaluación de comandos del asistente.
 */
class SkillEvaluator(
    private val ranker: SkillRanker,
    private val skillContext: SkillContext,
    private val onSpeak: suspend (String) -> Unit = {},
    private val onReopenMic: suspend () -> Unit = {}
) {
    private val _state = MutableStateFlow(AssistantState())
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    /**
     * Procesa una entrada de texto proveniente de voz (STT) o teclado.
     */
    suspend fun processInput(userInput: String): SkillOutput {
        val rawTrimmed = userInput.trim()
        if (rawTrimmed.isBlank()) {
            return SkillOutput(speech = "", displayText = "")
        }

        // Limpiar prefijos comunes de invocación ("Oye Hendrix", "Hendrix", "Hey Hendrix", etc.)
        // para que las habilidades locales offline coincidan al 100% aunque el usuario incluya el nombre.
        var cleanedInput = rawTrimmed
        val lower = rawTrimmed.lowercase()
        val prefixes = listOf("oye hendrix", "hey hendrix", "hola hendrix", "ok hendrix", "hendrix", "oye", "hola")
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                var candidate = rawTrimmed.substring(prefix.length).trim().trimStart(',', ':', ';', '-').trim()
                if (candidate.lowercase().startsWith("por favor")) {
                    candidate = candidate.substring(9).trim().trimStart(',', ':', ';', '-').trim()
                }
                if (candidate.isNotBlank()) {
                    cleanedInput = candidate
                }
                break
            }
        }

        _state.value = _state.value.copy(
            isProcessing = true,
            currentPrompt = rawTrimmed,
            error = null
        )

        try {
            // 1. Determinar la mejor habilidad (evaluando con el texto limpio)
            val match = ranker.findBestSkill(skillContext, cleanedInput)

            // 2. Ejecutar la habilidad seleccionada
            val output = match.skill.execute(skillContext, cleanedInput, match.score)

            // 3. Registrar en historial
            val entry = InteractionEntry(
                userInput = rawTrimmed,
                skillName = match.skill.info.name,
                output = output
            )

            _state.value = _state.value.copy(
                isProcessing = false,
                currentPrompt = null,
                history = _state.value.history + entry
            )

            // 4. Síntesis de voz (TTS) si hay respuesta hablada
            if (output.speech.isNotBlank()) {
                _state.value = _state.value.copy(isSpeaking = true)
                try {
                    onSpeak(output.speech)
                } finally {
                    _state.value = _state.value.copy(isSpeaking = false)
                }
            }

            // 5. Manejar plan de interacción (ej: reabrir micrófono si la IA o skill pregunta algo)
            when (output.interactionPlan) {
                is InteractionPlan.ReopenMicrophone -> {
                    onReopenMic()
                }
                else -> { /* Finalizar normalmente */ }
            }

            return output

        } catch (e: Exception) {
            val errorOutput = SkillOutput(
                speech = "Ocurrió un problema al procesar la solicitud: ${e.localizedMessage ?: "error desconocido"}",
                displayText = "Error: ${e.localizedMessage}",
                success = false
            )
            _state.value = _state.value.copy(
                isProcessing = false,
                isSpeaking = false,
                error = e.localizedMessage
            )
            if (errorOutput.speech.isNotBlank()) {
                onSpeak(errorOutput.speech)
            }
            return errorOutput
        }
    }
}

package com.asistente.celular.nlu.evaluator

import com.asistente.celular.nlu.skill.InteractionPlan
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.asistente.celular.nlu.context.ConversationSessionTracker
import com.asistente.celular.nlu.context.CoreferenceResolver
import com.asistente.celular.nlu.context.DialogEntity
import com.asistente.celular.nlu.context.EntityType
import com.asistente.celular.nlu.ui.SmartBulbUiPayload
import com.asistente.celular.nlu.ui.VolumeUiPayload

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
    private val sessionTracker: ConversationSessionTracker = ConversationSessionTracker(),
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
        val prefixes = listOf(
            "oye hendrix", "hey hendrix", "hola hendrix", "ok hendrix", "hendrix",
            "buenos dias", "buenos días", "buenas tardes", "buenas noches",
            "oye", "hola"
        )
        var matchedPrefixOnly = false
        for (prefix in prefixes) {
            if (lower == prefix || lower.startsWith("$prefix ") || lower.startsWith("$prefix,") || lower.startsWith("$prefix:") || lower.startsWith("$prefix;")) {
                var candidate = rawTrimmed.substring(prefix.length).trim().trimStart(',', ':', ';', '-').trim()
                if (candidate.lowercase().startsWith("por favor")) {
                    candidate = candidate.substring(9).trim().trimStart(',', ':', ';', '-').trim()
                }
                if (candidate.isNotBlank()) {
                    cleanedInput = candidate
                } else {
                    matchedPrefixOnly = true
                }
                break
            }
        }

        if (matchedPrefixOnly) {
            val greeting = "¡Hola! ¿En qué te puedo ayudar?"
            val output = SkillOutput(
                speech = greeting,
                displayText = greeting,
                interactionPlan = InteractionPlan.ReopenMicrophone()
            )
            val entry = InteractionEntry(
                userInput = rawTrimmed,
                skillName = "Hendrix",
                output = output
            )
            _state.value = _state.value.copy(
                isProcessing = false,
                currentPrompt = null,
                history = _state.value.history + entry
            )
            if (output.speech.isNotBlank()) {
                _state.value = _state.value.copy(isSpeaking = true)
                try {
                    onSpeak(output.speech)
                } finally {
                    _state.value = _state.value.copy(isSpeaking = false)
                }
            }
            onReopenMic()
            return output
        }

        _state.value = _state.value.copy(
            isProcessing = true,
            currentPrompt = rawTrimmed,
            error = null
        )

        try {
            // 1. Resolver correferencias y anáforas basadas en turnos previos ("apágala", "hazla más tenue", etc.)
            val resolved = CoreferenceResolver.resolve(cleanedInput, sessionTracker)
            val effectiveInput = resolved.resolvedText

            // 2. Determinar la mejor habilidad
            val match = ranker.findBestSkill(skillContext, effectiveInput)

            // 3. Ejecutar la habilidad seleccionada
            val output = match.skill.execute(skillContext, effectiveInput, match.score)

            // 4. Rastrear entidad para multiturno
            val detectedEntity = when (val payload = output.payload) {
                is SmartBulbUiPayload -> DialogEntity(payload.deviceName, EntityType.LIGHT)
                is VolumeUiPayload -> DialogEntity(payload.streamName, EntityType.VOLUME_STREAM)
                else -> {
                    when {
                        match.skill.info.id.contains("media") -> DialogEntity("música", EntityType.MEDIA_TRACK)
                        match.skill.info.id.contains("smarthome") -> DialogEntity("foco", EntityType.LIGHT)
                        else -> resolved.resolvedEntity
                    }
                }
            }

            sessionTracker.recordTurn(
                userInput = effectiveInput,
                executedSkillId = match.skill.info.id,
                primaryEntity = detectedEntity,
                assistantResponse = output.speech
            )

            // 5. Registrar en historial
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

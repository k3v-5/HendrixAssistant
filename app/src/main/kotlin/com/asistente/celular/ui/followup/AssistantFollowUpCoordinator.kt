package com.asistente.celular.ui.followup

import android.util.Log
import com.asistente.celular.voice.earcon.EarconEngine
import com.asistente.celular.voice.earcon.EarconType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estados del Modo Continuación (Follow-up Mode) conversacional.
 */
enum class FollowUpState {
    INACTIVE,
    LISTENING,
    COMPLETED
}

/**
 * Coordinador de Modo Continuación estilo Alexa / Echo Show.
 * Mantiene el micrófono y la interfaz activos durante una ventana de 6 segundos tras finalizar
 * la respuesta del asistente, permitiendo encadenar comandos sin repetir la palabra de activación ("Hendrix").
 */
class AssistantFollowUpCoordinator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val earconEngine: EarconEngine? = null,
    private val windowDurationMillis: Long = 6000L
) {
    companion object {
        private const val TAG = "FollowUpCoordinator"

        private val CLOSING_PHRASES = setOf(
            "gracias", "muchas gracias", "listo", "adios", "adiós",
            "cancela", "cierra", "nada mas", "nada más", "ya está", "ya esta",
            "detente", "termina", "eso es todo"
        )
    }

    private val _state = MutableStateFlow(FollowUpState.INACTIVE)
    val state: StateFlow<FollowUpState> = _state.asStateFlow()

    private var timeoutJob: Job? = null

    /**
     * Inicia la ventana de seguimiento tras concluir el habla del TTS.
     */
    fun startFollowUpWindow(
        onListenAgain: () -> Unit,
        onDismiss: () -> Unit
    ) {
        cancel()
        _state.value = FollowUpState.LISTENING
        Log.i(TAG, "Iniciando ventana de seguimiento conversacional (${windowDurationMillis}ms)...")

        // Activa la escucha sin repetir la palabra clave
        onListenAgain()

        timeoutJob = scope.launch {
            delay(windowDurationMillis)
            if (_state.value == FollowUpState.LISTENING) {
                Log.i(TAG, "Ventana de seguimiento finalizada por inactividad. Cerrando amablemente...")
                earconEngine?.playEarcon(EarconType.DISMISS_PROMPT)
                _state.value = FollowUpState.COMPLETED
                onDismiss()
            }
        }
    }

    /**
     * Evalúa si una frase hablada por el usuario es un agradecimiento o despedida de cierre.
     */
    fun isClosingPhrase(phrase: String): Boolean {
        val clean = phrase.lowercase(Locale.ROOT).trim().replace("[.,!¡?¿]".toRegex(), "")
        return clean in CLOSING_PHRASES ||
                clean.startsWith("gracias") ||
                clean.startsWith("adiós") ||
                clean.startsWith("adios") ||
                clean.startsWith("eso es todo")
    }

    /**
     * Cancela la ventana activa de seguimiento.
     */
    fun cancel() {
        timeoutJob?.cancel()
        timeoutJob = null
        if (_state.value != FollowUpState.INACTIVE) {
            _state.value = FollowUpState.INACTIVE
        }
    }
}

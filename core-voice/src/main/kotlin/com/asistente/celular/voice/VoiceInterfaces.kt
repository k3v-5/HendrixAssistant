package com.asistente.celular.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * Estados del subsistema de voz.
 */
enum class VoiceState {
    IDLE,
    WAITING_FOR_WAKEWORD,
    LISTENING_USER_SPEECH,
    PROCESSING_AUDIO,
    SPEAKING
}

/**
 * Interfaz para el motor de reconocimiento de voz a texto (STT / ASR).
 */
interface SttEngine {
    val isListening: Boolean

    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun stopListening()
    fun release()
}

/**
 * Interfaz para la síntesis de voz (TTS / Text to Speech).
 */
interface TtsEngine {
    val isSpeaking: Boolean

    /**
     * Sintetiza y reproduce el texto, suspendiendo la corrutina hasta que termina de hablar.
     */
    suspend fun speak(text: String)

    fun stop()
    fun release()
}

/**
 * Datos del evento de activación de palabra clave.
 */
data class WakeWordEvent(
    val keyword: String,
    val fullUtterance: String = keyword,
    val command: String? = null
)

/**
 * Interfaz para la detección de palabra de activación (KWS / Wake Word).
 */
interface WakeWordEngine {
    val isListening: Boolean
    val currentKeyword: String

    fun startListening(onKeywordDetected: (keyword: String) -> Unit)
    fun startListeningWithEvent(onEventDetected: (WakeWordEvent) -> Unit) {
        startListening { onEventDetected(WakeWordEvent(it)) }
    }
    fun stopListening()
    fun release()
}

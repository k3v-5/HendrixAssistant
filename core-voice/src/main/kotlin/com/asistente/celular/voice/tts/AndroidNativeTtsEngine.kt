package com.asistente.celular.voice.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.asistente.celular.voice.TtsEngine
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Motor de síntesis de voz offline que aprovecha el sintetizador nativo de Android.
 * Funciona sin conexión a internet y sin modelos pesados adicionales.
 */
class AndroidNativeTtsEngine(
    context: Context,
    private val preferredLocale: Locale = Locale("es", "ES"),
    var pitch: Float = 1.08f,
    var speechRate: Float = 1.02f
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingSpeechQueue = mutableListOf<String>()

    @Volatile
    override var isSpeaking: Boolean = false
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(preferredLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Idioma español no soportado directamente, usando idioma del sistema.")
                    tts?.language = Locale.getDefault()
                }

                // Ajustar entonación cálida y natural para que no suene robótica
                tts?.setPitch(pitch)
                tts?.setSpeechRate(speechRate)

                // Seleccionar automáticamente la voz en español más amigable y de mayor fidelidad
                configureBestFriendlyVoice()

                isInitialized = true
                Log.d(TAG, "TTS nativo inicializado con voz amigable (pitch=$pitch, rate=$speechRate).")
            } else {
                Log.e(TAG, "Fallo al inicializar TTS de Android.")
            }
        }
    }

    private fun configureBestFriendlyVoice() {
        try {
            val voices = tts?.voices ?: return
            val spanishVoices = voices.filter { voice ->
                voice.locale.language == "es" && !voice.isNetworkConnectionRequired
            }
            if (spanishVoices.isEmpty()) return

            // Preferir voces de alta calidad con entonación natural
            val friendlyVoice = spanishVoices
                .filter { voice ->
                    !voice.name.contains("male", ignoreCase = true) &&
                    (voice.quality >= android.speech.tts.Voice.QUALITY_HIGH || voice.name.contains("local"))
                }
                .maxByOrNull { it.quality }
                ?: spanishVoices.maxByOrNull { it.quality }

            if (friendlyVoice != null) {
                tts?.voice = friendlyVoice
                Log.i(TAG, "Voz amigable seleccionada: ${friendlyVoice.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Aviso seleccionando voz amigable: ${e.message}")
        }
    }

    override suspend fun speak(text: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS aún no inicializado, omitiendo reproducción inmediata.")
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val utteranceId = UUID.randomUUID().toString()
        isSpeaking = true

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                if (id == utteranceId) isSpeaking = true
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    isSpeaking = false
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    isSpeaking = false
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) {
                    Log.e(TAG, "Error en TTS utterance: $errorCode")
                    isSpeaking = false
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        })

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speechRate)
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

        continuation.invokeOnCancellation {
            stop()
        }
    }

    override fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    override fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "AndroidNativeTts"
    }
}

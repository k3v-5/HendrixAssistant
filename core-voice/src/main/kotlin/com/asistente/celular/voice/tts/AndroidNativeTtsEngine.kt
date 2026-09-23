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
    var pitch: Float = 1.0f,
    var speechRate: Float = 1.0f,
    var isWhisperMode: Boolean = false
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = kotlinx.coroutines.CompletableDeferred<Boolean>()

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

                // Ajustar entonación y velocidad naturales por defecto (1.0f)
                tts?.setPitch(pitch)
                tts?.setSpeechRate(speechRate)

                // Seleccionar automáticamente la voz en español de mayor fidelidad y claridad disponible
                configureBestFriendlyVoice()

                isInitialized = true
                if (!initDeferred.isCompleted) {
                    initDeferred.complete(true)
                }
                Log.d(TAG, "TTS nativo inicializado con voz natural (pitch=$pitch, rate=$speechRate).")
            } else {
                Log.e(TAG, "Fallo al inicializar TTS de Android.")
                if (!initDeferred.isCompleted) {
                    initDeferred.complete(false)
                }
            }
        }
    }

    /**
     * Actualiza la afinación (pitch) y velocidad de habla (speechRate) en caliente.
     */
    fun updateVoiceParameters(newPitch: Float, newSpeechRate: Float) {
        this.pitch = newPitch
        this.speechRate = newSpeechRate
        tts?.setPitch(newPitch)
        tts?.setSpeechRate(newSpeechRate)
        Log.d(TAG, "Parámetros de voz TTS actualizados: pitch=$newPitch, speechRate=$newSpeechRate")
    }

    private fun configureBestFriendlyVoice() {
        try {
            val voices = tts?.voices ?: return
            val spanishVoices = voices.filter { voice ->
                voice.locale.language.equals("es", ignoreCase = true) && !voice.isNetworkConnectionRequired
            }
            if (spanishVoices.isEmpty()) return

            // Priorizar voces offline modernas, fluidas y de alta calidad técnica, evitando sintetizadores obsoletos (legacy/compact)
            val candidateVoices = spanishVoices.filter { voice ->
                val lower = voice.name.lowercase()
                !lower.contains("legacy") && !lower.contains("compact") && !lower.contains("low")
            }.ifEmpty { spanishVoices }

            val selectedVoice = candidateVoices
                .sortedWith(
                    compareByDescending<android.speech.tts.Voice> { it.quality }
                        .thenByDescending { it.locale.country.equals(preferredLocale.country, ignoreCase = true) }
                        .thenByDescending {
                            val lower = it.name.lowercase()
                            lower.contains("neural") || lower.contains("local") || lower.contains("natural")
                        }
                )
                .firstOrNull() ?: spanishVoices.maxByOrNull { it.quality }

            if (selectedVoice != null) {
                tts?.voice = selectedVoice
                Log.i(TAG, "Voz TTS de alta calidad seleccionada: ${selectedVoice.name} (calidad=${selectedVoice.quality})")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Aviso seleccionando voz TTS: ${e.message}")
        }
    }

    override suspend fun speak(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        if (!isInitialized) {
            val ready = try {
                kotlinx.coroutines.withTimeoutOrNull(2500L) {
                    initDeferred.await()
                } ?: false
            } catch (e: Exception) {
                false
            }
            if (!ready || tts == null) {
                Log.w(TAG, "TTS aún no inicializado o falló, omitiendo reproducción inmediata.")
                return
            }
        }

        suspendCancellableCoroutine<Unit> { continuation ->

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
        
        val effectivePitch = if (isWhisperMode) pitch * 0.88f else pitch
        val effectiveRate = if (isWhisperMode) speechRate * 0.90f else speechRate
        val effectiveVolume = if (isWhisperMode) 0.35f else 1.0f

        tts?.setPitch(effectivePitch)
        tts?.setSpeechRate(effectiveRate)
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveVolume)

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

        continuation.invokeOnCancellation {
            stop()
        }
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

package com.asistente.celular.voice.kws

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.asistente.celular.voice.WakeWordEngine
import java.text.Normalizer
import java.util.Locale

/**
 * Motor de detección de palabra clave (Wake Word) continua para Android.
 * Escucha en bucle continuo y tolerante a variantes fonéticas en español
 * ("oye hendrix", "hendrix", "oye jendrix", "oye henry", "oye endrix", "hola hendrix").
 *
 * Implementa control de pausa/reanudación para ceder el micrófono cuando la UI
 * interactiva requiera capturar la orden del usuario.
 */
class AndroidContinuousWakeWordEngine(
    private val context: Context,
    private val keywords: List<String> = DEFAULT_KEYWORDS,
    private val preferredLocale: Locale = Locale("es", "ES")
) : WakeWordEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var onKeywordDetectedCallback: ((String) -> Unit)? = null

    @Volatile
    override var isListening: Boolean = false
        private set

    @Volatile
    var isPaused: Boolean = false
        private set

    override val currentKeyword: String
        get() = keywords.firstOrNull() ?: "oye hendrix"

    private val restartRunnable = Runnable {
        if (isListening && !isPaused) {
            startInternalSession()
        }
    }

    override fun startListening(onKeywordDetected: (keyword: String) -> Unit) {
        this.onKeywordDetectedCallback = onKeywordDetected
        this.isListening = true
        this.isPaused = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post { startInternalSession() }
    }

    /**
     * Pausa temporalmente la escucha para ceder el micrófono a la interfaz de comando.
     */
    fun pause() {
        isPaused = true
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error pausando reconocedor: ${e.message}")
            }
        }
    }

    /**
     * Reanuda la escucha en segundo plano una vez que la orden ha sido completada.
     */
    fun resume() {
        if (!isListening) return
        isPaused = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, 300)
    }

    override fun stopListening() {
        isListening = false
        isPaused = false
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.post {
            destroyRecognizer()
        }
    }

    override fun release() {
        stopListening()
        onKeywordDetectedCallback = null
    }

    private fun startInternalSession() {
        if (!isListening || isPaused) return

        try {
            destroyRecognizer()

            val appContext = context.applicationContext
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                Log.e(TAG, "SpeechRecognizer no disponible en este dispositivo.")
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(createListener())
            }

            val langTag = if (preferredLocale.language == "es") preferredLocale.toLanguageTag() else "es-ES"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Permitir ciclos continuos
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Sesión de escucha de Wake Word iniciada con '$langTag'.")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando sesión interna de Wake Word: ${e.message}", e)
            scheduleRestart(1500)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "WakeWord: listo para escuchar audio.")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "WakeWord: inicio de voz detectado.")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                Log.d(TAG, "WakeWord onError ($error)")
                if (!isListening || isPaused) return

                val delay = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 250L // Silencio normal, reiniciar rápido
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_AUDIO -> 1000L // Conflicto de mic, esperar un segundo
                    else -> 500L
                }
                scheduleRestart(delay)
            }

            override fun onResults(results: Bundle?) {
                if (!isListening || isPaused) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val detected = matches?.let { checkMatches(it) }

                if (detected != null) {
                    handleKeywordMatch(detected)
                } else {
                    scheduleRestart(200)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isListening || isPaused) return

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val detected = matches?.let { checkMatches(it) }

                if (detected != null) {
                    handleKeywordMatch(detected)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun handleKeywordMatch(detectedKeyword: String) {
        Log.i(TAG, "¡Palabra de activación detectada!: '$detectedKeyword'")
        // Pausar inmediatamente la escucha de fondo para ceder el micrófono
        pause()
        onKeywordDetectedCallback?.invoke(detectedKeyword)
    }

    private fun checkMatches(candidates: List<String>): String? {
        for (candidate in candidates) {
            val normalized = normalizeText(candidate)
            for (kw in keywords) {
                val normalizedKw = normalizeText(kw)
                if (normalized.contains(normalizedKw)) {
                    return kw
                }
            }
        }
        return null
    }

    private fun normalizeText(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed.replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun scheduleRestart(delayMillis: Long) {
        mainHandler.removeCallbacks(restartRunnable)
        if (isListening && !isPaused) {
            mainHandler.postDelayed(restartRunnable, delayMillis)
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destruyendo reconocedor: ${e.message}")
        }
        speechRecognizer = null
    }

    companion object {
        private const val TAG = "ContinuousWakeWord"

        val DEFAULT_KEYWORDS = listOf(
            "oye hendrix",
            "hendrix",
            "oye jendrix",
            "oye henry",
            "oye endrix",
            "hola hendrix"
        )
    }
}

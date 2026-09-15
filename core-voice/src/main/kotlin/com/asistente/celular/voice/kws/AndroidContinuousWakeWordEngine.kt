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
import com.asistente.celular.voice.audio.AudioRecordSource
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Motor de detección de palabra clave (Wake Word) continua para Android.
 *
 * Utiliza captura silenciosa con AudioRecord (sin pitidos del sistema ni reconexiones de audio)
 * y detección de actividad vocal (VAD energética). Solo cuando se detecta voz sostenida
 * se evalúa la coincidencia de las palabras clave en español ("oye hendrix", etc.).
 */
class AndroidContinuousWakeWordEngine(
    private val context: Context,
    private val keywords: List<String> = DEFAULT_KEYWORDS,
    private val preferredLocale: Locale = Locale("es", "ES")
) : WakeWordEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioRecordSource = AudioRecordSource(sampleRate = 16000)
    private var speechRecognizer: SpeechRecognizer? = null
    private var onKeywordDetectedCallback: ((String) -> Unit)? = null

    @Volatile
    override var isListening: Boolean = false
        private set

    @Volatile
    var isPaused: Boolean = false
        private set

    private val isVerifyingSpeech = AtomicBoolean(false)
    private var consecutiveVoiceFrames = 0
    private var lastVerificationTimestamp = 0L

    override val currentKeyword: String
        get() = keywords.firstOrNull() ?: "oye hendrix"

    override fun startListening(onKeywordDetected: (keyword: String) -> Unit) {
        this.onKeywordDetectedCallback = onKeywordDetected
        this.isListening = true
        this.isPaused = false
        this.consecutiveVoiceFrames = 0
        this.isVerifyingSpeech.set(false)

        startSilentAudioMonitoring()
    }

    private fun startSilentAudioMonitoring() {
        if (!isListening || isPaused || isVerifyingSpeech.get()) return

        audioRecordSource.start { floatBuffer, sampleCount ->
            if (!isListening || isPaused || isVerifyingSpeech.get()) return@start

            // Calcular energía RMS (Root Mean Square) del buffer de audio (~100ms)
            var sum = 0.0
            for (i in 0 until sampleCount) {
                val s = floatBuffer[i]
                sum += s * s
            }
            val rms = sqrt(sum / sampleCount).toFloat()

            // Umbral de voz activa (> 0.040f es voz cercana; el silencio de habitación suele estar < 0.015f)
            if (rms >= VOICE_RMS_THRESHOLD) {
                consecutiveVoiceFrames++
                // Al menos 2 frames consecutivos (~200ms de energía vocal sostenida)
                if (consecutiveVoiceFrames >= REQUIRED_VOICE_FRAMES) {
                    consecutiveVoiceFrames = 0
                    val now = System.currentTimeMillis()
                    // Enfriamiento de 2.0 segundos entre verificaciones
                    if (now - lastVerificationTimestamp > COOLDOWN_MILLIS) {
                        lastVerificationTimestamp = now
                        mainHandler.post { triggerSpeechVerification() }
                    }
                }
            } else {
                if (consecutiveVoiceFrames > 0) {
                    consecutiveVoiceFrames--
                }
            }
        }
        Log.d(TAG, "Monitoreo silencioso de audio activo (AudioRecord).")
    }

    private fun triggerSpeechVerification() {
        if (!isListening || isPaused || !isVerifyingSpeech.compareAndSet(false, true)) return

        // 1. Detener AudioRecord para liberar el hardware del micrófono
        audioRecordSource.stop()

        mainHandler.post {
            try {
                destroyRecognizer()

                val appContext = context.applicationContext
                if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                    Log.w(TAG, "SpeechRecognizer no disponible para verificación.")
                    resumeSilentMonitoringAfterDelay(COOLDOWN_MILLIS)
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            Log.d(TAG, "Verificación de palabra clave onError ($error)")
                            finishVerificationAndResume()
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val detected = matches?.let { checkMatches(it) }
                            if (detected != null) {
                                handleKeywordMatch(detected)
                            } else {
                                finishVerificationAndResume()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val detected = matches?.let { checkMatches(it) }
                            if (detected != null) {
                                handleKeywordMatch(detected)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val langTag = if (preferredLocale.language == "es") preferredLocale.toLanguageTag() else "es-ES"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error en verificación de voz: ${e.message}", e)
                finishVerificationAndResume()
            }
        }
    }

    private fun finishVerificationAndResume() {
        destroyRecognizer()
        resumeSilentMonitoringAfterDelay(COOLDOWN_MILLIS)
    }

    private fun resumeSilentMonitoringAfterDelay(delayMillis: Long) {
        mainHandler.postDelayed({
            isVerifyingSpeech.set(false)
            if (isListening && !isPaused) {
                startSilentAudioMonitoring()
            }
        }, delayMillis)
    }

    private fun handleKeywordMatch(detectedKeyword: String) {
        Log.i(TAG, "¡Palabra de activación detectada!: '$detectedKeyword'")
        destroyRecognizer()
        pause()
        onKeywordDetectedCallback?.invoke(detectedKeyword)
    }

    fun pause() {
        isPaused = true
        audioRecordSource.stop()
        mainHandler.post { destroyRecognizer() }
    }

    fun resume() {
        if (!isListening) return
        isPaused = false
        isVerifyingSpeech.set(false)
        mainHandler.postDelayed({
            if (isListening && !isPaused) {
                startSilentAudioMonitoring()
            }
        }, 300)
    }

    override fun stopListening() {
        isListening = false
        isPaused = false
        isVerifyingSpeech.set(false)
        audioRecordSource.stop()
        mainHandler.post { destroyRecognizer() }
    }

    override fun release() {
        stopListening()
        onKeywordDetectedCallback = null
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
        private const val VOICE_RMS_THRESHOLD = 0.040f
        private const val REQUIRED_VOICE_FRAMES = 2
        private const val COOLDOWN_MILLIS = 2000L

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

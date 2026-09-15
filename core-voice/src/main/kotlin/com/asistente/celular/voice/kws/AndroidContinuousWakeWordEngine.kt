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
    private var onEventDetectedCallback: ((com.asistente.celular.voice.WakeWordEvent) -> Unit)? = null

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
        this.onEventDetectedCallback = null
        this.isListening = true
        this.isPaused = false
        this.consecutiveVoiceFrames = 0
        this.isVerifyingSpeech.set(false)

        startSilentAudioMonitoring()
    }

    override fun startListeningWithEvent(onEventDetected: (com.asistente.celular.voice.WakeWordEvent) -> Unit) {
        this.onEventDetectedCallback = onEventDetected
        this.onKeywordDetectedCallback = { kw -> onEventDetected(com.asistente.celular.voice.WakeWordEvent(kw)) }
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

    private var lastDetectedEvent: com.asistente.celular.voice.WakeWordEvent? = null

    private fun triggerSpeechVerification() {
        if (!isListening || isPaused || !isVerifyingSpeech.compareAndSet(false, true)) return

        // 1. Detener AudioRecord para liberar el hardware del micrófono
        audioRecordSource.stop()
        lastDetectedEvent = null

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
                            val fallbackEvent = lastDetectedEvent
                            if (fallbackEvent != null) {
                                Log.i(TAG, "Activando con evento de respaldo detectado en parciales tras error ASR ($error)")
                                handleKeywordMatch(fallbackEvent)
                            } else {
                                finishVerificationAndResume()
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val event = matches?.let { extractWakeWordEvent(it) } ?: lastDetectedEvent
                            if (event != null) {
                                handleKeywordMatch(event)
                            } else {
                                finishVerificationAndResume()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val event = matches?.let { extractWakeWordEvent(it) }
                            if (event != null) {
                                lastDetectedEvent = event
                                Log.d(TAG, "Palabra clave en parciales: ${event.keyword}, comando provisional: '${event.command}'")
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
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 700L)
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

    private fun handleKeywordMatch(event: com.asistente.celular.voice.WakeWordEvent) {
        Log.i(TAG, "¡Activación detectada!: keyword='${event.keyword}', command='${event.command}'")
        destroyRecognizer()
        pause()
        if (onEventDetectedCallback != null) {
            onEventDetectedCallback?.invoke(event)
        } else {
            onKeywordDetectedCallback?.invoke(event.keyword)
        }
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
        onEventDetectedCallback = null
    }

    private fun extractWakeWordEvent(candidates: List<String>): com.asistente.celular.voice.WakeWordEvent? {
        val sortedKeywords = keywords.sortedByDescending { it.length }
        for (candidate in candidates) {
            val normalized = normalizeText(candidate)
            for (kw in sortedKeywords) {
                val normalizedKw = normalizeText(kw)
                val idx = normalized.indexOf(normalizedKw)
                if (idx >= 0) {
                    val after = normalized.substring(idx + normalizedKw.length).trim()
                    val before = normalized.substring(0, idx).trim()
                    val rawCommand = if (after.isNotBlank()) after else before

                    var clean = rawCommand.trimStart { it == ',' || it == ':' || it == ';' || it == '.' || it.isWhitespace() }
                    val fillers = listOf(
                        "por favor", "porfa", "puedes", "podrias", "podrías",
                        "me puedes", "me podrias", "me podrías", "quiero que", "hazme el favor de", "me"
                    )
                    var changed = true
                    while (changed) {
                        changed = false
                        clean = clean.trimStart { it == ',' || it == ':' || it == ';' || it == '.' || it.isWhitespace() }
                        for (filler in fillers) {
                            if (clean.startsWith(filler)) {
                                clean = clean.removePrefix(filler).trim()
                                changed = true
                            }
                        }
                    }
                    val cleanCommand = clean
                        .trimStart { it == ',' || it == ':' || it == ';' || it == '.' || it.isWhitespace() }
                        .trimEnd { it == ',' || it == ':' || it == ';' || it == '.' || it.isWhitespace() }
                        .takeIf { it.isNotBlank() }

                    return com.asistente.celular.voice.WakeWordEvent(
                        keyword = kw,
                        fullUtterance = candidate,
                        command = cleanCommand
                    )
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
        private const val VOICE_RMS_THRESHOLD = 0.075f
        private const val REQUIRED_VOICE_FRAMES = 4
        private const val COOLDOWN_MILLIS = 3500L

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

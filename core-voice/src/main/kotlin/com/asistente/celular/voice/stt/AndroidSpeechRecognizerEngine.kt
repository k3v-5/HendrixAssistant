package com.asistente.celular.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.asistente.celular.voice.SttEngine
import java.util.Locale

/**
 * Motor de reconocimiento de voz a texto (STT) utilizando la API nativa de Android.
 * Compatible con modelos offline instalados en el sistema y reconocimiento en tiempo real.
 */
class AndroidSpeechRecognizerEngine(
    private val context: Context,
    private val preferredLocale: Locale = Locale.getDefault()
) : SttEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var lastRecognizedPartial: String = ""
    private var hasRetriedDisconnect: Boolean = false

    @Volatile
    override var isListening: Boolean = false
        private set

    override fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        hasRetriedDisconnect = false
        startListeningInternal(onPartialResult, onFinalResult, onError, isRetry = false)
    }

    private fun startListeningInternal(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
        isRetry: Boolean
    ) {
        mainHandler.post {
            try {
                lastRecognizedPartial = ""

                val appContext = context.applicationContext
                if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                    Log.e(TAG, "isRecognitionAvailable devolvió false")
                    onError(IllegalStateException("Reconocimiento de voz no disponible en este dispositivo."))
                    return@post
                }

                // Si no existe el reconocedor o venimos de un error de desconexión, crearlo
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                } else {
                    // Si ya existe, cancelar cualquier sesión activa previa sin destruir el enlace IPC
                    try {
                        speechRecognizer?.cancel()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error cancelando sesión previa de reconocedor: ${e.message}")
                    }
                }

                var isFinalDelivered = false

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer listo para escuchar audio.")
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "Detección de voz iniciada.")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "Fin de voz detectado.")
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        if (isFinalDelivered) {
                            Log.d(TAG, "onError ($error) ignorado porque el resultado final ya fue entregado.")
                            return
                        }
                        Log.w(TAG, "SpeechRecognizer onError ($error): ${getErrorDescription(error)}")

                        // Si el servicio del sistema se desconectó (error 11 = ERROR_SERVER_DISCONNECTED), reintentar una vez
                        if (error == 11 && !hasRetriedDisconnect) {
                            hasRetriedDisconnect = true
                            Log.w(TAG, "ERROR_SERVER_DISCONNECTED (11) detectado. Reintentando reconexión limpia en 200ms...")
                            try {
                                speechRecognizer?.destroy()
                            } catch (_: Exception) {}
                            speechRecognizer = null
                            mainHandler.postDelayed({
                                startListeningInternal(onPartialResult, onFinalResult, onError, isRetry = true)
                            }, 200L)
                            return
                        }

                        // Si ya teníamos texto parcial antes de que el motor cortara por timeout o silencio, ¡usarlo como resultado final!
                        if (lastRecognizedPartial.isNotBlank()) {
                            isFinalDelivered = true
                            val recovered = lastRecognizedPartial
                            lastRecognizedPartial = ""
                            Log.i(TAG, "Recuperando texto parcial tras error $error: \"$recovered\"")
                            onFinalResult(recovered)
                            return
                        }
                        isFinalDelivered = true
                        val errorMessage = getErrorDescription(error)
                        onError(IllegalStateException(errorMessage))
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        if (isFinalDelivered) {
                            Log.d(TAG, "onResults ignorado porque ya se había entregado resultado.")
                            return
                        }
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().takeUnless { it.isNullOrBlank() } ?: lastRecognizedPartial
                        lastRecognizedPartial = ""
                        isFinalDelivered = true
                        if (text.isNotBlank()) {
                            Log.d(TAG, "Resultado final de voz: \"$text\"")
                            onFinalResult(text)
                        } else {
                            onError(IllegalStateException("No se detectó voz. Toca el micrófono e intenta de nuevo."))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            lastRecognizedPartial = partial
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val langTag = if (preferredLocale.language == "es") preferredLocale.toLanguageTag() else "es-ES"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    // Permitir reconocimiento holgado: +1 segundo adicional antes de cortar la escucha por pausa/silencio
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2800L)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                Log.d(TAG, "startListening llamado exitosamente con idioma '$langTag'.")
            } catch (e: Exception) {
                isListening = false
                Log.e(TAG, "Fallo al iniciar escucha: ${e.message}", e)
                onError(e)
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.stopListening()
                Log.d(TAG, "stopListening ejecutado.")
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener SpeechRecognizer: ${e.message}", e)
            }
        }
    }

    override fun release() {
        mainHandler.post {
            try {
                isListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error al liberar SpeechRecognizer: ${e.message}", e)
            }
        }
    }

    private fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Error de grabación de audio."
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de reconocimiento."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos de micrófono insuficientes."
            SpeechRecognizer.ERROR_NETWORK -> "Error de red al procesar la voz."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado."
            SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció ninguna frase."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El motor de reconocimiento está ocupado."
            SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de reconocimiento."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz (tiempo agotado)."
            10 -> "Demasiadas peticiones al servicio de voz."
            11 -> "El servicio de voz se desconectó temporalmente. Toca el micrófono para intentar de nuevo."
            12 -> "Idioma no soportado por el motor de voz."
            13 -> "Idioma no disponible sin conexión."
            14 -> "No se pudo verificar el soporte del idioma."
            else -> "Error del servicio de voz ($errorCode)."
        }
    }

    companion object {
        private const val TAG = "AndroidSpeechEngine"
    }
}

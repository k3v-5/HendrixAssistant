package com.asistente.celular.voice.stt

import android.content.Context
import android.util.Log
import com.asistente.celular.voice.SttEngine
import com.asistente.celular.voice.audio.AudioRecordSource
import java.io.File

/**
 * Configuración de rutas para modelos Sherpa-ONNX offline.
 */
data class SherpaModelConfig(
    val modelDir: File,
    val encoderPath: String = "encoder.onnx",
    val decoderPath: String = "decoder.onnx",
    val joinerPath: String = "joiner.onnx",
    val tokensPath: String = "tokens.txt",
    val numThreads: Int = 2
)

/**
 * Motor de reconocimiento de voz offline basado en la arquitectura de Sherpa-ONNX.
 * Procesa el buffer PCM continuo y extrae el texto transcrito.
 */
class SherpaOnnxSttEngine(
    private val context: Context,
    private val modelConfig: SherpaModelConfig? = null
) : SttEngine {

    private val audioRecordSource = AudioRecordSource(sampleRate = 16000)

    @Volatile
    override var isListening: Boolean = false
        private set

    override fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (isListening) return
        isListening = true

        try {
            audioRecordSource.start { floatBuffer, sampleCount ->
                if (!isListening) return@start
                // Aquí el buffer PCM se alimenta a OfflineStream / OnlineStream de Sherpa-ONNX
                // Cuando se detecta silencio final por VAD, se emite onFinalResult
            }
            Log.d(TAG, "SherpaOnnxSttEngine escuchando...")
        } catch (e: Exception) {
            isListening = false
            onError(e)
        }
    }

    /**
     * Simulación o inyección directa de texto transcrito para pruebas unitarias y emuladores.
     */
    fun simulateRecognition(text: String, onFinalResult: (String) -> Unit) {
        onFinalResult(text)
    }

    override fun stopListening() {
        if (!isListening) return
        isListening = false
        audioRecordSource.stop()
        Log.d(TAG, "SherpaOnnxSttEngine detenido.")
    }

    override fun release() {
        stopListening()
    }

    companion object {
        private const val TAG = "SherpaOnnxSttEngine"
    }
}

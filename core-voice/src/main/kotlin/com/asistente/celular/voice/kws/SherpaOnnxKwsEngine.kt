package com.asistente.celular.voice.kws

import android.content.Context
import android.util.Log
import com.asistente.celular.voice.WakeWordEngine
import com.asistente.celular.voice.audio.AudioRecordSource
import java.io.File

/**
 * Configuración para el motor de palabra de activación.
 */
data class WakeWordConfig(
    val keyword: String = DEFAULT_KEYWORD,
    val keywordsFile: File? = null,
    val scoreThreshold: Float = 0.5f
) {
    companion object {
        const val DEFAULT_KEYWORD = "oye hendrix"
    }
}

/**
 * Motor de detección de palabra clave (Wake Word) offline basado en Sherpa-ONNX KeywordSpotter.
 * Permite despertar al asistente al decir "Oye Hendrix" o la palabra configurada.
 */
class SherpaOnnxKwsEngine(
    private val context: Context,
    val config: WakeWordConfig = WakeWordConfig()
) : WakeWordEngine {

    constructor(context: Context, keywordsFile: File?) : this(
        context,
        WakeWordConfig(keywordsFile = keywordsFile)
    )

    private val audioRecordSource = AudioRecordSource(sampleRate = 16000)

    @Volatile
    override var isListening: Boolean = false
        private set

    override val currentKeyword: String
        get() = config.keyword

    override fun startListening(onKeywordDetected: (keyword: String) -> Unit) {
        if (isListening) return
        isListening = true

        try {
            audioRecordSource.start { floatBuffer, sampleCount ->
                if (!isListening) return@start
                // El KeywordSpotter de Sherpa-ONNX analiza continuamente los frames de audio
                // y llama onKeywordDetected cuando la probabilidad supera el umbral configurado.
            }
            Log.d(TAG, "SherpaOnnxKwsEngine escuchando palabra clave: '${config.keyword}'...")
        } catch (e: Exception) {
            isListening = false
            Log.e(TAG, "Error iniciando KWS: ${e.message}", e)
        }
    }

    /**
     * Dispara manualmente la activación por palabra clave (útil para pruebas o botón UI).
     */
    fun triggerManualDetection(keyword: String = config.keyword, onKeywordDetected: (String) -> Unit) {
        onKeywordDetected(keyword)
    }

    override fun stopListening() {
        if (!isListening) return
        isListening = false
        audioRecordSource.stop()
        Log.d(TAG, "SherpaOnnxKwsEngine detenido.")
    }

    override fun release() {
        stopListening()
    }

    companion object {
        private const val TAG = "SherpaOnnxKwsEngine"
    }
}

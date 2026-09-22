package com.asistente.celular.voice.stt

import android.content.Context
import com.asistente.celular.voice.SttEngine

/**
 * Tipos de motor de reconocimiento de voz a texto disponibles.
 */
enum class SttEngineType(val displayName: String) {
    /** Motor nativo del sistema Android (SpeechRecognizer con paquetes de idioma). */
    ANDROID_SYSTEM("Reconocimiento del Sistema Android"),

    /** Motor local autónomo Sherpa-ONNX / Whisper 100% Offline sin dependencias externas. */
    OFFLINE_SHERPA_ONNX("Offline Autónomo (Sherpa-ONNX)");

    companion object {
        fun fromName(name: String?): SttEngineType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ANDROID_SYSTEM
        }
    }
}

/**
 * Factoría para la creación e intercambio dinámico de motores de reconocimiento de voz.
 */
object SttEngineFactory {

    fun createEngine(
        context: Context,
        type: SttEngineType = SttEngineType.ANDROID_SYSTEM,
        modelConfig: SherpaModelConfig? = null
    ): SttEngine {
        return when (type) {
            SttEngineType.ANDROID_SYSTEM -> AndroidSpeechRecognizerEngine(context)
            SttEngineType.OFFLINE_SHERPA_ONNX -> SherpaOnnxSttEngine(context, modelConfig)
        }
    }
}

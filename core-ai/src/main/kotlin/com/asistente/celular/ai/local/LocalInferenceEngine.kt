package com.asistente.celular.ai.local

import java.io.File

/**
 * Configuración de parámetros para la inferencia on-device.
 */
data class LocalInferenceConfig(
    val contextSize: Int = 2048,
    val threads: Int = calculateDefaultThreads(),
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 256,
    val useMmap: Boolean = true
) {
    companion object {
        fun calculateDefaultThreads(): Int {
            val cores = Runtime.getRuntime().availableProcessors()
            // Usar núcleos balanceados para no congelar la UI ni sobrecalentar la batería
            return (cores / 2).coerceIn(2, 6)
        }
    }
}

/**
 * Abstracción de contrato para cualquier motor de inferencia local de IA (llama.cpp, LiteRT, ONNX, etc.).
 * Cumple con los principios de desacoplamiento e inversión de dependencias de GEMINI.md.
 */
interface LocalInferenceEngine {
    val isLoaded: Boolean
    val loadedModelId: String?
    val loadedModelFile: File?

    /**
     * Carga un modelo GGUF en memoria RAM de forma segura.
     */
    suspend fun loadModel(
        modelFile: File,
        modelId: String,
        config: LocalInferenceConfig = LocalInferenceConfig()
    ): Result<Unit>

    /**
     * Ejecuta inferencia generativa on-device y retorna la respuesta completa.
     */
    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        maxTokens: Int = 256,
        temperature: Float = 0.7f
    ): Result<String>

    /**
     * Libera los recursos nativos C++ y la memoria RAM del modelo.
     */
    fun unload()
}

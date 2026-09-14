package com.asistente.celular.ai.local

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.codeshipping.llamakotlin.LlamaConfig
import org.codeshipping.llamakotlin.LlamaModel
import java.io.File

/**
 * Implementación de [LocalInferenceEngine] basada en llama.cpp nativo (C++/JNI)
 * optimizado para ARM64 NEON en dispositivos Android.
 */
class LlamaCppInferenceEngine : LocalInferenceEngine {

    private val mutex = Mutex()
    private var activeModel: LlamaModel? = null
    override var loadedModelId: String? = null
        private set
    override var loadedModelFile: File? = null
        private set

    override val isLoaded: Boolean
        get() = activeModel != null

    override suspend fun loadModel(
        modelFile: File,
        modelId: String,
        config: LocalInferenceConfig
    ): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (!modelFile.exists()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("El archivo del modelo no existe: ${modelFile.absolutePath}")
                    )
                }

                // Si ya está cargado este mismo modelo, no recargar
                if (activeModel != null && loadedModelId == modelId) {
                    return@withContext Result.success(Unit)
                }

                // Descargar modelo previo si había uno
                unloadInternal()

                Log.i(TAG, "Iniciando carga nativa de modelo GGUF: ${modelFile.name} (Hilos: ${config.threads}, Ctx: ${config.contextSize})")

                val llamaConfig = LlamaConfig().apply {
                    contextSize = config.contextSize
                    threads = config.threads
                    temperature = config.temperature
                    topP = config.topP
                    maxTokens = config.maxTokens
                    useMmap = config.useMmap
                }

                val model = LlamaModel.load(modelFile.absolutePath, llamaConfig)
                activeModel = model
                loadedModelId = modelId
                loadedModelFile = modelFile

                Log.i(TAG, "Modelo GGUF cargado exitosamente en memoria: $modelId")
                Result.success(Unit)
            } catch (e: Throwable) {
                Log.e(TAG, "Error cargando modelo GGUF $modelId: ${e.message}", e)
                unloadInternal()
                Result.failure(e)
            }
        }
    }

    override suspend fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ): Result<String> = mutex.withLock {
        withContext(Dispatchers.Default) {
            val model = activeModel ?: return@withContext Result.failure(
                IllegalStateException("No hay ningún modelo local cargado en memoria")
            )

            try {
                val formattedPrompt = formatPromptForModel(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    modelId = loadedModelId ?: ""
                )

                Log.d(TAG, "Ejecutando inferencia on-device con llama.cpp...")
                val startTime = System.currentTimeMillis()

                val rawResponse = model.generate(formattedPrompt)

                val elapsed = System.currentTimeMillis() - startTime
                val cleanResponse = cleanModelOutput(rawResponse)

                Log.d(TAG, "Inferencia completada en ${elapsed}ms: '$cleanResponse'")
                Result.success(cleanResponse)
            } catch (e: Throwable) {
                Log.e(TAG, "Error durante inferencia nativa llama.cpp: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override fun unload() {
        unloadInternal()
    }

    private fun unloadInternal() {
        try {
            activeModel?.close()
        } catch (e: Throwable) {
            Log.w(TAG, "Aviso al cerrar contexto LlamaModel: ${e.message}")
        } finally {
            activeModel = null
            loadedModelId = null
            loadedModelFile = null
            Log.i(TAG, "Recursos nativos de llama.cpp liberados de memoria")
        }
    }

    companion object {
        private const val TAG = "LlamaCppEngine"

        /**
         * Aplica la plantilla de chat adecuada según el modelo (ChatML para Qwen, Llama3 para SmolLM).
         */
        fun formatPromptForModel(prompt: String, systemPrompt: String, modelId: String): String {
            val lowerId = modelId.lowercase()
            return when {
                lowerId.contains("qwen") -> formatChatML(system = systemPrompt, user = prompt)
                lowerId.contains("smollm") || lowerId.contains("llama") -> formatLlama3(system = systemPrompt, user = prompt)
                else -> formatChatML(system = systemPrompt, user = prompt)
            }
        }

        fun formatChatML(system: String, user: String): String = buildString {
            if (system.isNotBlank()) {
                append("<|im_start|>system\n").append(system).append("<|im_end|>\n")
            }
            append("<|im_start|>user\n").append(user).append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }

        fun formatLlama3(system: String, user: String): String = buildString {
            append("<|begin_of_text|>")
            if (system.isNotBlank()) {
                append("<|start_header_id|>system<|end_header_id|>\n\n").append(system).append("<|eot_id|>")
            }
            append("<|start_header_id|>user<|end_header_id|>\n\n").append(user).append("<|eot_id|>")
            append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        }

        fun cleanModelOutput(output: String): String {
            return output
                .replace("<|im_end|>", "")
                .replace("<|im_start|>", "")
                .replace("<|eot_id|>", "")
                .replace("<|end_of_text|>", "")
                .trim()
        }
    }
}

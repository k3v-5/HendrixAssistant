package com.asistente.celular.ai.client

import android.util.Log
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP unificado para múltiples proveedores de Inteligencia Artificial.
 */
open class LlmClient(
    private val localModelManager: com.asistente.celular.ai.local.LocalModelManager? = null,
    private val localInferenceEngine: com.asistente.celular.ai.local.LocalInferenceEngine? = null,
    val trafficAuditor: com.asistente.celular.ai.audit.AiTrafficAuditor? = null,
    val piiScrubber: com.asistente.celular.ai.audit.PiiScrubber? = null,
    private val configProvider: () -> LlmConfig
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Envía un prompt a la IA configurada y retorna el texto de respuesta.
     */
    suspend fun generateResponse(prompt: String, overrideConfig: LlmConfig? = null): Result<String> = withContext(Dispatchers.IO) {
        val config = overrideConfig ?: configProvider()
        val startTime = System.currentTimeMillis()
        val scrubResult = piiScrubber?.inspectAndScrub(prompt)
        val sanitizedPrompt = scrubResult?.scrubbedText ?: prompt
        val promptBytes = sanitizedPrompt.toByteArray(Charsets.UTF_8).size.toLong()
        val estimatedPromptTokens = (sanitizedPrompt.length / 4).coerceAtLeast(1)

        val destination = when (config.provider) {
            AiProvider.GEMINI -> com.asistente.celular.ai.audit.AiDestinationType.CLOUD_GEMINI
            AiProvider.OPENAI -> com.asistente.celular.ai.audit.AiDestinationType.CLOUD_OPENAI
            AiProvider.GROQ -> com.asistente.celular.ai.audit.AiDestinationType.CLOUD_GROQ
            AiProvider.OLLAMA -> com.asistente.celular.ai.audit.AiDestinationType.LOCAL_LAN_OLLAMA
            AiProvider.LOCAL_SLM -> com.asistente.celular.ai.audit.AiDestinationType.LOCAL_NPU_SLM
        }

        return@withContext try {
            val responseText = when (config.provider) {
                AiProvider.GEMINI -> callGemini(sanitizedPrompt, config)
                AiProvider.OPENAI -> callOpenAiCompatible(
                    endpoint = "https://api.openai.com/v1/chat/completions",
                    prompt = sanitizedPrompt,
                    config = config
                )
                AiProvider.GROQ -> callOpenAiCompatible(
                    endpoint = "https://api.groq.com/openai/v1/chat/completions",
                    prompt = sanitizedPrompt,
                    config = config
                )
                AiProvider.OLLAMA -> callOllama(sanitizedPrompt, config)
                AiProvider.LOCAL_SLM -> executeLocalSlm(sanitizedPrompt, config)
            }
            val latency = System.currentTimeMillis() - startTime
            val responseBytes = responseText.toByteArray(Charsets.UTF_8).size.toLong()
            val estimatedResponseTokens = (responseText.length / 4).coerceAtLeast(1)

            trafficAuditor?.recordEvent(
                com.asistente.celular.ai.audit.AiTrafficRecord(
                    destination = destination,
                    modelName = config.modelName,
                    promptBytes = promptBytes,
                    estimatedPromptTokens = estimatedPromptTokens,
                    responseBytes = responseBytes,
                    estimatedResponseTokens = estimatedResponseTokens,
                    latencyMs = latency,
                    isFullyLocal = destination.isLocal,
                    piiDetectedCount = scrubResult?.detectedCount ?: 0,
                    piiScrubbed = (scrubResult?.detectedCount ?: 0) > 0,
                    success = true
                )
            )

            Result.success(responseText)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            trafficAuditor?.recordEvent(
                com.asistente.celular.ai.audit.AiTrafficRecord(
                    destination = destination,
                    modelName = config.modelName,
                    promptBytes = promptBytes,
                    estimatedPromptTokens = estimatedPromptTokens,
                    responseBytes = 0L,
                    estimatedResponseTokens = 0,
                    latencyMs = latency,
                    isFullyLocal = destination.isLocal,
                    piiDetectedCount = scrubResult?.detectedCount ?: 0,
                    piiScrubbed = (scrubResult?.detectedCount ?: 0) > 0,
                    success = false,
                    errorMessage = e.message
                )
            )
            Log.e(TAG, "Error llamando a proveedor IA [${config.provider}]: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Emite un flujo continuo de tokens (streaming) conforme la IA los genera.
     */
    fun streamResponse(prompt: String, overrideConfig: LlmConfig? = null): Flow<String> = flow {
        val config = overrideConfig ?: configProvider()
        val res = generateResponse(prompt, config)
        if (res.isSuccess) {
            val full = res.getOrThrow()
            val words = full.split(" ")
            for (i in words.indices) {
                emit(words[i] + if (i < words.size - 1) " " else "")
            }
        } else {
            throw res.exceptionOrNull() ?: RuntimeException("Error en inferencia LLM")
        }
    }

    /**
     * Envía un prompt acompañado de una imagen binaria (ej. snapshot WebP de la pantalla)
     * al proveedor de IA para análisis multimodal bajo demanda.
     */
    suspend fun generateMultimodalResponse(
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String = "image/webp",
        overrideConfig: LlmConfig? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = overrideConfig ?: configProvider()
        val startTime = System.currentTimeMillis()
        val scrubResult = piiScrubber?.inspectAndScrub(prompt)
        val sanitizedPrompt = scrubResult?.scrubbedText ?: prompt
        val promptBytes = (sanitizedPrompt.toByteArray(Charsets.UTF_8).size + imageBytes.size).toLong()
        val estimatedPromptTokens = (sanitizedPrompt.length / 4).coerceAtLeast(1) + 258 // Estimado visión tokens

        val destination = when (config.provider) {
            AiProvider.GEMINI -> com.asistente.celular.ai.audit.AiDestinationType.CLOUD_GEMINI
            AiProvider.OPENAI -> com.asistente.celular.ai.audit.AiDestinationType.CLOUD_OPENAI
            AiProvider.GROQ -> com.asistente.celular.ai.audit.AiDestinationType.CLOUD_GROQ
            AiProvider.OLLAMA -> com.asistente.celular.ai.audit.AiDestinationType.LOCAL_LAN_OLLAMA
            AiProvider.LOCAL_SLM -> com.asistente.celular.ai.audit.AiDestinationType.LOCAL_NPU_SLM
        }

        return@withContext try {
            val responseText = when (config.provider) {
                AiProvider.GEMINI -> callGeminiMultimodal(sanitizedPrompt, imageBytes, mimeType, config)
                AiProvider.OPENAI, AiProvider.GROQ -> callOpenAiMultimodal(
                    endpoint = if (config.provider == AiProvider.OPENAI)
                        "https://api.openai.com/v1/chat/completions"
                    else
                        "https://api.groq.com/openai/v1/chat/completions",
                    prompt = sanitizedPrompt,
                    imageBytes = imageBytes,
                    mimeType = mimeType,
                    config = config
                )
                AiProvider.OLLAMA -> callOllama(sanitizedPrompt, config)
                AiProvider.LOCAL_SLM -> executeLocalSlm(sanitizedPrompt, config)
            }
            val latency = System.currentTimeMillis() - startTime
            val responseBytes = responseText.toByteArray(Charsets.UTF_8).size.toLong()
            val estimatedResponseTokens = (responseText.length / 4).coerceAtLeast(1)

            trafficAuditor?.recordEvent(
                com.asistente.celular.ai.audit.AiTrafficRecord(
                    destination = destination,
                    modelName = config.modelName,
                    promptBytes = promptBytes,
                    estimatedPromptTokens = estimatedPromptTokens,
                    responseBytes = responseBytes,
                    estimatedResponseTokens = estimatedResponseTokens,
                    latencyMs = latency,
                    isFullyLocal = destination.isLocal,
                    piiDetectedCount = scrubResult?.detectedCount ?: 0,
                    piiScrubbed = (scrubResult?.detectedCount ?: 0) > 0,
                    success = true
                )
            )

            Result.success(responseText)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            trafficAuditor?.recordEvent(
                com.asistente.celular.ai.audit.AiTrafficRecord(
                    destination = destination,
                    modelName = config.modelName,
                    promptBytes = promptBytes,
                    estimatedPromptTokens = estimatedPromptTokens,
                    responseBytes = 0L,
                    estimatedResponseTokens = 0,
                    latencyMs = latency,
                    isFullyLocal = destination.isLocal,
                    piiDetectedCount = scrubResult?.detectedCount ?: 0,
                    piiScrubbed = (scrubResult?.detectedCount ?: 0) > 0,
                    success = false,
                    errorMessage = e.message
                )
            )
            Log.e(TAG, "Error en llamada multimodal [${config.provider}]: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun callGeminiMultimodal(
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String,
        config: LlmConfig
    ): String {
        require(config.apiKey.isNotBlank()) { "Se requiere una API Key para Google Gemini." }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}"
        val base64Data = java.util.Base64.getEncoder().encodeToString(imageBytes)

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", config.systemPrompt)))
            })
            val parts = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                put(JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", mimeType)
                        put("data", base64Data)
                    })
                })
            }
            val contents = JSONArray().apply {
                put(JSONObject().put("parts", parts))
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", config.temperature)
                put("maxOutputTokens", config.maxTokens)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("Error de Gemini Vision (${response.code}): $errorBody")
            }
            val bodyString = response.body?.string() ?: throw IllegalStateException("Respuesta vacía de Gemini")
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val partsArr = content.getJSONArray("parts")
                if (partsArr.length() > 0) {
                    return partsArr.getJSONObject(0).getString("text").trim()
                }
            }
            throw IllegalStateException("Formato inesperado en respuesta de Gemini Vision")
        }
    }

    private fun callOpenAiMultimodal(
        endpoint: String,
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String,
        config: LlmConfig
    ): String {
        require(config.apiKey.isNotBlank()) { "Se requiere una API Key para ${config.provider.displayName}." }

        val base64Data = java.util.Base64.getEncoder().encodeToString(imageBytes)
        val dataUrl = "data:$mimeType;base64,$base64Data"

        val jsonBody = JSONObject().apply {
            put("model", config.modelName)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", config.systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    val contentArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", dataUrl)
                            })
                        })
                    }
                    put("content", contentArr)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("Error de ${config.provider.displayName} Vision (${response.code}): $errorBody")
            }
            val bodyString = response.body?.string() ?: throw IllegalStateException("Respuesta vacía")
            val root = JSONObject(bodyString)
            val choices = root.getJSONArray("choices")
            if (choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
            }
            throw IllegalStateException("No se encontraron choices en la respuesta")
        }
    }

    private fun callGemini(prompt: String, config: LlmConfig): String {
        require(config.apiKey.isNotBlank()) { "Se requiere una API Key para Google Gemini." }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}"

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", config.systemPrompt)))
            })
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", config.temperature)
                put("maxOutputTokens", config.maxTokens)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("Error de Gemini (${response.code}): $errorBody")
            }
            val bodyString = response.body?.string() ?: throw IllegalStateException("Respuesta vacía de Gemini")
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim()
                }
            }
            throw IllegalStateException("Formato inesperado en respuesta de Gemini")
        }
    }

    private fun callOpenAiCompatible(endpoint: String, prompt: String, config: LlmConfig): String {
        require(config.apiKey.isNotBlank()) { "Se requiere una API Key para ${config.provider.displayName}." }

        val jsonBody = JSONObject().apply {
            put("model", config.modelName)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", config.systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("Error de ${config.provider.displayName} (${response.code}): $errorBody")
            }
            val bodyString = response.body?.string() ?: throw IllegalStateException("Respuesta vacía")
            val root = JSONObject(bodyString)
            val choices = root.getJSONArray("choices")
            if (choices.length() > 0) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
            }
            throw IllegalStateException("No se encontraron choices en la respuesta")
        }
    }

    private fun callOllama(prompt: String, config: LlmConfig): String {
        val baseUrl = config.customEndpoint?.takeIf { it.isNotBlank() } ?: "http://10.0.2.2:11434"
        val url = if (baseUrl.endsWith("/")) "${baseUrl}api/chat" else "$baseUrl/api/chat"

        val jsonBody = JSONObject().apply {
            put("model", config.modelName)
            put("stream", false)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", config.systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw IllegalStateException("Error de Ollama (${response.code}): $errorBody")
            }
            val bodyString = response.body?.string() ?: throw IllegalStateException("Respuesta vacía de Ollama")
            val root = JSONObject(bodyString)
            return root.getJSONObject("message").getString("content").trim()
        }
    }

    private suspend fun executeLocalSlm(prompt: String, config: LlmConfig): String {
        val manager = localModelManager
        val spec = manager?.getModelSpec(config.modelName)
            ?: com.asistente.celular.ai.local.LocalModelCatalog.findById(config.modelName)
            ?: com.asistente.celular.ai.local.LocalModelCatalog.DEFAULT_LOCAL_MODEL

        val isDownloaded = manager?.isModelDownloaded(spec.id) == true
        val modelFile = manager?.getModelFile(spec.id)

        if (!isDownloaded || modelFile == null || !modelFile.exists()) {
            return "El modelo local '${spec.name}' (${spec.formattedSize}) no está descargado aún en tu teléfono. Abre Ajustes en la app y pulsa en 'Descargar' para utilizarlo sin internet."
        }

        val engine = localInferenceEngine
            ?: return "Modelo local '${spec.name}' listo en almacenamiento (${modelFile.length() / (1024 * 1024)} MB). El motor de inferencia nativo no está inicializado."

        // Cargar modelo si aún no está cargado o si cambió el modelo seleccionado
        if (!engine.isLoaded || engine.loadedModelId != spec.id) {
            val loadResult = engine.loadModel(modelFile = modelFile, modelId = spec.id)
            if (loadResult.isFailure) {
                val err = loadResult.exceptionOrNull()?.message ?: "Error desconocido"
                throw IllegalStateException("Error cargando el modelo '${spec.name}' en memoria: $err")
            }
        }

        // Ejecutar inferencia generativa on-device con contexto del modelo activo
        val localSystemPrompt = "${config.systemPrompt}\nTe llamas Hendrix y estás ejecutándote de forma 100% local y offline en el teléfono mediante el modelo ${spec.name} (${spec.parameterCount}, ${spec.quantization})."
        val genResult = engine.generate(
            prompt = prompt,
            systemPrompt = localSystemPrompt,
            maxTokens = config.maxTokens,
            temperature = config.temperature
        )

        return genResult.getOrElse { error ->
            throw IllegalStateException("Error durante la generación de respuesta local: ${error.message}")
        }
    }

    companion object {
        private const val TAG = "LlmClient"
    }
}

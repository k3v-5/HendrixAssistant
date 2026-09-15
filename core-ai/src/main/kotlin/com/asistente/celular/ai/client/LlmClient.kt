package com.asistente.celular.ai.client

import android.util.Log
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import kotlinx.coroutines.Dispatchers
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

        return@withContext try {
            val responseText = when (config.provider) {
                AiProvider.GEMINI -> callGemini(prompt, config)
                AiProvider.OPENAI -> callOpenAiCompatible(
                    endpoint = "https://api.openai.com/v1/chat/completions",
                    prompt = prompt,
                    config = config
                )
                AiProvider.GROQ -> callOpenAiCompatible(
                    endpoint = "https://api.groq.com/openai/v1/chat/completions",
                    prompt = prompt,
                    config = config
                )
                AiProvider.OLLAMA -> callOllama(prompt, config)
                AiProvider.LOCAL_SLM -> executeLocalSlm(prompt, config)
            }
            Result.success(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error llamando a proveedor IA [${config.provider}]: ${e.message}", e)
            Result.failure(e)
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

package com.asistente.celular.ai.model

import com.asistente.celular.ai.BuildConfig
import kotlinx.serialization.Serializable

/**
 * Proveedores de IA compatibles.
 */
@Serializable
enum class AiProvider(val displayName: String, val defaultModel: String) {
    GEMINI("Google Gemini", "gemini-flash-latest"),
    GROQ("Groq (Llama 3)", "llama-3.1-8b-instant"),
    OPENAI("OpenAI (ChatGPT)", "gpt-4o-mini"),
    OLLAMA("Ollama Local / Servidor Propio", "llama3.2"),
    LOCAL_SLM("Modelo Local en Dispositivo", "smollm2-135m")
}

/**
 * Configuración activa para la inferencia de Inteligencia Artificial.
 */
@Serializable
data class LlmConfig(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = DEFAULT_GOOGLE_API_KEY,
    val modelName: String = provider.defaultModel,
    val customEndpoint: String? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 500,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val isModelHarnessEnabled: Boolean = true
) {
    companion object {
        val DEFAULT_GOOGLE_API_KEY: String
            get() = BuildConfig.DEFAULT_GEMINI_API_KEY

        const val DEFAULT_SYSTEM_PROMPT = """Eres el asistente inteligente Hendrix de un dispositivo móvil Android.
Tu función es responder de forma concisa, clara, directa y en español.
Evita introducciones innecesarias o textos excesivamente largos a menos que se te pida explícitamente, ya que tu respuesta será sintetizada por voz."""
    }
}

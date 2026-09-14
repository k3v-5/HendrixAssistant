package com.asistente.celular.data

import android.content.Context
import android.content.SharedPreferences
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig

/**
 * Repositorio persistente de configuración para el Asistente.
 * Almacena de forma permanente en SharedPreferences las claves,
 * el estado del servicio en segundo plano y las preferencias del usuario.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isWakeWordActive: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ACTIVE, value).apply()

    var isModelHarnessEnabled: Boolean
        get() = prefs.getBoolean(KEY_MODEL_HARNESS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MODEL_HARNESS_ENABLED, value).apply()

    var selectedProvider: AiProvider
        get() {
            val name = prefs.getString(KEY_AI_PROVIDER, AiProvider.GEMINI.name)
            return try {
                AiProvider.valueOf(name ?: AiProvider.GEMINI.name)
            } catch (e: Exception) {
                AiProvider.GEMINI
            }
        }
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER, value.name).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, LlmConfig.DEFAULT_GOOGLE_API_KEY) ?: LlmConfig.DEFAULT_GOOGLE_API_KEY
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var customModelName: String
        get() {
            val saved = prefs.getString(KEY_MODEL_NAME, null)
            if (saved == null || saved == "gemini-1.5-flash") {
                return selectedProvider.defaultModel
            }
            return saved
        }
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value).apply()

    var customEndpoint: String?
        get() = prefs.getString(KEY_CUSTOM_ENDPOINT, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_ENDPOINT, value).apply()

    var activeLocalModelId: String
        get() = prefs.getString(KEY_ACTIVE_LOCAL_MODEL_ID, com.asistente.celular.ai.local.LocalModelCatalog.DEFAULT_LOCAL_MODEL.id)
            ?: com.asistente.celular.ai.local.LocalModelCatalog.DEFAULT_LOCAL_MODEL.id
        set(value) = prefs.edit().putString(KEY_ACTIVE_LOCAL_MODEL_ID, value).apply()

    /**
     * Carga el objeto LlmConfig completo con los valores persistidos.
     */
    fun loadLlmConfig(): LlmConfig {
        val provider = selectedProvider
        val apiKey = when (provider) {
            AiProvider.GEMINI -> geminiApiKey
            else -> prefs.getString("api_key_${provider.name}", "") ?: ""
        }

        val model = if (provider == AiProvider.LOCAL_SLM) {
            activeLocalModelId
        } else {
            customModelName
        }

        return LlmConfig(
            provider = provider,
            apiKey = apiKey,
            modelName = model,
            customEndpoint = customEndpoint,
            isModelHarnessEnabled = isModelHarnessEnabled
        )
    }

    /**
     * Guarda la configuración activa de IA de forma permanente.
     */
    fun saveLlmConfig(config: LlmConfig) {
        selectedProvider = config.provider
        isModelHarnessEnabled = config.isModelHarnessEnabled
        if (config.provider == AiProvider.LOCAL_SLM) {
            activeLocalModelId = config.modelName
        } else {
            customModelName = config.modelName
        }
        customEndpoint = config.customEndpoint

        if (config.provider == AiProvider.GEMINI) {
            geminiApiKey = config.apiKey
        } else {
            prefs.edit().putString("api_key_${config.provider.name}", config.apiKey).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "asistente_celular_settings"
        private const val KEY_WAKE_WORD_ACTIVE = "wake_word_active"
        private const val KEY_MODEL_HARNESS_ENABLED = "model_harness_enabled"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_CUSTOM_ENDPOINT = "custom_endpoint"
        private const val KEY_ACTIVE_LOCAL_MODEL_ID = "active_local_model_id"
    }
}

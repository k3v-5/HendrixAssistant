package com.asistente.celular.data

import android.content.Context
import android.content.SharedPreferences
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.hardware.ShakeSensitivity
import com.asistente.celular.voice.kws.WakeWordSensitivity

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

    var wakeWordSensitivity: WakeWordSensitivity
        get() {
            val name = prefs.getString(KEY_WAKE_WORD_SENSITIVITY, WakeWordSensitivity.MEDIUM.name)
            return WakeWordSensitivity.fromName(name)
        }
        set(value) = prefs.edit().putString(KEY_WAKE_WORD_SENSITIVITY, value.name).apply()

    var isShakeToWakeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_TO_WAKE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_TO_WAKE, value).apply()

    var shakeSensitivity: ShakeSensitivity
        get() {
            val name = prefs.getString(KEY_SHAKE_SENSITIVITY, ShakeSensitivity.NORMAL.name)
            return ShakeSensitivity.fromName(name)
        }
        set(value) = prefs.edit().putString(KEY_SHAKE_SENSITIVITY, value.name).apply()

    var isPocketSilenceEnabled: Boolean
        get() = prefs.getBoolean(KEY_POCKET_SILENCE, true)
        set(value) = prefs.edit().putBoolean(KEY_POCKET_SILENCE, value).apply()

    var isFlipToMuteEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLIP_TO_MUTE, true)
        set(value) = prefs.edit().putBoolean(KEY_FLIP_TO_MUTE, value).apply()

    var ttsPitch: Float
        get() {
            val saved = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
            return if (saved == 0.85f) 1.0f else saved
        }
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()

    var ttsSpeechRate: Float
        get() {
            val saved = prefs.getFloat(KEY_TTS_SPEECH_RATE, 1.0f)
            return if (saved == 0.98f) 1.0f else saved
        }
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEECH_RATE, value).apply()

    var smartHomeCustomSubnet: String?
        get() = prefs.getString(KEY_SMART_HOME_CUSTOM_SUBNET, null)
        set(value) = prefs.edit().putString(KEY_SMART_HOME_CUSTOM_SUBNET, value?.takeIf { it.isNotBlank() }).apply()

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

    var personality: com.asistente.celular.ai.personality.AssistantPersonality
        get() {
            val name = prefs.getString(KEY_PERSONALITY, com.asistente.celular.ai.personality.AssistantPersonality.STANDARD.name)
            return try {
                com.asistente.celular.ai.personality.AssistantPersonality.valueOf(name ?: com.asistente.celular.ai.personality.AssistantPersonality.STANDARD.name)
            } catch (_: Exception) {
                com.asistente.celular.ai.personality.AssistantPersonality.STANDARD
            }
        }
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value.name).apply()

    var zeroCloudMode: Boolean
        get() = prefs.getBoolean(KEY_ZERO_CLOUD_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_ZERO_CLOUD_MODE, value).apply()

    /**
     * Carga el objeto LlmConfig completo con los valores persistidos.
     */
    fun loadLlmConfig(): LlmConfig {
        val provider = if (zeroCloudMode) AiProvider.LOCAL_SLM else selectedProvider
        val apiKey = when (provider) {
            AiProvider.GEMINI -> geminiApiKey
            else -> prefs.getString("api_key_${provider.name}", "") ?: ""
        }

        val model = if (provider == AiProvider.LOCAL_SLM) {
            activeLocalModelId
        } else {
            customModelName
        }

        val temperature = prefs.getFloat(KEY_LLM_TEMPERATURE, 0.7f)
        val maxTokens = prefs.getInt(KEY_LLM_MAX_TOKENS, 500)
        val systemPrompt = prefs.getString(KEY_LLM_SYSTEM_PROMPT, LlmConfig.DEFAULT_SYSTEM_PROMPT)
            ?: LlmConfig.DEFAULT_SYSTEM_PROMPT

        return LlmConfig(
            provider = provider,
            apiKey = apiKey,
            modelName = model,
            customEndpoint = customEndpoint,
            temperature = temperature,
            maxTokens = maxTokens,
            systemPrompt = systemPrompt,
            isModelHarnessEnabled = isModelHarnessEnabled,
            personality = personality,
            zeroCloudMode = zeroCloudMode
        )
    }

    /**
     * Guarda la configuración activa de IA de forma permanente.
     */
    fun saveLlmConfig(config: LlmConfig) {
        selectedProvider = config.provider
        isModelHarnessEnabled = config.isModelHarnessEnabled
        personality = config.personality
        zeroCloudMode = config.zeroCloudMode
        if (config.provider == AiProvider.LOCAL_SLM) {
            activeLocalModelId = config.modelName
        } else {
            customModelName = config.modelName
        }
        customEndpoint = config.customEndpoint

        prefs.edit()
            .putFloat(KEY_LLM_TEMPERATURE, config.temperature)
            .putInt(KEY_LLM_MAX_TOKENS, config.maxTokens)
            .putString(KEY_LLM_SYSTEM_PROMPT, config.systemPrompt)
            .apply()

        if (config.provider == AiProvider.GEMINI) {
            geminiApiKey = config.apiKey
        } else {
            prefs.edit().putString("api_key_${config.provider.name}", config.apiKey).apply()
        }
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "asistente_celular_settings"
        private const val KEY_WAKE_WORD_ACTIVE = "wake_word_active"
        private const val KEY_WAKE_WORD_SENSITIVITY = "wake_word_sensitivity"
        private const val KEY_MODEL_HARNESS_ENABLED = "model_harness_enabled"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_CUSTOM_ENDPOINT = "custom_endpoint"
        private const val KEY_ACTIVE_LOCAL_MODEL_ID = "active_local_model_id"
        private const val KEY_PERSONALITY = "assistant_personality"
        private const val KEY_ZERO_CLOUD_MODE = "zero_cloud_mode"
        private const val KEY_SHAKE_TO_WAKE = "shake_to_wake_enabled"
        private const val KEY_SHAKE_SENSITIVITY = "shake_sensitivity"
        private const val KEY_POCKET_SILENCE = "pocket_silence_enabled"
        private const val KEY_FLIP_TO_MUTE = "flip_to_mute_enabled"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_SPEECH_RATE = "tts_speech_rate"
        private const val KEY_SMART_HOME_CUSTOM_SUBNET = "smarthome_custom_subnet"
        private const val KEY_LLM_TEMPERATURE = "llm_temperature"
        private const val KEY_LLM_MAX_TOKENS = "llm_max_tokens"
        private const val KEY_LLM_SYSTEM_PROMPT = "llm_system_prompt"
        private const val KEY_OVERLAY_ENABLED = "is_overlay_enabled"
        private const val KEY_STT_ENGINE_TYPE = "stt_engine_type"
        private const val KEY_OFFLINE_ASR_MODEL_ID = "offline_asr_model_id"
    }

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var sttEngineType: com.asistente.celular.voice.stt.SttEngineType
        get() = com.asistente.celular.voice.stt.SttEngineType.fromName(prefs.getString(KEY_STT_ENGINE_TYPE, null))
        set(value) = prefs.edit().putString(KEY_STT_ENGINE_TYPE, value.name).apply()

    var offlineAsrModelId: String
        get() = prefs.getString(KEY_OFFLINE_ASR_MODEL_ID, "sherpa_onnx_zipformer_es") ?: "sherpa_onnx_zipformer_es"
        set(value) = prefs.edit().putString(KEY_OFFLINE_ASR_MODEL_ID, value).apply()
}

package com.asistente.celular.nlu.ui

/**
 * Contrato base para cargas útiles de interfaz interactiva (Generative UI)
 * desplegadas en la ventana flotante estilo Gemini / Siri.
 */
sealed interface AssistantUiPayload

data class SmartBulbUiPayload(
    val deviceName: String,
    val isPowerOn: Boolean,
    val brightness: Int = 100,
    val colorRgb: Int? = null,
    val colorTemp: Int? = null,
    val activeMode: String = "normal", // "normal", "candle", "party", "night"
    val ipAddress: String? = null
) : AssistantUiPayload

data class BatteryUiPayload(
    val percent: Int,
    val isCharging: Boolean,
    val chargeSource: String? = null,
    val isPowerSaveMode: Boolean = false
) : AssistantUiPayload

data class FlashlightUiPayload(
    val isOn: Boolean
) : AssistantUiPayload

data class VolumeUiPayload(
    val percent: Int,
    val streamType: Int,
    val streamName: String
) : AssistantUiPayload

data class BrightnessUiPayload(
    val percent: Int,
    val hasPermission: Boolean = true
) : AssistantUiPayload

data class CurrencyUiPayload(
    val amount: String,
    val fromCurrency: String,
    val fromName: String,
    val toCurrency: String,
    val toName: String,
    val resultAmount: String,
    val rate: String
) : AssistantUiPayload

data class NotesUiPayload(
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val id: String = ""
) : AssistantUiPayload

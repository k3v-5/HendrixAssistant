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

data class RoutineUiPayload(
    val routineId: String,
    val name: String,
    val triggerPhrase: String,
    val actionsCount: Int,
    val actionSummaries: List<String>,
    val isCreated: Boolean = true
) : AssistantUiPayload

data class WhatsAppQuickReplyPayload(
    val senderName: String,
    val messageSnippet: String,
    val suggestedReplies: List<String>,
    val notificationKey: String = ""
) : AssistantUiPayload

data class PomodoroWidgetPayload(
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val currentPhase: String = "work"
) : AssistantUiPayload

data class TallyCounterWidgetPayload(
    val title: String,
    val currentCount: Int = 0,
    val step: Int = 1
) : AssistantUiPayload

data class ScreenVisionUiPayload(
    val packageName: String?,
    val title: String?,
    val texts: List<String>,
    val summary: String
) : AssistantUiPayload

data class OcrGlanceUiPayload(
    val fullText: String,
    val lines: List<String>
) : AssistantUiPayload

data class DrivingModeUiPayload(
    val isActive: Boolean,
    val connectedDeviceName: String? = null
) : AssistantUiPayload

data class ExpenseReportUiPayload(
    val totalAmount: Double,
    val currency: String,
    val recentExpenses: List<com.asistente.celular.nlu.expenses.ExpenseItem>,
    val categoryTotals: Map<String, Double>
) : AssistantUiPayload

data class LocalFileResultsUiPayload(
    val query: String,
    val files: List<com.asistente.celular.nlu.files.LocalFileItem>
) : AssistantUiPayload

data class EmergencySosUiPayload(
    val isTriggered: Boolean,
    val locationUrl: String?,
    val emergencyContactsNotified: List<String>
) : AssistantUiPayload

data class Otp2FaUiPayload(
    val code: String,
    val sender: String,
    val serviceName: String? = null
) : AssistantUiPayload

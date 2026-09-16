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

data class VoiceprintUiPayload(
    val speakerName: String,
    val isMatch: Boolean,
    val confidencePercent: Int,
    val registeredProfilesCount: Int
) : AssistantUiPayload

data class KnowledgeRagUiPayload(
    val query: String,
    val results: List<com.asistente.celular.nlu.rag.KnowledgeSearchResult>,
    val topEntity: String? = null
) : AssistantUiPayload

data class VoiceJournalUiPayload(
    val summary: String,
    val sentiment: String,
    val actionItems: List<String>,
    val topics: List<String>
) : AssistantUiPayload

data class MeshSyncUiPayload(
    val localDeviceName: String,
    val peersCount: Int,
    val isBroadcasting: Boolean,
    val lastClipboardSnippet: String? = null
) : AssistantUiPayload

data class CallScreeningUiPayload(
    val callerNumber: String,
    val callerName: String?,
    val spamPercent: Int,
    val statusText: String,
    val liveSnippet: String
) : AssistantUiPayload

data class InterpreterUiPayload(
    val langA: String,
    val langB: String,
    val lastSpeaker: String,
    val lastOriginal: String,
    val lastTranslated: String
) : AssistantUiPayload

data class ContextTriggerUiPayload(
    val activeZone: String,
    val connectedWifi: String?,
    val activeRulesCount: Int,
    val triggeredRuleName: String? = null
) : AssistantUiPayload

data class AmbientDockUiPayload(
    val isDocked: Boolean,
    val dockType: String,
    val isNightMode: Boolean,
    val ambientMessage: String? = null
) : AssistantUiPayload

data class BatteryHealthUiPayload(
    val level: Int,
    val temperature: Float,
    val currentMa: Long,
    val thermalStatus: String,
    val smartCutoffTarget: Int = 80
) : AssistantUiPayload

data class PrivacyFirewallUiPayload(
    val blockedTrackersCount: Int,
    val recentTrackers: List<String>,
    val isFirewallActive: Boolean = true
) : AssistantUiPayload

data class SecurityAuditUiPayload(
    val securityScore: Int,
    val riskyAppsCount: Int,
    val topRiskyApps: List<String>
) : AssistantUiPayload

data class HardwareGestureUiPayload(
    val lastDetectedGesture: String,
    val isListening: Boolean,
    val sensitivity: Float = 1.0f
) : AssistantUiPayload

data class CameraDirectorUiPayload(
    val countdownSeconds: Int,
    val activeLens: String,
    val detectedPeopleCount: Int,
    val statusMessage: String
) : AssistantUiPayload

data class HealthTelemetryUiPayload(
    val steps: Int,
    val goalSteps: Int = 10000,
    val heartRate: Int,
    val sleepHours: Float,
    val recoveryText: String
) : AssistantUiPayload

data class TaskPlanUiPayload(
    val planId: String,
    val userGoal: String,
    val steps: List<com.asistente.celular.nlu.planner.PlanStep>,
    val statusText: String
) : AssistantUiPayload

data class AutomotiveUiPayload(
    val isCarConnected: Boolean,
    val headUnitName: String?,
    val currentTemplate: String,
    val shortcuts: List<String>
) : AssistantUiPayload

data class WearCompanionUiPayload(
    val connectedWearCount: Int,
    val devicesSummary: String,
    val lastSyncText: String
) : AssistantUiPayload

data class MeetingRecorderUiPayload(
    val meetingTitle: String,
    val turnsCount: Int,
    val latestTurnSpeaker: String?,
    val agreements: List<String>,
    val isRecording: Boolean
) : AssistantUiPayload

data class MultiModelOrchestratorUiPayload(
    val selectedModel: String,
    val expectedLatencyMs: Int,
    val fastPathActive: Boolean,
    val complexity: String
) : AssistantUiPayload

data class DocumentChatUiPayload(
    val fileName: String,
    val question: String,
    val answer: String,
    val sectionReference: String,
    val confidencePercent: Int
) : AssistantUiPayload

data class SoundscapeUiPayload(
    val soundscapeName: String,
    val isPlaying: Boolean,
    val volumePercent: Int,
    val remainingMinutes: Int? = null
) : AssistantUiPayload

data class VoiceCraftUiPayload(
    val styleName: String,
    val pitchShift: Float,
    val speechRate: Float,
    val formantFactor: Float
) : AssistantUiPayload

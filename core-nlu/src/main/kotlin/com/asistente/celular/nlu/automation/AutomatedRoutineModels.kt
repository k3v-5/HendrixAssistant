package com.asistente.celular.nlu.automation

import java.util.UUID

/**
 * Tipo de transición para disparadores de red Wi-Fi.
 */
enum class WifiTransition {
    CONNECTED,
    DISCONNECTED
}

/**
 * Tipo de transición para disparadores de geocercas.
 */
enum class GeofenceTransition {
    ENTER,
    EXIT
}

/**
 * Disparador o evento desencadenante de una rutina automatizada "Zero-Touch".
 */
sealed interface AutomatedRoutineTrigger {
    data class WifiSsidTrigger(
        val ssid: String,
        val transition: WifiTransition = WifiTransition.CONNECTED
    ) : AutomatedRoutineTrigger

    data class GeofenceTrigger(
        val zoneName: String,
        val transition: GeofenceTransition = GeofenceTransition.ENTER
    ) : AutomatedRoutineTrigger

    data class PcEventTrigger(
        val eventType: String // e.g. "RENDER_COMPLETED", "GPU_OVERHEAT", "PC_ONLINE"
    ) : AutomatedRoutineTrigger

    data class ChargingTrigger(
        val isCharging: Boolean = true
    ) : AutomatedRoutineTrigger

    data class ScheduleTrigger(
        val timeString: String, // e.g. "08:00"
        val daysOfWeek: List<Int> = emptyList() // 1 = Monday ... 7 = Sunday
    ) : AutomatedRoutineTrigger

    data class VoicePhraseTrigger(
        val phrases: List<String>
    ) : AutomatedRoutineTrigger
}

/**
 * Acción ejecutable dentro de una rutina automatizada.
 */
sealed interface AutomatedRoutineAction {
    data class AssistantCommandAction(
        val commandText: String
    ) : AutomatedRoutineAction

    data class PcQuickCommandAction(
        val command: String // e.g. "wake_on_lan", "unlock", "sleep"
    ) : AutomatedRoutineAction

    data class PcStudioSceneAction(
        val sceneId: String
    ) : AutomatedRoutineAction

    data class PcPluginAction(
        val pluginId: String,
        val actionId: String,
        val params: Map<String, Any> = emptyMap()
    ) : AutomatedRoutineAction

    data class SpeakTtsAction(
        val text: String
    ) : AutomatedRoutineAction

    data class DelayAction(
        val delayMillis: Long
    ) : AutomatedRoutineAction

    data class EnterDeskStandbyAction(
        val isKeepScreenOn: Boolean = true
    ) : AutomatedRoutineAction
}

/**
 * Rutina de automatización completa "Zero-Touch" compuesta por disparadores y acciones encadenadas.
 */
data class AutomatedRoutine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconEmoji: String = "⚡",
    val isEnabled: Boolean = true,
    val triggers: List<AutomatedRoutineTrigger> = emptyList(),
    val actions: List<AutomatedRoutineAction> = emptyList(),
    val lastTriggeredEpoch: Long = 0L,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

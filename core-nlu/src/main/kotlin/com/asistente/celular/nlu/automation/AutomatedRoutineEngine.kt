package com.asistente.celular.nlu.automation

import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Motor de evaluación y ejecución de rutinas automatizadas "Zero-Touch".
 */
class AutomatedRoutineEngine(
    initialRoutines: List<AutomatedRoutine> = emptyList()
) {
    private val routines = ConcurrentHashMap<String, AutomatedRoutine>()

    init {
        initialRoutines.forEach { routines[it.id] = it }
    }

    fun getRoutines(): List<AutomatedRoutine> = routines.values.toList()

    fun getRoutine(id: String): AutomatedRoutine? = routines[id]

    fun registerRoutine(routine: AutomatedRoutine) {
        routines[routine.id] = routine
    }

    fun deleteRoutine(id: String): Boolean = routines.remove(id) != null

    fun toggleRoutine(id: String): Boolean {
        var newStatus = false
        val updated = routines.computeIfPresent(id) { _, r ->
            newStatus = !r.isEnabled
            r.copy(isEnabled = newStatus)
        }
        return updated?.isEnabled ?: false
    }

    fun evaluateWifiEvent(ssid: String, transition: WifiTransition): List<AutomatedRoutine> {
        return routines.values.filter { routine ->
            routine.isEnabled && routine.triggers.any { trigger ->
                trigger is AutomatedRoutineTrigger.WifiSsidTrigger &&
                        trigger.transition == transition &&
                        trigger.ssid.equals(ssid, ignoreCase = true)
            }
        }
    }

    fun evaluateGeofenceEvent(zoneName: String, transition: GeofenceTransition): List<AutomatedRoutine> {
        return routines.values.filter { routine ->
            routine.isEnabled && routine.triggers.any { trigger ->
                trigger is AutomatedRoutineTrigger.GeofenceTrigger &&
                        trigger.transition == transition &&
                        trigger.zoneName.equals(zoneName, ignoreCase = true)
            }
        }
    }

    fun evaluatePcEvent(eventType: String): List<AutomatedRoutine> {
        return routines.values.filter { routine ->
            routine.isEnabled && routine.triggers.any { trigger ->
                trigger is AutomatedRoutineTrigger.PcEventTrigger &&
                        trigger.eventType.equals(eventType, ignoreCase = true)
            }
        }
    }

    fun evaluateChargingEvent(isCharging: Boolean): List<AutomatedRoutine> {
        return routines.values.filter { routine ->
            routine.isEnabled && routine.triggers.any { trigger ->
                trigger is AutomatedRoutineTrigger.ChargingTrigger &&
                        trigger.isCharging == isCharging
            }
        }
    }

    fun evaluateVoiceEvent(phrase: String): List<AutomatedRoutine> {
        val lower = phrase.lowercase().trim()
        return routines.values.filter { routine ->
            routine.isEnabled && routine.triggers.any { trigger ->
                trigger is AutomatedRoutineTrigger.VoicePhraseTrigger &&
                        trigger.phrases.any { lower.contains(it.lowercase()) }
            }
        }
    }

    suspend fun executeRoutine(
        routine: AutomatedRoutine,
        actionExecutor: suspend (AutomatedRoutineAction) -> Boolean
    ): Int {
        var executedCount = 0
        for (action in routine.actions) {
            when (action) {
                is AutomatedRoutineAction.DelayAction -> {
                    delay(action.delayMillis)
                    executedCount++
                }
                else -> {
                    val success = actionExecutor(action)
                    if (success) executedCount++
                }
            }
        }
        routines.computeIfPresent(routine.id) { _, r -> r.copy(lastTriggeredEpoch = System.currentTimeMillis()) }
        return executedCount
    }
}

package com.asistente.celular.context

import android.content.Context
import com.asistente.celular.nlu.context.ContextRule
import com.asistente.celular.nlu.context.ContextTriggerEngine
import com.asistente.celular.nlu.context.ContextTriggerType
import com.asistente.celular.nlu.context.CurrentContextState
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinador de disparadores de contexto y geocercas inteligentes por red.
 */
class GeofenceRuleCoordinator(
    private val context: Context
) : ContextTriggerEngine {

    private val rules = ConcurrentHashMap<String, ContextRule>()
    private var currentState = CurrentContextState(
        currentWifiSsid = "Home_WiFi_5G",
        currentGeofenceZone = "Casa",
        isCharging = false,
        isDriving = false,
        activeProfileName = "Hogar"
    )

    init {
        // Reglas de ejemplo preconfiguradas
        val homeWifiRule = ContextRule(
            id = "rule_home_arrival",
            name = "Llegada a Casa",
            triggerType = ContextTriggerType.WIFI_SSID_CONNECTED,
            triggerValue = "Home_WiFi_5G",
            actionsToExecute = listOf("enciende la luz de la sala", "bienvenido a casa"),
            isEnabled = true
        )
        val officeRule = ContextRule(
            id = "rule_office_arrival",
            name = "Llegada al Trabajo",
            triggerType = ContextTriggerType.WIFI_SSID_CONNECTED,
            triggerValue = "Office_Secure_WiFi",
            actionsToExecute = listOf("silenciar el móvil", "abrir la agenda de hoy"),
            isEnabled = true
        )
        rules[homeWifiRule.id] = homeWifiRule
        rules[officeRule.id] = officeRule
    }

    override suspend fun evaluateContextChanges(newState: CurrentContextState): List<ContextRule> {
        val triggered = mutableListOf<ContextRule>()
        val previousState = currentState
        currentState = newState

        // Evaluar conexión Wi-Fi nueva
        if (newState.currentWifiSsid != null && newState.currentWifiSsid != previousState.currentWifiSsid) {
            val matching = rules.values.filter {
                it.isEnabled && it.triggerType == ContextTriggerType.WIFI_SSID_CONNECTED &&
                        it.triggerValue.equals(newState.currentWifiSsid, ignoreCase = true)
            }
            triggered.addAll(matching)
        }

        // Evaluar cambio de zona geofence
        if (newState.currentGeofenceZone != null && newState.currentGeofenceZone != previousState.currentGeofenceZone) {
            val matching = rules.values.filter {
                it.isEnabled && it.triggerType == ContextTriggerType.GEOFENCE_ENTER &&
                        it.triggerValue.equals(newState.currentGeofenceZone, ignoreCase = true)
            }
            triggered.addAll(matching)
        }

        return triggered
    }

    override fun registerRule(rule: ContextRule) {
        rules[rule.id] = rule
    }

    override fun getRegisteredRules(): List<ContextRule> {
        return rules.values.toList()
    }

    override fun deleteRule(ruleId: String): Boolean {
        return rules.remove(ruleId) != null
    }

    override fun getCurrentState(): CurrentContextState = currentState
}

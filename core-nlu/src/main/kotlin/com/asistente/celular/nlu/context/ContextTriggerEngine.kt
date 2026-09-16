package com.asistente.celular.nlu.context

enum class ContextTriggerType {
    WIFI_SSID_CONNECTED,
    WIFI_SSID_DISCONNECTED,
    GEOFENCE_ENTER,
    GEOFENCE_EXIT,
    BLUETOOTH_DEVICE_CONNECTED,
    CHARGER_CONNECTED,
    TIME_WINDOW
}

data class ContextRule(
    val id: String,
    val name: String,
    val triggerType: ContextTriggerType,
    val triggerValue: String, // e.g. "MiCasa_5G", "Oficina", "mac_address"
    val actionsToExecute: List<String>, // list of voice/assistant commands
    val isEnabled: Boolean = true
)

data class CurrentContextState(
    val currentWifiSsid: String? = null,
    val currentGeofenceZone: String? = null,
    val isCharging: Boolean = false,
    val isDriving: Boolean = false,
    val activeProfileName: String = "Normal"
)

/**
 * Contrato para el motor de rutinas y disparadores de contexto proactivo.
 */
interface ContextTriggerEngine {
    suspend fun evaluateContextChanges(newState: CurrentContextState): List<ContextRule>
    fun registerRule(rule: ContextRule)
    fun getRegisteredRules(): List<ContextRule>
    fun deleteRule(ruleId: String): Boolean
    fun getCurrentState(): CurrentContextState
}

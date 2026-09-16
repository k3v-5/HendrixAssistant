package com.asistente.celular.nlu.automotive

enum class CarScreenTemplate {
    NAVIGATION,
    VOICE_COMMAND,
    MEDIA_PLAYER,
    SMART_HOME_QUICK_ACTIONS
}

data class CarNavigationShortcut(
    val title: String,
    val destinationAddress: String,
    val estimatedMinutes: Int,
    val iconType: String = "home" // "home", "work", "gas", "parking"
)

data class AutomotiveScreenState(
    val isCarConnected: Boolean = false,
    val headUnitName: String? = null,
    val currentTemplate: CarScreenTemplate = CarScreenTemplate.VOICE_COMMAND,
    val shortcuts: List<CarNavigationShortcut> = emptyList(),
    val isMicActive: Boolean = false
)

/**
 * Contrato para la orquestación automotriz y enlace con pantallas de vehículos (Android Auto).
 */
interface AutomotiveCarController {
    fun notifyCarConnected(headUnitName: String)
    fun notifyCarDisconnected()
    fun getCurrentScreenState(): AutomotiveScreenState
    fun switchTemplate(template: CarScreenTemplate)
    suspend fun triggerVoiceSessionFromSteeringWheel(): Boolean
}

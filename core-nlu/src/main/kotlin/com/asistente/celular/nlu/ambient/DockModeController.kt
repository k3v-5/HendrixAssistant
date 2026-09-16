package com.asistente.celular.nlu.ambient

enum class DockType {
    WIRELESS_CHARGER,
    DESK_CRADLE,
    NIGHTSTAND,
    CAR_DOCK
}

data class DockModeState(
    val isDocked: Boolean = false,
    val dockType: DockType? = null,
    val clockTheme: String = "minimal_oled",
    val isNightModeActive: Boolean = false,
    val ambientMessage: String? = null
)

/**
 * Contrato para el control del Modo Ambient Dock / Estación Inteligente.
 */
interface DockModeController {
    fun notifyDockStateChanged(isDocked: Boolean, type: DockType?)
    fun getCurrentDockState(): DockModeState
    fun setNightTheme(enabled: Boolean)
    fun setCustomAmbientMessage(message: String?)
}

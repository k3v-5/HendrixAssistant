package com.asistente.celular.ambient

import android.content.Context
import com.asistente.celular.nlu.ambient.DockModeController
import com.asistente.celular.nlu.ambient.DockModeState
import com.asistente.celular.nlu.ambient.DockType

/**
 * Coordinador para el modo Ambient Dock y pantalla inteligente para base de carga o noche.
 */
class DockModeCoordinator(
    private val context: Context
) : DockModeController {

    private var dockState = DockModeState(
        isDocked = false,
        dockType = null,
        clockTheme = "minimal_oled",
        isNightModeActive = false,
        ambientMessage = "Hendrix en reposo"
    )

    override fun notifyDockStateChanged(isDocked: Boolean, type: DockType?) {
        dockState = dockState.copy(
            isDocked = isDocked,
            dockType = type,
            ambientMessage = if (isDocked) "Estación de carga activa" else "Hendrix en reposo"
        )
    }

    override fun getCurrentDockState(): DockModeState = dockState

    override fun setNightTheme(enabled: Boolean) {
        dockState = dockState.copy(
            isNightModeActive = enabled,
            clockTheme = if (enabled) "night_dim_red" else "minimal_oled"
        )
    }

    override fun setCustomAmbientMessage(message: String?) {
        dockState = dockState.copy(ambientMessage = message)
    }
}

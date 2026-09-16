package com.asistente.celular.automotive

import android.content.Context
import com.asistente.celular.nlu.automotive.AutomotiveCarController
import com.asistente.celular.nlu.automotive.AutomotiveScreenState
import com.asistente.celular.nlu.automotive.CarNavigationShortcut
import com.asistente.celular.nlu.automotive.CarScreenTemplate

/**
 * Coordinador para integración con pantallas de vehículos Android Auto y CarAppService.
 */
class HendrixCarAppCoordinator(
    private val context: Context
) : AutomotiveCarController {

    private var screenState = AutomotiveScreenState(
        isCarConnected = false,
        headUnitName = null,
        currentTemplate = CarScreenTemplate.VOICE_COMMAND,
        shortcuts = listOf(
            CarNavigationShortcut("Casa", "Av. Principal 123", 18, "home"),
            CarNavigationShortcut("Oficina", "Paseo de la Reforma 500", 25, "work"),
            CarNavigationShortcut("Gasolinera Cercana", "Estación Central", 5, "gas")
        ),
        isMicActive = false
    )

    override fun notifyCarConnected(headUnitName: String) {
        screenState = screenState.copy(
            isCarConnected = true,
            headUnitName = headUnitName,
            currentTemplate = CarScreenTemplate.NAVIGATION
        )
    }

    override fun notifyCarDisconnected() {
        screenState = screenState.copy(
            isCarConnected = false,
            headUnitName = null,
            currentTemplate = CarScreenTemplate.VOICE_COMMAND
        )
    }

    override fun getCurrentScreenState(): AutomotiveScreenState = screenState

    override fun switchTemplate(template: CarScreenTemplate) {
        screenState = screenState.copy(currentTemplate = template)
    }

    override suspend fun triggerVoiceSessionFromSteeringWheel(): Boolean {
        screenState = screenState.copy(isMicActive = true)
        return true
    }
}

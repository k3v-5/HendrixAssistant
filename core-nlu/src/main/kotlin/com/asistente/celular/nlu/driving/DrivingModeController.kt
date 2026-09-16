package com.asistente.celular.nlu.driving

/**
 * Contrato desacoplado para el control y estado del Modo Conducción (In-Car Companion).
 * Sigue el principio OCP de GEMINI.md.
 */
interface DrivingModeController {
    fun isDrivingModeActive(): Boolean
    fun setDrivingMode(active: Boolean): Boolean
    fun getConnectedVehicleName(): String?
}

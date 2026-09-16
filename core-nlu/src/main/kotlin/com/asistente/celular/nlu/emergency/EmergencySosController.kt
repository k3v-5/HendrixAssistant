package com.asistente.celular.nlu.emergency

/**
 * Resultado estructurado tras activar la secuencia SOS de emergencia.
 */
data class EmergencyTriggerResult(
    val success: Boolean,
    val locationUrl: String?,
    val notifiedContacts: List<String>,
    val sirenStarted: Boolean,
    val flashlightSosStarted: Boolean
)

/**
 * Contrato desacoplado para el controlador de emergencia y SOS manos libres (OCP).
 */
interface EmergencySosController {
    suspend fun triggerEmergencySos(): EmergencyTriggerResult
    fun cancelEmergencySos(): Boolean
    fun isEmergencyActive(): Boolean
}

package com.asistente.celular.nlu.rhythm

/**
 * Contrato desacoplado para rutinas proactivas de ritmo circadiano (sueño y despertar).
 */
interface SleepWakeController {
    suspend fun executeGoodNightRoutine(): String
    suspend fun executeGoodMorningRoutine(): String
}

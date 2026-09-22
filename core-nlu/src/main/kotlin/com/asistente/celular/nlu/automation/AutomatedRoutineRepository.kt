package com.asistente.celular.nlu.automation

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de persistencia y gestión de rutinas automatizadas "Zero-Touch".
 */
interface AutomatedRoutineRepository {
    val engine: AutomatedRoutineEngine
    val routines: StateFlow<List<AutomatedRoutine>>

    suspend fun saveRoutine(routine: AutomatedRoutine)
    suspend fun deleteRoutine(id: String)
    suspend fun toggleRoutine(id: String): Boolean
}

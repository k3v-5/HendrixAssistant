package com.asistente.celular.nlu.routines

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio abstracto para la gestión y consulta de rutinas de usuario.
 * Sigue las directrices de desacoplamiento e inversión de dependencias de GEMINI.md.
 */
interface RoutineRepository {
    /**
     * Flujo reactivo con la lista de rutinas configuradas en el sistema.
     */
    val routines: StateFlow<List<RoutineItem>>

    /**
     * Obtiene una rutina por su identificador único.
     */
    suspend fun getRoutineById(id: String): RoutineItem?

    /**
     * Busca si alguna rutina habilitada coincide con la frase o intención de activación proporcionada.
     */
    suspend fun findMatchingRoutine(inputPhrase: String): RoutineItem?

    /**
     * Crea o actualiza una rutina en el almacenamiento.
     */
    suspend fun saveRoutine(routine: RoutineItem)

    /**
     * Elimina una rutina por su identificador único.
     */
    suspend fun deleteRoutine(id: String): Boolean
}

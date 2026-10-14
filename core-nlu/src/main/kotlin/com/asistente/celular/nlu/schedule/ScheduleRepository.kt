package com.asistente.celular.nlu.schedule

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato formal para el repositorio reactivo y offline-first de Horarios de Actividades.
 * Sigue el principio de inversión de dependencias (DIP) y segregación de interfaces (ISP).
 */
interface ScheduleRepository {
    /**
     * Flujo reactivo con la lista completa de actividades programadas.
     */
    val activities: StateFlow<List<ScheduleActivityItem>>

    /**
     * Agrega una nueva actividad al horario y persiste atómicamente.
     */
    suspend fun addActivity(activity: ScheduleActivityItem): ScheduleActivityItem

    /**
     * Actualiza una actividad existente.
     */
    suspend fun updateActivity(activity: ScheduleActivityItem): Boolean

    /**
     * Elimina una actividad por su ID.
     */
    suspend fun deleteActivity(id: String): Boolean

    /**
     * Activa o desactiva una actividad sin eliminarla.
     */
    suspend fun toggleActivityEnabled(id: String): ScheduleActivityItem?

    /**
     * Obtiene una actividad por su identificador único.
     */
    suspend fun getActivityById(id: String): ScheduleActivityItem?

    /**
     * Obtiene las actividades programadas para un día de la semana específico,
     * ordenadas cronológicamente por hora de inicio.
     */
    suspend fun getActivitiesForDay(day: ScheduleDay): List<ScheduleActivityItem>

    /**
     * Obtiene todas las categorías únicas utilizadas en el horario.
     */
    suspend fun getAllCategories(): List<String>
}

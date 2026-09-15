package com.asistente.celular.nlu.calendar

/**
 * Contrato abstracto para consultar y crear eventos en el calendario del usuario.
 */
interface CalendarRepository {

    /**
     * Comprueba si la aplicación cuenta con los permisos necesarios para acceder al calendario.
     */
    fun hasCalendarPermission(): Boolean

    /**
     * Obtiene los eventos comprendidos en el intervalo temporal [startMillis, endMillis].
     */
    suspend fun getEvents(startMillis: Long, endMillis: Long): List<CalendarEventItem>

    /**
     * Obtiene los próximos eventos a partir del instante actual.
     */
    suspend fun getUpcomingEvents(limit: Int = 5): List<CalendarEventItem>

    /**
     * Crea un nuevo evento en el calendario principal del dispositivo.
     * Retorna el id del evento creado o null si no se pudo crear.
     */
    suspend fun createEvent(
        title: String,
        startMillis: Long,
        durationMinutes: Int = 60,
        description: String = "",
        location: String = ""
    ): Long?
}

package com.asistente.celular.nlu.calendar

import kotlinx.serialization.Serializable

/**
 * Modelo de dominio desacoplado para representar eventos de calendario.
 */
@Serializable
data class CalendarEventItem(
    val id: Long,
    val title: String,
    val description: String = "",
    val startMillis: Long,
    val endMillis: Long,
    val location: String = "",
    val isAllDay: Boolean = false,
    val calendarName: String = "",
    val metadata: Map<String, String> = emptyMap()
)

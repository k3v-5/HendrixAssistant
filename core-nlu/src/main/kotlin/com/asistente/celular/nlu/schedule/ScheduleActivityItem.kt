package com.asistente.celular.nlu.schedule

import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.UUID

/**
 * Representación tipada de los días de la semana para horarios de actividades.
 */
@Serializable
enum class ScheduleDay(val dayIndex: Int, val shortLabel: String, val fullLabel: String) {
    MONDAY(1, "Lun", "Lunes"),
    TUESDAY(2, "Mar", "Martes"),
    WEDNESDAY(3, "Mié", "Miércoles"),
    THURSDAY(4, "Jue", "Jueves"),
    FRIDAY(5, "Vie", "Viernes"),
    SATURDAY(6, "Sáb", "Sábado"),
    SUNDAY(7, "Dom", "Domingo");

    companion object {
        fun fromDayIndex(index: Int): ScheduleDay? = entries.firstOrNull { it.dayIndex == index }

        /**
         * Retorna el ScheduleDay correspondiente al Calendar.DAY_OF_WEEK de Java.
         * Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
         */
        fun fromCalendarDay(calendarDay: Int): ScheduleDay {
            return when (calendarDay) {
                Calendar.MONDAY -> MONDAY
                Calendar.TUESDAY -> TUESDAY
                Calendar.WEDNESDAY -> WEDNESDAY
                Calendar.THURSDAY -> THURSDAY
                Calendar.FRIDAY -> FRIDAY
                Calendar.SATURDAY -> SATURDAY
                Calendar.SUNDAY -> SUNDAY
                else -> MONDAY
            }
        }

        fun currentDay(): ScheduleDay {
            val calendar = Calendar.getInstance()
            return fromCalendarDay(calendar.get(Calendar.DAY_OF_WEEK))
        }

        val WEEKDAYS: Set<ScheduleDay> = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        val WEEKEND: Set<ScheduleDay> = setOf(SATURDAY, SUNDAY)
        val ALL_DAYS: Set<ScheduleDay> = entries.toSet()
    }
}

/**
 * Modelo de datos inmutable para Actividades de Horario (Schedule Activity).
 * Diseñado conforme a los principios de escalabilidad preventiva y modelos evolutivos.
 */
@Serializable
data class ScheduleActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val daysOfWeek: Set<ScheduleDay> = setOf(ScheduleDay.MONDAY),
    val startTime: String, // Formato "HH:mm" (24 horas), ej. "08:00"
    val endTime: String,   // Formato "HH:mm" (24 horas), ej. "10:30"
    val category: String = DEFAULT_CATEGORY,
    val colorHex: String = DEFAULT_COLOR,
    val location: String = "",
    val isReminderEnabled: Boolean = false,
    val reminderOffsetMinutes: Int = 10,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val DEFAULT_CATEGORY = "General"
        const val DEFAULT_COLOR = "#00F0FF" // NeonCyan por defecto

        val PRESET_CATEGORIES = listOf(
            "Trabajo",
            "Estudio",
            "Desarrollo / Código",
            "Música / Producción",
            "Salud / Gimnasio",
            "Descanso / Rutina",
            "Personal"
        )

        val PRESET_COLORS = listOf(
            "#00F0FF", // Neon Cyan
            "#A855F7", // Neon Purple
            "#10B981", // Neon Green
            "#F59E0B", // Neon Amber
            "#EF4444", // Neon Red
            "#C084FC", // Neon Lilac
            "#38BDF8"  // Sky Blue
        )
    }

    /**
     * Verifica si la actividad se ejecuta en el día especificado.
     */
    fun occursOn(day: ScheduleDay): Boolean = daysOfWeek.contains(day)

    /**
     * Verifica si la actividad está en curso en un momento determinado.
     * @param day Día a evaluar.
     * @param timeStr Hora en formato "HH:mm".
     */
    fun isOngoingAt(day: ScheduleDay, timeStr: String): Boolean {
        if (!isEnabled || !occursOn(day)) return false
        val (normStart, normEnd) = normalizeTimes(startTime, endTime)
        return timeStr in normStart..normEnd
    }

    /**
     * Calcula la duración en minutos entre startTime y endTime.
     */
    fun durationMinutes(): Int {
        val startMins = parseMinutes(startTime)
        val endMins = parseMinutes(endTime)
        return if (endMins >= startMins) {
            endMins - startMins
        } else {
            (24 * 60 - startMins) + endMins // Cruza medianoche
        }
    }

    private fun parseMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun normalizeTimes(start: String, end: String): Pair<String, String> {
        val s = if (start.length == 4 && start[1] == ':') "0$start" else start
        val e = if (end.length == 4 && end[1] == ':') "0$end" else end
        return Pair(s, e)
    }
}

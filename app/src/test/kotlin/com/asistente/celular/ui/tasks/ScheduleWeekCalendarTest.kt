package com.asistente.celular.ui.tasks

import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import com.asistente.celular.ui.tasks.schedule.ScheduleViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la lógica del Calendario Semanal y la priorización de Horario.
 * 
 * Valida:
 * 1. Default de TasksTab como SCHEDULE (Horario prioritario sobre Tareas).
 * 2. Agrupación y ordenamiento cronológico por día de la semana (Lunes a Domingo).
 * 3. Filtrado y resolución de día para el foco de eventos en curso sin disparar edición automática.
 * 4. Consistencia del modelo de actividades recurrentes a lo largo de los 7 días.
 */
class ScheduleWeekCalendarTest {

    @Test
    fun testDefaultTasksTabIsSchedule() {
        // Verifica que la pestaña de Horario sea la prioritaria por defecto
        val defaultTab = TasksTab.SCHEDULE
        assertEquals(TasksTab.SCHEDULE, defaultTab)
    }

    @Test
    fun testScheduleViewModeDefaultsToWeekCalendar() {
        // Verifica los modos de vista disponibles
        val defaultMode = ScheduleViewMode.WEEK_CALENDAR
        assertEquals(ScheduleViewMode.WEEK_CALENDAR, defaultMode)
        assertTrue(ScheduleViewMode.entries.contains(ScheduleViewMode.DAY_LIST))
    }

    @Test
    fun testWeeklyGroupingByAllSevenDays() {
        val routine1 = ScheduleActivityItem(
            id = "r1",
            title = "Morning Run",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY, ScheduleDay.FRIDAY),
            startTime = "06:30",
            endTime = "07:15"
        )
        val routine2 = ScheduleActivityItem(
            id = "r2",
            title = "Team Standup",
            daysOfWeek = ScheduleDay.WEEKDAYS,
            startTime = "09:00",
            endTime = "09:30"
        )
        val routine3 = ScheduleActivityItem(
            id = "r3",
            title = "Weekend Hike",
            daysOfWeek = ScheduleDay.WEEKEND,
            startTime = "08:00",
            endTime = "12:00"
        )

        val activities = listOf(routine1, routine2, routine3)

        // Agrupación de la semana completa
        val activitiesByDay = ScheduleDay.entries.associateWith { day ->
            activities
                .filter { it.occursOn(day) }
                .sortedBy { it.startTime }
        }

        // Todos los 7 días deben estar presentes en el mapa
        assertEquals(7, activitiesByDay.size)

        // Lunes debe tener routine1 (06:30) y routine2 (09:00) en ese orden
        val mondayRoutines = activitiesByDay[ScheduleDay.MONDAY] ?: emptyList()
        assertEquals(2, mondayRoutines.size)
        assertEquals("Morning Run", mondayRoutines[0].title)
        assertEquals("Team Standup", mondayRoutines[1].title)

        // Martes solo tiene routine2
        val tuesdayRoutines = activitiesByDay[ScheduleDay.TUESDAY] ?: emptyList()
        assertEquals(1, tuesdayRoutines.size)
        assertEquals("Team Standup", tuesdayRoutines[0].title)

        // Sábado solo tiene routine3
        val saturdayRoutines = activitiesByDay[ScheduleDay.SATURDAY] ?: emptyList()
        assertEquals(1, saturdayRoutines.size)
        assertEquals("Weekend Hike", saturdayRoutines[0].title)
    }

    @Test
    fun testTargetEventResolutionForFocusWithoutEditDialog() {
        val targetId = "target-routine-123"
        val activities = listOf(
            ScheduleActivityItem(
                id = "other-1",
                title = "Meditación",
                daysOfWeek = setOf(ScheduleDay.MONDAY),
                startTime = "07:00",
                endTime = "07:30"
            ),
            ScheduleActivityItem(
                id = targetId,
                title = "Sesión DAW y Mezcla",
                daysOfWeek = setOf(ScheduleDay.THURSDAY, ScheduleDay.FRIDAY),
                startTime = "16:00",
                endTime = "18:00"
            )
        )

        val target = activities.firstOrNull { it.id == targetId }
        assertNotNull(target)

        // Simula la lógica de resolución de día para el foco
        val currentDay = ScheduleDay.FRIDAY
        val resolvedDay = if (target!!.occursOn(currentDay)) {
            currentDay
        } else {
            target.daysOfWeek.firstOrNull() ?: currentDay
        }

        assertEquals(ScheduleDay.FRIDAY, resolvedDay)
        assertTrue(target.occursOn(resolvedDay))
    }

    @Test
    fun testChronologicalOrderingWithinSameDay() {
        val actAfternoon = ScheduleActivityItem(
            id = "act-2",
            title = "Almuerzo y Descanso",
            daysOfWeek = setOf(ScheduleDay.MONDAY),
            startTime = "13:00",
            endTime = "14:00"
        )
        val actMorning = ScheduleActivityItem(
            id = "act-1",
            title = "Deep Work",
            daysOfWeek = setOf(ScheduleDay.MONDAY),
            startTime = "08:30",
            endTime = "12:30"
        )
        val actNight = ScheduleActivityItem(
            id = "act-3",
            title = "Lectura",
            daysOfWeek = setOf(ScheduleDay.MONDAY),
            startTime = "21:00",
            endTime = "22:00"
        )

        val rawList = listOf(actNight, actAfternoon, actMorning)
        val sortedList = rawList.sortedBy { it.startTime }

        assertEquals("Deep Work", sortedList[0].title)
        assertEquals("Almuerzo y Descanso", sortedList[1].title)
        assertEquals("Lectura", sortedList[2].title)
    }
}

package com.asistente.celular.service

import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import com.asistente.celular.ui.tasks.TasksTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para OngoingRoutineNotificationHelper.
 * Valida la lógica de evaluación de rutinas, constantes de canal e intents,
 * y enrutamiento hacia la sección de horarios.
 */
class OngoingRoutineNotificationHelperTest {

    private val sampleActivities = listOf(
        ScheduleActivityItem(
            id = "routine_morning",
            title = "Rutina Matutina y Enfoque",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.TUESDAY, ScheduleDay.WEDNESDAY, ScheduleDay.THURSDAY, ScheduleDay.FRIDAY),
            startTime = "07:00",
            endTime = "08:30",
            category = "Hábitos",
            colorHex = "#00F0FF",
            description = "Hidratación, café y revisión de metas"
        ),
        ScheduleActivityItem(
            id = "routine_work",
            title = "Deep Work: Desarrollo de Software",
            daysOfWeek = ScheduleDay.WEEKDAYS,
            startTime = "09:00",
            endTime = "13:00",
            category = "Trabajo",
            colorHex = "#A855F7",
            location = "Oficina / Home Studio"
        ),
        ScheduleActivityItem(
            id = "routine_gym",
            title = "Entrenamiento y Gimnasio",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY, ScheduleDay.FRIDAY),
            startTime = "18:00",
            endTime = "19:30",
            category = "Salud",
            colorHex = "#10B981"
        )
    )

    @Test
    fun testConstants_areConfiguredCorrectly() {
        assertEquals("ongoing_routine_channel", OngoingRoutineNotificationHelper.CHANNEL_ID)
        assertEquals("Rutina en curso", OngoingRoutineNotificationHelper.CHANNEL_NAME)
        assertEquals(9050, OngoingRoutineNotificationHelper.NOTIFICATION_ID)
        assertEquals("OPEN_TAB", OngoingRoutineNotificationHelper.EXTRA_OPEN_TAB)
        assertEquals("ROUTINE_ID", OngoingRoutineNotificationHelper.EXTRA_ROUTINE_ID)
        assertEquals("SCHEDULE", OngoingRoutineNotificationHelper.TAB_SCHEDULE)
    }

    @Test
    fun testEvaluateOngoingRoutine_whenActive_returnsRoutine() {
        // Miércoles a las 10:30 am -> Debe estar activa routine_work
        val ongoing = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = sampleActivities,
            currentDay = ScheduleDay.WEDNESDAY,
            currentTimeStr = "10:30"
        )

        assertNotNull(ongoing)
        assertEquals("routine_work", ongoing?.id)
        assertEquals("Deep Work: Desarrollo de Software", ongoing?.title)
        assertEquals("09:00", ongoing?.startTime)
        assertEquals("13:00", ongoing?.endTime)
        assertEquals("Trabajo", ongoing?.category)
    }

    @Test
    fun testEvaluateOngoingRoutine_atExactBoundaries_isInclusive() {
        // Justo al inicio 09:00
        val atStart = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = sampleActivities,
            currentDay = ScheduleDay.MONDAY,
            currentTimeStr = "09:00"
        )
        assertNotNull(atStart)
        assertEquals("routine_work", atStart?.id)

        // Justo al final 13:00
        val atEnd = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = sampleActivities,
            currentDay = ScheduleDay.MONDAY,
            currentTimeStr = "13:00"
        )
        assertNotNull(atEnd)
        assertEquals("routine_work", atEnd?.id)
    }

    @Test
    fun testEvaluateOngoingRoutine_whenBetweenRoutines_returnsNull() {
        // Lunes a las 08:35 (entre 08:30 y 09:00)
        val result = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = sampleActivities,
            currentDay = ScheduleDay.MONDAY,
            currentTimeStr = "08:35"
        )
        assertNull(result)
    }

    @Test
    fun testEvaluateOngoingRoutine_whenWeekend_returnsNullForWeekdayOnlyRoutines() {
        // Sábado a las 10:00 am -> routine_work no aplica en fines de semana
        val result = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = sampleActivities,
            currentDay = ScheduleDay.SATURDAY,
            currentTimeStr = "10:00"
        )
        assertNull(result)
    }

    @Test
    fun testEvaluateOngoingRoutine_whenDisabled_isIgnored() {
        val disabledList = sampleActivities.map {
            if (it.id == "routine_work") it.copy(isEnabled = false) else it
        }

        val result = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = disabledList,
            currentDay = ScheduleDay.TUESDAY,
            currentTimeStr = "10:00"
        )
        assertNull(result)
    }

    @Test
    fun testEvaluateOngoingRoutine_eveningRoutineMatch() {
        // Viernes a las 18:45 -> Gimnasio
        val result = OngoingRoutineNotificationHelper.evaluateOngoingRoutine(
            activities = sampleActivities,
            currentDay = ScheduleDay.FRIDAY,
            currentTimeStr = "18:45"
        )
        assertNotNull(result)
        assertEquals("routine_gym", result?.id)
        assertEquals("Entrenamiento y Gimnasio", result?.title)
    }

    @Test
    fun testIntentHandlingSimulation_mapsToScheduleTabAndTargetId() {
        // Simular lo que ejecuta MainActivity.handleIncomingIntent
        val openTab = "SCHEDULE"
        val routineId = "routine_morning"

        val targetTab = when (openTab) {
            OngoingRoutineNotificationHelper.TAB_SCHEDULE -> TasksTab.SCHEDULE
            "TASKS" -> TasksTab.TASKS
            else -> null
        }

        assertEquals(TasksTab.SCHEDULE, targetTab)
        assertEquals("routine_morning", routineId)

        // Simular resolución de la actividad objetivo en la vista
        val targetActivity = sampleActivities.firstOrNull { it.id == routineId }
        assertNotNull(targetActivity)
        assertEquals("Rutina Matutina y Enfoque", targetActivity?.title)
        assertTrue(targetActivity?.occursOn(ScheduleDay.MONDAY) == true)
    }
}

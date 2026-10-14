package com.asistente.celular.ui.components

import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OngoingRoutineStripTest {

    private val sampleRoutines = listOf(
        ScheduleActivityItem(
            id = "r1",
            title = "Rutina Matutina y Planificación",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.TUESDAY, ScheduleDay.WEDNESDAY),
            startTime = "07:00",
            endTime = "08:30",
            category = "Personal",
            colorHex = "#00F0FF"
        ),
        ScheduleActivityItem(
            id = "r2",
            title = "Bloque de Desarrollo / Deep Work",
            daysOfWeek = ScheduleDay.WEEKDAYS,
            startTime = "09:00",
            endTime = "13:00",
            category = "Trabajo",
            colorHex = "#A855F7"
        ),
        ScheduleActivityItem(
            id = "r3",
            title = "Gimnasio y Cardio",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY, ScheduleDay.FRIDAY),
            startTime = "18:00",
            endTime = "19:30",
            category = "Salud / Gimnasio",
            colorHex = "#10B981"
        )
    )

    @Test
    fun testFindOngoingRoutine_whenActive_returnsMatchingRoutine() {
        // Lunes a las 10:15 am -> Debe estar activa "Bloque de Desarrollo"
        val ongoing = findOngoingRoutine(sampleRoutines, ScheduleDay.MONDAY, "10:15")
        assertNotNull(ongoing)
        assertEquals("r2", ongoing?.id)
        assertEquals("Bloque de Desarrollo / Deep Work", ongoing?.title)
        assertEquals("13:00", ongoing?.endTime)
    }

    @Test
    fun testFindOngoingRoutine_atExactBoundaries_isInclusive() {
        // Exactamente al inicio 09:00
        val atStart = findOngoingRoutine(sampleRoutines, ScheduleDay.FRIDAY, "09:00")
        assertNotNull(atStart)
        assertEquals("r2", atStart?.id)

        // Exactamente al final 13:00
        val atEnd = findOngoingRoutine(sampleRoutines, ScheduleDay.FRIDAY, "13:00")
        assertNotNull(atEnd)
        assertEquals("r2", atEnd?.id)
    }

    @Test
    fun testFindOngoingRoutine_whenOutsideTimeRange_returnsNull() {
        // Lunes a las 08:31 am (entre rutina matutina que terminó 08:30 y bloque de trabajo que inicia 09:00)
        val ongoing = findOngoingRoutine(sampleRoutines, ScheduleDay.MONDAY, "08:31")
        assertNull(ongoing)
    }

    @Test
    fun testFindOngoingRoutine_whenWrongDay_returnsNull() {
        // Domingo a las 10:00 am -> Ninguna rutina de trabajo activa
        val ongoing = findOngoingRoutine(sampleRoutines, ScheduleDay.SUNDAY, "10:00")
        assertNull(ongoing)
    }

    @Test
    fun testFindOngoingRoutine_whenRoutineDisabled_isIgnored() {
        val disabledRoutines = sampleRoutines.map {
            if (it.id == "r2") it.copy(isEnabled = false) else it
        }

        // Lunes a las 10:00 am, pero r2 está desactivada
        val ongoing = findOngoingRoutine(disabledRoutines, ScheduleDay.MONDAY, "10:00")
        assertNull(ongoing)
    }

    @Test
    fun testClickFlow_whenRoutineIsActive_passesRoutineToCallback() {
        val ongoing = findOngoingRoutine(sampleRoutines, ScheduleDay.MONDAY, "10:00")
        assertNotNull(ongoing)

        var targetedRoutine: ScheduleActivityItem? = null
        val onRoutineClick: (ScheduleActivityItem) -> Unit = { routine ->
            targetedRoutine = routine
        }

        if (ongoing != null) {
            onRoutineClick(ongoing)
        }

        assertNotNull(targetedRoutine)
        assertEquals("r2", targetedRoutine?.id)
        assertEquals("Bloque de Desarrollo / Deep Work", targetedRoutine?.title)
    }

    @Test
    fun testClickFlow_fallbackWhenNoSpecificCallback() {
        var defaultClicked = false
        val onClick: () -> Unit = { defaultClicked = true }

        val ongoing = findOngoingRoutine(sampleRoutines, ScheduleDay.MONDAY, "10:00")
        val onRoutineClick: ((ScheduleActivityItem) -> Unit)? = null

        if (onRoutineClick != null && ongoing != null) {
            onRoutineClick(ongoing)
        } else {
            onClick()
        }

        assertTrue(defaultClicked)
    }
}

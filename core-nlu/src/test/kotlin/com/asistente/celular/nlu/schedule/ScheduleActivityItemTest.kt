package com.asistente.celular.nlu.schedule

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleActivityItemTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testScheduleDayMapping() {
        assertEquals(ScheduleDay.MONDAY, ScheduleDay.fromDayIndex(1))
        assertEquals(ScheduleDay.SUNDAY, ScheduleDay.fromDayIndex(7))
        assertEquals(null, ScheduleDay.fromDayIndex(0))
        assertEquals(null, ScheduleDay.fromDayIndex(8))

        assertEquals(ScheduleDay.MONDAY, ScheduleDay.fromCalendarDay(Calendar.MONDAY))
        assertEquals(ScheduleDay.FRIDAY, ScheduleDay.fromCalendarDay(Calendar.FRIDAY))
        assertEquals(ScheduleDay.SUNDAY, ScheduleDay.fromCalendarDay(Calendar.SUNDAY))

        assertEquals(5, ScheduleDay.WEEKDAYS.size)
        assertTrue(ScheduleDay.WEEKDAYS.contains(ScheduleDay.MONDAY))
        assertFalse(ScheduleDay.WEEKDAYS.contains(ScheduleDay.SATURDAY))

        assertEquals(2, ScheduleDay.WEEKEND.size)
        assertTrue(ScheduleDay.WEEKEND.contains(ScheduleDay.SATURDAY))
        assertTrue(ScheduleDay.WEEKEND.contains(ScheduleDay.SUNDAY))
    }

    @Test
    fun testScheduleActivityItemDefaultsAndOccursOn() {
        val item = ScheduleActivityItem(
            title = "Desarrollo de Software",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY, ScheduleDay.FRIDAY),
            startTime = "09:00",
            endTime = "11:30",
            category = "Trabajo",
            colorHex = "#00F0FF"
        )

        assertTrue(item.occursOn(ScheduleDay.MONDAY))
        assertTrue(item.occursOn(ScheduleDay.WEDNESDAY))
        assertTrue(item.occursOn(ScheduleDay.FRIDAY))
        assertFalse(item.occursOn(ScheduleDay.TUESDAY))
        assertFalse(item.occursOn(ScheduleDay.SUNDAY))

        assertEquals(150, item.durationMinutes()) // 2h 30m = 150m
    }

    @Test
    fun testIsOngoingAt() {
        val item = ScheduleActivityItem(
            title = "Gimnasio",
            daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.TUESDAY),
            startTime = "07:00",
            endTime = "08:30"
        )

        // En rango
        assertTrue(item.isOngoingAt(ScheduleDay.MONDAY, "07:00"))
        assertTrue(item.isOngoingAt(ScheduleDay.MONDAY, "07:45"))
        assertTrue(item.isOngoingAt(ScheduleDay.MONDAY, "08:30"))

        // Fuera de hora
        assertFalse(item.isOngoingAt(ScheduleDay.MONDAY, "06:59"))
        assertFalse(item.isOngoingAt(ScheduleDay.MONDAY, "08:31"))

        // Día incorrecto
        assertFalse(item.isOngoingAt(ScheduleDay.WEDNESDAY, "07:30"))

        // Deshabilitado
        val disabledItem = item.copy(isEnabled = false)
        assertFalse(disabledItem.isOngoingAt(ScheduleDay.MONDAY, "07:30"))
    }

    @Test
    fun testJsonSerializationRoundTrip() {
        val original = ScheduleActivityItem(
            id = "test-uuid-1234",
            title = "Sesión de Grabación Ableton",
            description = "Producción de beats y mezcla vocal",
            daysOfWeek = setOf(ScheduleDay.TUESDAY, ScheduleDay.THURSDAY, ScheduleDay.SATURDAY),
            startTime = "16:00",
            endTime = "19:00",
            category = "Música / Producción",
            colorHex = "#A855F7",
            location = "Home Studio",
            isReminderEnabled = true,
            reminderOffsetMinutes = 15,
            isEnabled = true,
            metadata = mapOf("daw" to "Ableton Live 12")
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<ScheduleActivityItem>(serialized)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.title, deserialized.title)
        assertEquals(original.description, deserialized.description)
        assertEquals(original.daysOfWeek, deserialized.daysOfWeek)
        assertEquals(original.startTime, deserialized.startTime)
        assertEquals(original.endTime, deserialized.endTime)
        assertEquals(original.category, deserialized.category)
        assertEquals(original.colorHex, deserialized.colorHex)
        assertEquals(original.location, deserialized.location)
        assertEquals(original.isReminderEnabled, deserialized.isReminderEnabled)
        assertEquals(original.reminderOffsetMinutes, deserialized.reminderOffsetMinutes)
        assertEquals(original.isEnabled, deserialized.isEnabled)
        assertEquals("Ableton Live 12", deserialized.metadata["daw"])
    }
}

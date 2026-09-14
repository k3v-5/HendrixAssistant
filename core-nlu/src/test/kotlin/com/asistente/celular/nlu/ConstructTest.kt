package com.asistente.celular.nlu

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OrConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.parser.SpanishDateTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ConstructTest {

    @Test
    fun testWordConstructMatches() {
        val word = WordConstruct("enciende", "prende")
        val ctx = MatchContext("Prende la luz")
        assertTrue(word.match(ctx))
        assertEquals(1, ctx.tokenIndex)
    }

    @Test
    fun testSequenceWithCapturingSlot() {
        // "abre [appName]"
        val pattern = SequenceConstruct(
            WordConstruct("abre", "inicia", "lanzar"),
            CapturingConstruct("appName")
        )

        val ctx = MatchContext("Abre WhatsApp")
        val matches = pattern.match(ctx)
        assertTrue(matches)
        assertEquals("whatsapp", ctx.capturedSlots["appName"])
    }

    @Test
    fun testOrConstructOptions() {
        val pattern = SequenceConstruct(
            WordConstruct("linterna"),
            OrConstruct(
                WordConstruct("on", "encendida", "prender"),
                WordConstruct("off", "apagada", "apagar")
            )
        )

        val ctx1 = MatchContext("linterna encendida")
        assertTrue(pattern.match(ctx1))

        val ctx2 = MatchContext("linterna apagar")
        assertTrue(pattern.match(ctx2))
    }

    @Test
    fun testSpanishTimerDurationParser() {
        assertEquals(300L, SpanishDateTimeParser.parseDurationSeconds("5 minutos"))
        assertEquals(1800L, SpanishDateTimeParser.parseDurationSeconds("media hora"))
        assertEquals(90L, SpanishDateTimeParser.parseDurationSeconds("1 minuto y 30 segundos"))
    }

    @Test
    fun testSpanishAlarmTimeParser() {
        val time1 = SpanishDateTimeParser.parseTime("a las 7 de la manana")
        assertNotNull(time1)
        assertEquals(LocalTime.of(7, 0), time1)

        val time2 = SpanishDateTimeParser.parseTime("a las 8 y media de la noche")
        assertNotNull(time2)
        assertEquals(LocalTime.of(20, 30), time2)
    }

    @Test
    fun testSpanishDateAndDateTimeParser() {
        val baseDate = java.time.LocalDate.of(2026, 9, 14) // Monday
        val baseDateTime = java.time.LocalDateTime.of(baseDate, java.time.LocalTime.of(10, 0))

        val d1 = SpanishDateTimeParser.parseDate("mañana", baseDate)
        assertEquals(java.time.LocalDate.of(2026, 9, 15), d1)

        val d2 = SpanishDateTimeParser.parseDate("el viernes", baseDate)
        assertEquals(java.time.LocalDate.of(2026, 9, 18), d2)

        val dt = SpanishDateTimeParser.parseDateTime("mañana a las 5 pm", baseDateTime)
        assertNotNull(dt)
        assertEquals(java.time.LocalDateTime.of(2026, 9, 15, 17, 0), dt)

        val cleaned = SpanishDateTimeParser.stripTemporalExpressions("comprar leche mañana a las 5 pm")
        assertEquals("comprar leche", cleaned)
    }
}

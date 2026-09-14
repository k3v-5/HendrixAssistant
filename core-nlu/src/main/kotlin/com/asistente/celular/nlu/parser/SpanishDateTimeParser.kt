package com.asistente.celular.nlu.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Parser ligero en Kotlin para extraer números escritos en español ("cinco", "veinte", "15").
 */
object SpanishNumberParser {
    private val WORDS_TO_NUMBERS = mapOf(
        "cero" to 0, "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3,
        "cuatro" to 4, "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8,
        "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
        "catorce" to 14, "quince" to 15, "dieciseis" to 16, "diecisiete" to 17,
        "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21,
        "veintidos" to 22, "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25,
        "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60,
        "setenta" to 70, "ochenta" to 80, "noventa" to 90, "cien" to 100
    )

    fun parseNumber(token: String): Int? {
        val clean = token.trim().lowercase()
        clean.toIntOrNull()?.let { return it }
        return WORDS_TO_NUMBERS[clean]
    }
}

/**
 * Parser ligero para extraer duración de temporizadores, fechas y horas en español.
 */
object SpanishDateTimeParser {

    private val MONTHS = mapOf(
        "enero" to Month.JANUARY, "febrero" to Month.FEBRUARY, "marzo" to Month.MARCH,
        "abril" to Month.APRIL, "mayo" to Month.MAY, "junio" to Month.JUNE,
        "julio" to Month.JULY, "agosto" to Month.AUGUST, "septiembre" to Month.SEPTEMBER,
        "setiembre" to Month.SEPTEMBER, "octubre" to Month.OCTOBER,
        "noviembre" to Month.NOVEMBER, "diciembre" to Month.DECEMBER
    )

    private val DAYS_OF_WEEK = mapOf(
        "lunes" to DayOfWeek.MONDAY,
        "martes" to DayOfWeek.TUESDAY,
        "miercoles" to DayOfWeek.WEDNESDAY,
        "jueves" to DayOfWeek.THURSDAY,
        "viernes" to DayOfWeek.FRIDAY,
        "sabado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY
    )

    /**
     * Normaliza tildes a vocales simples.
     */
    fun normalize(text: String): String {
        return text.lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
    }

    /**
     * Extrae duración en segundos a partir de frases como:
     * - "5 minutos"
     * - "diez segundos"
     * - "media hora"
     * - "1 hora y media"
     */
    fun parseDurationSeconds(text: String): Long? {
        val lower = normalize(text)
        var totalSeconds = 0L
        var foundAny = false

        if (lower.contains("media hora")) {
            totalSeconds += 1800
            foundAny = true
        } else if (lower.contains("un cuarto de hora") || lower.contains("cuarto de hora")) {
            totalSeconds += 900
            foundAny = true
        }

        val regex = "(\\d+|uno|una|un|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta|cuarenta|cincuenta)\\s*(horas?|hrs?|minutos?|mins?|segundos?|segs?)".toRegex()
        val matches = regex.findAll(lower)

        for (match in matches) {
            val numStr = match.groupValues[1]
            val unitStr = match.groupValues[2]
            val num = SpanishNumberParser.parseNumber(numStr) ?: continue
            foundAny = true

            when {
                unitStr.startsWith("hora") || unitStr.startsWith("hr") -> totalSeconds += num * 3600
                unitStr.startsWith("min") -> totalSeconds += num * 60
                unitStr.startsWith("seg") -> totalSeconds += num
            }
        }

        return if (foundAny && totalSeconds > 0) totalSeconds else null
    }

    /**
     * Extrae hora (LocalTime) para alarmas y tareas:
     * - "7 de la mañana" -> 07:00
     * - "8 y media de la noche" -> 20:30
     * - "15:45" -> 15:45
     * - "cuatro y cuarto de la tarde" -> 16:15
     */
    fun parseTime(text: String): LocalTime? {
        val lower = normalize(text)

        // Formato digital HH:mm
        val digitalRegex = "(\\d{1,2})[:.](\\d{2})".toRegex()
        digitalRegex.find(lower)?.let { match ->
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) return LocalTime.of(h, m)
        }

        // Extraer componentes verbales
        val isPm = lower.contains("tarde") || lower.contains("noche") || lower.contains("pm")
        val isAm = lower.contains("manana") || lower.contains("madrugada") || lower.contains("am")

        val hourRegex = "(?:a las?|para las?)?\\s*(\\d+|un[oa]?|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce)".toRegex()
        val hourMatch = hourRegex.find(lower) ?: return null
        var hour = SpanishNumberParser.parseNumber(hourMatch.groupValues[1]) ?: return null

        var minutes = 0
        if (lower.contains("media")) {
            minutes = 30
        } else if (lower.contains("cuarto")) {
            minutes = 15
        } else {
            val minRegex = "(?:y|con)\\s*(\\d+|cinco|diez|quince|veinte|veinticinco|treinta|cuarenta|cincuenta)".toRegex()
            minRegex.find(lower)?.let { mMatch ->
                SpanishNumberParser.parseNumber(mMatch.groupValues[1])?.let { minutes = it }
            }
        }

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        return if (hour in 0..23 && minutes in 0..59) LocalTime.of(hour, minutes) else null
    }

    /**
     * Extrae fecha (LocalDate) en español:
     * - "hoy" -> baseDate
     * - "mañana" -> baseDate.plusDays(1)
     * - "pasado mañana" -> baseDate.plusDays(2)
     * - "el viernes" -> próximo viernes
     * - "el 15 de marzo" -> 15 de marzo
     */
    fun parseDate(text: String, baseDate: LocalDate = LocalDate.now()): LocalDate? {
        val lower = normalize(text)

        if (lower.contains("pasado manana")) {
            return baseDate.plusDays(2)
        }
        if (lower.contains("manana")) {
            return baseDate.plusDays(1)
        }
        if (lower.contains("hoy")) {
            return baseDate
        }

        // Días de la semana ("el viernes", "este sabado")
        for ((name, dayOfWeek) in DAYS_OF_WEEK) {
            if (lower.contains(name)) {
                return if (baseDate.dayOfWeek == dayOfWeek) {
                    baseDate.plusWeeks(1)
                } else {
                    baseDate.with(TemporalAdjusters.next(dayOfWeek))
                }
            }
        }

        // "el 15 de mayo", "20 de octubre"
        val dayMonthRegex = "(?:el\\s*)?(\\d{1,2})\\s+de\\s+([a-z]+)".toRegex()
        dayMonthRegex.find(lower)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()
            val monthName = match.groupValues[2]
            val month = MONTHS[monthName]
            if (day != null && month != null && day in 1..month.length(baseDate.isLeapYear)) {
                var year = baseDate.year
                var target = LocalDate.of(year, month, day)
                if (target.isBefore(baseDate)) {
                    target = target.plusYears(1)
                }
                return target
            }
        }

        return null
    }

    /**
     * Extrae fecha y hora combinadas (LocalDateTime):
     * - "mañana a las 5 pm" -> mañana 17:00
     * - "hoy a las 8 de la noche" -> hoy 20:00
     * - "el viernes a las 10" -> próximo viernes 10:00
     */
    fun parseDateTime(text: String, baseDateTime: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val date = parseDate(text, baseDateTime.toLocalDate())
        val time = parseTime(text)

        if (date != null && time != null) {
            return LocalDateTime.of(date, time)
        }

        if (date != null) {
            // Si solo se especificó fecha, por defecto a las 9:00 AM
            return LocalDateTime.of(date, LocalTime.of(9, 0))
        }

        if (time != null) {
            // Si solo se especificó hora: si ya pasó hoy, programar para mañana
            val todayTarget = LocalDateTime.of(baseDateTime.toLocalDate(), time)
            return if (todayTarget.isAfter(baseDateTime)) {
                todayTarget
            } else {
                todayTarget.plusDays(1)
            }
        }

        return null
    }

    /**
     * Limpia expresiones de tiempo comunes del texto para dejar solo la descripción/título de la tarea.
     */
    fun stripTemporalExpressions(text: String): String {
        var clean = text
        val temporalPatterns = listOf(
            "(?i)\\b(para|el|este)?\\s*(hoy|pasado\\s+mañana|mañana|pasado\\s+manana|manana)\\b",
            "(?i)\\b(para|el|este)\\s+(lunes|martes|mi[eé]rcoles|jueves|viernes|s[aá]bado|domingo)\\b",
            "(?i)\\b(a\\s+las?|para\\s+las?)\\s*\\d{1,2}(?::\\d{2})?\\s*(?:de\\s+la\\s+(?:mañana|manana|tarde|noche)|pm|am)?\\b",
            "(?i)\\b(?:de\\s+la\\s+(?:mañana|manana|tarde|noche)|pm|am)\\b",
            "(?i)\\b(el\\s+)?\\d{1,2}\\s+de\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)\\b"
        )

        for (pattern in temporalPatterns) {
            clean = clean.replace(pattern.toRegex(), " ")
        }
        return clean.replace("\\s+".toRegex(), " ").trim()
    }
}

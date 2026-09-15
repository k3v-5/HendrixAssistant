package com.asistente.celular.skills.calendar

import android.content.Intent
import android.provider.CalendarContract
import com.asistente.celular.nlu.calendar.CalendarEventItem
import com.asistente.celular.nlu.calendar.CalendarRepository
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.parser.SpanishDateTimeParser
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Habilidad para consultar la agenda y programar reuniones o eventos en el calendario.
 */
class CalendarSkill(
    private val calendarRepository: CalendarRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "calendar_skill",
        name = "Calendario y Agenda",
        description = "Consulta y agenda eventos, citas o reuniones en tu calendario."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES"))
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))

    private val queryPatterns: List<Construct> = listOf(
        // "¿Qué tengo en el calendario hoy?" / "¿Qué eventos tengo?"
        SequenceConstruct(
            WordConstruct("que"),
            OptionalConstruct(WordConstruct("tengo", "hay")),
            OptionalConstruct(WordConstruct("en", "de")),
            OptionalConstruct(WordConstruct("el", "mi")),
            WordConstruct("calendario", "agenda", "reuniones", "eventos"),
            OptionalConstruct(WordConstruct("hoy", "manana", "para hoy", "para manana"))
        ),
        // "Mis eventos de hoy" / "Mis reuniones"
        SequenceConstruct(
            WordConstruct("mis"),
            WordConstruct("eventos", "reuniones", "citas", "compromisos"),
            OptionalConstruct(WordConstruct("de", "para")),
            OptionalConstruct(WordConstruct("hoy", "manana"))
        ),
        // "Próxima reunión" / "Próximo evento"
        SequenceConstruct(
            WordConstruct("proxima", "proximo", "siguiente"),
            WordConstruct("reunion", "evento", "cita")
        ),
        // "Calendario" / "Mi agenda"
        WordConstruct("calendario", "agenda")
    )

    private val createPatterns: List<Construct> = listOf(
        // "Agrega un evento {title}" / "Crea una reunión {title}" / "Agenda una cita {title}"
        SequenceConstruct(
            WordConstruct("agrega", "agregar", "crea", "crear", "agenda", "agendar", "programa", "programar", "pon"),
            OptionalConstruct(WordConstruct("un", "una")),
            WordConstruct("evento", "reunion", "cita", "compromiso"),
            CapturingConstruct("title")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        // 1. Patrones de creación de eventos
        for (pattern in createPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx) && ctx.isAtEnd) {
                val slots = ctx.capturedSlots.toMutableMap()
                slots["action"] = "create"
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = slots
                )
            }
        }

        // 2. Patrones de consulta
        for (pattern in queryPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                return SkillScore(
                    confidence = 0.90f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = mapOf("action" to "query")
                )
            }
        }

        // Detección directa por palabras clave
        if (normalized.contains("calendario") || normalized.contains("reuniones") || normalized.contains("agenda")) {
            return SkillScore(
                confidence = 0.85f,
                specificity = Specificity.NORMAL,
                capturedSlots = mapOf("action" to "query")
            )
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val action = score.capturedSlots["action"] ?: "query"

        if (!calendarRepository.hasCalendarPermission()) {
            return handleMissingPermission(context)
        }

        return if (action == "create") {
            handleCreateEvent(context, input, score.capturedSlots["title"] ?: "")
        } else {
            handleQueryEvents(input)
        }
    }

    private suspend fun handleQueryEvents(input: String): SkillOutput {
        val lower = input.lowercase()
        val isTomorrow = lower.contains("mañana") || lower.contains("manana")
        val isNextOnly = lower.contains("proxima") || lower.contains("siguiente")

        val targetDate = if (isTomorrow) LocalDate.now(zoneId).plusDays(1) else LocalDate.now(zoneId)
        val dayStart = targetDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = targetDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

        val events = if (isNextOnly) {
            calendarRepository.getUpcomingEvents(limit = 1)
        } else {
            calendarRepository.getEvents(dayStart, dayEnd)
        }

        if (events.isEmpty()) {
            val dayName = if (isTomorrow) "mañana" else "hoy"
            val msg = "No tienes eventos programados para $dayName."
            return SkillOutput(speech = msg, displayText = msg, success = true)
        }

        val speech = buildString {
            if (isNextOnly) {
                val ev = events.first()
                val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(ev.startMillis), zoneId)
                append("Tu próxima reunión es '${ev.title}' a las ${start.format(timeFormatter)}.")
            } else {
                val dayName = if (isTomorrow) "mañana" else "hoy"
                append("Tienes ${events.size} evento${if (events.size > 1) "s" else ""} para $dayName: ")
                val descriptions = events.map { ev ->
                    val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(ev.startMillis), zoneId)
                    "${ev.title} a las ${start.format(timeFormatter)}"
                }
                append(descriptions.joinToString(", "))
                append(".")
            }
        }

        val display = buildString {
            appendLine("📅 **Eventos en Calendario (${events.size}):**")
            for (ev in events) {
                val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(ev.startMillis), zoneId)
                val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(ev.endMillis), zoneId)
                val timeStr = if (ev.isAllDay) "Todo el día" else "${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
                val loc = if (ev.location.isNotBlank()) " 📍 ${ev.location}" else ""
                appendLine("• **${ev.title}** ($timeStr)$loc")
            }
        }

        return SkillOutput(speech = speech, displayText = display.trim(), success = true)
    }

    private suspend fun handleCreateEvent(context: SkillContext, fullInput: String, titleSlot: String): SkillOutput {
        val parsedDate = SpanishDateTimeParser.parseDate(fullInput) ?: LocalDate.now(zoneId)
        val parsedTime = SpanishDateTimeParser.parseTime(fullInput) ?: LocalTime.of(10, 0)
        val cleanTitle = SpanishDateTimeParser.stripTemporalExpressions(titleSlot).trim()
            .ifBlank { "Nuevo Evento" }
            .replaceFirstChar { it.uppercase() }

        val startDateTime = LocalDateTime.of(parsedDate, parsedTime)
        val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val eventId = calendarRepository.createEvent(
            title = cleanTitle,
            startMillis = startMillis,
            durationMinutes = 60
        )

        return if (eventId != null) {
            val speech = "Agendé '$cleanTitle' para el ${parsedDate.format(dateFormatter)} a las ${parsedTime.format(timeFormatter)}."
            val display = "📅 **Evento Agendado:**\n• **$cleanTitle**\n• Fecha: ${parsedDate.format(dateFormatter)}\n• Hora: ${parsedTime.format(timeFormatter)}"
            SkillOutput(speech = speech, displayText = display, success = true)
        } else {
            openSystemCalendarInsert(context, cleanTitle, startMillis)
            val msg = "Abrí el calendario para que confirmes y guardes el evento '$cleanTitle'."
            SkillOutput(speech = msg, displayText = msg, success = true)
        }
    }

    private fun handleMissingPermission(context: SkillContext): SkillOutput {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)
        } catch (_: Exception) {}

        val msg = "Abrí tu calendario. Recuerda otorgar el permiso de calendario en los Ajustes si deseas que lo consulte directamente."
        return SkillOutput(speech = msg, displayText = msg, success = true)
    }

    private fun openSystemCalendarInsert(context: SkillContext, title: String, startMillis: Long) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)
        } catch (_: Exception) {}
    }
}

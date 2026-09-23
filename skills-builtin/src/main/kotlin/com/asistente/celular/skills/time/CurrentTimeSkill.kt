package com.asistente.celular.skills.time

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.OrConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Habilidad offline para responder la hora y fecha actual con alta precisión gramatical.
 */
class CurrentTimeSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "current_time_skill",
        name = "Hora y Fecha",
        description = "Informa la hora y la fecha actual."
    ),
    specificity = Specificity.HIGH
) {
    override val patterns: List<Construct> = listOf(
        // 1. "qué hora es", "dime la hora", "hora actual", "qué hora tienes"
        SequenceConstruct(
            OptionalConstruct(
                OrConstruct(
                    WordConstruct("dime", "decime", "dame"),
                    SequenceConstruct(WordConstruct("me", "te"), WordConstruct("dices", "puedes")),
                    WordConstruct("que", "cual")
                )
            ),
            OptionalConstruct(WordConstruct("la", "el", "es")),
            WordConstruct("hora"),
            OptionalConstruct(WordConstruct("es", "tienes", "actual", "por favor"))
        ),
        // 2. "qué día es hoy", "qué fecha es", "cuál es la fecha de hoy", "en qué día estamos"
        SequenceConstruct(
            OptionalConstruct(
                OrConstruct(
                    WordConstruct("que", "cual", "en", "a"),
                    WordConstruct("dime", "decime"),
                    SequenceConstruct(WordConstruct("me", "te"), WordConstruct("dices", "puedes"))
                )
            ),
            OptionalConstruct(WordConstruct("es", "de", "el", "la", "que")),
            WordConstruct("dia", "fecha"),
            OptionalConstruct(WordConstruct("es", "de", "en", "hoy", "actual", "estamos")),
            OptionalConstruct(WordConstruct("hoy", "actual"))
        ),
        // 3. "hoy qué día es", "hoy qué fecha es"
        SequenceConstruct(
            WordConstruct("hoy"),
            OptionalConstruct(WordConstruct("que", "cual")),
            WordConstruct("dia", "fecha"),
            OptionalConstruct(WordConstruct("es", "tenemos"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        val isTimeQuery = normalized.contains("hora") &&
                (normalized.contains("que") || normalized.contains("dime") || normalized.contains("actual") || normalized.contains("la hora"))

        val isDateQuery = (normalized.contains("dia") || normalized.contains("fecha")) &&
                (normalized.contains("hoy") || normalized.contains("que") || normalized.contains("cual") ||
                 normalized.contains("dime") || normalized.contains("actual") || normalized.contains("estamos") || normalized.contains("fecha es"))

        if (isTimeQuery || isDateQuery) {
            val superScore = super.score(context, input)
            if (superScore.confidence >= 0.65f) {
                return superScore.copy(confidence = 0.98f, specificity = Specificity.HIGH)
            }
            val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                matchedWords = tokens.size,
                totalWords = tokens.size
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val normalized = MatchContext.normalize(input)
        val isDateQuery = normalized.contains("dia") || normalized.contains("fecha") || normalized.contains("hoy")

        return if (isDateQuery) {
            val now = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
            val formattedDate = now.format(formatter).replaceFirstChar { it.uppercase() }
            val msg = "Hoy es $formattedDate."
            SkillOutput(speech = msg, displayText = "📅 $msg", success = true)
        } else {
            val now = LocalTime.now()
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = now.format(formatter)
            val msg = "Son las $formattedTime."
            SkillOutput(speech = msg, displayText = "⏰ $msg", success = true)
        }
    }
}

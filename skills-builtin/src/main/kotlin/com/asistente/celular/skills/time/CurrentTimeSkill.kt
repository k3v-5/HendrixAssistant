package com.asistente.celular.skills.time

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
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
 * Habilidad offline para responder la hora y fecha actual.
 */
class CurrentTimeSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "current_time_skill",
        name = "Hora y Fecha",
        description = "Informa la hora y la fecha actual."
    ),
    specificity = Specificity.NORMAL
) {
    override val patterns: List<Construct> = listOf(
        // "qué hora es", "dime la hora"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("dime", "que")),
            WordConstruct("hora"),
            OptionalConstruct(WordConstruct("es", "tienes"))
        ),
        // "qué día es hoy", "fecha de hoy"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("que", "cual")),
            WordConstruct("dia", "fecha"),
            OptionalConstruct(WordConstruct("es", "de")),
            OptionalConstruct(WordConstruct("hoy"))
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val normalized = com.asistente.celular.nlu.construct.MatchContext.normalize(input)
        val isDateQuery = normalized.contains("dia") || normalized.contains("fecha") || normalized.contains("hoy")

        return if (isDateQuery) {
            val now = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
            val formattedDate = now.format(formatter).replaceFirstChar { it.uppercase() }
            val msg = "Hoy es $formattedDate."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } else {
            val now = LocalTime.now()
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = now.format(formatter)
            val msg = "Son las $formattedTime."
            SkillOutput(speech = msg, displayText = msg, success = true)
        }
    }
}

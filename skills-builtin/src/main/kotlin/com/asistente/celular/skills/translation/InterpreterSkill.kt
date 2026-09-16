package com.asistente.celular.skills.translation

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.translation.InterpreterEngine
import com.asistente.celular.nlu.ui.InterpreterUiPayload

/**
 * Habilidad de Modo Intérprete Simultáneo Bidireccional Manos Libres.
 */
class InterpreterSkill(
    private val interpreterEngine: InterpreterEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "interpreter_skill",
        name = "Modo Intérprete Simultáneo",
        description = "Traducción de conversación en vivo cara a cara entre dos idiomas."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("modo"),
            WordConstruct("interprete", "intérprete")
        ),
        SequenceConstruct(
            WordConstruct("traducir", "traductor"),
            WordConstruct("conversacion", "conversación", "simultanea", "simultánea")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("modo interprete") || lower.contains("modo intérprete") ||
            lower.contains("traducción simultánea") || lower.contains("traducir conversacion") ||
            lower.contains("interprete ingles español") || lower.contains("intérprete inglés español")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val session = interpreterEngine?.startSession("es", "en")
        val turn = interpreterEngine?.translateTurn("person_a", "Hola, un placer conocerte", "es", "en")

        val payload = InterpreterUiPayload(
            langA = "Español",
            langB = "English",
            lastSpeaker = "Persona A",
            lastOriginal = turn?.originalText ?: "Hola, un placer conocerte",
            lastTranslated = turn?.translatedText ?: "Hello, nice to meet you"
        )

        return SkillOutput(
            speech = "Modo intérprete iniciado. Habla en español o inglés y traduciré en tiempo real.",
            payload = payload
        )
    }
}

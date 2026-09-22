package com.asistente.celular.skills.pc

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

/**
 * Habilidad NLU para activar el Modo "Desk Standby" (Smart Display de Escritorio).
 */
class DeskStandbySkill(
    private val onActivateStandby: (() -> Unit)? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "desk_standby_skill",
        name = "Modo Escritorio (Desk Standby)",
        description = "Transforma la pantalla en un HUD inteligente con reloj futurista, telemetría de PC y controles rápidos."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("activa", "activar", "pon", "poner", "inicia", "iniciar", "abre", "abrir")),
            OptionalConstruct(WordConstruct("el", "la", "en")),
            WordConstruct("modo", "pantalla"),
            WordConstruct("escritorio", "standby", "desk", "dock", "smart display")
        ),
        SequenceConstruct(
            WordConstruct("smart display", "desk standby", "modo dock")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("modo escritorio") || lower.contains("desk standby") ||
            lower.contains("pantalla de escritorio") || lower.contains("smart display") ||
            lower.contains("modo dock")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    suspend fun execute(context: SkillContext, input: String): SkillOutput =
        execute(context, input, SkillScore.PERFECT_MATCH)

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        onActivateStandby?.invoke()
        return SkillOutput(
            speech = "Activando el modo de pantalla inteligente de escritorio.",
            displayText = "🖥️ Modo Desk Standby Activado",
            payload = "ACTION_DESK_STANDBY"
        )
    }
}

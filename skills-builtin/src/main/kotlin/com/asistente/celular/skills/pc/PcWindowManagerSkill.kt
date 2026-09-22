package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad NLU para controlar la disposición de ventanas en Windows (minimizar todo, maximizar, mover de monitor).
 * Ejemplo: "minimiza todo en la PC", "pantalla completa en la PC", "pasa la ventana a la otra pantalla".
 */
class PcWindowManagerSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_window_manager_skill",
        name = "Gestor de Ventanas de PC",
        description = "Minimiza, maximiza y reubica ventanas en Windows de forma remota."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("minimiza", "maximiza", "pantalla", "mueve", "pasa"),
            OptionalConstruct(WordConstruct("todo", "todas", "las", "ventanas", "completa", "la", "ventana")),
            OptionalConstruct(WordConstruct("en", "a")),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("pc", "computadora", "monitor", "pantalla"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if ((lower.contains("minimiza todo") || lower.contains("mostrar escritorio") || lower.contains("minimizar todo") ||
             lower.contains("maximiza") || lower.contains("maximizar") || lower.contains("pantalla completa") ||
             lower.contains("mueve la ventana") || lower.contains("pasa la ventana") || lower.contains("al otro monitor") || lower.contains("a la otra pantalla")) &&
            (lower.contains("en la pc") || lower.contains("de la pc") || lower.contains("en mi pc") || lower.contains("pc"))
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    suspend fun execute(context: SkillContext, input: String): SkillOutput =
        execute(context, input, SkillScore.PERFECT_MATCH)

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge ?: return SkillOutput(
            speech = "No hay conexión con la PC para gestionar ventanas.",
            displayText = "Sin conexión con la PC"
        )

        val lower = MatchContext.normalize(input)
        val action = when {
            lower.contains("minimiza") || lower.contains("minimizar") || lower.contains("escritorio") -> "MINIMIZE_ALL"
            lower.contains("maximiza") || lower.contains("maximizar") || lower.contains("pantalla completa") -> "TOGGLE_MAXIMIZE"
            lower.contains("otro monitor") || lower.contains("otra pantalla") || lower.contains("mueve") || lower.contains("pasa") -> "MOVE_NEXT_MONITOR"
            lower.contains("izquierda") -> "SNAP_LEFT"
            lower.contains("derecha") -> "SNAP_RIGHT"
            else -> "MINIMIZE_ALL"
        }

        val success = bridge.executeWindowCommand(action)
        return if (success) {
            val desc = when (action) {
                "MINIMIZE_ALL" -> "Se han minimizado todas las ventanas en la PC."
                "TOGGLE_MAXIMIZE" -> "Se ha maximizado la ventana activa en la PC."
                "MOVE_NEXT_MONITOR" -> "Se ha movido la ventana al siguiente monitor en la PC."
                "SNAP_LEFT" -> "Ventana acoplada a la izquierda en la PC."
                "SNAP_RIGHT" -> "Ventana acoplada a la derecha en la PC."
                else -> "Comando de ventana ejecutado en la PC."
            }
            SkillOutput(speech = desc, displayText = desc)
        } else {
            SkillOutput(
                speech = "No se pudo ejecutar la acción de ventana en la PC.",
                displayText = "Error al gestionar ventana en la PC"
            )
        }
    }
}

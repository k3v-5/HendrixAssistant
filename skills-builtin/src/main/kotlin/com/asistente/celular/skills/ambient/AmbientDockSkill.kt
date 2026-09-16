package com.asistente.celular.skills.ambient

import com.asistente.celular.nlu.ambient.DockModeController
import com.asistente.celular.nlu.ambient.DockType
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
import com.asistente.celular.nlu.ui.AmbientDockUiPayload

/**
 * Habilidad de Modo Ambient Dock y Pantalla Inteligente para Estación de Noche.
 */
class AmbientDockSkill(
    private val dockController: DockModeController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "ambient_dock_skill",
        name = "Modo Ambient Dock",
        description = "Convierte el teléfono en una pantalla inteligente ambiente de reposo para la mesita de noche."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("modo"),
            WordConstruct("dock", "ambiente", "mesita", "estacion", "estación"),
            OptionalConstruct(WordConstruct("de", "noche"))
        ),
        SequenceConstruct(
            WordConstruct("pantalla"),
            WordConstruct("ambiente", "de", "inteligente"),
            OptionalConstruct(WordConstruct("reposo", "noche"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("modo dock") || lower.contains("pantalla ambiente") ||
            lower.contains("estacion de noche") || lower.contains("estación de noche") ||
            lower.contains("modo mesita")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        dockController?.notifyDockStateChanged(true, DockType.NIGHTSTAND)
        dockController?.setNightTheme(true)
        val state = dockController?.getCurrentDockState()

        val payload = AmbientDockUiPayload(
            isDocked = true,
            dockType = "Estación de Noche",
            isNightMode = state?.isNightModeActive ?: true,
            ambientMessage = "Hendrix en modo ambiente: pantalla de bajo brillo y escucha lejana activa."
        )

        return SkillOutput(
            speech = "Modo Ambient Dock activado. Visualización OLED minimalista y controles nocturnos listos.",
            payload = payload
        )
    }
}

package com.asistente.celular.skills.gesture

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
import com.asistente.celular.nlu.ui.HardwareGestureUiPayload
import com.asistente.celular.voice.gesture.HardwareGestureDetector

/**
 * Habilidad de Gestos Físicos de Hardware (Quick Tap & Flip to Shush).
 */
class HardwareGestureSkill(
    private val gestureDetector: HardwareGestureDetector? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "hardware_gesture_skill",
        name = "Gestos Físicos de Hardware",
        description = "Configura y activa gestos de acelerómetro como doble toque trasero o voltear para silenciar."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("gestos", "gesto"),
            WordConstruct("fisicos", "físicos", "de"),
            OptionalConstruct(WordConstruct("hardware"))
        ),
        SequenceConstruct(
            WordConstruct("toque", "doble"),
            WordConstruct("trasero", "tap")
        ),
        SequenceConstruct(
            WordConstruct("voltear", "girar"),
            WordConstruct("para"),
            WordConstruct("silenciar", "callar")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("gestos fisicos") || lower.contains("gestos físicos") ||
            lower.contains("toque trasero") || lower.contains("quick tap") ||
            lower.contains("voltear para silenciar") || lower.contains("flip to shush")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val payload = HardwareGestureUiPayload(
            lastDetectedGesture = "Doble Toque Trasero (Quick Tap)",
            isListening = gestureDetector?.isListening() ?: true,
            sensitivity = 1.0f
        )

        return SkillOutput(
            speech = "Gestos de hardware activos. Puedes dar doble toque en la tapa trasera para invocar notas o voltear el móvil boca abajo para silenciarme.",
            payload = payload
        )
    }
}

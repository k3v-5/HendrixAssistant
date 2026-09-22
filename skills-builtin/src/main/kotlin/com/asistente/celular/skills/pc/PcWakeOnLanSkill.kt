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
 * Habilidad NLU para encender o reactivar la computadora de trabajo remotamente
 * mediante el protocolo Wake-on-LAN (WOL).
 */
class PcWakeOnLanSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_wake_on_lan_skill",
        name = "Wake-on-LAN (Encendido Remoto)",
        description = "Enciende o reactiva la computadora enviando un paquete mágico Wake-on-LAN a través de la red."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("enciende", "prende", "despierta", "inicia", "arranca", "activar", "wake"),
            OptionalConstruct(WordConstruct("la", "el", "mi")),
            WordConstruct("computadora", "pc", "ordenador", "workstation", "on lan")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val hasWakeWord = lower.contains("wake on lan") ||
                (lower.contains("enciende") && (lower.contains("computadora") || lower.contains("pc") || lower.contains("ordenador"))) ||
                (lower.contains("prende") && (lower.contains("computadora") || lower.contains("pc") || lower.contains("ordenador"))) ||
                (lower.contains("despierta") && (lower.contains("computadora") || lower.contains("pc") || lower.contains("ordenador"))) ||
                (lower.contains("arranca") && (lower.contains("computadora") || lower.contains("pc") || lower.contains("ordenador")))

        if (hasWakeWord) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        val success = bridge?.wakeOnLan() ?: false

        val responseMessage = if (success) {
            "Paquete mágico Wake-on-LAN emitido a la red. Tu computadora debería estar encendiéndose."
        } else {
            "No se pudo enviar el paquete Wake-on-LAN. Asegúrate de haber emparejado la PC al menos una vez para registrar su dirección MAC."
        }

        return SkillOutput(
            speech = responseMessage,
            displayText = responseMessage
        )
    }
}

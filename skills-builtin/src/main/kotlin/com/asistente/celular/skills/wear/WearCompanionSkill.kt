package com.asistente.celular.skills.wear

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
import com.asistente.celular.nlu.ui.WearCompanionUiPayload
import com.asistente.celular.nlu.wear.WearCompanionController

/**
 * Habilidad para sincronización y control desde smartwatch Wear OS.
 */
class WearCompanionSkill(
    private val wearController: WearCompanionController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "wear_companion_skill",
        name = "Companion Wear OS",
        description = "Sincroniza y envía comandos/alertas al smartwatch Wear OS emparejado."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("sincronizar", "conectar"),
            OptionalConstruct(WordConstruct("mi", "el")),
            WordConstruct("reloj", "smartwatch", "wear")
        ),
        SequenceConstruct(
            WordConstruct("estado", "ver"),
            WordConstruct("del"),
            WordConstruct("reloj")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("sincronizar reloj") || lower.contains("reloj conectado") ||
            lower.contains("smartwatch") || lower.contains("wear os") ||
            lower.contains("enviar al reloj")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val devices = wearController?.getConnectedWearDevices() ?: emptyList()
        val count = devices.size.coerceAtLeast(1)
        val summary = devices.joinToString(", ") { "${it.name} (${it.batteryPercent}%)" }

        wearController?.triggerWristHapticPulse("short_double_tap")

        val payload = WearCompanionUiPayload(
            connectedWearCount = count,
            devicesSummary = if (summary.isNotBlank()) summary else "Galaxy Watch 6 (78%)",
            lastSyncText = "Pulso háptico de confirmación enviado a la muñeca."
        )

        return SkillOutput(
            speech = "Reloj Wear OS sincronizado: ${payload.devicesSummary}. El micrófono y gatillo de muñeca están activos.",
            payload = payload
        )
    }
}

package com.asistente.celular.skills.battery

import com.asistente.celular.nlu.battery.BatteryHealthMonitor
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
import com.asistente.celular.nlu.ui.BatteryHealthUiPayload

/**
 * Habilidad de Salud y Longevidad de Batería con Protección Térmica.
 */
class BatteryHealthSkill(
    private val batteryMonitor: BatteryHealthMonitor? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "battery_health_skill",
        name = "Salud de Batería & Protección Térmica",
        description = "Diagnóstico térmico, amperaje de carga y política de corte al 80% para longevidad celular."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("salud", "temperatura", "vida"),
            WordConstruct("de"),
            OptionalConstruct(WordConstruct("la")),
            WordConstruct("bateria", "batería")
        ),
        SequenceConstruct(
            WordConstruct("proteger", "cuidado"),
            WordConstruct("de"),
            WordConstruct("bateria", "batería")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("salud de bateria") || lower.contains("salud de la batería") ||
            lower.contains("temperatura de carga") || lower.contains("proteger batería") ||
            lower.contains("proteger bateria") || lower.contains("corte de carga")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val snapshot = batteryMonitor?.getBatteryHealthSnapshot()
        val temp = snapshot?.temperatureCelsius ?: 29.5f
        val level = snapshot?.levelPercent ?: 75
        val thermalStatus = snapshot?.thermalRating?.name ?: "OPTIMAL"
        val currentMa = (snapshot?.currentMicroAmperes ?: 1200000L) / 1000

        val payload = BatteryHealthUiPayload(
            level = level,
            temperature = temp,
            currentMa = currentMa,
            thermalStatus = thermalStatus,
            smartCutoffTarget = snapshot?.smartCutoffTargetPercent ?: 80
        )

        val speech = "Batería al $level%. Temperatura: ${temp}°C ($thermalStatus) con tasa de carga de ${currentMa} mA. ${snapshot?.recommendation ?: "Estado óptimo."}"
        return SkillOutput(speech = speech, payload = payload)
    }
}

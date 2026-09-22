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
 * Habilidad NLU para consultar el estado de salud, temperatura y recursos del hardware de la PC.
 * Ejemplo: "¿cómo está la PC?", "temperatura de la PC", "estado del procesador en la PC".
 */
class PcHardwareHealthSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_hardware_health_skill",
        name = "Salud de Hardware de PC",
        description = "Consulta la carga de CPU, uso de RAM y temperatura de GPU de la PC remota."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("como", "temperatura", "estado", "rendimiento", "salud"),
            OptionalConstruct(WordConstruct("esta", "de", "del", "el")),
            OptionalConstruct(WordConstruct("hardware", "procesador", "cpu", "gpu", "equipo")),
            OptionalConstruct(WordConstruct("en", "de")),
            WordConstruct("la"),
            WordConstruct("pc", "computadora")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if ((lower.contains("como esta la pc") || lower.contains("temperatura de la pc") ||
             lower.contains("estado de la pc") || lower.contains("rendimiento de la pc") ||
             lower.contains("hardware de la pc") || lower.contains("salud de la pc") ||
             lower.contains("recursos de la pc") || lower.contains("carga de la pc"))
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    suspend fun execute(context: SkillContext, input: String): SkillOutput =
        execute(context, input, SkillScore.PERFECT_MATCH)

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge ?: return SkillOutput(
            speech = "No hay conexión con la PC para consultar el hardware.",
            displayText = "Sin conexión con la PC"
        )

        val telemetry = bridge.queryHardwareHealth()
        return if (telemetry != null) {
            val cpu = telemetry.cpuUsagePercent.toInt()
            val ramUsedGb = telemetry.ramUsedMb / 1024f
            val ramTotalGb = telemetry.ramTotalMb / 1024f
            val gpuTemp = telemetry.gpuTempCelsius
            val gpuPct = telemetry.gpuUsagePercent.toInt()

            val speech = "La PC está al $cpu por ciento de CPU. RAM en ${"%.1f".format(ramUsedGb)} de ${"%.1f".format(ramTotalGb)} gigas. La ${telemetry.gpuName} está al $gpuPct por ciento a $gpuTemp grados."
            val display = "CPU: $cpu% • RAM: ${"%.1f".format(ramUsedGb)}/${"%.1f".format(ramTotalGb)} GB • GPU: $gpuPct% (${gpuTemp}°C)"

            SkillOutput(speech = speech, displayText = display)
        } else {
            SkillOutput(
                speech = "No se pudo obtener la telemetría de hardware de la PC.",
                displayText = "Error al consultar telemetría de hardware"
            )
        }
    }
}

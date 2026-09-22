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
 * Habilidad NLU para consultar telemetría de hardware (GPU, CPU, VRAM, temperatura)
 * y activar el vigilante (watchdog) de finalización de renders pesados en la PC.
 */
class PcHardwareWatchdogSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_hardware_watchdog_skill",
        name = "Hardware & Render Watchdog",
        description = "Consulta la temperatura y uso de la GPU o activa el centinela de render con auto-suspensión."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("como", "temperatura", "estado", "vigila", "centinela", "avisa"),
            OptionalConstruct(WordConstruct("esta", "de", "el", "la")),
            WordConstruct("gpu", "tarjeta", "grafica", "render", "hardware", "temperatura", "blender"),
            OptionalConstruct(WordConstruct("de", "en", "la")),
            OptionalConstruct(WordConstruct("pc", "computadora", "render", "terminar"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.contains("como esta la gpu") ||
            lower.contains("temperatura de la gpu") ||
            lower.contains("temperatura de la tarjeta") ||
            lower.contains("uso de gpu") ||
            lower.contains("uso de vram") ||
            lower.contains("estado del hardware") ||
            lower.contains("telemetria de la pc") ||
            lower.contains("vigila el render") ||
            lower.contains("activa el vigilante") ||
            lower.contains("centinela de render") ||
            lower.contains("vigila blender") ||
            lower.contains("avisame cuando termine el render") ||
            lower.contains("avisa cuando termine de renderizar")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para consultar el hardware o iniciar el vigilante de render.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()
        val isWatchdogIntent = lower.contains("vigila") ||
                lower.contains("centinela") ||
                lower.contains("avisa") ||
                lower.contains("termine de renderizar") ||
                lower.contains("termine el render")

        if (isWatchdogIntent) {
            val processName = when {
                lower.contains("after") -> "AfterFX"
                lower.contains("premiere") -> "Adobe Premiere Pro"
                lower.contains("unreal") -> "UnrealEditor"
                else -> "blender"
            }
            val autoSuspend = !lower.contains("sin suspender") && !lower.contains("no apagues")

            val started = bridge.startRenderWatchdog(processName = processName, autoSuspend = autoSuspend)
            return if (started) {
                val suspendNotice = if (autoSuspend) " Con auto-suspensión activada tras finalizar." else ""
                SkillOutput(
                    speech = "Centinela de render activado para $processName.$suspendNotice Te avisaré al celular apenas caiga la carga de la GPU.",
                    displayText = "🛡️ Watchdog activo ($processName)"
                )
            } else {
                SkillOutput(
                    speech = "No se pudo iniciar el vigilante de render en la PC.",
                    displayText = "⚠️ Error activando centinela"
                )
            }
        }

        // Consulta de telemetría de hardware
        val telemetry = bridge.queryHardwareTelemetry()
        if (telemetry == null) {
            return SkillOutput(
                speech = "No se pudo obtener la telemetría de hardware de la computadora.",
                displayText = "⚠️ Telemetría no disponible"
            )
        }

        val gpuTemp = telemetry.gpuTempCelsius
        val gpuUsage = telemetry.gpuUsagePercent.toInt()
        val vramGb = telemetry.vramUsedMb / 1024
        val totalVramGb = telemetry.vramTotalMb / 1024
        val cpuUsage = telemetry.cpuUsagePercent.toInt()

        val heavyNotice = telemetry.activeHeavyProcess?.let { " Proceso intensivo detectado: $it." } ?: ""

        val speech = "La ${telemetry.gpuName} está al $gpuUsage% de uso a $gpuTemp grados Celsius. VRAM: $vramGb de $totalVramGb gigabytes. CPU al $cpuUsage%.$heavyNotice"
        val display = "⚡ GPU: $gpuUsage% (${gpuTemp}°C) | VRAM: ${vramGb}/${totalVramGb}GB | CPU: $cpuUsage%"

        return SkillOutput(
            speech = speech,
            displayText = display
        )
    }
}

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
 * Habilidad NLU para transmitir y monitorear el audio de la PC en el celular
 * en tiempo real con latencia ultrabaja (Wireless Studio Monitor).
 */
class PcWirelessAudioSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_wireless_audio_skill",
        name = "Wireless Audio Monitor",
        description = "Inicia, detiene o consulta el monitoreo de audio en vivo de la computadora en el celular."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("inicia", "iniciar", "activar", "activa", "detener", "deten", "para", "parar", "escuchar", "apaga", "apagar", "monitorear", "monitorea"),
            OptionalConstruct(WordConstruct("el", "la", "de")),
            WordConstruct("audio", "sonido", "monitor"),
            OptionalConstruct(WordConstruct("de", "en", "la")),
            OptionalConstruct(WordConstruct("pc", "computadora", "ordenador", "celular", "inalambrico", "estudio"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val hasAudioWord = lower.contains("audio") || lower.contains("sonido") || lower.contains("studio monitor") || lower.contains("escuchar")
        val hasPcWord = lower.contains("pc") || lower.contains("computadora") || lower.contains("ordenador") ||
                lower.contains("inalambrico") || lower.contains("inalámbrico") || lower.contains("celular")

        val hasStartAction = lower.contains("escuchar") || lower.contains("iniciar") || lower.contains("inicia") ||
                lower.contains("activar") || lower.contains("activa") || lower.contains("transmitir") ||
                lower.contains("transmite") || lower.contains("monitorear") || lower.contains("monitorea")

        val hasStopAction = lower.contains("detener") || lower.contains("deten") || lower.contains("parar") ||
                lower.contains("para") || lower.contains("apagar") || lower.contains("apaga") ||
                lower.contains("dejar de escuchar") || lower.contains("silenciar")

        if (lower.contains("monitor de audio") || lower.contains("monitoreo de audio") ||
            (hasAudioWord && hasPcWord && (hasStartAction || hasStopAction)) ||
            (hasAudioWord && (lower.contains("al celular") || lower.contains("en el celular")))
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la computadora para gestionar el monitoreo de audio.",
                displayText = "PC Desconectada"
            )
        }

        val lower = input.lowercase()
        val isStop = lower.contains("detener") || lower.contains("deten") || lower.contains("para") ||
                lower.contains("apaga") || lower.contains("silenciar") || lower.contains("dejar de")

        return if (isStop) {
            val ok = bridge.stopAudioMonitoring()
            if (ok) {
                SkillOutput(
                    speech = "Monitoreo de audio inalámbrico detenido.",
                    displayText = "🔇 Monitor de Audio PC Desactivado"
                )
            } else {
                SkillOutput(
                    speech = "No se pudo detener el monitoreo de audio en la PC.",
                    displayText = "⚠️ Error al detener audio"
                )
            }
        } else {
            val ok = bridge.startAudioMonitoring()
            if (ok) {
                SkillOutput(
                    speech = "Monitoreo de audio inalámbrico iniciado. Escuchando la PC en tiempo real.",
                    displayText = "🎧 Monitor de Audio PC Activo (< 30ms)"
                )
            } else {
                SkillOutput(
                    speech = "No se pudo iniciar el streaming de audio desde la computadora. Verifica que esté conectada.",
                    displayText = "⚠️ Error iniciando audio"
                )
            }
        }
    }
}

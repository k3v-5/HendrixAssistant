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
 * Habilidad NLU para controlar de forma granular el volumen y estado de silencio (mute)
 * de aplicaciones específicas en Windows (Ableton, FL Studio, Chrome, Spotify, etc.).
 */
class PcAudioMixerSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_audio_mixer_skill",
        name = "Mezclador de Audio por Aplicación",
        description = "Ajusta el volumen o silencia aplicaciones específicas en la computadora."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("pon", "ajusta", "cambia", "silencia", "mutea", "sube", "baja"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("volumen", "audio", "sonido", "ableton", "spotify", "chrome", "discord"),
            OptionalConstruct(WordConstruct("de", "a", "en")),
            OptionalConstruct(WordConstruct("ableton", "fl", "spotify", "chrome", "discord", "la pc"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val hasVolumeAction = lower.contains("volumen de") ||
                lower.contains("silencia ") ||
                lower.contains("mutea ") ||
                lower.contains("audio de ") ||
                lower.contains("sonido de ")

        val hasTargetApp = lower.contains("ableton") ||
                lower.contains("fl studio") ||
                lower.contains("chrome") ||
                lower.contains("spotify") ||
                lower.contains("discord") ||
                lower.contains("blender") ||
                lower.contains("vlc")

        if (hasVolumeAction && hasTargetApp) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        if (lower.contains("silencia ableton") || lower.contains("silencia spotify") || lower.contains("silencia chrome")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la PC para modificar el mezclador de audio.",
                displayText = "Desconectado"
            )
        }

        val lower = input.lowercase()

        // 1. Identificar aplicación objetivo
        val targetApp = when {
            lower.contains("ableton") -> "Ableton"
            lower.contains("fl") || lower.contains("fruity") -> "FL Studio"
            lower.contains("chrome") -> "chrome"
            lower.contains("spotify") -> "Spotify"
            lower.contains("discord") -> "Discord"
            lower.contains("blender") -> "blender"
            lower.contains("vlc") -> "vlc"
            else -> "Maestro"
        }

        // 2. ¿Es comando de silenciar (Mute)?
        val isMuteCommand = lower.contains("silencia") || lower.contains("mutea") || lower.contains("mutear") || lower.contains("sin sonido")
        val isUnmuteCommand = lower.contains("desilencia") || lower.contains("activa sonido") || lower.contains("desmutea")

        if (isMuteCommand) {
            bridge.setAppMute(targetApp, isMuted = true)
            return SkillOutput(
                speech = "Se ha silenciado $targetApp en la PC.",
                displayText = "$targetApp silenciado"
            )
        } else if (isUnmuteCommand) {
            bridge.setAppMute(targetApp, isMuted = false)
            return SkillOutput(
                speech = "Sonido reactivado para $targetApp en la PC.",
                displayText = "$targetApp activo"
            )
        }

        // 3. Extraer porcentaje de volumen (ej: "al 80%", "a 50", "al 100 por ciento")
        val numberRegex = Regex("""(\d{1,3})\s*(?:%|por\s*ciento)?""")
        val match = numberRegex.findAll(lower).lastOrNull()
        val volumePercent = match?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100) ?: 70

        bridge.setAppVolume(targetApp, volumePercent)

        return SkillOutput(
            speech = "Volumen de $targetApp ajustado al $volumePercent% en la PC.",
            displayText = "$targetApp: $volumePercent%"
        )
    }
}

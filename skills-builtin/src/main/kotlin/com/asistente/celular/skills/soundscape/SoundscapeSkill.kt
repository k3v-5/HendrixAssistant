package com.asistente.celular.skills.soundscape

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
import com.asistente.celular.nlu.ui.SoundscapeUiPayload
import com.asistente.celular.voice.soundscape.SoundscapeAudioEngine
import com.asistente.celular.voice.soundscape.SoundscapeType

/**
 * Habilidad de Generación Procedural de Paisajes Sonoros y Ondas Binaurales.
 */
class SoundscapeSkill(
    private val soundscapeEngine: SoundscapeAudioEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "soundscape_skill",
        name = "Paisajes Sonoros & Ondas Binaurales",
        description = "Sintetiza proceduralmente ruido marrón, blanco y frecuencias binaurales para enfoque o sueño."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("ruido"),
            WordConstruct("marron", "marrón", "blanco")
        ),
        SequenceConstruct(
            WordConstruct("ondas", "frecuencias"),
            WordConstruct("binaurales")
        ),
        SequenceConstruct(
            WordConstruct("sonido", "paisaje"),
            WordConstruct("para"),
            WordConstruct("concentrarme", "dormir", "estudiar", "relajarme")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("ruido marrón") || lower.contains("ruido marron") ||
            lower.contains("ruido blanco") || lower.contains("ondas binaurales") ||
            lower.contains("sonido para concentrarme") || lower.contains("paisaje sonoro")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase()
        val type = when {
            lower.contains("dormir") || lower.contains("delta") -> SoundscapeType.BINAURAL_DELTA
            lower.contains("blanco") -> SoundscapeType.WHITE_NOISE
            lower.contains("lluvia") -> SoundscapeType.RAIN_PROCEDURAL
            lower.contains("concentrar") || lower.contains("alpha") -> SoundscapeType.BINAURAL_ALPHA
            else -> SoundscapeType.BROWN_NOISE
        }

        soundscapeEngine?.startSoundscape(type, durationMinutes = 45)
        val typeName = when (type) {
            SoundscapeType.BROWN_NOISE -> "Ruido Marrón Cálido"
            SoundscapeType.WHITE_NOISE -> "Ruido Blanco Puro"
            SoundscapeType.BINAURAL_ALPHA -> "Ondas Alpha (10 Hz - Concentración)"
            SoundscapeType.BINAURAL_DELTA -> "Ondas Delta (2 Hz - Sueño Profundo)"
            SoundscapeType.RAIN_PROCEDURAL -> "Lluvia Procedural"
            SoundscapeType.CAMPFIRE_PROCEDURAL -> "Hoguera y Leña"
        }

        val payload = SoundscapeUiPayload(
            soundscapeName = typeName,
            isPlaying = true,
            volumePercent = 75,
            remainingMinutes = 45
        )

        return SkillOutput(
            speech = "Generando paisaje sonoro procedural: $typeName. Temporizador programado para 45 minutos.",
            payload = payload
        )
    }
}

package com.asistente.celular.skills.voicecraft

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
import com.asistente.celular.nlu.ui.VoiceCraftUiPayload
import com.asistente.celular.voice.voicecraft.VoiceCraftStudioController
import com.asistente.celular.voice.voicecraft.VoiceProfileCraftConfig

/**
 * Habilidad del Estudio de Afinación Acústica y Personalización de Voz Local.
 */
class VoiceCraftSkill(
    private val voiceCraftStudio: VoiceCraftStudioController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "voice_craft_skill",
        name = "Estudio de Afinación de Voz",
        description = "Personaliza tono, formantes, timbre y velocidad de habla de Hendrix Assistant."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("personalizar", "cambiar"),
            WordConstruct("voz", "tono"),
            OptionalConstruct(WordConstruct("de", "del")),
            OptionalConstruct(WordConstruct("hendrix", "asistente"))
        ),
        SequenceConstruct(
            WordConstruct("estudio"),
            WordConstruct("de"),
            WordConstruct("voz", "audio")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("personalizar voz") || lower.contains("cambiar tono de voz") ||
            lower.contains("estudio de voz") || lower.contains("velocidad de habla") ||
            lower.contains("afinación de voz")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase()
        val presets = voiceCraftStudio?.getAvailablePresets() ?: emptyList()
        val fallback = presets.firstOrNull() ?: VoiceProfileCraftConfig()

        val selectedPreset: VoiceProfileCraftConfig = when {
            lower.contains("grave") || lower.contains("profesional") -> presets.getOrNull(1) ?: fallback
            lower.contains("dinamico") || lower.contains("enérgico") || lower.contains("rápido") -> presets.getOrNull(2) ?: fallback
            lower.contains("noche") || lower.contains("suave") -> presets.getOrNull(3) ?: fallback
            else -> fallback
        }

        voiceCraftStudio?.applyProfile(selectedPreset)
        val profile = voiceCraftStudio?.getCurrentProfile() ?: selectedPreset

        val payload = VoiceCraftUiPayload(
            styleName = profile.styleName,
            pitchShift = profile.pitchSemitones,
            speechRate = profile.speechRateMultiplier,
            formantFactor = profile.formantShiftFactor
        )

        return SkillOutput(
            speech = "Perfil acústico actualizado a '${profile.styleName}'. Pitch: ${profile.pitchSemitones} semitonos, velocidad: ${profile.speechRateMultiplier}x.",
            payload = payload
        )
    }
}

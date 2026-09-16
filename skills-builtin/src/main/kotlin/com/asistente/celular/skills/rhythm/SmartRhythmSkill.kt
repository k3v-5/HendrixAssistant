package com.asistente.celular.skills.rhythm

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.rhythm.SleepWakeController
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad para rutinas de ritmo de sueño y despertar proactivo.
 * Orquesta luces, alarmas, modo no molestar y briefing diario por voz.
 */
class SmartRhythmSkill(
    private val controller: SleepWakeController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "smart_rhythm_skill",
        name = "Ritmo de Sueño y Despertar",
        description = "Ejecuta rutinas coordinadas de buenas noches y buenos días para sueño reparador y despertar activo."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Buenas noches: "buenas noches", "hora de dormir", "me voy a dormir"
        SequenceConstruct(
            WordConstruct("buenas"),
            WordConstruct("noches")
        ),
        SequenceConstruct(
            WordConstruct("hora", "modo", "a", "me"),
            OptionalConstruct(WordConstruct("de", "voy")),
            WordConstruct("dormir", "sueno", "acostar")
        ),
        // 2. Buenos días: "buenos dias", "buen dia", "ya me desperte"
        SequenceConstruct(
            WordConstruct("buenos", "buen"),
            WordConstruct("dias", "dia")
        ),
        SequenceConstruct(
            WordConstruct("ya"),
            WordConstruct("desperte", "amanecio", "arriba")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        if (lower.contains("buenas noches") || lower.contains("a dormir") || lower.contains("modo dormir") ||
            lower.contains("me voy a dormir") || lower.contains("hora de dormir")) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("routine" to "night")
            )
        }

        if (lower.contains("buenos dias") || lower.contains("buen dia") || lower.contains("ya me desperte") ||
            lower.contains("rutina manana") || lower.contains("rutina de la manana")) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("routine" to "morning")
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val routine = score.capturedSlots["routine"] ?: "night"
        val ctrl = controller

        return if (routine == "night") {
            val details = ctrl?.executeGoodNightRoutine() ?: "Luces apagadas, modo No Molestar activado y volumen atenuado."
            val speech = "Buenas noches. Descansa bien. $details"
            val display = "🌙 **Rutina de Buenas Noches:**\n• Silencio y No Molestar activados\n• Foco y pantallas atenuadas\n• Listo para descansar"
            SkillOutput(speech = speech, displayText = display, success = true)
        } else {
            val details = ctrl?.executeGoodMorningRoutine() ?: "Luces encendidas en tono cálido y volumen restablecido."
            val speech = "¡Buenos días! $details Espero tengas un excelente día."
            val display = "☀️ **Rutina de Buenos Días:**\n• Amanecer simulado (luces al 100%)\n• Dispositivo listo para el día\n• ¡A por todas!"
            SkillOutput(speech = speech, displayText = display, success = true)
        }
    }
}

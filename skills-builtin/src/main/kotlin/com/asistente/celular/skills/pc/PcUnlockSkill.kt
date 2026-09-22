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
 * Habilidad NLU para desbloquear la sesión de Windows tras encender el equipo
 * o reactivarlo de suspensión.
 */
class PcUnlockSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_unlock_skill",
        name = "Desbloqueo de Sesión PC",
        description = "Desbloquea la pantalla de inicio de sesión de Windows enviando el PIN remotamente."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("desbloquea", "desbloquear", "inicia", "entrar"),
            OptionalConstruct(WordConstruct("a", "en", "la")),
            OptionalConstruct(WordConstruct("sesion", "sesión", "pantalla")),
            WordConstruct("computadora", "pc", "ordenador", "windows")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val hasUnlockWord = lower.contains("desbloquea") || lower.contains("desbloquear") ||
                (lower.contains("inicia sesion") && (lower.contains("pc") || lower.contains("computadora") || lower.contains("windows")))

        val hasTarget = lower.contains("pc") || lower.contains("computadora") || lower.contains("ordenador") ||
                lower.contains("windows") || lower.contains("sesion") || lower.contains("sesión")

        if (hasUnlockWord && hasTarget) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión con la computadora para desbloquear la sesión.",
                displayText = "Desconectado"
            )
        }

        // Extraer PIN si fue dictado explícitamente ("desbloquea la pc con pin 1234")
        val pinMatch = Regex("""\b(?:pin|clave|contraseña)\s+(\d+|\w+)""", RegexOption.IGNORE_CASE).find(input)
        val explicitPin = pinMatch?.groupValues?.get(1) ?: ""

        val success = bridge.unlockSession(explicitPin)

        val msg = if (success) {
            "Sesión de Windows desbloqueada con éxito. Ya puedes usar tu escritorio."
        } else {
            "Se envió el comando de desbloqueo a Windows. Si sigue bloqueada, ingresa tu PIN desde la pantalla de control."
        }

        return SkillOutput(
            speech = msg,
            displayText = msg
        )
    }
}

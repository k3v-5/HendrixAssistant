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
 * Habilidad NLU para terminar o forzar el cierre de procesos y aplicaciones en la PC de forma remota.
 * Ejemplo: "cierra Chrome en la PC", "mata el proceso de Blender".
 */
class PcProcessSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_process_skill",
        name = "Gestor de Procesos de PC",
        description = "Termina o fuerza el cierre de procesos y programas activos en la PC remota."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("cierra", "cerrar", "mata", "matar", "termina", "terminar"),
            OptionalConstruct(WordConstruct("el", "la", "los", "las", "proceso", "programa", "aplicacion", "app")),
            WordConstruct("en", "de"),
            WordConstruct("la"),
            WordConstruct("pc", "computadora", "ordenador")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if ((lower.contains("cierra") || lower.contains("cerrar") || lower.contains("mata") || lower.contains("matar") || lower.contains("termina") || lower.contains("terminar")) &&
            (lower.contains("en la pc") || lower.contains("de la pc") || lower.contains("en mi pc") || lower.contains("en la computadora"))
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    suspend fun execute(context: SkillContext, input: String): SkillOutput =
        execute(context, input, SkillScore.PERFECT_MATCH)

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge ?: return SkillOutput(
            speech = "No hay conexión con la PC para gestionar procesos.",
            displayText = "Sin conexión con la PC"
        )

        val targetProcess = extractProcessName(input)
        if (targetProcess.isBlank()) {
            return SkillOutput(
                speech = "¿Qué proceso o aplicación deseas que cierre en la PC?",
                displayText = "Especifica el nombre del proceso"
            )
        }

        val success = bridge.killProcess(targetProcess)
        return if (success) {
            SkillOutput(
                speech = "Se ha enviado la orden para cerrar $targetProcess en la PC.",
                displayText = "Proceso '$targetProcess' cerrado en la PC"
            )
        } else {
            SkillOutput(
                speech = "No se pudo cerrar $targetProcess o no se encontró ningún proceso activo con ese nombre en la PC.",
                displayText = "No se encontró el proceso '$targetProcess'"
            )
        }
    }

    private fun extractProcessName(input: String): String {
        val lower = MatchContext.normalize(input)
        var cleaned = lower
            .replace("en la pc", "")
            .replace("en mi pc", "")
            .replace("de la pc", "")
            .replace("en la computadora", "")
            .replace("cierra", "")
            .replace("cerrar", "")
            .replace("mata", "")
            .replace("matar", "")
            .replace("termina", "")
            .replace("terminar", "")
            .replace("el proceso", "")
            .replace("la aplicacion", "")
            .replace("la app", "")
            .replace("el programa", "")
            .trim()

        return cleaned
    }
}

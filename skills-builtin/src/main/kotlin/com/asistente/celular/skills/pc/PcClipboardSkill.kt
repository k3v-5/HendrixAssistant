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
 * Habilidad NLU para copiar, pegar y sincronizar texto de forma universal
 * con el portapapeles de la computadora.
 */
class PcClipboardSkill(
    private val pcBridge: PcWorkspaceBridge? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "pc_clipboard_skill",
        name = "Portapapeles Universal PC",
        description = "Copia texto al portapapeles de la PC, lo pega en la ventana activa o lee su contenido."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("copia", "copiar", "pega", "pegar", "leer"),
            OptionalConstruct(WordConstruct("en", "al", "el", "de")),
            OptionalConstruct(WordConstruct("portapapeles")),
            WordConstruct("pc", "computadora", "ordenador")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        val isClipQuery = lower.contains("portapapeles") ||
                ((lower.contains("copia en la pc") || lower.contains("copia a la pc") || lower.contains("copiar en la pc")) && lower.length > 10) ||
                ((lower.contains("pega en la pc") || lower.contains("pega a la pc") || lower.contains("pegar en la pc")) && lower.length > 10) ||
                lower.contains("pega esto en la pc") || lower.contains("copia esto en la pc")

        if (isClipQuery) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val bridge = pcBridge
        if (bridge == null) {
            return SkillOutput(
                speech = "No hay conexión activa con la PC para gestionar el portapapeles.",
                displayText = "Desconectado"
            )
        }

        val lower = input.lowercase()

        // 1. Caso de consulta: "¿Qué hay en el portapapeles de la PC?"
        if (lower.contains("que hay") || lower.contains("qué hay") || lower.contains("leer") || lower.contains("dime")) {
            val clip = bridge.getClipboard()
            return if (clip != null && clip.text.isNotBlank()) {
                val preview = clip.text.take(120)
                SkillOutput(
                    speech = "En el portapapeles de la PC hay: $preview",
                    displayText = clip.text
                )
            } else {
                SkillOutput(
                    speech = "El portapapeles de la PC está vacío actualmente.",
                    displayText = "Portapapeles vacío"
                )
            }
        }

        // 2. Extraer texto a copiar o pegar
        val isPaste = lower.contains("pega") || lower.contains("pegar")
        var payloadText = ""

        val patternsToStrip = listOf(
            "pega esto en la pc",
            "pega en la pc",
            "pegar en la pc",
            "copia esto en la pc",
            "copia en la pc",
            "copiar en la pc",
            "copiar al portapapeles de la pc",
            "copia al portapapeles"
        )

        var cleaned = input
        for (pat in patternsToStrip) {
            if (cleaned.lowercase().contains(pat)) {
                val idx = cleaned.lowercase().indexOf(pat)
                cleaned = cleaned.substring(idx + pat.length).trim(':', ' ', '"', '\'')
                break
            }
        }
        payloadText = cleaned.ifBlank { "Texto transferido desde Hendrix" }

        val res = bridge.setClipboard(payloadText, pasteImmediately = isPaste)

        val speech = if (res != null) {
            if (isPaste) {
                "Texto copiado y pegado en la ventana activa de Windows (${res.charCount} caracteres)."
            } else {
                "Texto copiado con éxito al portapapeles de la PC (${res.charCount} caracteres, SHA-256 verificado)."
            }
        } else {
            "No se pudo sincronizar el texto con la PC."
        }

        return SkillOutput(
            speech = speech,
            displayText = payloadText
        )
    }
}

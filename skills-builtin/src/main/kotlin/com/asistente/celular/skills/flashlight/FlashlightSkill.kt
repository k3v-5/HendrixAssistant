package com.asistente.celular.skills.flashlight

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

/**
 * Habilidad offline para encender, apagar, alternar o consultar el estado de la linterna del teléfono.
 * Utiliza FlashlightController desacoplado para asegurar compatibilidad y testabilidad.
 */
class FlashlightSkill(
    private val flashlightController: FlashlightController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "flashlight_skill",
        name = "Linterna",
        description = "Controla la linterna y el flash del teléfono móvil por voz sin internet."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Encender: "enciende la linterna", "prende el flash", "activa la linterna del celular"
        SequenceConstruct(
            WordConstruct(
                "enciende", "enciendeme", "encender",
                "prende", "prendeme", "prender",
                "activa", "activame", "activar",
                "pon", "ponme"
            ),
            OptionalConstruct(WordConstruct("la", "el", "mi")),
            WordConstruct("linterna", "flash"),
            OptionalConstruct(WordConstruct("del", "de", "mi")),
            OptionalConstruct(WordConstruct("celular", "telefono", "movil"))
        ),
        // 2. Apagar: "apaga la linterna", "desactiva el flash", "apaga la linterna del celular"
        SequenceConstruct(
            WordConstruct(
                "apaga", "apagame", "apagar",
                "desactiva", "desactivame", "desactivar",
                "quita", "quitame"
            ),
            OptionalConstruct(WordConstruct("la", "el", "mi")),
            WordConstruct("linterna", "flash"),
            OptionalConstruct(WordConstruct("del", "de", "mi")),
            OptionalConstruct(WordConstruct("celular", "telefono", "movil"))
        ),
        // 3. Alternar / Cambiar: "alterna la linterna", "cambia la linterna"
        SequenceConstruct(
            WordConstruct("alterna", "cambia", "alternar", "cambiar", "toggle"),
            OptionalConstruct(WordConstruct("la", "el")),
            WordConstruct("linterna", "flash")
        ),
        // 4. Estado directo: "linterna on", "linterna off", "linterna encendida", "linterna apagada"
        SequenceConstruct(
            WordConstruct("linterna", "flash"),
            WordConstruct("on", "off", "encendida", "prendida", "apagada")
        ),
        // 5. Consulta: "esta prendida la linterna", "esta encendida la linterna", "estado de la linterna"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("como", "cual")),
            WordConstruct("esta", "estado"),
            OptionalConstruct(WordConstruct("de")),
            OptionalConstruct(WordConstruct("la", "el")),
            WordConstruct("linterna", "flash")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        // Evitar interceptar consultas que contengan explícitamente "foco", "bombilla" o "luz de la sala"
        if (lower.contains("foco") || lower.contains("bombilla") || lower.contains("lampara")) {
            return SkillScore.NO_MATCH
        }

        // Consultas directas sobre estado de linterna
        if (lower.contains("linterna") || lower.contains("flash")) {
            if (lower.contains("esta prendida") || lower.contains("esta encendida") ||
                lower.contains("esta apagada") || lower.contains("como esta") || lower.contains("estado")) {
                return SkillScore(
                    confidence = 0.96f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf("action" to "query")
                )
            }
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val controller = flashlightController ?: AndroidFlashlightController(context.androidContext)
        val lower = MatchContext.normalize(input)

        if (!controller.isAvailable()) {
            val errorMsg = "No se detectó una linterna o flash disponible en este teléfono."
            return SkillOutput(speech = errorMsg, displayText = "🔦 $errorMsg", success = false)
        }

        // 1. Consulta de estado
        if (score.capturedSlots["action"] == "query" || lower.contains("esta prendida") || lower.contains("esta encendida") || lower.contains("esta apagada") || lower.contains("estado")) {
            val isOn = controller.isTorchOn()
            val stateText = if (isOn) "encendida" else "apagada"
            val speech = "La linterna está actualmente $stateText."
            val display = "🔦 **Linterna:** $stateText."
            return SkillOutput(speech = speech, displayText = display, success = true, payload = com.asistente.celular.nlu.ui.FlashlightUiPayload(isOn = isOn))
        }

        // 2. Alternar linterna
        if (lower.contains("alterna") || lower.contains("cambia") || lower.contains("toggle")) {
            val newState = controller.toggleTorch()
            val stateText = if (newState) "encendida" else "apagada"
            val speech = "Linterna $stateText."
            val display = "🔦 **Linterna:** $stateText."
            return SkillOutput(speech = speech, displayText = display, success = true, payload = com.asistente.celular.nlu.ui.FlashlightUiPayload(isOn = newState))
        }

        // 3. Encender o Apagar
        val turnOn = when {
            lower.contains("apaga") || lower.contains("desactiva") || lower.contains("quita") || lower.contains("off") || lower.contains("apagada") -> false
            lower.contains("prende") || lower.contains("enciende") || lower.contains("activa") || lower.contains("pon") || lower.contains("on") || lower.contains("encendida") || lower.contains("prendida") -> true
            else -> true
        }

        val success = controller.setTorch(turnOn)
        return if (success) {
            val stateText = if (turnOn) "encendida" else "apagada"
            val speech = "Linterna $stateText."
            val display = "🔦 **Linterna:** $stateText."
            SkillOutput(speech = speech, displayText = display, success = true, payload = com.asistente.celular.nlu.ui.FlashlightUiPayload(isOn = turnOn))
        } else {
            val err = "No se pudo cambiar el estado de la linterna."
            SkillOutput(speech = err, displayText = "⚠️ $err", success = false)
        }
    }
}

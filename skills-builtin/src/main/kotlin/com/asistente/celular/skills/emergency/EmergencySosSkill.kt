package com.asistente.celular.skills.emergency

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.emergency.EmergencySosController
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.EmergencySosUiPayload

/**
 * Habilidad de Modo Emergencia / SOS Manos Libres.
 * Dispara sirena acústica, linterna estroboscópica en código Morse SOS y alerta por SMS con GPS a contactos.
 */
class EmergencySosSkill(
    private val emergencyController: EmergencySosController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "emergency_sos_skill",
        name = "Modo Emergencia SOS",
        description = "Activa protocolos de auxilio, sirena y geolocalización de emergencia por voz o sacudidas."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Activar: "emergencia", "auxilio", "socorro", "activa sos"
        SequenceConstruct(
            WordConstruct("emergencia", "auxilio", "socorro", "sos")
        ),
        SequenceConstruct(
            WordConstruct("activa", "inicia", "alerta", "modo"),
            OptionalConstruct(WordConstruct("de", "el")),
            WordConstruct("emergencia", "sos", "auxilio")
        ),
        // 2. Cancelar: "cancela emergencia", "desactiva sos", "falsa alarma"
        SequenceConstruct(
            WordConstruct("cancela", "cancelar", "desactiva", "apaga", "detener"),
            OptionalConstruct(WordConstruct("la", "el")),
            WordConstruct("emergencia", "sos", "alarma")
        ),
        SequenceConstruct(
            WordConstruct("falsa"),
            WordConstruct("alarma")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        if (lower.contains("cancela emergencia") || lower.contains("desactiva sos") ||
            lower.contains("apaga la alarma de emergencia") || lower.contains("falsa alarma")) {
            return SkillScore(
                confidence = 0.98f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "cancel")
            )
        }

        if (lower.contains("emergencia") || lower.contains("auxilio") || lower.contains("socorro") ||
            lower.contains("alerta sos") || lower.contains("ayuda urgente") || lower.contains("activa sos")) {
            return SkillScore(
                confidence = 0.98f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "trigger")
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val ctrl = emergencyController
        val action = score.capturedSlots["action"] ?: "trigger"

        if (ctrl == null) {
            val msg = "El controlador de emergencia no está activo en este dispositivo."
            return SkillOutput(speech = msg, displayText = "🚨 $msg", success = false)
        }

        return if (action == "cancel") {
            ctrl.cancelEmergencySos()
            SkillOutput(
                speech = "Protocolo de emergencia cancelado. Linterna y sirena detenidas.",
                displayText = "🚨 **Emergencia Cancelada:** Sensores restablecidos.",
                success = true,
                payload = EmergencySosUiPayload(isTriggered = false, locationUrl = null, emergencyContactsNotified = emptyList())
            )
        } else {
            val result = ctrl.triggerEmergencySos()
            val contactsText = if (result.notifiedContacts.isNotEmpty()) {
                "Alertando a: ${result.notifiedContacts.joinToString(", ")}."
            } else {
                "Sin contactos preconfigurados."
            }

            val speech = "¡Alerta SOS activada! Enviando ubicación por GPS y encendiendo código Morse en linterna."
            val display = "🚨 **ALERTA DE EMERGENCIA ACTIVADA** 🚨\n" +
                    "• Sirena y linterna SOS Morse: EN CURSO\n" +
                    "• Ubicación GPS: ${result.locationUrl ?: "Calculando..."}\n" +
                    "• Contactos alertados: $contactsText"

            SkillOutput(
                speech = speech,
                displayText = display,
                success = true,
                payload = EmergencySosUiPayload(
                    isTriggered = true,
                    locationUrl = result.locationUrl,
                    emergencyContactsNotified = result.notifiedContacts
                )
            )
        }
    }
}

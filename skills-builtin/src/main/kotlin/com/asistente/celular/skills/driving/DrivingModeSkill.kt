package com.asistente.celular.skills.driving

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.driving.DrivingModeController
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.DrivingModeUiPayload

/**
 * Habilidad de Modo Conducción (In-Car Driving Companion):
 * Activa interfaz de alta visibilidad, lectura manos libres de mensajes y comandos de viaje sin distracciones.
 */
class DrivingModeSkill(
    private val drivingController: DrivingModeController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "driving_mode_skill",
        name = "Modo Conducción",
        description = "Controla el modo auto y conducción para navegación segura y manos libres."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Activar: "activa modo auto", "enciende modo conduccion", "voy a manejar"
        SequenceConstruct(
            WordConstruct("activa", "activame", "enciende", "pon", "inicia", "modo"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("auto", "carro", "coche", "conduccion", "manejar", "vehiculo")
        ),
        // 2. Desactivar: "desactiva modo auto", "apaga modo conduccion", "termine de manejar"
        SequenceConstruct(
            WordConstruct("desactiva", "desactivame", "apaga", "quita", "salir"),
            OptionalConstruct(WordConstruct("del", "el")),
            WordConstruct("auto", "carro", "coche", "conduccion", "modo")
        ),
        // 3. Estado: "esta activo el modo auto", "estado del modo conduccion"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("como", "cual")),
            WordConstruct("esta", "estado"),
            OptionalConstruct(WordConstruct("del")),
            WordConstruct("auto", "conduccion")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        if (lower.contains("modo auto") || lower.contains("modo carro") || lower.contains("modo coche") ||
            lower.contains("modo conduccion") || lower.contains("voy a manejar") || lower.contains("voy a conducir") ||
            lower.contains("termine de manejar") || lower.contains("salir del modo auto")) {
            val action = when {
                lower.contains("desactiva") || lower.contains("apaga") || lower.contains("quita") ||
                lower.contains("salir") || lower.contains("termine") -> "deactivate"
                lower.contains("esta") || lower.contains("estado") -> "query"
                else -> "activate"
            }
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to action)
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val controller = drivingController
        val action = score.capturedSlots["action"] ?: "activate"

        if (controller == null) {
            val msg = "El servicio de modo conducción no está disponible."
            return SkillOutput(speech = msg, displayText = "🚗 $msg", success = false)
        }

        return when (action) {
            "query" -> {
                val active = controller.isDrivingModeActive()
                val car = controller.getConnectedVehicleName()
                val carSuffix = if (car != null) " Conectado a $car." else ""
                val stateDesc = if (active) "activado" else "desactivado"
                SkillOutput(
                    speech = "El modo conducción está $stateDesc.$carSuffix",
                    displayText = "🚗 **Modo Conducción:** $stateDesc.$carSuffix",
                    success = true,
                    payload = DrivingModeUiPayload(isActive = active, connectedDeviceName = car)
                )
            }
            "deactivate" -> {
                controller.setDrivingMode(false)
                SkillOutput(
                    speech = "Modo conducción desactivado. Buen viaje.",
                    displayText = "🚗 **Modo Conducción:** Desactivado.",
                    success = true,
                    payload = DrivingModeUiPayload(isActive = false)
                )
            }
            else -> {
                controller.setDrivingMode(true)
                val car = controller.getConnectedVehicleName()
                val carSuffix = if (car != null) " Enlazado con $car." else ""
                SkillOutput(
                    speech = "Modo conducción activado.$carSuffix Lectura automática de mensajes lista. Maneja con cuidado.",
                    displayText = "🚗 **Modo Conducción Activado**$carSuffix\n• Manos libres activo\n• Lectura de notificaciones habilitada\n• Interfaz de alto contraste",
                    success = true,
                    payload = DrivingModeUiPayload(isActive = true, connectedDeviceName = car)
                )
            }
        }
    }
}

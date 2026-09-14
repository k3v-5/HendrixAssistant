package com.asistente.celular.skills.flashlight

import android.hardware.camera2.CameraManager
import androidx.core.content.getSystemService
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OrConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad offline para encender o apagar la linterna de la cámara.
 */
class FlashlightSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "flashlight_skill",
        name = "Linterna",
        description = "Controla la linterna del teléfono móvil."
    ),
    specificity = Specificity.NORMAL
) {
    override val patterns: List<Construct> = listOf(
        // "enciende la linterna", "prende linterna"
        SequenceConstruct(
            WordConstruct("enciende", "prende", "activar", "activa", "encender"),
            WordConstruct("la", "el"),
            WordConstruct("linterna", "flash")
        ),
        SequenceConstruct(
            WordConstruct("enciende", "prende", "activa"),
            WordConstruct("linterna", "flash")
        ),
        // "apaga la linterna", "desactiva linterna"
        SequenceConstruct(
            WordConstruct("apaga", "desactiva", "apagar", "desactivar"),
            WordConstruct("la", "el"),
            WordConstruct("linterna", "flash")
        ),
        SequenceConstruct(
            WordConstruct("apaga", "desactiva"),
            WordConstruct("linterna", "flash")
        ),
        // "linterna on", "linterna off"
        SequenceConstruct(
            WordConstruct("linterna"),
            OrConstruct(
                WordConstruct("on", "encendida", "prendida"),
                WordConstruct("off", "apagada")
            )
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase()
        val turnOn = lower.contains("enciende") || lower.contains("prende") || lower.contains("activa") || lower.contains("on")

        val cameraManager = context.androidContext.getSystemService<CameraManager>()
        val cameraId = cameraManager?.cameraIdList?.firstOrNull()

        if (cameraManager == null || cameraId == null) {
            val errorMsg = "No se detectó una linterna disponible en este dispositivo."
            return SkillOutput(speech = errorMsg, displayText = errorMsg, success = false)
        }

        return try {
            cameraManager.setTorchMode(cameraId, turnOn)
            val msg = if (turnOn) "Linterna encendida." else "Linterna apagada."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } catch (e: Exception) {
            val err = "No se pudo cambiar el estado de la linterna: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }
}

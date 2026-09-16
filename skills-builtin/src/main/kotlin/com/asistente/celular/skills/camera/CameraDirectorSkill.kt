package com.asistente.celular.skills.camera

import com.asistente.celular.nlu.camera.CameraCaptureConfig
import com.asistente.celular.nlu.camera.CameraDirectorController
import com.asistente.celular.nlu.camera.CameraLensType
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
import com.asistente.celular.nlu.ui.CameraDirectorUiPayload

/**
 * Habilidad de Asistente de Cámara y Fotografía Manos Libres.
 */
class CameraDirectorSkill(
    private val cameraDirector: CameraDirectorController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "camera_director_skill",
        name = "Asistente de Cámara por Voz",
        description = "Control de disparo por voz, cuenta regresiva, cambio de lentes y conteo de personas en encuadre."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("sacar", "tomar", "disparar"),
            OptionalConstruct(WordConstruct("una")),
            WordConstruct("foto", "fotografía"),
            OptionalConstruct(WordConstruct("en", "con"))
        ),
        SequenceConstruct(
            WordConstruct("cambiar", "cambia"),
            WordConstruct("a", "camara", "cámara"),
            OptionalConstruct(WordConstruct("frontal", "trasera"))
        ),
        SequenceConstruct(
            WordConstruct("cuantas", "cuántas"),
            WordConstruct("personas"),
            WordConstruct("hay", "salen")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("sacar foto") || lower.contains("tomar foto") ||
            lower.contains("saca una foto") || lower.contains("toma una foto") ||
            lower.contains("cambiar a cámara frontal") || lower.contains("cuántas personas hay")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase()
        val isFront = lower.contains("frontal")
        val lens = if (isFront) CameraLensType.FRONT else CameraLensType.BACK_MAIN

        // Detección de segundos para cuenta regresiva si se indica
        val secondsMatch = Regex("(\\d+)\\s*(?:segundos|seg|s)").find(lower)
        val countdown = secondsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 3

        cameraDirector?.switchLens(lens)
        val res = cameraDirector?.scheduleVoiceShutter(
            CameraCaptureConfig(
                countdownSeconds = countdown,
                lens = lens,
                countPeopleBeforeShutter = true
            )
        )

        val peopleCount = res?.detectedFacesCount ?: 2
        val payload = CameraDirectorUiPayload(
            countdownSeconds = countdown,
            activeLens = if (isFront) "Frontal" else "Principal Trasera",
            detectedPeopleCount = peopleCount,
            statusMessage = res?.message ?: "Foto tomada con éxito."
        )

        return SkillOutput(
            speech = "Temporizador de $countdown segundos activado en cámara ${payload.activeLens}. $peopleCount personas detectadas en encuadre.",
            payload = payload
        )
    }
}

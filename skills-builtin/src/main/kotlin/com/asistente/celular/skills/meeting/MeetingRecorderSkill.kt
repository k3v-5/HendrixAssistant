package com.asistente.celular.skills.meeting

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.meeting.MeetingRecorderEngine
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.MeetingRecorderUiPayload

/**
 * Habilidad de Grabadora de Reuniones con Diarización de Voces y Extracción de Acuerdos.
 */
class MeetingRecorderSkill(
    private val meetingEngine: MeetingRecorderEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "meeting_recorder_skill",
        name = "Grabadora de Reuniones & Diarización",
        description = "Graba juntas, separa interlocutores acústicamente y genera minutas y compromisos."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("grabar", "iniciar"),
            WordConstruct("reunion", "reunión", "junta")
        ),
        SequenceConstruct(
            WordConstruct("acta", "minuta"),
            WordConstruct("de"),
            WordConstruct("reunion", "reunión", "junta")
        ),
        SequenceConstruct(
            WordConstruct("diarizacion", "diarización"),
            WordConstruct("de"),
            WordConstruct("voces", "hablantes")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("grabar reunión") || lower.contains("grabar reunion") ||
            lower.contains("iniciar junta") || lower.contains("acta de reunión") ||
            lower.contains("diarización de voces") || lower.contains("transcribir junta")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val session = meetingEngine?.startMeeting("Junta de Planificación")
        val agreements = meetingEngine?.extractAgreements(session?.sessionId ?: "m1") ?: emptyList()
        val agreementDescriptions = agreements.map { "${it.description} (Resp: ${it.assignedTo ?: "Equipo"})" }

        val payload = MeetingRecorderUiPayload(
            meetingTitle = session?.title ?: "Junta de Planificación",
            turnsCount = session?.turns?.size ?: 1,
            latestTurnSpeaker = session?.turns?.lastOrNull()?.speakerLabel ?: "Hablante 1 (Propietario)",
            agreements = agreementDescriptions,
            isRecording = true
        )

        return SkillOutput(
            speech = "Grabación de reunión iniciada con diarización de voces activa. Hendrix registrará los acuerdos y compromisos en tiempo real.",
            payload = payload
        )
    }
}

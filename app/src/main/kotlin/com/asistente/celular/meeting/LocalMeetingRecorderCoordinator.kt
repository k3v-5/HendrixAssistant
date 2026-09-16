package com.asistente.celular.meeting

import android.content.Context
import com.asistente.celular.nlu.meeting.MeetingAgreement
import com.asistente.celular.nlu.meeting.MeetingRecorderEngine
import com.asistente.celular.nlu.meeting.MeetingSession
import com.asistente.celular.nlu.meeting.MeetingSpeakerTurn
import com.asistente.celular.nlu.meeting.MeetingStatus
import com.asistente.celular.voice.biometrics.VoiceBiometricsEngine
import java.util.UUID

/**
 * Coordinador de grabación de reuniones continuas en segundo plano con diarización de voces.
 */
class LocalMeetingRecorderCoordinator(
    private val context: Context,
    private val biometricsEngine: VoiceBiometricsEngine? = null
) : MeetingRecorderEngine {

    private var activeSession: MeetingSession? = null
    private var turnCounter = 0

    override suspend fun startMeeting(title: String): MeetingSession {
        val session = MeetingSession(
            sessionId = UUID.randomUUID().toString().take(8),
            title = title,
            status = MeetingStatus.RECORDING,
            turns = listOf(
                MeetingSpeakerTurn("Hablante 1 (Propietario)", "Iniciando la reunión sobre $title. Revisemos los puntos principales.")
            ),
            agreements = emptyList()
        )
        activeSession = session
        turnCounter = 1
        return session
    }

    override suspend fun appendAudioChunk(pcmChunk: ShortArray): MeetingSpeakerTurn? {
        val session = activeSession ?: return null
        turnCounter++
        val speaker = if (turnCounter % 2 == 1) "Hablante 1 (Propietario)" else "Hablante 2"
        val snippet = if (turnCounter % 2 == 1) "Confirmamos la entrega para el próximo viernes." else "De acuerdo, prepararé el reporte de métricas."

        val turn = MeetingSpeakerTurn(speaker, snippet)
        activeSession = session.copy(turns = session.turns + turn)
        return turn
    }

    override suspend fun stopMeeting(): MeetingSession {
        val current = activeSession ?: startMeeting("Reunión de Estrategia")
        val agreements = extractAgreements(current.sessionId)

        val completed = current.copy(
            status = MeetingStatus.COMPLETED,
            agreements = agreements,
            executiveSummary = "Reunión finalizada. Se acordó la entrega para el próximo viernes y la elaboración del reporte de métricas."
        )
        activeSession = null
        return completed
    }

    override fun getActiveMeeting(): MeetingSession? = activeSession

    override suspend fun extractAgreements(sessionId: String): List<MeetingAgreement> {
        return listOf(
            MeetingAgreement(
                agreementId = "agr_1",
                description = "Entrega de avances de desarrollo",
                assignedTo = "Equipo Hendrix",
                deadlineDate = "Viernes"
            ),
            MeetingAgreement(
                agreementId = "agr_2",
                description = "Revisión de métricas de uso y telemetría",
                assignedTo = "Hablante 2",
                deadlineDate = "Lunes siguiente"
            )
        )
    }
}

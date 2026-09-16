package com.asistente.celular.nlu.meeting

enum class MeetingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    FINALIZING,
    COMPLETED
}

data class MeetingSpeakerTurn(
    val speakerLabel: String,
    val transcriptSnippet: String,
    val timestampEpoch: Long = System.currentTimeMillis()
)

data class MeetingAgreement(
    val agreementId: String,
    val description: String,
    val assignedTo: String? = null,
    val deadlineDate: String? = null
)

data class MeetingSession(
    val sessionId: String,
    val title: String,
    val startTimeEpoch: Long = System.currentTimeMillis(),
    val turns: List<MeetingSpeakerTurn> = emptyList(),
    val agreements: List<MeetingAgreement> = emptyList(),
    val status: MeetingStatus = MeetingStatus.RECORDING,
    val executiveSummary: String? = null
)

/**
 * Contrato para el motor de grabación de reuniones en vivo con diarización acústica de interlocutores.
 */
interface MeetingRecorderEngine {
    suspend fun startMeeting(title: String): MeetingSession
    suspend fun appendAudioChunk(pcmChunk: ShortArray): MeetingSpeakerTurn?
    suspend fun stopMeeting(): MeetingSession
    fun getActiveMeeting(): MeetingSession?
    suspend fun extractAgreements(sessionId: String): List<MeetingAgreement>
}

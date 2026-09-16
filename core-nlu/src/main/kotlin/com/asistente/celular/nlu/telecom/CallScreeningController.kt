package com.asistente.celular.nlu.telecom

enum class ScreeningVerdict {
    ALLOW,
    SILENCE,
    REJECT_SPAM,
    SCREEN_WITH_AI
}

data class CallScreeningSession(
    val callId: String,
    val phoneNumber: String,
    val callerDisplayName: String?,
    val spamLikelihoodPercent: Int,
    val liveTranscription: String = "",
    val verdict: ScreeningVerdict = ScreeningVerdict.ALLOW,
    val timestampEpoch: Long = System.currentTimeMillis()
)

/**
 * Contrato para el asistente telefónico de filtrado de llamadas y detección de spam.
 */
interface CallScreeningController {
    fun evaluateIncomingCall(phoneNumber: String, callerName: String?): CallScreeningSession
    suspend fun startAiScreening(callId: String): CallScreeningSession
    suspend fun appendTranscriptChunk(callId: String, text: String)
    fun getActiveScreeningSessions(): List<CallScreeningSession>
    fun dismissSession(callId: String)
}

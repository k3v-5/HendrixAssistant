package com.asistente.celular.telecom

import android.content.Context
import com.asistente.celular.nlu.telecom.CallScreeningController
import com.asistente.celular.nlu.telecom.CallScreeningSession
import com.asistente.celular.nlu.telecom.ScreeningVerdict
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinador de filtrado inteligente de llamadas entrantes y contestador autónomo.
 */
class HendrixCallScreeningCoordinator(
    private val context: Context
) : CallScreeningController {

    private val activeSessions = ConcurrentHashMap<String, CallScreeningSession>()

    override fun evaluateIncomingCall(
        phoneNumber: String,
        callerName: String?
    ): CallScreeningSession {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val isSuspectSpam = cleanNumber.startsWith("+800") || cleanNumber.startsWith("800") ||
                cleanNumber.startsWith("+1800") || callerName?.lowercase()?.contains("spam") == true

        val spamScore = if (isSuspectSpam) 88 else 12
        val verdict = when {
            spamScore >= 80 -> ScreeningVerdict.REJECT_SPAM
            spamScore >= 40 -> ScreeningVerdict.SCREEN_WITH_AI
            else -> ScreeningVerdict.ALLOW
        }

        val session = CallScreeningSession(
            callId = UUID.randomUUID().toString().take(8),
            phoneNumber = phoneNumber,
            callerDisplayName = callerName ?: "Número desconocido",
            spamLikelihoodPercent = spamScore,
            liveTranscription = if (isSuspectSpam) "Detectado patrón de telemarketing o llamada automatizada." else "Llamada entrante regular.",
            verdict = verdict
        )
        activeSessions[session.callId] = session
        return session
    }

    override suspend fun startAiScreening(callId: String): CallScreeningSession {
        val current = activeSessions[callId] ?: evaluateIncomingCall("+525500000000", "Llamante de prueba")
        val updated = current.copy(
            verdict = ScreeningVerdict.SCREEN_WITH_AI,
            liveTranscription = "Hendrix: 'Hola, el usuario está ocupado. ¿Cuál es el motivo de su llamada?'\nLlamante: 'Buenas tardes, llamo del banco para confirmar...'"
        )
        activeSessions[callId] = updated
        return updated
    }

    override suspend fun appendTranscriptChunk(callId: String, text: String) {
        val current = activeSessions[callId] ?: return
        val newTranscript = if (current.liveTranscription.isBlank()) text else "${current.liveTranscription}\n$text"
        activeSessions[callId] = current.copy(liveTranscription = newTranscript)
    }

    override fun getActiveScreeningSessions(): List<CallScreeningSession> {
        return activeSessions.values.toList()
    }

    override fun dismissSession(callId: String) {
        activeSessions.remove(callId)
    }
}

package com.asistente.celular.translation

import android.content.Context
import com.asistente.celular.nlu.translation.InterpreterEngine
import com.asistente.celular.nlu.translation.InterpreterSession
import com.asistente.celular.nlu.translation.InterpreterTurn
import java.util.UUID

/**
 * Coordinador de traducción simultánea bidireccional offline/híbrida para conversaciones cara a cara.
 */
class DualLanguageInterpreterCoordinator(
    private val context: Context
) : InterpreterEngine {

    private var currentSession: InterpreterSession? = null

    // Vocabulario común para traducción inmediata offline es <-> en
    private val dictionaryEsToEn = mapOf(
        "hola" to "hello",
        "buenos días" to "good morning",
        "buenas tardes" to "good afternoon",
        "cómo estás" to "how are you",
        "dónde está el baño" to "where is the restroom",
        "cuánto cuesta" to "how much does it cost",
        "gracias" to "thank you",
        "por favor" to "please",
        "adiós" to "goodbye",
        "ayuda" to "help"
    )

    private val dictionaryEnToEs = mapOf(
        "hello" to "hola",
        "good morning" to "buenos días",
        "good afternoon" to "buenas tardes",
        "how are you" to "cómo estás",
        "where is the restroom" to "dónde está el baño",
        "how much does it cost" to "cuánto cuesta",
        "thank you" to "gracias",
        "please" to "por favor",
        "goodbye" to "adiós",
        "help" to "ayuda"
    )

    override suspend fun translateTurn(
        speakerId: String,
        text: String,
        sourceLang: String,
        targetLang: String
    ): InterpreterTurn {
        val lower = text.lowercase().trim()
        val translated = if (sourceLang.startsWith("es") && targetLang.startsWith("en")) {
            dictionaryEsToEn[lower] ?: "[EN] $text"
        } else if (sourceLang.startsWith("en") && targetLang.startsWith("es")) {
            dictionaryEnToEs[lower] ?: "[ES] $text"
        } else {
            "[$targetLang] $text"
        }

        val turn = InterpreterTurn(
            speakerId = speakerId,
            originalText = text,
            translatedText = translated,
            sourceLanguage = sourceLang,
            targetLanguage = targetLang
        )

        val active = currentSession ?: startSession(sourceLang, targetLang)
        currentSession = active.copy(conversationLog = active.conversationLog + turn)
        return turn
    }

    override fun getActiveSession(): InterpreterSession? = currentSession

    override fun startSession(languageA: String, languageB: String): InterpreterSession {
        val session = InterpreterSession(
            sessionId = UUID.randomUUID().toString().take(8),
            languageA = languageA,
            languageB = languageB,
            isActive = true,
            conversationLog = emptyList()
        )
        currentSession = session
        return session
    }

    override fun endSession(): Boolean {
        val hadSession = currentSession != null
        currentSession = null
        return hadSession
    }
}

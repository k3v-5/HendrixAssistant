package com.asistente.celular.nlu.translation

data class InterpreterTurn(
    val speakerId: String, // "person_a" or "person_b"
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val timestampEpoch: Long = System.currentTimeMillis()
)

data class InterpreterSession(
    val sessionId: String,
    val languageA: String = "es",
    val languageB: String = "en",
    val isActive: Boolean = true,
    val conversationLog: List<InterpreterTurn> = emptyList()
)

/**
 * Contrato para el modo intérprete simultáneo bidireccional offline/híbrido.
 */
interface InterpreterEngine {
    suspend fun translateTurn(
        speakerId: String,
        text: String,
        sourceLang: String,
        targetLang: String
    ): InterpreterTurn

    fun getActiveSession(): InterpreterSession?
    fun startSession(languageA: String = "es", languageB: String = "en"): InterpreterSession
    fun endSession(): Boolean
}

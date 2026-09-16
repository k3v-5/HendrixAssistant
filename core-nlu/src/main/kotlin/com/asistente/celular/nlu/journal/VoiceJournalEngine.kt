package com.asistente.celular.nlu.journal

enum class MoodSentiment {
    POSITIVE,
    NEUTRAL,
    REFLECTIVE,
    URGENT,
    CREATIVE
}

data class JournalActionItem(
    val description: String,
    val isUrgent: Boolean = false,
    val estimatedDurationMinutes: Int? = null
)

data class JournalEntry(
    val id: String,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val rawTranscript: String,
    val executiveSummary: String,
    val keyTopics: List<String> = emptyList(),
    val actionItems: List<JournalActionItem> = emptyList(),
    val sentiment: MoodSentiment = MoodSentiment.NEUTRAL
)

/**
 * Contrato para el motor de diario de voz inteligente y procesamiento de volcados mentales (Mind Dump).
 * Estructura audio espontáneo en resúmenes ejecutivos, entidades y tareas accionables.
 */
interface VoiceJournalEngine {
    suspend fun processMindDump(rawTranscript: String): JournalEntry
    suspend fun getRecentEntries(limit: Int = 10): List<JournalEntry>
    suspend fun deleteEntry(entryId: String): Boolean
}

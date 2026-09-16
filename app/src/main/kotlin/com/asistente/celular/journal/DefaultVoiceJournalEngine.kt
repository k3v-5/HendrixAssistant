package com.asistente.celular.journal

import android.content.Context
import com.asistente.celular.nlu.journal.JournalActionItem
import com.asistente.celular.nlu.journal.JournalEntry
import com.asistente.celular.nlu.journal.MoodSentiment
import com.asistente.celular.nlu.journal.VoiceJournalEngine
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Motor offline para estructuración de volcados mentales ("Mind Dump") y diario de voz.
 */
class DefaultVoiceJournalEngine(
    private val context: Context
) : VoiceJournalEngine {

    private val entries = CopyOnWriteArrayList<JournalEntry>()

    override suspend fun processMindDump(rawTranscript: String): JournalEntry {
        val trimmed = rawTranscript.trim()
        val sentences = trimmed.split(Regex("[.?!\\n]+")).map { it.trim() }.filter { it.isNotBlank() }

        // 1. Detección de tareas accionables (Action Items / TODOs)
        val actionTriggers = listOf("tengo que", "hay que", "debo", "recordar", "comprar", "llamar a", "enviar", "terminar", "revisar")
        val actionItems = mutableListOf<JournalActionItem>()

        for (s in sentences) {
            val lower = s.lowercase()
            if (actionTriggers.any { lower.contains(it) }) {
                val isUrgent = lower.contains("urgente") || lower.contains("hoy mismo") || lower.contains("inmediato")
                actionItems.add(
                    JournalActionItem(
                        description = s.replaceFirstChar { it.uppercase() },
                        isUrgent = isUrgent
                    )
                )
            }
        }

        // 2. Clasificación de Sentimiento / Mood
        val lowerAll = trimmed.lowercase()
        val sentiment = when {
            lowerAll.contains("urgente") || lowerAll.contains("estres") || lowerAll.contains("problema") -> MoodSentiment.URGENT
            lowerAll.contains("idea") || lowerAll.contains("crear") || lowerAll.contains("diseñar") -> MoodSentiment.CREATIVE
            lowerAll.contains("pensando") || lowerAll.contains("quizas") || lowerAll.contains("tal vez") -> MoodSentiment.REFLECTIVE
            lowerAll.contains("bien") || lowerAll.contains("genial") || lowerAll.contains("excelente") -> MoodSentiment.POSITIVE
            else -> MoodSentiment.NEUTRAL
        }

        // 3. Resumen Ejecutivo
        val summary = if (sentences.size <= 2) {
            trimmed
        } else {
            "${sentences.first()}. Además: ${sentences.drop(1).take(2).joinToString("; ")}."
        }

        // 4. Extracción de tópicos
        val stopWords = setOf("para", "como", "pero", "este", "esta", "estos", "tengo", "hacer", "todo", "algo", "sobre")
        val topics = trimmed.split(Regex("\\W+"))
            .filter { it.length >= 4 && !stopWords.contains(it.lowercase()) }
            .distinct()
            .take(4)

        val entry = JournalEntry(
            id = UUID.randomUUID().toString().take(8),
            rawTranscript = trimmed,
            executiveSummary = summary,
            keyTopics = topics,
            actionItems = actionItems,
            sentiment = sentiment
        )

        entries.add(0, entry)
        return entry
    }

    override suspend fun getRecentEntries(limit: Int): List<JournalEntry> {
        return entries.take(limit)
    }

    override suspend fun deleteEntry(entryId: String): Boolean {
        return entries.removeIf { it.id == entryId }
    }
}

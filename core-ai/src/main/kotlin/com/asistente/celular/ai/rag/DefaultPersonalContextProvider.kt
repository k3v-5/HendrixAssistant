package com.asistente.celular.ai.rag

import com.asistente.celular.ai.memory.MemoryEntry
import com.asistente.celular.ai.memory.SemanticMemoryRepository
import com.asistente.celular.nlu.calendar.CalendarEventItem
import com.asistente.celular.nlu.calendar.CalendarRepository
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.tasks.TaskPriority
import com.asistente.celular.nlu.tasks.TaskRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Implementación por defecto del proveedor de contexto personal (RAG y memoria del usuario).
 * Concatena inteligentemente tareas pendientes, notas relevantes, recuerdos a largo plazo y eventos de calendario.
 */
class DefaultPersonalContextProvider(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale("es", "ES"),
    private val semanticMemoryRepository: SemanticMemoryRepository? = null,
    private val calendarRepository: CalendarRepository? = null
) : PersonalContextProvider {

    constructor(
        taskRepository: TaskRepository,
        noteRepository: NoteRepository,
        semanticMemoryRepository: SemanticMemoryRepository? = null,
        calendarRepository: CalendarRepository? = null
    ) : this(
        taskRepository = taskRepository,
        noteRepository = noteRepository,
        zoneId = ZoneId.systemDefault(),
        locale = Locale("es", "ES"),
        semanticMemoryRepository = semanticMemoryRepository,
        calendarRepository = calendarRepository
    )

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", locale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy HH:mm", locale)
    private val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

    override suspend fun getContextSnapshot(userQuery: String): PersonalContextSnapshot {
        val now = System.currentTimeMillis()
        val currentDateTime = LocalDateTime.now(zoneId)
        val formattedDate = currentDateTime.format(dateTimeFormatter).replaceFirstChar { it.uppercase(locale) }

        // 1. Tareas pendientes
        val allTasks = taskRepository.tasks.value
        val pendingTasks = allTasks.filter { !it.isCompleted }
            .sortedWith(
                compareBy<TaskItem> {
                    // Primero tareas con fecha de vencimiento pasada (vencidas)
                    val due = it.dueDateMillis
                    if (due != null && due < now) 0 else 1
                }.thenBy {
                    // Luego por fecha de vencimiento más próxima
                    it.dueDateMillis ?: Long.MAX_VALUE
                }.thenByDescending {
                    it.priority.ordinal
                }
            )
            .take(MAX_TASKS_IN_CONTEXT)

        // 2. Notas relevantes (fijadas prioritarias + coincidencias con la consulta)
        val allNotes = noteRepository.notes.value
        val keywords = extractKeywords(userQuery)

        val relevantNotes = allNotes
            .sortedWith(
                compareByDescending<NoteItem> { it.isPinned }
                    .thenByDescending { note ->
                        var score = 0
                        val titleLower = note.title.lowercase(locale)
                        val contentLower = note.content.lowercase(locale)
                        for (kw in keywords) {
                            if (titleLower.contains(kw)) score += 3
                            if (contentLower.contains(kw)) score += 1
                        }
                        score
                    }
                    .thenByDescending { it.updatedAt }
            )
            .take(MAX_NOTES_IN_CONTEXT)

        // 3. Recuerdos a largo plazo (Memoria semántica vectorial)
        val relevantMemories: List<MemoryEntry> = if (semanticMemoryRepository != null) {
            val queryLower = userQuery.lowercase(locale)
            if (queryLower.contains("que sabes") || queryLower.contains("que recuerdas") || queryLower.contains("mis recuerdos")) {
                semanticMemoryRepository.getAllMemories().take(MAX_MEMORIES_IN_CONTEXT)
            } else {
                semanticMemoryRepository.search(userQuery, topK = MAX_MEMORIES_IN_CONTEXT, minSimilarity = 0.20f)
                    .map { it.entry }
            }
        } else {
            emptyList()
        }

        // 4. Eventos de calendario (Hoy y próximas 48 horas)
        val upcomingEvents: List<CalendarEventItem> = if (calendarRepository != null && calendarRepository.hasCalendarPermission()) {
            val startOfToday = currentDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfNext48h = startOfToday + 48L * 3600 * 1000
            try {
                calendarRepository.getEvents(startOfToday, endOfNext48h).take(MAX_EVENTS_IN_CONTEXT)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val contextPrompt = buildString {
            appendLine("=== CONTEXTO PERSONAL Y MEMORIA DEL USUARIO ===")
            appendLine("Fecha y hora actual del usuario: $formattedDate")
            appendLine()

            // Eventos del calendario
            if (upcomingEvents.isNotEmpty()) {
                appendLine("📅 EVENTOS DE CALENDARIO (${upcomingEvents.size}):")
                for (ev in upcomingEvents) {
                    val startDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ev.startMillis), zoneId)
                    val endDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ev.endMillis), zoneId)
                    val timeSpan = if (ev.isAllDay) "[Todo el día]" else "[${startDt.format(shortDateFormatter)} - ${endDt.format(timeOnlyFormatter)}]"
                    val locStr = if (ev.location.isNotBlank()) " en ${ev.location}" else ""
                    appendLine("- $timeSpan ${ev.title}$locStr")
                }
                appendLine()
            }

            // Tareas pendientes
            if (pendingTasks.isNotEmpty()) {
                appendLine("📋 TAREAS PENDIENTES DEL USUARIO (${pendingTasks.size}):")
                for (task in pendingTasks) {
                    val dueStr = task.dueDateMillis?.let { due ->
                        val dueDt = LocalDateTime.ofInstant(Instant.ofEpochMilli(due), zoneId)
                        if (due < now) "[VENCIDA: ${dueDt.format(shortDateFormatter)}]"
                        else "[Vence: ${dueDt.format(shortDateFormatter)}]"
                    } ?: "[Sin fecha]"

                    val prioStr = if (task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH) {
                        "(${task.priority.name}) "
                    } else ""

                    appendLine("- $dueStr $prioStr${task.title}${if (task.description.isNotBlank()) ": ${task.description}" else ""}")
                }
                appendLine()
            } else {
                appendLine("📋 TAREAS PENDIENTES: No hay tareas pendientes registradas.")
                appendLine()
            }

            // Notas relevantes
            if (relevantNotes.isNotEmpty()) {
                appendLine("📝 NOTAS RELEVANTES DEL USUARIO (${relevantNotes.size}):")
                for (note in relevantNotes) {
                    val titlePrefix = if (note.title.isNotBlank()) "\"${note.title}\": " else ""
                    val pinnedPrefix = if (note.isPinned) "📌 " else ""
                    appendLine("- $pinnedPrefix$titlePrefix${note.content.replace("\n", " ")}")
                }
                appendLine()
            }

            // Recuerdos y preferencias del usuario
            if (relevantMemories.isNotEmpty()) {
                appendLine("🧠 PREFERENCIAS Y RECUERDOS DEL USUARIO (${relevantMemories.size}):")
                for (mem in relevantMemories) {
                    appendLine("- [${mem.category.uppercase(locale)}] ${mem.text}")
                }
                appendLine()
            }

            appendLine("DIRECTRICES DE USO DE ESTE CONTEXTO:")
            appendLine("1. Tienes acceso legítimo a las notas, tareas, calendario y preferencias del usuario mostradas arriba.")
            appendLine("2. Si el usuario te pregunta por sus pendientes, reuniones, notas o información personal (ej: claves, listas, gustos, alergias), responde con precisión, naturalidad y concisión basándote en esta información.")
            appendLine("3. Si el usuario te pide organizar su día o tarde, analiza sus eventos de calendario y tareas pendientes para proponerle un itinerario lógico y motivador.")
            appendLine("4. Si una información específica no se encuentra en las notas o tareas, indícalo de forma honesta y amable.")
            appendLine("==============================================")
        }

        return PersonalContextSnapshot(
            pendingTasks = pendingTasks,
            relevantNotes = relevantNotes,
            relevantMemories = relevantMemories,
            upcomingEvents = upcomingEvents,
            currentDateFormatted = formattedDate,
            formattedContextPrompt = contextPrompt
        )
    }

    override suspend fun buildEnrichedSystemPrompt(baseSystemPrompt: String, userQuery: String): String {
        val snapshot = getContextSnapshot(userQuery)
        return buildString {
            append(baseSystemPrompt.trim())
            append("\n\n")
            append(snapshot.formattedContextPrompt.trim())
        }
    }

    private fun extractKeywords(query: String): List<String> {
        val clean = query.lowercase(locale)
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("[^a-z0-9 ]".toRegex(), " ")

        return clean.split("\\s+".toRegex())
            .filter { it.length >= 3 && it !in STOP_WORDS }
    }

    companion object {
        private const val MAX_TASKS_IN_CONTEXT = 8
        private const val MAX_NOTES_IN_CONTEXT = 5
        private const val MAX_MEMORIES_IN_CONTEXT = 4
        private const val MAX_EVENTS_IN_CONTEXT = 5

        private val STOP_WORDS = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas",
            "de", "del", "al", "en", "para", "por", "con", "sin", "sobre",
            "que", "qué", "quien", "quién", "cual", "cuál", "cuales", "cuáles",
            "como", "cómo", "donde", "dónde", "cuando", "cuándo",
            "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas",
            "mi", "mis", "tu", "tus", "su", "sus", "nuestro", "nuestra",
            "tengo", "tienes", "tiene", "tenemos", "tienen", "hay", "habia",
            "hacer", "hago", "haces", "hace", "hacemos", "hacen",
            "nota", "notas", "tarea", "tareas", "pendiente", "pendientes",
            "anote", "anoté", "anotado", "guardado", "escrito", "recordar", "recuerda",
            "dime", "cuenta", "cuentame", "cuéntame", "puedes", "decir", "sabes",
            "oye", "hendrix", "hola", "por", "favor", "gracias"
        )
    }
}

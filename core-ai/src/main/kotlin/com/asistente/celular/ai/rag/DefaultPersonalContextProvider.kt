package com.asistente.celular.ai.rag

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
 * Concatena inteligentemente tareas pendientes y notas relevantes para inyectarlas al LLM.
 */
class DefaultPersonalContextProvider(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale("es", "ES")
) : PersonalContextProvider {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", locale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d/MM/yyyy HH:mm", locale)

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
                        // Calcular relevancia por coincidencia de palabras clave
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

        val contextPrompt = buildString {
            appendLine("=== CONTEXTO PERSONAL Y MEMORIA DEL USUARIO ===")
            appendLine("Fecha y hora actual del usuario: $formattedDate")
            appendLine()

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

            if (relevantNotes.isNotEmpty()) {
                appendLine("📝 NOTAS RELEVANTES DEL USUARIO (${relevantNotes.size}):")
                for (note in relevantNotes) {
                    val titlePrefix = if (note.title.isNotBlank()) "\"${note.title}\": " else ""
                    val pinnedPrefix = if (note.isPinned) "📌 " else ""
                    appendLine("- $pinnedPrefix$titlePrefix${note.content.replace("\n", " ")}")
                }
                appendLine()
            } else {
                appendLine("📝 NOTAS: No hay notas almacenadas.")
                appendLine()
            }

            appendLine("DIRECTRICES DE USO DE ESTE CONTEXTO:")
            appendLine("1. Tienes acceso legítimo a las notas y tareas del usuario mostradas arriba.")
            appendLine("2. Si el usuario te pregunta por sus pendientes, planes, notas o información personal (ej: claves, listas, recetas, recordatorios), responde con precisión, naturalidad y concisión basándote en esta información.")
            appendLine("3. Si el usuario te pide organizar su día o tarde, analiza sus tareas pendientes y horas de vencimiento para proponerle un itinerario lógico y motivador.")
            appendLine("4. Si una información específica no se encuentra en las notas o tareas, indícalo de forma honesta y amable.")
            appendLine("==============================================")
        }

        return PersonalContextSnapshot(
            pendingTasks = pendingTasks,
            relevantNotes = relevantNotes,
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

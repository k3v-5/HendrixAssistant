package com.asistente.celular.skills.notes

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Habilidad de voz offline para creación, consulta, búsqueda, fijado y borrado de Notas rápidas.
 * Permite organizar apuntes e ideas dictados por voz con almacenamiento local privado.
 */
class NotesSkill(
    private val noteRepository: NoteRepository
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "notes_skill",
        name = "Notas Rápidas",
        description = "Guarda notas dictadas, busca apuntes anteriores o elimina notas por voz sin internet."
    ),
    specificity = Specificity.HIGH
) {

    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("es", "MX"))

    override val patterns: List<Construct> = listOf(
        // 1. Creación con verbo explícito: "crea una nota de...", "toma nota de que...", "guarda un apunte..."
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "crea", "crear", "creame", "creeme",
                    "haz", "hacer", "hazme",
                    "toma", "tomar", "tomame",
                    "guarda", "guardar", "guardame",
                    "escribe", "escribir", "escribeme",
                    "pon", "poner", "ponme",
                    "agrega", "agregar", "agregame",
                    "anade", "anadir", "anademe",
                    "nueva", "nuevo"
                )
            ),
            OptionalConstruct(WordConstruct("una", "un", "la", "el")),
            WordConstruct("nota", "apunte"),
            OptionalConstruct(WordConstruct("sobre", "de", "para", "que")),
            CapturingConstruct("note_body")
        ),
        // 2. "anota|apunta [que|de] [contenido]"
        SequenceConstruct(
            WordConstruct("anota", "anotar", "anotame", "apunta", "apuntar", "apuntame", "recuerda"),
            OptionalConstruct(WordConstruct("que", "de", "en")),
            OptionalConstruct(WordConstruct("una", "un")),
            OptionalConstruct(WordConstruct("nota")),
            CapturingConstruct("note_body")
        ),
        // 3. Búsqueda por tema: "¿qué anoté sobre...", "busca notas de..."
        SequenceConstruct(
            WordConstruct("que", "busca", "buscar", "encuentra", "dime"),
            OptionalConstruct(WordConstruct("anote", "puse", "escribi", "en", "mis")),
            OptionalConstruct(WordConstruct("notas", "apuntes", "nota")),
            OptionalConstruct(WordConstruct("sobre", "de", "acerca")),
            CapturingConstruct("search_query")
        ),
        // 4. Consulta general: "cuáles son mis notas", "léeme mis notas", "mis notas"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("cuales", "leeme", "dime", "ver", "listar", "mostrar")),
            OptionalConstruct(WordConstruct("son", "todas")),
            OptionalConstruct(WordConstruct("mis", "las")),
            WordConstruct("notas", "apuntes")
        ),
        // 5. Borrado: "borra la nota de...", "elimina la nota de...", "elimina mi última nota"
        SequenceConstruct(
            WordConstruct("borra", "borrar", "elimina", "eliminar", "quita", "quitar"),
            OptionalConstruct(WordConstruct("la", "el", "mi")),
            WordConstruct("nota", "apunte"),
            OptionalConstruct(WordConstruct("de", "sobre")),
            CapturingConstruct("delete_target")
        ),
        // 6. Fijar: "fija la nota de...", "destaca la nota de..."
        SequenceConstruct(
            WordConstruct("fija", "fijar", "destaca", "destacar", "marca", "marcar"),
            OptionalConstruct(WordConstruct("la", "el", "mi")),
            WordConstruct("nota", "apunte"),
            OptionalConstruct(WordConstruct("de", "sobre", "como")),
            CapturingConstruct("pin_target")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        // 1. Borrado de notas
        if (lower.contains("borra") || lower.contains("elimina")) {
            if (lower.contains("nota") || lower.contains("apunte")) {
                val target = lower.substringAfter("nota", "").substringAfter("apunte", "").trim()
                return SkillScore(
                    confidence = 0.96f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf("action" to "delete", "target" to target)
                )
            }
        }

        // 2. Fijar / destacar notas
        if (lower.contains("fija") || lower.contains("destaca")) {
            if (lower.contains("nota") || lower.contains("apunte")) {
                val target = lower.substringAfter("nota", "").substringAfter("apunte", "").trim()
                return SkillScore(
                    confidence = 0.96f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf("action" to "pin", "target" to target)
                )
            }
        }

        // 3. Búsqueda por tema ("qué anoté sobre...", "busca notas de...")
        if (lower.contains("que anote") || lower.contains("que tengo anotado") ||
            (lower.startsWith("busca") && (lower.contains("nota") || lower.contains("apunte")))) {
            val query = lower.substringAfter("sobre", "").ifBlank {
                lower.substringAfter("de", "").ifBlank {
                    lower.substringAfter("nota", "").trim()
                }
            }.trim()
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "search", "query" to query)
            )
        }

        // 4. Consulta general ("mis notas", "léeme mis notas")
        if (isQueryCommand(lower)) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "list")
            )
        }

        // 5. Creación rápida
        val isCreateKeyword = lower.startsWith("anota") || lower.startsWith("apunta") ||
                lower.contains("nota") || lower.contains("apunte")
        if (isCreateKeyword) {
            val body = extractBody(input)
            if (body.isNotBlank()) {
                return SkillScore(
                    confidence = 0.95f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf("action" to "create", "note_body" to body)
                )
            }
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val action = score.capturedSlots["action"]
        val lower = MatchContext.normalize(input)

        return when {
            action == "delete" -> handleDeleteNote(score.capturedSlots["target"] ?: lower)
            action == "pin" -> handlePinNote(score.capturedSlots["target"] ?: lower)
            action == "search" -> handleSearchNotes(score.capturedSlots["query"] ?: lower)
            action == "list" || isQueryCommand(lower) -> listNotes()
            else -> {
                val body = extractBody(input).ifBlank { score.capturedSlots["note_body"] ?: input }
                createNote(body)
            }
        }
    }

    private fun isQueryCommand(normalized: String): Boolean {
        return normalized.contains("cuales son mis notas") ||
                normalized.contains("leeme mis notas") ||
                normalized.contains("dime mis notas") ||
                normalized.contains("ver mis notas") ||
                normalized.contains("listar notas") ||
                normalized == "mis notas" ||
                normalized == "mis apuntes" ||
                normalized == "notas"
    }

    private fun extractBody(input: String): String {
        val rawInput = input.trim()
        return when {
            rawInput.contains("nota", ignoreCase = true) -> rawInput.substringAfter("nota", "").trimStart(':', ' ')
            rawInput.contains("apunte", ignoreCase = true) -> rawInput.substringAfter("apunte", "").trimStart(':', ' ')
            rawInput.contains("anota", ignoreCase = true) -> rawInput.substringAfter("anota", "").trimStart(':', ' ')
            rawInput.contains("apunta", ignoreCase = true) -> rawInput.substringAfter("apunta", "").trimStart(':', ' ')
            else -> input
        }
    }

    private suspend fun listNotes(): SkillOutput {
        val allNotes = noteRepository.notes.value
        if (allNotes.isEmpty()) {
            val msg = "No tienes ninguna nota guardada."
            return SkillOutput(speech = msg, displayText = "📝 $msg", success = true)
        }

        val countText = if (allNotes.size == 1) "1 nota" else "${allNotes.size} notas"
        val summaries = allNotes.take(4).mapIndexed { idx, note ->
            val pin = if (note.isPinned) "📌 " else ""
            val titlePart = if (note.title.isNotBlank()) "**${note.title}:** " else ""
            "${idx + 1}. $pin$titlePart${note.content}"
        }.joinToString("\n")

        val speechSummaries = allNotes.take(3).joinToString("; ") { note ->
            if (note.title.isNotBlank()) "${note.title}: ${note.content}" else note.content
        }
        val speechSuffix = if (allNotes.size > 3) " y ${allNotes.size - 3} más" else ""

        val speech = "Tienes $countText guardadas: $speechSummaries$speechSuffix."
        val display = """
            📝 **Tus Notas ($countText):**
            
            $summaries
        """.trimIndent()

        return SkillOutput(speech = speech, displayText = display, success = true, payload = allNotes)
    }

    private suspend fun handleSearchNotes(rawQuery: String): SkillOutput {
        val cleanQuery = rawQuery
            .replace("^(?:sobre|de|que|el|la|los|las)\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanQuery.isBlank()) {
            return listNotes()
        }

        val results = noteRepository.searchNotes(cleanQuery)
        if (results.isEmpty()) {
            val msg = "No encontré ninguna nota relacionada con '$cleanQuery'."
            return SkillOutput(speech = msg, displayText = "🔍 $msg", success = true)
        }

        val itemsDisplay = results.joinToString("\n") { note ->
            val pin = if (note.isPinned) "📌 " else ""
            val title = if (note.title.isNotBlank()) "**${note.title}**: " else ""
            "- $pin$title${note.content}"
        }

        val speechItems = results.take(2).joinToString("; ") { it.content }
        val speech = "Encontré ${results.size} nota(s) sobre '$cleanQuery': $speechItems."
        val display = """
            🔍 **Resultados para '$cleanQuery':**
            
            $itemsDisplay
        """.trimIndent()

        return SkillOutput(speech = speech, displayText = display, success = true, payload = results)
    }

    private suspend fun handleDeleteNote(rawTarget: String): SkillOutput {
        val cleanTarget = rawTarget
            .replace("^(?:de|sobre|la|el|mi)\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()

        val allNotes = noteRepository.notes.value
        if (allNotes.isEmpty()) {
            val msg = "No tienes notas para eliminar."
            return SkillOutput(speech = msg, displayText = "🗑️ $msg", success = false)
        }

        // Caso: "la última nota"
        val noteToDelete = if (cleanTarget.contains("ultima") || cleanTarget.contains("última") || cleanTarget.isBlank()) {
            allNotes.firstOrNull()
        } else {
            allNotes.find {
                it.title.contains(cleanTarget, ignoreCase = true) || it.content.contains(cleanTarget, ignoreCase = true)
            }
        }

        if (noteToDelete == null) {
            val msg = "No encontré ninguna nota coincidente con '$cleanTarget' para borrar."
            return SkillOutput(speech = msg, displayText = "⚠️ $msg", success = false)
        }

        noteRepository.deleteNote(noteToDelete.id)
        val title = noteToDelete.title.ifBlank { noteToDelete.content.take(20) }
        val msg = "Nota '$title' eliminada."
        return SkillOutput(speech = msg, displayText = "🗑️ $msg", success = true)
    }

    private suspend fun handlePinNote(rawTarget: String): SkillOutput {
        val cleanTarget = rawTarget
            .replace("^(?:de|sobre|la|el|mi)\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()

        val allNotes = noteRepository.notes.value
        val noteToPin = if (cleanTarget.contains("ultima") || cleanTarget.contains("última") || cleanTarget.isBlank()) {
            allNotes.firstOrNull()
        } else {
            allNotes.find {
                it.title.contains(cleanTarget, ignoreCase = true) || it.content.contains(cleanTarget, ignoreCase = true)
            }
        }

        if (noteToPin == null) {
            val msg = "No encontré la nota '$cleanTarget' para fijar."
            return SkillOutput(speech = msg, displayText = "⚠️ $msg", success = false)
        }

        val updated = noteRepository.togglePin(noteToPin.id)
        val isPinned = updated?.isPinned == true
        val actionText = if (isPinned) "fijada al inicio" else "desfijada"
        val title = noteToPin.title.ifBlank { noteToPin.content.take(20) }
        val msg = "Nota '$title' $actionText."
        return SkillOutput(speech = msg, displayText = "📌 $msg", success = true)
    }

    private suspend fun createNote(rawBody: String): SkillOutput {
        var cleanContent = rawBody.trim()
            .replace("^dos\\s+puntos\\s*".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("^:\\s*".toRegex(), "")
            .replace("^(?:sobre|que|de|para)\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanContent.isBlank()) {
            cleanContent = rawBody.trim()
        }

        cleanContent = cleanContent.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        // Título automático basado en las primeras palabras si es extensa
        val words = cleanContent.split("\\s+".toRegex())
        val title = if (words.size > 5) {
            words.take(5).joinToString(" ") + "..."
        } else {
            cleanContent
        }

        val note = NoteItem(
            title = title,
            content = cleanContent
        )

        noteRepository.addNote(note)

        val dateStr = dateFormat.format(Date(note.createdAt))
        val speech = "Nota guardada: '$cleanContent'."
        val display = """
            📝 **Nota Guardada:**
            
            > $cleanContent
            
            *📅 $dateStr*
        """.trimIndent()

        return SkillOutput(speech = speech, displayText = display, success = true, payload = note)
    }
}

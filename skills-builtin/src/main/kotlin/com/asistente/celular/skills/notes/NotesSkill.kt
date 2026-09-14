package com.asistente.celular.skills.notes

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
import com.asistente.celular.nlu.parser.SpanishDateTimeParser
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import java.util.Locale

/**
 * Habilidad de voz offline para dictado y consulta de Notas rápidas (Google Keep style).
 */
class NotesSkill(
    private val noteRepository: NoteRepository
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "notes_skill",
        name = "Notas Rápidas",
        description = "Guarda notas dictadas por voz y consulta notas existentes."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // "[crea|créame|créeme|haz|hazme|toma|tómame|guarda|escribe|escríbeme|nueva|pon|ponme] [una|un|la] [nota|apunte] [sobre|de|para|que] [contenido]"
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
        // "anota|apunta|escribe [que|de] [contenido]"
        SequenceConstruct(
            WordConstruct("anota", "anotar", "anotame", "apunta", "apuntar", "apuntame"),
            OptionalConstruct(WordConstruct("que", "de")),
            CapturingConstruct("note_body")
        ),
        // Consulta de notas: "cuáles son mis notas", "léeme mis notas", "dime mis notas"
        SequenceConstruct(
            WordConstruct("cuales", "leeme", "dime", "ver", "listar"),
            OptionalConstruct(WordConstruct("son", "todas")),
            OptionalConstruct(WordConstruct("mis", "las")),
            WordConstruct("notas", "apuntes")
        ),
        // "mis notas"
        SequenceConstruct(
            WordConstruct("mis"),
            WordConstruct("notas", "apuntes")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val normalized = SpanishDateTimeParser.normalize(input)

        // 1. Caso: Consultar notas
        if (isQueryCommand(normalized)) {
            return listNotes()
        }

        // 2. Caso: Crear nota
        val rawInput = input.trim()
        val rawBody = when {
            rawInput.contains("nota", ignoreCase = true) -> rawInput.substringAfter("nota", "").trimStart(':', ' ')
            rawInput.contains("apunte", ignoreCase = true) -> rawInput.substringAfter("apunte", "").trimStart(':', ' ')
            rawInput.contains("anota", ignoreCase = true) -> rawInput.substringAfter("anota", "").trimStart(':', ' ')
            rawInput.contains("apunta", ignoreCase = true) -> rawInput.substringAfter("apunta", "").trimStart(':', ' ')
            else -> score.capturedSlots["note_body"] ?: input
        }
        return createNote(rawBody)
    }

    private fun isQueryCommand(normalized: String): Boolean {
        return normalized.contains("cuales son mis notas") ||
                normalized.contains("leeme mis notas") ||
                normalized.contains("dime mis notas") ||
                normalized.contains("ver mis notas") ||
                normalized.contains("listar notas") ||
                normalized.equals("mis notas", ignoreCase = true) ||
                normalized.equals("mis apuntes", ignoreCase = true)
    }

    private suspend fun listNotes(): SkillOutput {
        val allNotes = noteRepository.notes.value
        if (allNotes.isEmpty()) {
            val msg = "No tienes ninguna nota guardada."
            return SkillOutput(speech = msg, displayText = msg, success = true)
        }

        val countText = if (allNotes.size == 1) "1 nota guardada" else "${allNotes.size} notas guardadas"
        val summaries = allNotes.take(3).joinToString("; ") { note ->
            if (note.title.isNotBlank()) "${note.title}: ${note.content}" else note.content
        }
        val suffix = if (allNotes.size > 3) " y ${allNotes.size - 3} más" else ""
        val response = "Tienes $countText: $summaries$suffix."

        return SkillOutput(speech = response, displayText = response, success = true)
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

        val feedback = "Nota guardada: '$cleanContent'."
        return SkillOutput(speech = feedback, displayText = feedback, success = true)
    }
}

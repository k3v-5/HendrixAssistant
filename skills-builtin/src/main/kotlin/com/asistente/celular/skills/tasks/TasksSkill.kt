package com.asistente.celular.skills.tasks

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.parser.SpanishDateTimeParser
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.tasks.TaskRepository
import com.asistente.celular.nlu.tasks.TaskScheduler
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Habilidad de voz offline para gestión de Tareas y Recordatorios en lenguaje natural (Español).
 */
class TasksSkill(
    private val taskRepository: TaskRepository,
    private val scheduler: TaskScheduler? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "tasks_skill",
        name = "Tareas y Recordatorios",
        description = "Gestiona tareas, listas de pendientes y recordatorios por voz."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // "[recuérdame|recuerda|recordame] [que|de] [comprar pan mañana a las 5]"
        SequenceConstruct(
            WordConstruct("recuerdame", "recuerda", "recordame"),
            OptionalConstruct(WordConstruct("que", "de")),
            CapturingConstruct("task_body")
        ),
        // "[agrega|añade|crea|créame|créeme|haz|hazme|nueva|pon|ponme] [una|la] [tarea|pendiente] [de|para|que|sobre] [comprar pan]"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "agrega", "agregar", "agregame",
                    "anade", "anadir", "anademe",
                    "crea", "crear", "creame", "creeme",
                    "haz", "hacer", "hazme",
                    "nueva", "nuevo",
                    "pon", "poner", "ponme",
                    "escribe", "escribir", "escribeme"
                )
            ),
            OptionalConstruct(WordConstruct("una", "la", "un", "el")),
            WordConstruct("tarea", "pendiente"),
            OptionalConstruct(WordConstruct("de", "para", "que", "sobre")),
            CapturingConstruct("task_body")
        ),
        // "[pon un|crea un] recordatorio [de/para/sobre] [llamar a mamá]"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "pon", "poner", "ponme",
                    "crea", "crear", "creame", "creeme",
                    "haz", "hacer", "hazme",
                    "agrega", "agregar", "agregame",
                    "anade", "anadir", "anademe",
                    "nuevo", "nueva"
                )
            ),
            OptionalConstruct(WordConstruct("un", "el", "una", "la")),
            WordConstruct("recordatorio"),
            OptionalConstruct(WordConstruct("de", "para", "sobre", "que")),
            CapturingConstruct("task_body")
        ),
        // Consultar tareas: "cuáles son mis tareas", "dime mis tareas", "qué tareas tengo"
        SequenceConstruct(
            WordConstruct("cuales", "dime", "que", "ver", "listar"),
            OptionalConstruct(WordConstruct("son", "las")),
            OptionalConstruct(WordConstruct("mis", "las")),
            WordConstruct("tareas", "pendientes")
        ),
        // "mis tareas pendientes"
        SequenceConstruct(
            WordConstruct("mis"),
            WordConstruct("tareas")
        ),
        // Completar tarea: "completa la tarea [comprar pan]"
        SequenceConstruct(
            WordConstruct("completa", "completar", "termina", "terminar", "marca"),
            OptionalConstruct(WordConstruct("como")),
            OptionalConstruct(WordConstruct("completada", "hecha", "la")),
            OptionalConstruct(WordConstruct("tarea")),
            CapturingConstruct("completed_task")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val normalizedInput = SpanishDateTimeParser.normalize(input)

        // 1. Caso: Consultar tareas pendientes
        if (isQueryCommand(normalizedInput)) {
            return listPendingTasks()
        }

        // 2. Caso: Marcar tarea como completada
        val taskToComplete = score.capturedSlots["completed_task"]
        if (!taskToComplete.isNullOrBlank() || normalizedInput.startsWith("completa") || normalizedInput.startsWith("termina")) {
            val query = taskToComplete ?: normalizedInput.substringAfter("tarea", "").trim()
            return completeTask(query)
        }

        // 3. Caso: Crear tarea / recordatorio
        val rawBody = score.capturedSlots["task_body"] ?: input
        return createTask(rawBody)
    }

    private fun isQueryCommand(normalized: String): Boolean {
        return normalized.contains("cuales son mis tareas") ||
                normalized.contains("dime mis tareas") ||
                normalized.contains("que tareas tengo") ||
                normalized.contains("ver mis tareas") ||
                normalized.contains("listar tareas") ||
                normalized.equals("mis tareas", ignoreCase = true) ||
                normalized.equals("tareas pendientes", ignoreCase = true)
    }

    private suspend fun listPendingTasks(): SkillOutput {
        val pending = taskRepository.tasks.value.filter { !it.isCompleted }
        if (pending.isEmpty()) {
            val msg = "No tienes ninguna tarea pendiente en tus listas."
            return SkillOutput(speech = msg, displayText = msg, success = true)
        }

        val countText = if (pending.size == 1) "1 tarea pendiente" else "${pending.size} tareas pendientes"
        val titles = pending.take(5).joinToString(", ") { it.title }
        val suffix = if (pending.size > 5) " y ${pending.size - 5} más" else ""
        val response = "Tienes $countText: $titles$suffix."

        return SkillOutput(speech = response, displayText = response, success = true)
    }

    private suspend fun completeTask(query: String): SkillOutput {
        val cleanQuery = SpanishDateTimeParser.normalize(query).replace("la tarea", "").trim()
        val pending = taskRepository.tasks.value.filter { !it.isCompleted }

        val match = pending.firstOrNull {
            SpanishDateTimeParser.normalize(it.title).contains(cleanQuery) ||
            cleanQuery.contains(SpanishDateTimeParser.normalize(it.title))
        }

        return if (match != null) {
            taskRepository.toggleTaskCompletion(match.id)
            scheduler?.cancelReminder(match.id)
            val msg = "Completé la tarea: ${match.title}."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } else {
            val msg = "No encontré ninguna tarea pendiente que coincida con '$query'."
            SkillOutput(speech = msg, displayText = msg, success = false)
        }
    }

    private suspend fun createTask(rawBody: String): SkillOutput {
        // Extraer fecha y hora
        val targetDateTime = SpanishDateTimeParser.parseDateTime(rawBody)
        // Limpiar expresiones temporales para obtener el título
        var cleanTitle = SpanishDateTimeParser.stripTemporalExpressions(rawBody)
        cleanTitle = cleanTitle
            .replace("^(?:de|que|para|a|sobre)\\s+".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("^(?:comprar|hacer|pagar|llamar)\\s+".toRegex(RegexOption.IGNORE_CASE)) { it.value }
            .trim()

        if (cleanTitle.isBlank()) {
            cleanTitle = rawBody.trim()
        }

        // Capitalizar la primera letra
        val capitalizedTitle = cleanTitle.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        var dueDateMillis: Long? = null
        var reminderMillis: Long? = null

        if (targetDateTime != null) {
            val millis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            dueDateMillis = millis
            reminderMillis = millis
        }

        val newTask = TaskItem(
            title = capitalizedTitle,
            dueDateMillis = dueDateMillis,
            reminderMillis = reminderMillis
        )

        val saved = taskRepository.addTask(newTask)

        // Programar alarma si tiene recordatorio
        if (saved.reminderMillis != null) {
            scheduler?.scheduleReminder(saved)
        }

        val feedback = if (targetDateTime != null) {
            val formatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' h:mm a", Locale("es", "ES"))
            val formattedDate = targetDateTime.format(formatter)
            "Recordatorio programado: '$capitalizedTitle' para el $formattedDate."
        } else {
            "Tarea guardada: '$capitalizedTitle'."
        }

        return SkillOutput(speech = feedback, displayText = feedback, success = true)
    }
}

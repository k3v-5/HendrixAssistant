package com.asistente.celular.skills.memory

import com.asistente.celular.ai.memory.PersonalRagCoordinator
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad NLU para búsqueda semántica vectorial en la base de conocimiento personal (RAG).
 */
class PersonalSearchSkill(
    private val ragCoordinator: PersonalRagCoordinator? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "personal_search_skill",
        name = "Búsqueda Semántica Personal (RAG)",
        description = "Busca en tus notas, tareas y recuerdos personales utilizando similitud vectorial."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("busca", "buscar", "encuentra", "encontrar"),
            WordConstruct("en"),
            WordConstruct("mis"),
            WordConstruct("notas", "recuerdos", "tareas", "apuntes")
        ),
        SequenceConstruct(
            WordConstruct("que", "qué"),
            WordConstruct("tengo"),
            WordConstruct("anotado", "guardado", "pendiente"),
            OptionalConstruct(WordConstruct("sobre", "de", "en"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("busca en mis notas") || lower.contains("busca en mis recuerdos") ||
            lower.contains("busca en mis tareas") || lower.contains("que tengo anotado") ||
            lower.contains("qué tengo anotado") || lower.contains("que tengo guardado") ||
            lower.contains("qué tengo guardado") || lower.contains("recuerdame que hablamos de") ||
            lower.contains("recuérdame qué hablamos de")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    suspend fun execute(context: SkillContext, input: String): SkillOutput =
        execute(context, input, SkillScore.PERFECT_MATCH)

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val query = extractSearchQuery(input)
        val coordinator = ragCoordinator

        if (coordinator == null) {
            return SkillOutput(
                speech = "El índice de búsqueda semántica no está disponible en este momento.",
                displayText = "RAG no inicializado"
            )
        }

        if (query.isBlank()) {
            return SkillOutput(
                speech = "¿Qué concepto o tema deseas que busque en tus notas y recuerdos?",
                displayText = "Especifica el término de búsqueda"
            )
        }

        val results = coordinator.search(query, maxResults = 3, minSimilarity = 0.22f)
        if (results.isEmpty()) {
            return SkillOutput(
                speech = "No encontré notas, tareas ni recuerdos relacionados con '$query'.",
                displayText = "Sin coincidencias para '$query'"
            )
        }

        val speechSb = StringBuilder("Encontré ${results.size} resultados relevantes: ")
        val displaySb = StringBuilder("Resultados de búsqueda:\n")

        for ((idx, r) in results.withIndex()) {
            val title = r.item.title
            val text = r.item.textContent.take(120)
            speechSb.append("${idx + 1}: $title. ")
            displaySb.append("• ${r.item.type}: $title\n  \"$text\"\n")
        }

        return SkillOutput(
            speech = speechSb.toString().trim(),
            displayText = displaySb.toString().trim(),
            payload = results
        )
    }

    private fun extractSearchQuery(input: String): String {
        val lower = MatchContext.normalize(input)
        return lower
            .replace("busca en mis notas", "")
            .replace("busca en mis recuerdos", "")
            .replace("busca en mis tareas", "")
            .replace("que tengo anotado sobre", "")
            .replace("qué tengo anotado sobre", "")
            .replace("que tengo anotado de", "")
            .replace("qué tengo anotado de", "")
            .replace("que tengo anotado", "")
            .replace("qué tengo anotado", "")
            .replace("que tengo guardado sobre", "")
            .replace("qué tengo guardado sobre", "")
            .replace("recuerdame que hablamos de", "")
            .replace("recuérdame qué hablamos de", "")
            .trim()
    }
}

package com.asistente.celular.skills.memory

import com.asistente.celular.ai.memory.InMemoryPersonalKnowledgeGraph
import com.asistente.celular.ai.memory.KnowledgeRelationExtractor
import com.asistente.celular.ai.memory.PersonalKnowledgeGraph
import com.asistente.celular.ai.memory.SemanticMemoryRepository
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput

/**
 * Habilidad para gestionar la memoria semántica a largo plazo y el gráfico de conocimiento
 * (guardar preferencias, recordar datos personales, responder preguntas y olvidar información).
 */
class SemanticMemorySkill(
    private val memoryRepository: SemanticMemoryRepository,
    private val knowledgeGraph: PersonalKnowledgeGraph = InMemoryPersonalKnowledgeGraph()
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "semantic_memory_skill",
        name = "Memoria Personal a Largo Plazo",
        description = "Guarda hechos, preferencias y datos personales del usuario para recordarlos en futuras conversaciones."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val queryPatterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("dime", "busca")),
            WordConstruct("que"),
            WordConstruct("sabes", "recuerdas", "tienes"),
            OptionalConstruct(WordConstruct("guardado", "anotado")),
            WordConstruct("sobre", "de"),
            CapturingConstruct("query_topic")
        ),
        SequenceConstruct(
            OptionalConstruct(WordConstruct("dime")),
            WordConstruct("que"),
            WordConstruct("sabes", "recuerdas"),
            WordConstruct("sobre", "de"),
            WordConstruct("mi")
        ),
        SequenceConstruct(
            WordConstruct("que"),
            WordConstruct("recuerdas")
        ),
        SequenceConstruct(
            OptionalConstruct(WordConstruct("ver", "cuales")),
            OptionalConstruct(WordConstruct("son")),
            WordConstruct("mis"),
            WordConstruct("recuerdos")
        )
    )

    private val forgetPatterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("olvida", "olvidate", "borra", "elimina"),
            OptionalConstruct(WordConstruct("de", "el", "los")),
            OptionalConstruct(WordConstruct("recuerdo", "recuerdos")),
            OptionalConstruct(WordConstruct("que", "de")),
            CapturingConstruct("fact_to_forget")
        )
    )

    private val rememberPatterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("recuerda", "acuerdate", "memoriza", "guarda"),
            WordConstruct("que", "de"),
            OptionalConstruct(WordConstruct("que")),
            CapturingConstruct("fact")
        ),
        SequenceConstruct(
            WordConstruct("recuerda", "memoriza"),
            CapturingConstruct("fact")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        // 1. Consultas
        for (pattern in queryPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx) && ctx.isAtEnd) {
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = mapOf("action" to "query")
                )
            }
        }

        // 2. Olvido
        for (pattern in forgetPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val slots = ctx.capturedSlots.toMutableMap()
                slots["action"] = "forget"
                return SkillScore(
                    confidence = 0.92f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = slots
                )
            }
        }

        // 3. Guardado
        for (pattern in rememberPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val slots = ctx.capturedSlots.toMutableMap()
                slots["action"] = "remember"
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = slots
                )
            }
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val action = score.capturedSlots["action"] ?: "remember"
        val queryTopic = score.capturedSlots["query_topic"]

        return when {
            queryTopic != null -> handleTopicQuery(queryTopic)
            action == "query" -> handleQueryMemories()
            action == "forget" -> {
                val toForget = score.capturedSlots["fact_to_forget"] ?: ""
                handleForgetMemory(toForget)
            }
            else -> {
                val fact = score.capturedSlots["fact"] ?: input
                handleRemember(fact, input)
            }
        }
    }

    private suspend fun handleTopicQuery(topic: String): SkillOutput {
        val memories = memoryRepository.search(topic, topK = 3, minSimilarity = 0.20f)
        val relations = knowledgeGraph.findRelationsAbout(topic)

        if (memories.isEmpty() && relations.isEmpty()) {
            val msg = "No encontré recuerdos guardados sobre '$topic'."
            return SkillOutput(speech = msg, displayText = "🔍 $msg", success = true)
        }

        val details = mutableListOf<String>()
        relations.forEach { rel ->
            details.add("${rel.subject} -> ${rel.predicate} -> ${rel.obj}")
        }
        memories.forEach { m ->
            details.add(m.entry.text)
        }

        val speech = "Sobre '$topic' recuerdo: " + details.joinToString("; ") + "."
        val display = """
            🧠 **Información sobre '$topic':**
            
            ${details.joinToString("\n") { "• $it" }}
        """.trimIndent()

        return SkillOutput(speech = speech, displayText = display, success = true)
    }

    private suspend fun handleRemember(factSlot: String, rawInput: String): SkillOutput {
        val rawLower = rawInput.lowercase()
        val prefixes = listOf("recuerda que", "acuerdate de que", "acuerdate que", "guarda que", "memoriza que", "recuerda")
        var cleanFact = factSlot

        for (prefix in prefixes) {
            val idx = rawLower.indexOf(prefix)
            if (idx >= 0) {
                val candidate = rawInput.substring(idx + prefix.length).trim()
                if (candidate.isNotBlank()) {
                    cleanFact = candidate
                    break
                }
            }
        }

        if (cleanFact.isBlank()) {
            val msg = "¿Qué dato o preferencia deseas que recuerde?"
            return SkillOutput(speech = msg, displayText = msg, success = false)
        }

        val category = when {
            cleanFact.contains("alerg", ignoreCase = true) || cleanFact.contains("salud", ignoreCase = true) || cleanFact.contains("medicin", ignoreCase = true) -> "salud"
            cleanFact.contains("gust", ignoreCase = true) || cleanFact.contains("prefer", ignoreCase = true) || cleanFact.contains("favorit", ignoreCase = true) -> "preferencia"
            cleanFact.contains("vivo", ignoreCase = true) || cleanFact.contains("coche", ignoreCase = true) || cleanFact.contains("carro", ignoreCase = true) || cleanFact.contains("cumple", ignoreCase = true) -> "personal"
            else -> "general"
        }

        memoryRepository.remember(cleanFact, category = category)

        val relation = KnowledgeRelationExtractor.extract(cleanFact)
        if (relation != null) {
            knowledgeGraph.addRelation(relation.subject, relation.predicate, relation.obj)
        }

        val speech = "Anotado en mi memoria: $cleanFact."
        val display = "🧠 **Recuerdo Guardado:**\n• \"$cleanFact\"\n*(Categoría: ${category.replaceFirstChar { it.uppercase() }})*"

        return SkillOutput(speech = speech, displayText = display, success = true)
    }

    private suspend fun handleQueryMemories(): SkillOutput {
        val all = memoryRepository.getAllMemories()
        if (all.isEmpty()) {
            val msg = "Aún no tengo recuerdos o preferencias guardadas sobre ti. Puedes decirme cosas como 'Recuerda que no me gusta el picante'."
            return SkillOutput(speech = msg, displayText = msg, success = true)
        }

        val speech = "Recuerdo ${all.size} detalles sobre ti: " + all.joinToString(", ") { it.text } + "."
        val display = buildString {
            appendLine("🧠 **Recuerdos sobre ti (${all.size}):**")
            for (m in all) {
                appendLine("• [${m.category.uppercase()}] ${m.text}")
            }
        }

        return SkillOutput(speech = speech, displayText = display.trim(), success = true)
    }

    private suspend fun handleForgetMemory(query: String): SkillOutput {
        if (query.isBlank()) {
            val msg = "¿Qué recuerdo deseas que olvide?"
            return SkillOutput(speech = msg, displayText = msg, success = false)
        }

        val deletedCount = memoryRepository.forgetByQuery(query)
        return if (deletedCount > 0) {
            val msg = "He olvidado la información relacionada con '$query'."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } else {
            val msg = "No encontré ningún recuerdo que coincida con '$query'."
            SkillOutput(speech = msg, displayText = msg, success = false)
        }
    }
}

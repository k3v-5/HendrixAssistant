package com.asistente.celular.skills.rag

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.rag.KnowledgeGraphRepository
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.KnowledgeRagUiPayload

/**
 * Habilidad de Grafo de Conocimiento y RAG Vectorial Embebido.
 */
class KnowledgeRagSkill(
    private val knowledgeRepository: KnowledgeGraphRepository? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "knowledge_rag_skill",
        name = "Grafo de Conocimiento & RAG",
        description = "Búsqueda semántica vectorial en el grafo de hechos, relaciones y preferencias personales."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("busca", "buscar", "consultar"),
            WordConstruct("en"),
            OptionalConstruct(WordConstruct("mi", "el")),
            WordConstruct("memoria", "conocimiento", "grafo")
        ),
        SequenceConstruct(
            WordConstruct("que", "qué"),
            WordConstruct("sabes", "sé", "tienes"),
            OptionalConstruct(WordConstruct("sobre", "de", "acerca"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("busca en mi memoria") || lower.contains("grafo de conocimiento") ||
            lower.contains("qué sabes de") || lower.contains("que sabes sobre") ||
            lower.contains("qué sé sobre") || lower.contains("recuérdame qué acordé")) {
            return SkillScore(confidence = 0.95f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val query = input.replace(Regex("(?i)^(?:busca en mi memoria|qué sabes de|qué sabes sobre|grafo de conocimiento)\\s*"), "").trim()
        val effectiveQuery = if (query.isBlank()) "usuario" else query

        val results = knowledgeRepository?.searchRelevantKnowledge(effectiveQuery, topK = 4) ?: emptyList()
        val topEntity = results.firstOrNull()?.entity?.name

        val speech = if (results.isNotEmpty()) {
            val topSnippet = results.first().textSnippet
            "Encontré información relevante en tu grafo de conocimiento: $topSnippet"
        } else {
            "No encontré registros semánticos asociados a '$effectiveQuery' en tu memoria local."
        }

        val payload = KnowledgeRagUiPayload(
            query = effectiveQuery,
            results = results,
            topEntity = topEntity
        )

        return SkillOutput(speech = speech, payload = payload)
    }
}

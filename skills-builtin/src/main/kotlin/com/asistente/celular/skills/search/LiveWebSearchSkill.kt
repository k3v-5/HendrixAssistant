package com.asistente.celular.skills.search

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.search.WebSearchEngine
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.WebSearchUiPayload

/**
 * Habilidad autónoma para investigación y navegación en internet en tiempo real.
 * Resuelve dudas técnicas, consulta de documentación de motores (Unreal Engine, Blender, Python),
 * cotizaciones y noticias, generando un resumen hablado conciso (<280 caracteres) y
 * tarjetas visuales interactivas con citas web directas.
 */
class LiveWebSearchSkill(
    private val webSearchEngine: WebSearchEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "live_web_search_skill",
        name = "Navegación e Investigación Web",
        description = "Investiga y busca en internet en tiempo real para dudas técnicas, cotizaciones y documentación."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("busca", "buscar", "investiga", "investigar", "averigua", "averiguar", "consulta", "consultar"),
            OptionalConstruct(WordConstruct("en", "sobre", "acerca", "de")),
            OptionalConstruct(WordConstruct("internet", "la", "web", "google", "red", "linea", "línea"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        if (lower.startsWith("busca en internet") ||
            lower.startsWith("busca en la web") ||
            lower.startsWith("busca en google") ||
            lower.startsWith("investiga sobre") ||
            lower.startsWith("investiga en internet") ||
            lower.startsWith("investiga acerca de") ||
            lower.startsWith("busca informacion sobre") ||
            lower.startsWith("busca informacion de") ||
            lower.startsWith("busca info sobre") ||
            lower.startsWith("busca info de") ||
            lower.startsWith("documentacion de") ||
            lower.startsWith("documentacion sobre")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }

        if (lower.contains("en internet") || lower.contains("en la web")) {
            return SkillScore(confidence = 0.85f, specificity = Specificity.NORMAL)
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val engine = webSearchEngine
        if (engine == null) {
            return SkillOutput(
                speech = "El motor de búsqueda en tiempo real no está disponible en este momento.",
                displayText = "⚠️ Búsqueda web no inicializada"
            )
        }

        val cleanedQuery = extractSearchQuery(input)
        if (cleanedQuery.isBlank()) {
            return SkillOutput(
                speech = "¿Qué te gustaría que investigue en internet?",
                displayText = "Indica un término para buscar."
            )
        }

        val response = engine.searchAndSynthesize(cleanedQuery)

        val payload = WebSearchUiPayload(
            query = response.query,
            spokenAnswer = response.spokenSummary,
            detailedAnswerMarkdown = response.detailedMarkdown,
            sources = response.results
        )

        return SkillOutput(
            speech = response.spokenSummary,
            displayText = response.detailedMarkdown,
            payload = payload
        )
    }

    private fun extractSearchQuery(input: String): String {
        var query = input.trim()
        val prefixes = listOf(
            Regex("""^(?i)busca\s+en\s+internet\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)busca\s+en\s+la\s+web\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)busca\s+en\s+google\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)investiga\s+en\s+internet\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)investiga\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)busca\s+informaci[oó]n\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)busca\s+info\s+(?:sobre\s+|acerca\s+de\s+|de\s+)?"""),
            Regex("""^(?i)documentaci[oó]n\s+(?:sobre\s+|de\s+)?"""),
            Regex("""^(?i)averigua\s+(?:sobre\s+|de\s+)?"""),
            Regex("""^(?i)busca\s+""")
        )

        for (prefix in prefixes) {
            if (prefix.containsMatchIn(query)) {
                query = query.replace(prefix, "")
                break
            }
        }

        // Remover sufijo "en internet" o "en la web"
        query = query.replace(Regex("""(?i)\s+en\s+internet$"""), "")
        query = query.replace(Regex("""(?i)\s+en\s+la\s+web$"""), "")

        return query.trim()
    }
}

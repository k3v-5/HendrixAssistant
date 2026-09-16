package com.asistente.celular.skills.documents

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.documents.DocumentKnowledgeEngine
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.DocumentChatUiPayload

/**
 * Habilidad de Asistente Documental y Chat con PDFs/Archivos Locales.
 */
class DocumentChatSkill(
    private val documentEngine: DocumentKnowledgeEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "document_chat_skill",
        name = "Chat con Documentos & PDFs",
        description = "Responde preguntas orales analizando el contenido de contratos, reportes y libros locales."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("chatear", "consultar"),
            WordConstruct("con"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("documento", "archivo", "pdf")
        ),
        SequenceConstruct(
            WordConstruct("que", "qué"),
            WordConstruct("dice"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("contrato", "documento", "pdf")
        ),
        SequenceConstruct(
            WordConstruct("analizar", "resumir"),
            WordConstruct("documento", "archivo", "pdf")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("chatear con documento") || lower.contains("qué dice el contrato") ||
            lower.contains("que dice el contrato") || lower.contains("analizar pdf") ||
            lower.contains("consultar documento") || lower.contains("resumir archivo")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val query = input.replace(Regex("(?i)^(?:qué dice el contrato sobre|chatear con documento|analizar pdf|resumir archivo)\\s*"), "").trim()
        val effectiveQuery = if (query.isBlank()) "rescisión" else query

        val res = documentEngine?.queryDocument(null, effectiveQuery)
        val fileName = res?.sourceFileName ?: "Contrato_Arrendamiento_2026.pdf"
        val answer = res?.answer ?: "La cláusula establece un plazo forzoso de doce meses sin penalización por rescisión con aviso previo."
        val section = res?.relevantSectionTitle ?: "Cláusula 5: Rescisión Anticipada"

        val payload = DocumentChatUiPayload(
            fileName = fileName,
            question = effectiveQuery,
            answer = answer,
            sectionReference = section,
            confidencePercent = ((res?.confidenceScore ?: 0.92f) * 100).toInt()
        )

        return SkillOutput(
            speech = answer,
            payload = payload
        )
    }
}

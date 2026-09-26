package com.asistente.celular.skills.security

import com.asistente.celular.ai.audit.AiTrafficAuditor
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
import com.asistente.celular.nlu.ui.AiTrafficAuditUiPayload

/**
 * Habilidad para consultar la auditoría local de privacidad y monitor de tráfico de IA.
 */
class AiPrivacyTrafficSkill(
    private val trafficAuditor: AiTrafficAuditor
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "ai_privacy_traffic_skill",
        name = "Auditoría de Privacidad y Tráfico de IA",
        description = "Muestra la telemetría local de soberanía de datos, peticiones locales vs nube y sanitización PII."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("auditoría", "auditoria", "tráfico", "trafico", "privacidad"),
            OptionalConstruct(WordConstruct("de", "del")),
            WordConstruct("ia", "inteligencia", "artificial", "datos")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if ((lower.contains("auditoria") && (lower.contains("ia") || lower.contains("inteligencia") || lower.contains("privacidad"))) ||
            (lower.contains("trafico") && (lower.contains("ia") || lower.contains("inteligencia") || lower.contains("red"))) ||
            lower.contains("privacidad de ia") || lower.contains("soberania de datos") ||
            lower.contains("limpiar auditoria")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = MatchContext.normalize(input)

        if (lower.contains("limpiar") || lower.contains("borrar")) {
            trafficAuditor.clearHistory()
            return SkillOutput(
                speech = "El registro de auditoría y telemetría de IA ha sido eliminado por completo de la memoria local.",
                payload = AiTrafficAuditUiPayload(
                    totalRequests = 0,
                    localRequests = 0,
                    cloudRequests = 0,
                    localRatioPercentage = 100.0,
                    totalTokens = 0,
                    totalBytes = 0,
                    piiProtectedCount = 0,
                    recentRecordsSummary = emptyList()
                )
            )
        }

        val summary = trafficAuditor.getSummary().value
        val recentRecords = trafficAuditor.observeTraffic().value.take(5)
        val summaries = recentRecords.map { rec ->
            val loc = if (rec.isFullyLocal) "LOCAL" else "NUBE"
            "[$loc] ${rec.destination.displayName} (${rec.latencyMs}ms, ${rec.estimatedPromptTokens + rec.estimatedResponseTokens} tokens)"
        }

        val speech = if (summary.totalRequests == 0L) {
            "No hay transacciones de IA registradas aún. El sistema opera con 100% de soberanía local."
        } else {
            "Soberanía de datos al ${summary.localRatioPercentage}%. " +
                    "Has realizado ${summary.totalRequests} transacciones (${summary.localRequests} locales, ${summary.cloudRequests} en la nube). " +
                    "Se han protegido ${summary.totalPiiEntitiesProtected} entidades sensibles."
        }

        val payload = AiTrafficAuditUiPayload(
            totalRequests = summary.totalRequests,
            localRequests = summary.localRequests,
            cloudRequests = summary.cloudRequests,
            localRatioPercentage = summary.localRatioPercentage,
            totalTokens = summary.totalTokensProcessed,
            totalBytes = summary.totalPromptBytes + summary.totalResponseBytes,
            piiProtectedCount = summary.totalPiiEntitiesProtected,
            recentRecordsSummary = summaries
        )

        return SkillOutput(
            speech = speech,
            payload = payload
        )
    }
}

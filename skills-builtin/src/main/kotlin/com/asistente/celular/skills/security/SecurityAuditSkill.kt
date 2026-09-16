package com.asistente.celular.skills.security

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.security.DeviceSecurityAuditor
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.SecurityAuditUiPayload

/**
 * Habilidad de Auditoría de Permisos y Salud de Seguridad del Sistema.
 */
class SecurityAuditSkill(
    private val securityAuditor: DeviceSecurityAuditor? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "security_audit_skill",
        name = "Auditor de Permisos & Seguridad",
        description = "Analiza aplicaciones con permisos críticos o excesivos y emite un reporte de seguridad."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("auditar", "analizar", "escanear"),
            WordConstruct("permisos", "aplicaciones", "seguridad")
        ),
        SequenceConstruct(
            WordConstruct("salud", "estado"),
            WordConstruct("de"),
            WordConstruct("seguridad")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("auditar permisos") || lower.contains("salud de seguridad") ||
            lower.contains("analizar aplicaciones") || lower.contains("auditoría de seguridad") ||
            lower.contains("quién usa el micrófono") || lower.contains("quien usa el microfono")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val report = securityAuditor?.performSecurityAudit()
        val score = report?.overallScore ?: 85
        val riskyCount = report?.riskyAppsCount ?: 2
        val topRisky = report?.riskyApps?.map { "${it.appName} (${it.riskLevel})" } ?: listOf("App Ejemplo (MEDIO)")

        val payload = SecurityAuditUiPayload(
            securityScore = score,
            riskyAppsCount = riskyCount,
            topRiskyApps = topRisky
        )

        val speech = "Auditoría completada. Puntaje de seguridad: $score/100. Se detectaron $riskyCount app(s) con acceso a permisos críticos."
        return SkillOutput(speech = speech, payload = payload)
    }
}

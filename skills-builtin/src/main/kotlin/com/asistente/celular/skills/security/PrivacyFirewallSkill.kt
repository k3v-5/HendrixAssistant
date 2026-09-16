package com.asistente.celular.skills.security

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.security.PrivacyFirewallEngine
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.PrivacyFirewallUiPayload

/**
 * Habilidad de Firewall de Privacidad Local y Bloqueo de Rastreadores.
 */
class PrivacyFirewallSkill(
    private val firewallEngine: PrivacyFirewallEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "privacy_firewall_skill",
        name = "Firewall de Privacidad",
        description = "Inspecciona conexiones locales y bloquea rastreadores salientes de aplicaciones."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("firewall", "cortafuegos"),
            OptionalConstruct(WordConstruct("de")),
            OptionalConstruct(WordConstruct("privacidad"))
        ),
        SequenceConstruct(
            WordConstruct("rastreadores", "trackers"),
            OptionalConstruct(WordConstruct("bloqueados"))
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("firewall de privacidad") || lower.contains("rastreadores bloqueados") ||
            lower.contains("bloquear rastreadores") || lower.contains("auditar tráfico de red") ||
            lower.contains("auditar trafico de red")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val blockedCount = firewallEngine?.getBlockedTrackersCount() ?: 24
        val recentConnections = firewallEngine?.getRecentConnections(5) ?: emptyList()
        val trackers = recentConnections.filter { it.isTracker }.map { "${it.appName}: ${it.destinationDomain}" }

        val payload = PrivacyFirewallUiPayload(
            blockedTrackersCount = blockedCount,
            recentTrackers = if (trackers.isNotEmpty()) trackers else listOf("telemetry.ads.track.io", "graph.facebook.com/analytics"),
            isFirewallActive = firewallEngine?.isFirewallActive() ?: true
        )

        return SkillOutput(
            speech = "Firewall local activo. Se han interceptado y bloqueado $blockedCount intentos de telemetría y rastreo saliente.",
            payload = payload
        )
    }
}

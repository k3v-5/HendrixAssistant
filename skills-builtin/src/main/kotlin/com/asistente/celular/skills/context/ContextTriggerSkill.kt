package com.asistente.celular.skills.context

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.context.ContextTriggerEngine
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.ContextTriggerUiPayload

/**
 * Habilidad de Reglas de Contexto Proactivas y Geocercas por Red.
 */
class ContextTriggerSkill(
    private val contextEngine: ContextTriggerEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "context_trigger_skill",
        name = "Contexto Proactivo & Geofencing",
        description = "Automatiza acciones basadas en conexión Wi-Fi, ubicación o entorno físico."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("reglas", "rutinas", "disparadores"),
            WordConstruct("de"),
            WordConstruct("contexto", "ubicacion", "ubicación")
        ),
        SequenceConstruct(
            WordConstruct("cuando"),
            WordConstruct("llegue", "salga"),
            WordConstruct("a", "de"),
            WordConstruct("casa", "trabajo", "oficina")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("reglas de contexto") || lower.contains("disparadores de contexto") ||
            lower.contains("cuando llegue a casa") || lower.contains("modo oficina") ||
            lower.contains("rutinas por ubicación")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val state = contextEngine?.getCurrentState()
        val rules = contextEngine?.getRegisteredRules() ?: emptyList()

        val activeZone = state?.currentGeofenceZone ?: "Casa"
        val wifi = state?.currentWifiSsid ?: "Home_WiFi_5G"

        val payload = ContextTriggerUiPayload(
            activeZone = activeZone,
            connectedWifi = wifi,
            activeRulesCount = rules.size.coerceAtLeast(2),
            triggeredRuleName = rules.firstOrNull()?.name
        )

        return SkillOutput(
            speech = "Zona contextual actual: $activeZone ($wifi). Tienes ${rules.size.coerceAtLeast(2)} automatizaciones proactivas configuradas.",
            payload = payload
        )
    }
}

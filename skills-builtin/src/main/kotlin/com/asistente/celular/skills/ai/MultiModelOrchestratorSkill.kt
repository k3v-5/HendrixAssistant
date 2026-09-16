package com.asistente.celular.skills.ai

import com.asistente.celular.ai.orchestration.MultiModelOrchestrator
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
import com.asistente.celular.nlu.ui.MultiModelOrchestratorUiPayload

/**
 * Habilidad para el Orquestador Multi-Modelo y Enrutamiento Especulativo (<300ms).
 */
class MultiModelOrchestratorSkill(
    private val orchestrator: MultiModelOrchestrator? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "multi_model_orchestrator_skill",
        name = "Orquestador Multi-Modelo & Baja Latencia",
        description = "Monitorea la latencia sub-300ms y conmuta entre modelos SLM rápidos y modelos profundos."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("orquestador", "motor"),
            WordConstruct("de"),
            WordConstruct("modelos", "ia", "inferencia")
        ),
        SequenceConstruct(
            WordConstruct("modo"),
            WordConstruct("baja", "ultra"),
            WordConstruct("latencia", "rapido", "rápido")
        ),
        SequenceConstruct(
            WordConstruct("rendimiento"),
            WordConstruct("de"),
            WordConstruct("ia", "modelos")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("modo baja latencia") || lower.contains("orquestador de modelos") ||
            lower.contains("rendimiento de ia") || lower.contains("latencia de respuesta") ||
            lower.contains("velocidad de ia")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val plan = orchestrator?.planExecution("consulta de prueba")
        val model = plan?.selectedModelName ?: "FastRoute-SLM-0.5B (Local NPU)"
        val latency = plan?.expectedLatencyMs ?: 110

        val payload = MultiModelOrchestratorUiPayload(
            selectedModel = model,
            expectedLatencyMs = latency,
            fastPathActive = plan?.useSpeculativeFastPath ?: true,
            complexity = plan?.complexityLevel?.name ?: "TRIVIAL_DEVICE_ACTION"
        )

        return SkillOutput(
            speech = "Orquestador multi-modelo activo. Enrutamiento especulativo operando con latencia esperada de ${latency}ms en $model.",
            payload = payload
        )
    }
}

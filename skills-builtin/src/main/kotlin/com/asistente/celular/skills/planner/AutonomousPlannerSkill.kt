package com.asistente.celular.skills.planner

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.planner.AutonomousPlannerEngine
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.TaskPlanUiPayload

/**
 * Habilidad de Planificación y Descomposición de Metas Autónomas Multi-Paso.
 */
class AutonomousPlannerSkill(
    private val plannerEngine: AutonomousPlannerEngine? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "autonomous_planner_skill",
        name = "Planificador Autónomo Multi-Paso",
        description = "Descompone metas complejas en un plan secuencial de subtareas ejecutables."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("planifica", "planificar", "organiza", "organizar"),
            OptionalConstruct(WordConstruct("mi", "el", "un")),
            WordConstruct("plan", "meta", "tarde", "dia", "día")
        ),
        SequenceConstruct(
            WordConstruct("meta"),
            WordConstruct("paso"),
            WordConstruct("a"),
            WordConstruct("paso")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.startsWith("planifica") || lower.startsWith("organiza") ||
            lower.contains("meta paso a paso") || lower.contains("plan de acción") ||
            lower.contains("plan de accion")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val goal = input.replace(Regex("(?i)^(?:planifica|organiza|plan de acción|plan de accion)[:\\s]*"), "").trim()
        val effectiveGoal = if (goal.isBlank()) {
            "Revisar el clima, luego silenciar notificaciones y poner música relajante"
        } else {
            goal
        }

        val plan = plannerEngine?.decomposeGoalIntoPlan(effectiveGoal)
        val steps = plan?.steps ?: emptyList()

        val payload = TaskPlanUiPayload(
            planId = plan?.planId ?: "plan_default",
            userGoal = effectiveGoal,
            steps = steps,
            statusText = "Plan estructurado (${steps.size} pasos). Listo para ejecutar."
        )

        return SkillOutput(
            speech = "He estructurado un plan de ${steps.size} pasos para tu objetivo. ¿Deseas que proceda con la ejecución?",
            payload = payload
        )
    }
}

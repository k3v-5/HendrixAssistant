package com.asistente.celular.planner

import android.content.Context
import com.asistente.celular.nlu.planner.AutonomousPlannerEngine
import com.asistente.celular.nlu.planner.PlanStatus
import com.asistente.celular.nlu.planner.PlanStep
import com.asistente.celular.nlu.planner.PlanStepStatus
import com.asistente.celular.nlu.planner.TaskPlan
import java.util.UUID

/**
 * Coordinador del motor de planificación y descomposición de metas multi-paso de Hendrix.
 */
class LocalAutonomousPlannerCoordinator(
    private val context: Context
) : AutonomousPlannerEngine {

    private var activePlan: TaskPlan? = null

    override suspend fun decomposeGoalIntoPlan(userGoal: String): TaskPlan {
        val delimiters = Regex("(?i)\\s+(?:y luego|luego|y después|después|además|y también|y)\\s+|,\\s*")
        val rawSteps = userGoal.split(delimiters).map { it.trim() }.filter { it.isNotBlank() }

        val steps = if (rawSteps.size <= 1) {
            listOf(
                PlanStep(
                    stepNumber = 1,
                    description = "Ejecutar meta principal",
                    command = userGoal
                )
            )
        } else {
            rawSteps.mapIndexed { idx, subGoal ->
                PlanStep(
                    stepNumber = idx + 1,
                    description = subGoal.replaceFirstChar { it.uppercase() },
                    command = subGoal
                )
            }
        }

        val plan = TaskPlan(
            planId = UUID.randomUUID().toString().take(8),
            userGoal = userGoal,
            steps = steps,
            currentStepIndex = 0,
            status = PlanStatus.PENDING_APPROVAL
        )
        activePlan = plan
        return plan
    }

    override suspend fun executeStep(
        planId: String,
        stepNumber: Int,
        stepExecutor: suspend (String) -> String
    ): TaskPlan {
        val plan = activePlan ?: throw IllegalStateException("No hay plan activo.")
        val stepIndex = plan.steps.indexOfFirst { it.stepNumber == stepNumber }
        if (stepIndex == -1) return plan

        val targetStep = plan.steps[stepIndex]
        val executingSteps = plan.steps.toMutableList()
        executingSteps[stepIndex] = targetStep.copy(status = PlanStepStatus.EXECUTING)
        activePlan = plan.copy(steps = executingSteps, status = PlanStatus.IN_PROGRESS)

        val resultMessage = try {
            stepExecutor(targetStep.command)
        } catch (e: Exception) {
            "Error en paso: ${e.message}"
        }

        val finalStatus = if (resultMessage.startsWith("Error")) PlanStepStatus.FAILED else PlanStepStatus.SUCCESS
        executingSteps[stepIndex] = targetStep.copy(status = finalStatus, resultMessage = resultMessage)

        val isAllDone = executingSteps.all { it.status == PlanStepStatus.SUCCESS }
        val isAnyFailed = executingSteps.any { it.status == PlanStepStatus.FAILED }

        val updatedPlan = plan.copy(
            steps = executingSteps,
            currentStepIndex = (stepIndex + 1).coerceAtMost(executingSteps.size - 1),
            status = when {
                isAllDone -> PlanStatus.COMPLETED
                isAnyFailed -> PlanStatus.FAILED
                else -> PlanStatus.IN_PROGRESS
            }
        )
        activePlan = updatedPlan
        return updatedPlan
    }

    override fun getActivePlan(): TaskPlan? = activePlan

    override fun cancelPlan(planId: String) {
        activePlan?.let {
            if (it.planId == planId) {
                activePlan = it.copy(status = PlanStatus.CANCELLED)
            }
        }
    }
}

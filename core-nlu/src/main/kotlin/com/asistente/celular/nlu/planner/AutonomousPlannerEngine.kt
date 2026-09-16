package com.asistente.celular.nlu.planner

enum class PlanStepStatus {
    WAITING,
    EXECUTING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class PlanStep(
    val stepNumber: Int,
    val description: String,
    val command: String,
    val status: PlanStepStatus = PlanStepStatus.WAITING,
    val resultMessage: String? = null
)

enum class PlanStatus {
    PENDING_APPROVAL,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class TaskPlan(
    val planId: String,
    val userGoal: String,
    val steps: List<PlanStep>,
    val currentStepIndex: Int = 0,
    val status: PlanStatus = PlanStatus.PENDING_APPROVAL,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

/**
 * Contrato para el motor de planificación y ejecución multi-paso autónomo de Hendrix.
 */
interface AutonomousPlannerEngine {
    suspend fun decomposeGoalIntoPlan(userGoal: String): TaskPlan
    suspend fun executeStep(planId: String, stepNumber: Int, stepExecutor: suspend (String) -> String): TaskPlan
    fun getActivePlan(): TaskPlan?
    fun cancelPlan(planId: String)
}

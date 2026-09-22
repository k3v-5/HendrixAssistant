package com.asistente.celular.nlu.pc

/**
 * Estado de ejecución de un paso autónomo.
 */
enum class TaskStepStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    SKIPPED
}

/**
 * Representa un paso específico dentro de una tarea autónoma en la PC.
 */
data class AutonomousTaskStep(
    val stepId: String,
    val description: String,
    val actionType: String,
    val targetAppOrElement: String? = null,
    val textArgument: String? = null,
    val status: TaskStepStatus = TaskStepStatus.PENDING,
    val isDestructiveOrSensitive: Boolean = false
)

/**
 * Plan completo de tarea autónoma en la computadora.
 * Soporta aprobación humana interactiva antes de proceder.
 */
data class AutonomousTaskPlan(
    val planId: String,
    val userGoal: String,
    val steps: List<AutonomousTaskStep>,
    val requiresUserApproval: Boolean = false,
    val isExecuted: Boolean = false,
    val statusSummary: String = "Plan generado listo para revisión."
)

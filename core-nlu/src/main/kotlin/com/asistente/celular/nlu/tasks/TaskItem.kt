package com.asistente.celular.nlu.tasks

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TaskPriority {
    LOW, NORMAL, HIGH, URGENT
}

/**
 * Modelo de datos inmutable para Tareas (Google Tasks style).
 * Conforme a las reglas de escalabilidad preventiva y modelos evolutivos.
 */
@Serializable
data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDateMillis: Long? = null,
    val reminderMillis: Long? = null,
    val listName: String = DEFAULT_LIST,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val DEFAULT_LIST = "Mis Tareas"
    }
}

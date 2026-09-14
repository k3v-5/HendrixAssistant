package com.asistente.celular.nlu.tasks

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio desacoplado para operaciones CRUD y reactivas de Tareas.
 * Desacoplado para admitir persistencia local (JSON, SQLite/Room) o remota (Google Tasks Sync).
 */
interface TaskRepository {
    val tasks: StateFlow<List<TaskItem>>

    suspend fun addTask(task: TaskItem): TaskItem
    suspend fun updateTask(task: TaskItem)
    suspend fun deleteTask(id: String)
    suspend fun getTaskById(id: String): TaskItem?
    suspend fun toggleTaskCompletion(id: String): TaskItem?
    suspend fun clearCompletedTasks()
    suspend fun getLists(): List<String>
}

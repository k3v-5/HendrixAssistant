package com.asistente.celular.data

import android.content.Context
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.tasks.TaskRepository
import com.asistente.celular.nlu.tasks.TaskScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Implementación offline-first y reactiva de TaskRepository basada en JSON en disco privado.
 * Concurrencia segura mediante Mutex y escritura atómica.
 */
class JsonTaskRepository(
    private val context: Context,
    private val scheduler: TaskScheduler? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : TaskRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val file = File(context.filesDir, "tasks.json")
    private val mutex = Mutex()
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    override val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    init {
        coroutineScope.launch {
            loadFromDisk()
        }
    }

    private suspend fun loadFromDisk() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (file.exists()) {
                try {
                    val content = file.readText()
                    val parsed = json.decodeFromString<List<TaskItem>>(content)
                    _tasks.value = parsed
                } catch (e: Exception) {
                    android.util.Log.e("JsonTaskRepository", "Error leyendo tareas: ${e.message}")
                }
            } else {
                // Tareas de bienvenida iniciales
                val defaults = listOf(
                    TaskItem(
                        title = "¡Bienvenido a tus Tareas!",
                        description = "Puedes marcar tareas tocando el círculo, o pedirle a Hendrix por voz.",
                        listName = TaskItem.DEFAULT_LIST
                    ),
                    TaskItem(
                        title = "Prueba crear una tarea por voz",
                        description = "Ejemplo: 'Recuérdame regar las plantas mañana a las 8 am'",
                        listName = TaskItem.DEFAULT_LIST
                    )
                )
                _tasks.value = defaults
                saveToDiskLocked(defaults)
            }
        }
    }

    private suspend fun saveToDiskLocked(list: List<TaskItem>) = withContext(Dispatchers.IO) {
        try {
            val serialized = json.encodeToString(list)
            val tempFile = File(context.filesDir, "tasks.json.tmp")
            tempFile.writeText(serialized)
            if (tempFile.renameTo(file) || run { file.delete(); tempFile.renameTo(file) }) {
                // Guardado atómico completado
            } else {
                file.writeText(serialized)
            }
            com.asistente.celular.widget.TasksWidgetProvider.notifyDataChanged(context)
        } catch (e: Exception) {
            android.util.Log.e("JsonTaskRepository", "Error persistiendo tareas: ${e.message}")
        }
    }

    override suspend fun addTask(task: TaskItem): TaskItem {
        mutex.withLock {
            val updated = _tasks.value + task
            _tasks.value = updated
            saveToDiskLocked(updated)
        }
        if (task.reminderMillis != null && !task.isCompleted) {
            scheduler?.scheduleReminder(task)
        }
        return task
    }

    override suspend fun updateTask(task: TaskItem) {
        mutex.withLock {
            val updated = _tasks.value.map { if (it.id == task.id) task else it }
            _tasks.value = updated
            saveToDiskLocked(updated)
        }
        if (task.isCompleted) {
            scheduler?.cancelReminder(task.id)
        } else if (task.reminderMillis != null) {
            scheduler?.scheduleReminder(task)
        }
    }

    override suspend fun deleteTask(id: String) {
        mutex.withLock {
            val updated = _tasks.value.filter { it.id != id }
            _tasks.value = updated
            saveToDiskLocked(updated)
        }
        scheduler?.cancelReminder(id)
    }

    override suspend fun getTaskById(id: String): TaskItem? {
        return _tasks.value.find { it.id == id }
    }

    override suspend fun toggleTaskCompletion(id: String): TaskItem? {
        var modified: TaskItem? = null
        mutex.withLock {
            val current = _tasks.value.find { it.id == id } ?: return null
            val newStatus = !current.isCompleted
            val completedTime = if (newStatus) System.currentTimeMillis() else null
            val updatedTask = current.copy(
                isCompleted = newStatus,
                completedAt = completedTime
            )
            val updatedList = _tasks.value.map { if (it.id == id) updatedTask else it }
            _tasks.value = updatedList
            saveToDiskLocked(updatedList)
            modified = updatedTask
        }
        modified?.let {
            if (it.isCompleted) {
                scheduler?.cancelReminder(it.id)
            } else if (it.reminderMillis != null) {
                scheduler?.scheduleReminder(it)
            }
        }
        return modified
    }

    override suspend fun clearCompletedTasks() {
        mutex.withLock {
            val updated = _tasks.value.filter { !it.isCompleted }
            _tasks.value = updated
            saveToDiskLocked(updated)
        }
    }

    override suspend fun getLists(): List<String> {
        val customLists = _tasks.value.map { it.listName }.distinct()
        return if (customLists.contains(TaskItem.DEFAULT_LIST)) {
            customLists
        } else {
            listOf(TaskItem.DEFAULT_LIST) + customLists
        }
    }
}

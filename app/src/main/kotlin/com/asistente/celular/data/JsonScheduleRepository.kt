package com.asistente.celular.data

import android.content.Context
import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import com.asistente.celular.nlu.schedule.ScheduleRepository
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
 * Implementación offline-first y reactiva de ScheduleRepository basada en JSON en disco privado.
 * Concurrencia segura mediante Mutex y escritura atómica (tmp -> replace).
 * Garantiza resiliencia de datos y persistencia íntegra ante actualizaciones de la aplicación.
 */
class JsonScheduleRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ScheduleRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val file = File(context.filesDir, "schedule_activities.json")
    private val mutex = Mutex()
    private val _activities = MutableStateFlow<List<ScheduleActivityItem>>(emptyList())
    override val activities: StateFlow<List<ScheduleActivityItem>> = _activities.asStateFlow()

    init {
        coroutineScope.launch(ioDispatcher) {
            loadFromDisk()
        }
    }

    private suspend fun loadFromDisk() = withContext(ioDispatcher) {
        mutex.withLock {
            if (file.exists()) {
                try {
                    val content = file.readText()
                    if (content.isNotBlank()) {
                        val parsed = json.decodeFromString<List<ScheduleActivityItem>>(content)
                        _activities.value = parsed
                    }
                } catch (e: Exception) {
                    android.util.Log.e("JsonScheduleRepository", "Error leyendo actividades de horario: ${e.message}")
                }
            } else {
                // Actividades iniciales sugeridas para dar contexto inmediato al usuario
                val defaults = listOf(
                    ScheduleActivityItem(
                        title = "Planificación y Enfoque",
                        description = "Revisión de metas diarias y prioridades en Hendrix",
                        daysOfWeek = ScheduleDay.WEEKDAYS,
                        startTime = "09:00",
                        endTime = "09:45",
                        category = "Trabajo",
                        colorHex = "#00F0FF"
                    ),
                    ScheduleActivityItem(
                        title = "Bloque de Desarrollo / Deep Work",
                        description = "Sesión ininterrumpida de programación y arquitectura",
                        daysOfWeek = ScheduleDay.WEEKDAYS,
                        startTime = "10:00",
                        endTime = "13:30",
                        category = "Desarrollo / Código",
                        colorHex = "#A855F7"
                    ),
                    ScheduleActivityItem(
                        title = "Sesión de Ejercicio y Bienestar",
                        description = "Entrenamiento físico y caminata",
                        daysOfWeek = setOf(ScheduleDay.MONDAY, ScheduleDay.WEDNESDAY, ScheduleDay.FRIDAY),
                        startTime = "18:00",
                        endTime = "19:15",
                        category = "Salud / Gimnasio",
                        colorHex = "#10B981"
                    )
                )
                _activities.value = defaults
                saveToDiskLocked(defaults)
            }
        }
    }

    private suspend fun saveToDiskLocked(list: List<ScheduleActivityItem>) = withContext(ioDispatcher) {
        try {
            val serialized = json.encodeToString(list)
            val tempFile = File(context.filesDir, "schedule_activities.json.tmp")
            tempFile.writeText(serialized)
            if (tempFile.renameTo(file) || run { file.delete(); tempFile.renameTo(file) }) {
                // Guardado atómico completado
            } else {
                file.writeText(serialized)
            }
        } catch (e: Exception) {
            android.util.Log.e("JsonScheduleRepository", "Error persistiendo horario: ${e.message}")
        }
    }

    override suspend fun addActivity(activity: ScheduleActivityItem): ScheduleActivityItem {
        mutex.withLock {
            val updated = _activities.value + activity
            _activities.value = updated
            saveToDiskLocked(updated)
        }
        return activity
    }

    override suspend fun updateActivity(activity: ScheduleActivityItem): Boolean {
        var found = false
        mutex.withLock {
            val updated = _activities.value.map {
                if (it.id == activity.id) {
                    found = true
                    activity.copy(updatedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
            if (found) {
                _activities.value = updated
                saveToDiskLocked(updated)
            }
        }
        return found
    }

    override suspend fun deleteActivity(id: String): Boolean {
        var removed = false
        mutex.withLock {
            val prevSize = _activities.value.size
            val updated = _activities.value.filter { it.id != id }
            if (updated.size < prevSize) {
                removed = true
                _activities.value = updated
                saveToDiskLocked(updated)
            }
        }
        return removed
    }

    override suspend fun toggleActivityEnabled(id: String): ScheduleActivityItem? {
        var toggledItem: ScheduleActivityItem? = null
        mutex.withLock {
            val updated = _activities.value.map {
                if (it.id == id) {
                    val toggled = it.copy(
                        isEnabled = !it.isEnabled,
                        updatedAt = System.currentTimeMillis()
                    )
                    toggledItem = toggled
                    toggled
                } else {
                    it
                }
            }
            if (toggledItem != null) {
                _activities.value = updated
                saveToDiskLocked(updated)
            }
        }
        return toggledItem
    }

    override suspend fun getActivityById(id: String): ScheduleActivityItem? {
        return _activities.value.firstOrNull { it.id == id }
    }

    override suspend fun getActivitiesForDay(day: ScheduleDay): List<ScheduleActivityItem> {
        return _activities.value
            .filter { it.occursOn(day) }
            .sortedBy { it.startTime }
    }

    override suspend fun getAllCategories(): List<String> {
        val custom = _activities.value.map { it.category }.distinct()
        return (ScheduleActivityItem.PRESET_CATEGORIES + custom).distinct()
    }
}

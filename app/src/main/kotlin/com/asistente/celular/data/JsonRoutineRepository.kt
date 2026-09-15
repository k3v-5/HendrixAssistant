package com.asistente.celular.data

import android.content.Context
import com.asistente.celular.nlu.routines.RoutineAction
import com.asistente.celular.nlu.routines.RoutineItem
import com.asistente.celular.nlu.routines.RoutineRepository
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
 * Implementación JSON reactiva y persistente de RoutineRepository.
 * Almacena las rutinas de usuario en 'routines.json' con escritura atómica y Mutex.
 */
class JsonRoutineRepository(
    private val context: Context,
    private val scope: CoroutineScope
) : RoutineRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file = File(context.filesDir, "routines.json")
    private val mutex = Mutex()
    private val _routines = MutableStateFlow<List<RoutineItem>>(emptyList())
    override val routines: StateFlow<List<RoutineItem>> = _routines.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            loadRoutines()
        }
    }

    override suspend fun getRoutineById(id: String): RoutineItem? {
        return _routines.value.find { it.id == id }
    }

    override suspend fun findMatchingRoutine(inputPhrase: String): RoutineItem? {
        val clean = inputPhrase.lowercase().trim()
        val all = _routines.value.filter { it.isEnabled }

        // Coincidencia exacta o por frase detonante
        for (r in all) {
            if (r.name.equals(clean, ignoreCase = true)) return r
            for (trigger in r.triggerPhrases) {
                val tClean = trigger.lowercase().trim()
                if (clean == tClean || clean.contains(tClean)) return r
            }
        }
        return null
    }

    override suspend fun saveRoutine(routine: RoutineItem) {
        mutex.withLock {
            val current = _routines.value.toMutableList()
            val index = current.indexOfFirst { it.id == routine.id }
            if (index >= 0) {
                current[index] = routine
            } else {
                current.add(routine)
            }
            _routines.value = current
            persistToFile(current)
        }
    }

    override suspend fun deleteRoutine(id: String): Boolean {
        return mutex.withLock {
            val current = _routines.value.toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                _routines.value = current
                persistToFile(current)
            }
            removed
        }
    }

    private suspend fun loadRoutines() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.exists()) {
                val defaults = createDefaultRoutines()
                _routines.value = defaults
                persistToFile(defaults)
                return@withContext
            }

            try {
                val content = file.readText()
                val list = json.decodeFromString<List<RoutineItem>>(content)
                _routines.value = list
            } catch (_: Exception) {
                val defaults = createDefaultRoutines()
                _routines.value = defaults
            }
        }
    }

    private fun persistToFile(list: List<RoutineItem>) {
        try {
            val content = json.encodeToString(list)
            val tempFile = File(context.filesDir, "routines.json.tmp")
            tempFile.writeText(content)
            tempFile.renameTo(file)
        } catch (_: Exception) {}
    }

    private fun createDefaultRoutines(): List<RoutineItem> {
        return listOf(
            RoutineItem(
                id = "routine_good_night",
                name = "Buenas Noches",
                description = "Silencia el teléfono, ajusta el volumen al 15% y confirma el descanso.",
                triggerPhrases = listOf("buenas noches", "modo noche", "hora de dormir", "a dormir"),
                actions = listOf(
                    RoutineAction.ExecuteCommandAction("silencia el telefono"),
                    RoutineAction.ExecuteCommandAction("volumen al 15"),
                    RoutineAction.SpeakAction("Buenas noches. He activado el modo silencio y bajado el volumen para que descanses.")
                )
            ),
            RoutineItem(
                id = "routine_good_morning",
                name = "Buenos Días",
                description = "Ajusta el volumen matutino al 60% y saluda.",
                triggerPhrases = listOf("buenos dias", "buen dia", "despertar"),
                actions = listOf(
                    RoutineAction.ExecuteCommandAction("volumen al 60"),
                    RoutineAction.SpeakAction("¡Buenos días! Que tengas una jornada muy productiva.")
                )
            ),
            RoutineItem(
                id = "routine_focus_mode",
                name = "Modo Concentración",
                description = "Silencia notificaciones y arranca un temporizador Pomodoro de 25 minutos.",
                triggerPhrases = listOf("modo estudio", "modo concentracion", "hora de estudiar", "modo trabajo"),
                actions = listOf(
                    RoutineAction.ExecuteCommandAction("silencia el telefono"),
                    RoutineAction.ExecuteCommandAction("temporizador de 25 minutos"),
                    RoutineAction.SpeakAction("Modo concentración activado. Temporizador Pomodoro de 25 minutos en marcha.")
                )
            )
        )
    }
}

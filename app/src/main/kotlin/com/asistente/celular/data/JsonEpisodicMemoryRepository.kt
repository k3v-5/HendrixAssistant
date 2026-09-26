package com.asistente.celular.data

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.memory.episodic.CreativeSessionEntry
import com.asistente.celular.nlu.memory.episodic.EpisodicMemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repositorio persistente en JSON para la Memoria Episódica de Hendrix.
 * Guarda en disco las sesiones y proyectos recientes en DAWs, motores 3D y editores de código.
 */
class JsonEpisodicMemoryRepository(
    private val context: Context,
    private val scope: CoroutineScope
) : EpisodicMemoryRepository {

    companion object {
        private const val TAG = "JsonEpisodicMemoryRepo"
        private const val FILE_NAME = "episodic_creative_sessions.json"
        private const val MAX_HISTORY = 30
    }

    private val file = File(context.filesDir, FILE_NAME)
    private val _sessions = MutableStateFlow<List<CreativeSessionEntry>>(emptyList())
    val sessions: StateFlow<List<CreativeSessionEntry>> = _sessions.asStateFlow()

    init {
        scope.launch {
            loadSessions()
        }
    }

    private suspend fun loadSessions() = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            _sessions.value = emptyList()
            return@withContext
        }

        try {
            val content = file.readText()
            val arr = JSONArray(content)
            val list = mutableListOf<CreativeSessionEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseEntry(obj))
            }
            _sessions.value = list.sortedByDescending { it.lastActiveEpochMillis }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando memoria episódica: ${e.message}", e)
            _sessions.value = emptyList()
        }
    }

    override suspend fun recordSession(entry: CreativeSessionEntry) = withContext(Dispatchers.IO) {
        val current = _sessions.value.toMutableList()
        // Remover si ya existía para esa app y proyecto para actualizar la marca temporal
        current.removeAll { it.appName.equals(entry.appName, ignoreCase = true) && it.projectTitle.equals(entry.projectTitle, ignoreCase = true) }
        current.add(0, entry.copy(lastActiveEpochMillis = System.currentTimeMillis()))

        val trimmed = current.take(MAX_HISTORY)
        _sessions.value = trimmed
        saveToFile(trimmed)
    }

    override suspend fun getRecentSessions(limit: Int): List<CreativeSessionEntry> {
        return _sessions.value.take(limit)
    }

    override suspend fun getLastActiveSession(): CreativeSessionEntry? {
        return _sessions.value.firstOrNull()
    }

    override suspend fun getSessionForApp(appName: String): CreativeSessionEntry? {
        val query = appName.lowercase().trim()
        return _sessions.value.firstOrNull {
            it.appName.lowercase().contains(query) || query.contains(it.appName.lowercase())
        }
    }

    override suspend fun clearSessions() = withContext(Dispatchers.IO) {
        _sessions.value = emptyList()
        saveToFile(emptyList())
    }

    private fun saveToFile(list: List<CreativeSessionEntry>) {
        try {
            val arr = JSONArray()
            for (entry in list) {
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("appName", entry.appName)
                    put("projectTitle", entry.projectTitle)
                    put("projectPath", entry.projectPath ?: "")
                    put("lastActiveEpochMillis", entry.lastActiveEpochMillis)
                    put("notesSummary", entry.notesSummary ?: "")
                    put("gitBranchOrState", entry.gitBranchOrState ?: "")
                }
                arr.put(obj)
            }
            file.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando memoria episódica: ${e.message}", e)
        }
    }

    private fun parseEntry(obj: JSONObject): CreativeSessionEntry {
        return CreativeSessionEntry(
            id = obj.optString("id", ""),
            appName = obj.optString("appName", "App"),
            projectTitle = obj.optString("projectTitle", "Proyecto"),
            projectPath = obj.optString("projectPath", "").ifEmpty { null },
            lastActiveEpochMillis = obj.optLong("lastActiveEpochMillis", System.currentTimeMillis()),
            notesSummary = obj.optString("notesSummary", "").ifEmpty { null },
            gitBranchOrState = obj.optString("gitBranchOrState", "").ifEmpty { null }
        )
    }
}

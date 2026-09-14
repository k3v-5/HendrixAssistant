package com.asistente.celular.data

import android.content.Context
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
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
 * Implementación offline-first y reactiva de NoteRepository basada en JSON en disco privado.
 * Concurrencia segura mediante Mutex y escritura atómica.
 */
class JsonNoteRepository(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : NoteRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val file = File(context.filesDir, "notes.json")
    private val mutex = Mutex()
    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    override val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

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
                    val parsed = json.decodeFromString<List<NoteItem>>(content)
                    _notes.value = parsed
                } catch (e: Exception) {
                    android.util.Log.e("JsonNoteRepository", "Error leyendo notas: ${e.message}")
                }
            } else {
                val defaults = listOf(
                    NoteItem(
                        title = "Bienvenido a Notas",
                        content = "Aquí puedes guardar apuntes rápidos, ideas y listas dictadas por voz.",
                        isPinned = true
                    ),
                    NoteItem(
                        title = "Ejemplo de dictado",
                        content = "Di: 'Oye Hendrix, toma una nota: la clave del estacionamiento es 4567'",
                        isPinned = false
                    )
                )
                _notes.value = defaults
                saveToDiskLocked(defaults)
            }
        }
    }

    private suspend fun saveToDiskLocked(list: List<NoteItem>) = withContext(Dispatchers.IO) {
        try {
            val serialized = json.encodeToString(list)
            val tempFile = File(context.filesDir, "notes.json.tmp")
            tempFile.writeText(serialized)
            if (tempFile.renameTo(file) || run { file.delete(); tempFile.renameTo(file) }) {
                // Guardado atómico exitoso
            } else {
                file.writeText(serialized)
            }
        } catch (e: Exception) {
            android.util.Log.e("JsonNoteRepository", "Error guardando notas: ${e.message}")
        }
    }

    override suspend fun addNote(note: NoteItem): NoteItem {
        mutex.withLock {
            val updated = listOf(note) + _notes.value
            _notes.value = updated
            saveToDiskLocked(updated)
        }
        return note
    }

    override suspend fun updateNote(note: NoteItem) {
        mutex.withLock {
            val updated = _notes.value.map { if (it.id == note.id) note.copy(updatedAt = System.currentTimeMillis()) else it }
            _notes.value = updated
            saveToDiskLocked(updated)
        }
    }

    override suspend fun deleteNote(id: String) {
        mutex.withLock {
            val updated = _notes.value.filter { it.id != id }
            _notes.value = updated
            saveToDiskLocked(updated)
        }
    }

    override suspend fun getNoteById(id: String): NoteItem? {
        return _notes.value.find { it.id == id }
    }

    override suspend fun togglePin(id: String): NoteItem? {
        var modified: NoteItem? = null
        mutex.withLock {
            val current = _notes.value.find { it.id == id } ?: return null
            val updated = current.copy(isPinned = !current.isPinned, updatedAt = System.currentTimeMillis())
            val updatedList = _notes.value.map { if (it.id == id) updated else it }
            _notes.value = updatedList
            saveToDiskLocked(updatedList)
            modified = updated
        }
        return modified
    }

    override suspend fun searchNotes(query: String): List<NoteItem> {
        val q = query.trim().lowercase()
        return _notes.value.filter {
            it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
        }
    }
}

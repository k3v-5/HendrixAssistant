package com.asistente.celular.nlu.notes

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de repositorio desacoplado para operaciones CRUD y reactivas de Notas.
 */
interface NoteRepository {
    val notes: StateFlow<List<NoteItem>>

    suspend fun addNote(note: NoteItem): NoteItem
    suspend fun updateNote(note: NoteItem)
    suspend fun deleteNote(id: String)
    suspend fun getNoteById(id: String): NoteItem?
    suspend fun togglePin(id: String): NoteItem?
    suspend fun searchNotes(query: String): List<NoteItem>
}

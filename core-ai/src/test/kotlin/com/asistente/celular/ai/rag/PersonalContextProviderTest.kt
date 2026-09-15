package com.asistente.celular.ai.rag

import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.tasks.TaskPriority
import com.asistente.celular.nlu.tasks.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class PersonalContextProviderTest {

    private val fakeTaskRepo = object : TaskRepository {
        val list = MutableStateFlow(listOf(
            TaskItem(
                title = "Pagar el internet",
                dueDateMillis = System.currentTimeMillis() - 3600_000L, // Vencida
                priority = TaskPriority.URGENT
            ),
            TaskItem(
                title = "Comprar café",
                dueDateMillis = System.currentTimeMillis() + 86400_000L, // Mañana
                priority = TaskPriority.NORMAL
            ),
            TaskItem(
                title = "Tarea ya hecha",
                isCompleted = true
            )
        ))
        override val tasks: StateFlow<List<TaskItem>> = list

        override suspend fun addTask(task: TaskItem): TaskItem = task
        override suspend fun updateTask(task: TaskItem) {}
        override suspend fun deleteTask(id: String) {}
        override suspend fun getTaskById(id: String): TaskItem? = null
        override suspend fun toggleTaskCompletion(id: String): TaskItem? = null
        override suspend fun clearCompletedTasks() {}
        override suspend fun getLists(): List<String> = listOf("Mis Tareas")
    }

    private val fakeNoteRepo = object : NoteRepository {
        val list = MutableStateFlow(listOf(
            NoteItem(
                title = "Clave del porton",
                content = "El código es 9988*",
                isPinned = true
            ),
            NoteItem(
                title = "Lista de super",
                content = "Manzanas, peras y pan"
            )
        ))
        override val notes: StateFlow<List<NoteItem>> = list

        override suspend fun addNote(note: NoteItem): NoteItem = note
        override suspend fun updateNote(note: NoteItem) {}
        override suspend fun deleteNote(id: String) {}
        override suspend fun getNoteById(id: String): NoteItem? = null
        override suspend fun togglePin(id: String): NoteItem? = null
        override suspend fun searchNotes(query: String): List<NoteItem> = emptyList()
    }

    @Test
    fun testBuildEnrichedSystemPromptContainsTasksAndPinnedNotes() = runBlocking {
        val provider = DefaultPersonalContextProvider(fakeTaskRepo, fakeNoteRepo, ZoneId.of("UTC"))
        val enriched = provider.buildEnrichedSystemPrompt(
            baseSystemPrompt = "Eres Hendrix, asistente virtual.",
            userQuery = "¿Qué pendientes tengo y cuál es la clave del portón?"
        )

        assertTrue("Debe incluir el prompt base", enriched.contains("Eres Hendrix, asistente virtual."))
        assertTrue("Debe listar tarea pendiente urgente", enriched.contains("Pagar el internet"))
        assertTrue("Debe identificar tarea vencida", enriched.contains("VENCIDA"))
        assertTrue("Debe listar tarea pendiente futura", enriched.contains("Comprar café"))
        assertFalse("No debe listar tareas ya completadas", enriched.contains("Tarea ya hecha"))
        assertTrue("Debe incluir nota fijada", enriched.contains("9988*"))
    }
}

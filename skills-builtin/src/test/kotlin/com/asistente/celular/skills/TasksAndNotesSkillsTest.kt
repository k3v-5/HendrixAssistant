package com.asistente.celular.skills

import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.tasks.TaskRepository
import com.asistente.celular.skills.notes.NotesSkill
import com.asistente.celular.skills.tasks.TasksSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksAndNotesSkillsTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class InMemoryTaskRepository : TaskRepository {
        private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
        override val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

        override suspend fun addTask(task: TaskItem): TaskItem {
            _tasks.value = _tasks.value + task
            return task
        }

        override suspend fun updateTask(task: TaskItem) {
            _tasks.value = _tasks.value.map { if (it.id == task.id) task else it }
        }

        override suspend fun deleteTask(id: String) {
            _tasks.value = _tasks.value.filter { it.id != id }
        }

        override suspend fun getTaskById(id: String): TaskItem? {
            return _tasks.value.find { it.id == id }
        }

        override suspend fun toggleTaskCompletion(id: String): TaskItem? {
            val task = getTaskById(id) ?: return null
            val updated = task.copy(isCompleted = !task.isCompleted)
            updateTask(updated)
            return updated
        }

        override suspend fun clearCompletedTasks() {
            _tasks.value = _tasks.value.filter { !it.isCompleted }
        }

        override suspend fun getLists(): List<String> {
            return _tasks.value.map { it.listName }.distinct()
        }
    }

    private class InMemoryNoteRepository : NoteRepository {
        private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
        override val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

        override suspend fun addNote(note: NoteItem): NoteItem {
            _notes.value = _notes.value + note
            return note
        }

        override suspend fun updateNote(note: NoteItem) {
            _notes.value = _notes.value.map { if (it.id == note.id) note else it }
        }

        override suspend fun deleteNote(id: String) {
            _notes.value = _notes.value.filter { it.id != id }
        }

        override suspend fun getNoteById(id: String): NoteItem? {
            return _notes.value.find { it.id == id }
        }

        override suspend fun togglePin(id: String): NoteItem? {
            val note = getNoteById(id) ?: return null
            val updated = note.copy(isPinned = !note.isPinned)
            updateNote(updated)
            return updated
        }

        override suspend fun searchNotes(query: String): List<NoteItem> {
            return _notes.value.filter { it.content.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true) }
        }
    }

    @Test
    fun testTasksSkillMatchAndCreate() = runBlocking {
        val repo = InMemoryTaskRepository()
        val skill = TasksSkill(taskRepository = repo)

        val score = skill.score(dummyContext, "recuérdame comprar leche mañana a las 5 pm")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "recuérdame comprar leche mañana a las 5 pm", score)
        assertTrue(output.success)
        assertEquals(1, repo.tasks.value.size)

        val task = repo.tasks.value.first()
        assertTrue(task.title.contains("Comprar leche", ignoreCase = true))
        assertNotNull(task.dueDateMillis)
    }

    @Test
    fun testTasksSkillQueryAndComplete() = runBlocking {
        val repo = InMemoryTaskRepository()
        val skill = TasksSkill(taskRepository = repo)

        repo.addTask(TaskItem(title = "Pagar el internet"))

        val queryScore = skill.score(dummyContext, "cuáles son mis tareas")
        assertTrue(queryScore.isMatch)

        val queryOutput = skill.execute(dummyContext, "cuáles son mis tareas", queryScore)
        assertTrue(queryOutput.displayText.contains("Pagar el internet"))

        val completeScore = skill.score(dummyContext, "completa la tarea pagar el internet")
        assertTrue(completeScore.isMatch)

        val completeOutput = skill.execute(dummyContext, "completa la tarea pagar el internet", completeScore)
        assertTrue(completeOutput.success)
        assertTrue(repo.tasks.value.first().isCompleted)
    }

    @Test
    fun testNotesSkillMatchAndCreate() = runBlocking {
        val repo = InMemoryNoteRepository()
        val skill = NotesSkill(noteRepository = repo)

        val score = skill.score(dummyContext, "toma una nota: código de puerta 1234")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "toma una nota: código de puerta 1234", score)
        assertTrue(output.success)
        assertEquals(1, repo.notes.value.size)

        val note = repo.notes.value.first()
        assertTrue(note.content.contains("código de puerta 1234", ignoreCase = true))
    }

    @Test
    fun testCreemeUnaNotaMatchAndCreate() = runBlocking {
        val repo = InMemoryNoteRepository()
        val skill = NotesSkill(noteRepository = repo)

        val phrase = "créeme una nota sobre lo que tengo que hacer en el súper"
        val score = skill.score(dummyContext, phrase)
        assertTrue("Debe coincidir con la frase del usuario", score.isMatch)

        val output = skill.execute(dummyContext, phrase, score)
        assertTrue(output.success)
        assertEquals(1, repo.notes.value.size)

        val note = repo.notes.value.first()
        assertTrue(note.content.contains("lo que tengo que hacer en el súper", ignoreCase = true))
    }

    @Test
    fun testHazmeUnaTareaMatchAndCreate() = runBlocking {
        val repo = InMemoryTaskRepository()
        val skill = TasksSkill(taskRepository = repo)

        val phrase = "hazme una tarea comprar leche"
        val score = skill.score(dummyContext, phrase)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, phrase, score)
        assertTrue(output.success)
        assertEquals(1, repo.tasks.value.size)

        val task = repo.tasks.value.first()
        assertTrue(task.title.contains("Comprar leche", ignoreCase = true))
    }

    @Test
    fun testMultiTurnTaskCreationPromptWhenTitleMissing() = runBlocking {
        val repo = InMemoryTaskRepository()
        val skill = TasksSkill(taskRepository = repo)

        // Turno 1: Usuario dice "Agrégame una tarea para mañana" sin especificar el título
        val prompt1 = "agrégame una tarea para mañana"
        val score1 = skill.score(dummyContext, prompt1)
        assertTrue(score1.isMatch)

        val output1 = skill.execute(dummyContext, prompt1, score1)
        assertTrue(output1.success)
        // No debe crear ninguna tarea titulada 'Mañana'
        assertEquals(0, repo.tasks.value.size)
        // Debe pedir el título y reabrir el micrófono
        assertTrue(output1.interactionPlan is com.asistente.celular.nlu.skill.InteractionPlan.ReopenMicrophone)
        assertTrue(output1.speech.contains("¿Qué tarea deseas que te recuerde"))

        // Turno 2: Usuario responde con el título de la tarea
        val multiTurnContext = object : SkillContext {
            override val androidContext: android.content.Context get() = error("Dummy")
            override val isConnectedToInternet: Boolean = false
            override val previousOutput: SkillOutput? = output1
        }

        val prompt2 = "comprar leche"
        val score2 = skill.score(multiTurnContext, prompt2)
        assertTrue("El segundo turno debe ser capturado por la habilidad de tareas", score2.isMatch)

        val output2 = skill.execute(multiTurnContext, prompt2, score2)
        assertTrue(output2.success)
        assertEquals(1, repo.tasks.value.size)

        val savedTask = repo.tasks.value.first()
        assertEquals("Comprar leche", savedTask.title)
        assertNotNull(savedTask.dueDateMillis)
    }
}

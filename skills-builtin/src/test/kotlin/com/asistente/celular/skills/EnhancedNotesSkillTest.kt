package com.asistente.celular.skills

import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.notes.NoteRepository
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.notes.NotesSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedNotesSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class MockNoteRepository : NoteRepository {
        private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
        override val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

        override suspend fun addNote(note: NoteItem): NoteItem {
            _notes.value = listOf(note) + _notes.value
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
            val item = getNoteById(id) ?: return null
            val updated = item.copy(isPinned = !item.isPinned)
            updateNote(updated)
            return updated
        }

        override suspend fun searchNotes(query: String): List<NoteItem> {
            val q = query.trim().lowercase()
            return _notes.value.filter {
                it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
            }
        }
    }

    @Test
    fun testCreateNoteNaturalPhrasing() = runBlocking {
        val repo = MockNoteRepository()
        val skill = NotesSkill(repo)

        val input = "anota comprar leche y pan"
        val score = skill.score(dummyContext, input)
        assertTrue("Confidence should be >= 0.90: ${score.confidence}", score.confidence >= 0.90f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertEquals(1, repo.notes.value.size)
        assertTrue(repo.notes.value.first().content.contains("Comprar leche y pan", ignoreCase = true))
    }

    @Test
    fun testSearchNoteByTopic() = runBlocking {
        val repo = MockNoteRepository()
        val skill = NotesSkill(repo)

        repo.addNote(NoteItem(title = "Estacionamiento", content = "El auto quedó en el piso 3 zona B"))
        repo.addNote(NoteItem(title = "Supermercado", content = "Comprar café descafeinado"))

        val input = "qué anoté sobre el estacionamiento"
        val score = skill.score(dummyContext, input)
        assertTrue("Confidence should be >= 0.90: ${score.confidence}", score.confidence >= 0.90f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue("Speech should mention piso 3: ${output.speech}", output.speech.contains("piso 3"))
    }

    @Test
    fun testListNotes() = runBlocking {
        val repo = MockNoteRepository()
        val skill = NotesSkill(repo)

        repo.addNote(NoteItem(title = "Idea", content = "Diseñar widget flotante"))

        val input = "cuáles son mis notas"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.90f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("widget flotante"))
    }

    @Test
    fun testDeleteNote() = runBlocking {
        val repo = MockNoteRepository()
        val skill = NotesSkill(repo)

        val note = NoteItem(title = "Clave", content = "Clave del portón 5544")
        repo.addNote(note)
        assertEquals(1, repo.notes.value.size)

        val input = "borra la nota de la clave"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.90f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertEquals(0, repo.notes.value.size)
    }

    @Test
    fun testPinNote() = runBlocking {
        val repo = MockNoteRepository()
        val skill = NotesSkill(repo)

        val note = NoteItem(title = "Urgente", content = "Pagar el servicio de internet", isPinned = false)
        repo.addNote(note)

        val input = "fija la nota de urgente"
        val score = skill.score(dummyContext, input)
        assertTrue(score.confidence >= 0.90f)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue(repo.notes.value.first().isPinned)
    }

    @Test
    fun testGeneralQueriesDoNotMatchNotesSkill() = runBlocking {
        val repo = MockNoteRepository()
        val skill = NotesSkill(repo)

        val nonNoteInputs = listOf(
            "qué día es hoy",
            "que dia es hoy",
            "qué hora es",
            "dime la hora",
            "cuál es la fecha",
            "qué tiempo hace",
            "cuéntame un chiste",
            "hola cómo estás",
            "para el que era la materializacion"
        )

        for (input in nonNoteInputs) {
            val score = skill.score(dummyContext, input)
            assertFalse(
                "La frase '$input' NO debe coincidir con NotesSkill (confidence=${score.confidence})",
                score.isMatch
            )
        }
    }
}

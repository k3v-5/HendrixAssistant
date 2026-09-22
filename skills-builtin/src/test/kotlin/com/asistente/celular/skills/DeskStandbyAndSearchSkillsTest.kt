package com.asistente.celular.skills

import android.content.Context
import android.content.ContextWrapper
import com.asistente.celular.ai.memory.FeatureHashingEmbeddingEngine
import com.asistente.celular.ai.memory.PersonalRagCoordinator
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.skills.memory.PersonalSearchSkill
import com.asistente.celular.skills.pc.DeskStandbySkill
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DeskStandbyAndSearchSkillsTest {

    private lateinit var mockContext: SkillContext
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("skills_test").toFile()
        val androidCtx = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
        mockContext = object : SkillContext {
            override val androidContext: Context = androidCtx
            override val isConnectedToInternet: Boolean = true
            override val previousOutput: SkillOutput? = null
        }
    }

    @Test
    fun `test DeskStandbySkill score and execute`() = runBlocking {
        var standbyTriggered = false
        val skill = DeskStandbySkill(onActivateStandby = { standbyTriggered = true })

        val score = skill.score(mockContext, "activa el modo escritorio")
        assertTrue(score.isMatch)

        val output = skill.execute(mockContext, "activa el modo escritorio")
        assertTrue(standbyTriggered)
        assertTrue(output.speech.contains("pantalla inteligente"))
        assertEquals("ACTION_DESK_STANDBY", output.payload)
    }

    @Test
    fun `test PersonalSearchSkill score and retrieve notes`() = runBlocking {
        val rag = PersonalRagCoordinator(embeddingEngine = FeatureHashingEmbeddingEngine(dimensions = 128))
        rag.indexNote(
            id = "note_guitar",
            title = "Acordes de Guitarra",
            content = "Progresión de acordes para canción acústica",
            tags = listOf("musica")
        )

        val skill = PersonalSearchSkill(ragCoordinator = rag)

        val score = skill.score(mockContext, "busca en mis notas acordes de guitarra")
        assertTrue(score.isMatch)

        val output = skill.execute(mockContext, "busca en mis notas acordes de guitarra")
        assertTrue(output.speech.contains("Acordes de Guitarra") || output.speech.contains("resultados relevantes"))
    }
}

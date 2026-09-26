package com.asistente.celular.data

import com.asistente.celular.nlu.memory.episodic.CreativeProjectTitleParser
import com.asistente.celular.nlu.memory.episodic.CreativeSessionEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class JsonEpisodicMemoryRepositoryTest {

    @Test
    fun testParseCreativeWindowTitles() {
        // Unreal Engine
        val ueEntry = CreativeProjectTitleParser.parseWindowTitle("Unreal Editor - MyRPG_Level01", "UnrealEditor.exe")
        assertNotNull(ueEntry)
        assertEquals("Unreal Engine", ueEntry?.appName)
        assertEquals("MyRPG_Level01", ueEntry?.projectTitle)

        // Blender
        val blEntry = CreativeProjectTitleParser.parseWindowTitle("Blender - [D:\\Projects\\Character_Mesh.blend]", "blender.exe")
        assertNotNull(blEntry)
        assertEquals("Blender", blEntry?.appName)
        assertEquals("Character_Mesh", blEntry?.projectTitle)

        // Ableton Live
        val ablEntry = CreativeProjectTitleParser.parseWindowTitle("Ableton Live 11 Suite - Synthwave_Track_V2.als", "Ableton Live.exe")
        assertNotNull(ablEntry)
        assertEquals("Ableton Live", ablEntry?.appName)
        assertEquals("Synthwave_Track_V2", ablEntry?.projectTitle)

        // FL Studio
        val flEntry = CreativeProjectTitleParser.parseWindowTitle("FL Studio 21 - Trap_Beats_Draft.flp", "FL64.exe")
        assertNotNull(flEntry)
        assertEquals("FL Studio", flEntry?.appName)
        assertEquals("Trap_Beats_Draft", flEntry?.projectTitle)
    }

    @Test
    fun testRecordAndQueryRecentSessions() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val tempDir = File(System.getProperty("java.io.tmpdir"), "hendrix_episodic_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val dummyContext = object : android.content.ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }

        val repository = JsonEpisodicMemoryRepository(dummyContext, testScope)
        testScope.testScheduler.runCurrent()

        val entry1 = CreativeSessionEntry(
            id = "ue_1",
            appName = "Unreal Engine",
            projectTitle = "CityMap",
            lastActiveEpochMillis = System.currentTimeMillis() - 10000
        )
        val entry2 = CreativeSessionEntry(
            id = "bl_1",
            appName = "Blender",
            projectTitle = "CyberCar",
            lastActiveEpochMillis = System.currentTimeMillis()
        )

        repository.recordSession(entry1)
        repository.recordSession(entry2)
        testScope.testScheduler.runCurrent()

        val lastActive = repository.getLastActiveSession()
        assertNotNull(lastActive)
        assertEquals("CyberCar", lastActive?.projectTitle)

        val ueSession = repository.getSessionForApp("unreal")
        assertNotNull(ueSession)
        assertEquals("CityMap", ueSession?.projectTitle)

        val recent = repository.getRecentSessions(limit = 5)
        assertEquals(2, recent.size)

        tempDir.deleteRecursively()
    }
}

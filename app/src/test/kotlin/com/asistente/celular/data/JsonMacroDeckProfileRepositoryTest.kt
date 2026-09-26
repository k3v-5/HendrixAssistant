package com.asistente.celular.data

import android.content.Context
import android.content.ContextWrapper
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckAction
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckControl
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfile
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class JsonMacroDeckProfileRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var mockContext: Context
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("macro_deck_repo_test").toFile()
        mockContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testInitialDefaultsLoaded() = testScope.runTest {
        val repo = JsonMacroDeckProfileRepository(mockContext, testScope)
        repo.initJob.join()
        val initialProfiles = repo.profiles.value

        assertTrue(initialProfiles.isNotEmpty())
        assertEquals(MacroDeckProfileRegistry.ALL_PROFILES.size, initialProfiles.size)
        assertNotNull(repo.getProfileById("profile_blender"))
        assertNotNull(repo.getProfileById("profile_code"))
        assertNotNull(repo.getProfileById("profile_unreal"))
    }

    @Test
    fun testSaveCustomProfileAndMatchProcess() = testScope.runTest {
        val repo = JsonMacroDeckProfileRepository(mockContext, testScope)
        repo.initJob.join()

        val customProfile = MacroDeckProfile(
            id = "profile_godot",
            name = "Godot Engine 4",
            iconEmoji = "🤖",
            targetProcessRegex = "(?i).*godot.*",
            headerSubtitle = "Superficie de Control de Nivel y Escenas",
            themeAccentColorHex = 0xFF478CBF,
            controls = listOf(
                MacroDeckControl.MacroButton(
                    id = "gd_play",
                    label = "Play Escena",
                    iconEmoji = "▶️",
                    colorHex = 0xFF478CBF,
                    action = MacroDeckAction.ShortcutAction("F5", "Play Project")
                ),
                MacroDeckControl.MacroFader(
                    id = "gd_zoom",
                    label = "Zoom 2D",
                    iconEmoji = "🔍",
                    colorHex = 0xFFF59E0B,
                    targetParameter = "zoom",
                    minValue = 0.1f,
                    maxValue = 4f,
                    initialValue = 1f
                )
            )
        )

        repo.saveProfile(customProfile)

        val updatedProfiles = repo.profiles.value
        assertEquals(MacroDeckProfileRegistry.ALL_PROFILES.size + 1, updatedProfiles.size)
        assertEquals("Godot Engine 4", repo.getProfileById("profile_godot")?.name)

        // Comprobar coincidencia dinámica de proceso
        val matched = repo.findProfileForProcess("C:\\Godot\\Godot_v4.2.exe")
        assertEquals("profile_godot", matched.id)
        assertEquals(2, matched.controls.size)
    }

    @Test
    fun testDeleteAndResetToDefaults() = testScope.runTest {
        val repo = JsonMacroDeckProfileRepository(mockContext, testScope)
        repo.initJob.join()

        val tempProfile = MacroDeckProfile(
            id = "profile_temp",
            name = "Temp Profile",
            iconEmoji = "🧪",
            targetProcessRegex = "(?i).*temp.*",
            headerSubtitle = "Temporal",
            themeAccentColorHex = 0xFFEF4444,
            controls = emptyList()
        )

        repo.saveProfile(tempProfile)
        assertNotNull(repo.getProfileById("profile_temp"))

        repo.deleteProfile("profile_temp")
        assertTrue(repo.profiles.value.none { it.id == "profile_temp" })

        // Restaurar valores de fábrica
        repo.resetToDefaults()
        assertEquals(MacroDeckProfileRegistry.ALL_PROFILES.size, repo.profiles.value.size)
    }

    @Test
    fun testFallbackForUnmatchedProcess() = testScope.runTest {
        val repo = JsonMacroDeckProfileRepository(mockContext, testScope)
        repo.initJob.join()

        val fallback = repo.findProfileForProcess("calculator.exe")
        assertEquals(MacroDeckProfileRegistry.DEFAULT_DESKTOP_PROFILE.id, fallback.id)
    }
}

package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile
import com.asistente.celular.nlu.pc.dropzone.PcDropzoneInfo
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para PcDropzoneSkill.
 * Valida el reconocimiento de consultas sobre el buzón Dropzone y la respuesta formateada.
 */
class PcDropzoneSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockDropzoneBridge(
        private val customInfo: PcDropzoneInfo? = null
    ) : PcWorkspaceBridge {
        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.COMMAND_ONLY)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = null
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean = true
        override suspend fun executeQuickCommand(command: String): Boolean = true
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan =
            AutonomousTaskPlan(planId = "p1", userGoal = goalPrompt, steps = emptyList())
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = null
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun queryDropzoneInfo(): PcDropzoneInfo? = customInfo
    }

    @Test
    fun testScoring() {
        val skill = PcDropzoneSkill()

        assertTrue(skill.score(dummyContext, "donde se guardan los renders").isMatch)
        assertTrue(skill.score(dummyContext, "cual es la carpeta de drive").isMatch)
        assertTrue(skill.score(dummyContext, "ver carpeta de entregables").isMatch)
        assertTrue(skill.score(dummyContext, "ultimos renders").isMatch)
        assertTrue(skill.score(dummyContext, "donde se guardan las canciones").isMatch)

        assertFalse(skill.score(dummyContext, "como esta el clima de hoy").isMatch)
    }

    @Test
    fun testExecutionWithGoogleDrive() = runBlocking {
        val testInfo = PcDropzoneInfo(
            rootPath = "G:\\Mi unidad\\HendrixStudio",
            cloudProvider = "Google Drive",
            isCloudSynced = true,
            categories = mapOf(
                "blender_renders" to "G:\\Mi unidad\\HendrixStudio\\Blender\\Renders",
                "audio_exports" to "G:\\Mi unidad\\HendrixStudio\\Audio\\Exports"
            ),
            recentFiles = listOf(
                PcDropzoneFile(
                    fileName = "cyberpunk_city_001.png",
                    category = "blender_renders",
                    sizeBytes = 4194304L,
                    relativePath = "Blender/Renders/cyberpunk_city_001.png",
                    timestamp = 1700000000000L
                )
            )
        )

        val bridge = MockDropzoneBridge(testInfo)
        val skill = PcDropzoneSkill(bridge)
        val score = skill.score(dummyContext, "donde se guardan los renders")

        val output = skill.execute(dummyContext, "donde se guardan los renders", score)

        assertTrue(output.speech.contains("G:\\Mi unidad\\HendrixStudio"))
        assertTrue(output.speech.contains("Google Drive"))
        assertTrue(output.speech.contains("cyberpunk_city_001.png"))
    }

    @Test
    fun testExecutionWhenUnavailable() = runBlocking {
        val bridge = MockDropzoneBridge(customInfo = null)
        val skill = PcDropzoneSkill(bridge)
        val score = skill.score(dummyContext, "donde se guardan los renders")

        val output = skill.execute(dummyContext, "donde se guardan los renders", score)
        assertTrue(output.speech.contains("No hay información del buzón Dropzone disponible"))
    }
}

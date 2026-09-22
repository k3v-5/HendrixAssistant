package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.project.PcProjectCategory
import com.asistente.celular.nlu.pc.project.PcProjectItem
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcProjectSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockProjectBridge : PcWorkspaceBridge {
        var queryCategory: PcProjectCategory? = null
        var searchQuery: String? = null
        var launchedPath: String? = null

        val sampleProjects = listOf(
            PcProjectItem(
                id = "cyberpunk_als",
                name = "CyberpunkTrack.als",
                path = "D:\\Music\\CyberpunkTrack.als",
                category = PcProjectCategory.AUDIO_DAW,
                extension = "als",
                sizeBytes = 2048000L,
                lastModifiedEpoch = 1710000000000L
            ),
            PcProjectItem(
                id = "scifi_blend",
                name = "SciFiRobot.blend",
                path = "D:\\3D\\SciFiRobot.blend",
                category = PcProjectCategory.THREE_D_VFX,
                extension = "blend",
                sizeBytes = 5048000L,
                lastModifiedEpoch = 1710000500000L
            )
        )

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = null
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean = true
        override suspend fun executeQuickCommand(command: String): Boolean = true
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan {
            return AutonomousTaskPlan(planId = "dummy", userGoal = goalPrompt, steps = emptyList())
        }
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = null
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun queryRemoteProjects(
            category: PcProjectCategory?,
            query: String?
        ): List<PcProjectItem> {
            queryCategory = category
            searchQuery = query
            return sampleProjects.filter {
                (category == null || it.category == category) &&
                (query.isNullOrBlank() || it.name.contains(query, ignoreCase = true))
            }
        }

        override suspend fun launchRemoteProject(projectPath: String): Boolean {
            launchedPath = projectPath
            return true
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcProjectSkill()

        assertTrue(skill.score(dummyContext, "abre el proyecto de ableton Cyberpunk").isMatch)
        assertTrue(skill.score(dummyContext, "proyectos de audio").isMatch)
        assertTrue(skill.score(dummyContext, "proyectos de blender").isMatch)
        assertTrue(skill.score(dummyContext, "lanza el proyecto SciFiRobot").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "que tiempo hace hoy").isMatch)
        assertFalse(skill.score(dummyContext, "crea un recordatorio").isMatch)
    }

    @Test
    fun testQueryProjects() = runBlocking {
        val bridge = MockProjectBridge()
        val skill = PcProjectSkill(bridge)

        val score = skill.score(dummyContext, "proyectos de ableton")
        val output = skill.execute(dummyContext, "proyectos de ableton", score)

        assertEquals(PcProjectCategory.AUDIO_DAW, bridge.queryCategory)
        assertTrue(output.speech.contains("Encontré 1 proyectos"))
        assertTrue(output.speech.contains("CyberpunkTrack.als"))
    }

    @Test
    fun testLaunchProject() = runBlocking {
        val bridge = MockProjectBridge()
        val skill = PcProjectSkill(bridge)

        val score = skill.score(dummyContext, "abre el proyecto SciFiRobot")
        val output = skill.execute(dummyContext, "abre el proyecto SciFiRobot", score)

        assertEquals("D:\\3D\\SciFiRobot.blend", bridge.launchedPath)
        assertTrue(output.speech.contains("Abriendo el proyecto SciFiRobot.blend"))
    }

    @Test
    fun testDisconnectedBridge() = runBlocking {
        val skill = PcProjectSkill(null)
        val score = skill.score(dummyContext, "abre el proyecto Test")
        val output = skill.execute(dummyContext, "abre el proyecto Test", score)

        assertTrue(output.speech.contains("No hay conexión"))
    }
}

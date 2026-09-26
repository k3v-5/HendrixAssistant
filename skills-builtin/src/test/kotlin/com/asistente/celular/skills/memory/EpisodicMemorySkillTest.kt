package com.asistente.celular.skills.memory

import com.asistente.celular.nlu.memory.episodic.CreativeSessionEntry
import com.asistente.celular.nlu.memory.episodic.EpisodicMemoryRepository
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.EpisodicProjectUiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodicMemorySkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class FakeEpisodicRepository : EpisodicMemoryRepository {
        val sessionList = mutableListOf(
            CreativeSessionEntry(
                id = "1",
                appName = "Unreal Engine",
                projectTitle = "MySciFiShooter",
                projectPath = "F:\\UnrealProjects\\MySciFiShooter\\MySciFiShooter.uproject",
                lastActiveEpochMillis = System.currentTimeMillis() - 60000 // hace 1 minuto
            ),
            CreativeSessionEntry(
                id = "2",
                appName = "Blender",
                projectTitle = "CharacterRig_V4",
                projectPath = "F:\\Blender\\CharacterRig_V4.blend",
                lastActiveEpochMillis = System.currentTimeMillis() - 3600000 // hace 1 hora
            )
        )

        override suspend fun recordSession(entry: CreativeSessionEntry) {
            sessionList.add(0, entry)
        }

        override suspend fun getRecentSessions(limit: Int): List<CreativeSessionEntry> = sessionList.take(limit)

        override suspend fun getLastActiveSession(): CreativeSessionEntry? = sessionList.firstOrNull()

        override suspend fun getSessionForApp(appName: String): CreativeSessionEntry? {
            return sessionList.firstOrNull { it.appName.contains(appName, ignoreCase = true) }
        }

        override suspend fun clearSessions() {
            sessionList.clear()
        }
    }

    private class FakeEpisodicBridge : PcWorkspaceBridge {
        var launchedPath: String? = null

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = null
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean = true
        override suspend fun executeQuickCommand(command: String): Boolean = true
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan = error("unused")
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = null
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun launchRemoteProject(projectPath: String): Boolean {
            launchedPath = projectPath
            return true
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = EpisodicMemorySkill()

        assertTrue(skill.score(dummyContext, "¿dónde me quedé en el proyecto?").isMatch)
        assertTrue(skill.score(dummyContext, "¿en qué estaba trabajando?").isMatch)
        assertTrue(skill.score(dummyContext, "¿en qué proyecto estaba en unreal?").isMatch)
        assertTrue(skill.score(dummyContext, "¿cuál fue mi último proyecto?").isMatch)
        assertTrue(skill.score(dummyContext, "cierra el proyecto actual y abre el anterior").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "pon un temporizador").isMatch)
        assertFalse(skill.score(dummyContext, "apaga la linterna").isMatch)
    }

    @Test
    fun testQueryLastActiveProjectFlow() = runBlocking {
        val repo = FakeEpisodicRepository()
        val skill = EpisodicMemorySkill(repo)

        val input = "¿dónde me quedé en el proyecto?"
        val score = skill.score(dummyContext, input)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("MySciFiShooter"))
        assertTrue(output.speech.contains("Unreal Engine"))

        val payload = output.payload as? EpisodicProjectUiPayload
        assertNotNull(payload)
        assertEquals("MySciFiShooter", payload?.projectName)
        assertEquals("Unreal Engine", payload?.appName)
    }

    @Test
    fun testSwitchToPreviousProjectFlow() = runBlocking {
        val repo = FakeEpisodicRepository()
        val bridge = FakeEpisodicBridge()
        val skill = EpisodicMemorySkill(repo, bridge)

        val input = "cierra el proyecto actual y abre el anterior"
        val score = skill.score(dummyContext, input)
        val output = skill.execute(dummyContext, input, score)

        assertTrue(output.success)
        assertEquals("F:\\Blender\\CharacterRig_V4.blend", bridge.launchedPath)
        assertTrue(output.speech.contains("CharacterRig_V4"))
        assertTrue(output.speech.contains("Blender"))
    }
}

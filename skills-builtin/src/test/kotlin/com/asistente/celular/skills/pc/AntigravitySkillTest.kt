package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AntigravityChat
import com.asistente.celular.nlu.pc.AntigravityProject
import com.asistente.celular.nlu.pc.AntigravityTargetMode
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.AntigravityNavigatorUiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para AntigravitySkill.
 * Valida la comprensión sintáctica de intenciones, la navegación entre proyectos
 * y el flujo de extremo a extremo de creación y continuación de chats.
 */
class AntigravitySkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockAntigravityBridge : PcWorkspaceBridge {
        var lastMode: AntigravityTargetMode? = null
        var lastProjectId: String? = null
        var lastConversationId: String? = null
        var lastPrompt: String? = null

        val sampleProjects = listOf(
            AntigravityProject("p1", "HendrixAssistant-main", "file:///d:/Proyectos/TEST/HendrixAssistant-main", 1000L, 12),
            AntigravityProject("p2", "eRaindeONE", "file:///d:/Proyectos/NET/eRaindeONE", 900L, 8)
        )

        val sampleChats = listOf(
            AntigravityChat("c1", "Revisar Proyecto de Control Remoto", "Resumen de control", 1000L, 15, "p1"),
            AntigravityChat("c2", "Validar Cambios en Rama", "Merge rama develop", 950L, 6, "p2")
        )

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.COMMAND_ONLY)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(
            PcSystemTelemetry(hostname = "Dev-Station")
        )
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
        override suspend fun queryTelemetry(): PcSystemTelemetry? = telemetry.value
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun queryAntigravityProjects(): List<AntigravityProject> = sampleProjects

        override suspend fun queryAntigravityChats(projectId: String?): List<AntigravityChat> =
            if (projectId != null) sampleChats.filter { it.projectId == projectId } else sampleChats

        override suspend fun executeAntigravityAction(
            mode: AntigravityTargetMode,
            projectId: String?,
            conversationId: String?,
            prompt: String?
        ): Boolean {
            lastMode = mode
            lastProjectId = projectId
            lastConversationId = conversationId
            lastPrompt = prompt
            return true
        }
    }

    @Test
    fun testGrammarMatching() {
        val skill = AntigravitySkill()

        assertTrue(skill.score(dummyContext, "abre antigravity").isMatch)
        assertTrue(skill.score(dummyContext, "abrir antigravity").isMatch)
        assertTrue(skill.score(dummyContext, "proyectos en antigravity").isMatch)
        assertTrue(skill.score(dummyContext, "ver proyectos de antigravity").isMatch)
        assertTrue(skill.score(dummyContext, "en antigravity nuevo chat y escribe refactorizar").isMatch)
        assertTrue(skill.score(dummyContext, "escribe en antigravity como funciona esto").isMatch)
    }

    @Test
    fun testListProjectsFlow() = runBlocking {
        val bridge = MockAntigravityBridge()
        val skill = AntigravitySkill(bridge)

        val score = skill.score(dummyContext, "proyectos en antigravity")
        val output = skill.execute(dummyContext, "proyectos en antigravity", score)

        assertNotNull(output.payload)
        assertTrue(output.payload is AntigravityNavigatorUiPayload)

        val payload = output.payload as AntigravityNavigatorUiPayload
        assertEquals(2, payload.projects.size)
        assertEquals("HendrixAssistant-main", payload.projects[0].name)
        assertTrue(output.speech.contains("Tienes 2 proyectos"))
    }

    @Test
    fun testNewChatFlowWithPrompt() = runBlocking {
        val bridge = MockAntigravityBridge()
        val skill = AntigravitySkill(bridge)

        val query = "en antigravity nuevo chat y escribe crear nuevo componente atomico"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(AntigravityTargetMode.NEW_CHAT, bridge.lastMode)
        assertTrue(bridge.lastPrompt?.contains("crear nuevo componente atomico") == true)

        assertNotNull(output.payload)
        val payload = output.payload as AntigravityNavigatorUiPayload
        assertEquals("Nuevo chat iniciado", payload.statusMessage)
    }

    @Test
    fun testWriteToExistingChatFlow() = runBlocking {
        val bridge = MockAntigravityBridge()
        val skill = AntigravitySkill(bridge)

        val query = "escribe en antigravity generar pruebas unitarias"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertNotNull(bridge.lastMode)
        assertTrue(bridge.lastPrompt?.contains("generar pruebas unitarias") == true)

        assertNotNull(output.payload)
        val payload = output.payload as AntigravityNavigatorUiPayload
        assertEquals("Prompt enviado a Antigravity", payload.statusMessage)
    }

    @Test
    fun testDefaultOpenAntigravityFlow() = runBlocking {
        val bridge = MockAntigravityBridge()
        val skill = AntigravitySkill(bridge)

        val score = skill.score(dummyContext, "abre antigravity")
        val output = skill.execute(dummyContext, "abre antigravity", score)

        assertEquals(AntigravityTargetMode.LAUNCH_OR_FOCUS, bridge.lastMode)
        assertNotNull(output.payload)
        val payload = output.payload as AntigravityNavigatorUiPayload
        assertEquals(2, payload.projects.size)
        assertEquals(2, payload.recentChats.size)
        assertTrue(output.speech.contains("Abriendo Antigravity"))
    }
}

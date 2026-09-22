package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.workspace.PcGitRepositoryStatus
import com.asistente.celular.nlu.pc.workspace.PcRunningCreativeProcess
import com.asistente.celular.nlu.pc.workspace.PcTerminalErrorAlert
import com.asistente.celular.nlu.pc.workspace.PcWorkspaceContext
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcWorkspaceMemorySkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockWorkspaceMemoryBridge(
        var mockContext: PcWorkspaceContext? = null,
        var mockTerminalError: PcTerminalErrorAlert? = null
    ) : PcWorkspaceBridge {
        var queryWorkspaceContextCalled = false

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
        override val terminalErrorAlerts: StateFlow<PcTerminalErrorAlert?> = MutableStateFlow(mockTerminalError)

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

        override suspend fun queryWorkspaceContext(): PcWorkspaceContext? {
            queryWorkspaceContextCalled = true
            return mockContext
        }
    }

    @Test
    fun testScoreMatching() {
        val skill = PcWorkspaceMemorySkill()

        val score1 = skill.score(dummyContext, "¿Qué estaba editando en la PC?")
        assertEquals(1.0f, score1.confidence, 0.001f)
        assertEquals(Specificity.HIGH, score1.specificity)

        val score2 = skill.score(dummyContext, "en que rama me quede")
        assertEquals(1.0f, score2.confidence, 0.001f)

        val score3 = skill.score(dummyContext, "que error dio la terminal")
        assertEquals(1.0f, score3.confidence, 0.001f)

        val score4 = skill.score(dummyContext, "diagnostica el error de compilacion")
        assertEquals(1.0f, score4.confidence, 0.001f)
    }

    @Test
    fun testExecuteWorkspaceMemoryQuery() = runBlocking {
        val sampleContext = PcWorkspaceContext(
            foregroundProcess = "Code.exe",
            foregroundTitle = "Visual Studio Code - HendrixAssistant",
            activeGitRepos = listOf(
                PcGitRepositoryStatus(
                    repoName = "HendrixAssistant",
                    branch = "feature/macro-deck",
                    path = "D:\\Projects\\HendrixAssistant",
                    hasUncommittedChanges = true,
                    uncommittedFilesCount = 4,
                    lastCommitMessage = "feat: initial macro deck implementation"
                )
            ),
            runningCreativeProcesses = listOf(
                PcRunningCreativeProcess(
                    pid = 4321,
                    name = "Code.exe",
                    title = "Visual Studio Code",
                    category = "DEVELOPMENT",
                    cpuPercent = 2.4,
                    memoryMb = 512.0
                )
            )
        )

        val bridge = MockWorkspaceMemoryBridge(mockContext = sampleContext)
        val skill = PcWorkspaceMemorySkill(pcBridge = bridge)

        val score = skill.score(dummyContext, "¿Qué estaba haciendo en la PC?")
        val output = skill.execute(dummyContext, "¿Qué estaba haciendo en la PC?", score)

        assertTrue(bridge.queryWorkspaceContextCalled)
        assertTrue(output.speech.contains("Visual Studio Code"))
        assertTrue(output.speech.contains("HendrixAssistant"))
        assertTrue(output.speech.contains("feature/macro-deck"))
        assertTrue(output.speech.contains("4 archivos sin confirmar"))
        assertNotNull(output.displayText)
        assertTrue(output.displayText!!.contains("Memoria de Trabajo"))
    }

    @Test
    fun testExecuteTerminalDiagnosis() = runBlocking {
        val sampleError = PcTerminalErrorAlert(
            errorId = "err_gradle_01",
            source = "gradle",
            command = "gradlew assembleDebug",
            errorMessage = "e: Unresolved reference: MacroDeckProfile",
            failedFile = "MainActivity.kt",
            failedLine = 88,
            aiDiagnosisPrompt = "Error de símbolo no resuelto. Asegúrate de importar MacroDeckProfile."
        )

        val bridge = MockWorkspaceMemoryBridge(mockTerminalError = sampleError)
        val skill = PcWorkspaceMemorySkill(pcBridge = bridge)

        val score = skill.score(dummyContext, "diagnostica el error de la terminal")
        val output = skill.execute(dummyContext, "diagnostica el error de la terminal", score)

        assertTrue(output.speech.contains("gradle"))
        assertTrue(output.speech.contains("assembleDebug"))
        assertTrue(output.speech.contains("MainActivity.kt"))
        assertTrue(output.speech.contains("MacroDeckProfile"))
        assertNotNull(output.displayText)
        assertTrue(output.displayText!!.contains("Diagnóstico de Terminal"))
    }
}

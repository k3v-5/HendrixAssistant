package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcAdvancedSkillsTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockAdvancedBridge : PcWorkspaceBridge {
        var lastKilledProcess: String? = null
        var lastWindowAction: String? = null
        var mockHealth = PcHardwareTelemetry(
            gpuName = "NVIDIA RTX 4070",
            gpuUsagePercent = 42f,
            gpuTempCelsius = 55,
            vramUsedMb = 4096L,
            vramTotalMb = 12288L,
            cpuUsagePercent = 20f,
            ramUsedMb = 8192L,
            ramTotalMb = 32768L
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
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan = AutonomousTaskPlan("id", goalPrompt, emptyList())
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = null
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun killProcess(processName: String): Boolean {
            lastKilledProcess = processName
            return true
        }

        override suspend fun executeWindowCommand(action: String): Boolean {
            lastWindowAction = action
            return true
        }

        override suspend fun queryHardwareHealth(): PcHardwareTelemetry? {
            return mockHealth
        }
    }

    @Test
    fun `test PcProcessSkill matching and execution`() = runBlocking {
        val bridge = MockAdvancedBridge()
        val skill = PcProcessSkill(bridge)

        val score = skill.score(dummyContext, "cierra Chrome en la pc")
        assertTrue(score.confidence > 0.8f)

        val output = skill.execute(dummyContext, "cierra Chrome en la pc")
        assertEquals("chrome", bridge.lastKilledProcess)
        assertTrue(output.speech.contains("cerrar chrome"))
    }

    @Test
    fun `test PcWindowManagerSkill matching and execution`() = runBlocking {
        val bridge = MockAdvancedBridge()
        val skill = PcWindowManagerSkill(bridge)

        val scoreMin = skill.score(dummyContext, "minimiza todo en la pc")
        assertTrue(scoreMin.confidence > 0.8f)

        val outputMin = skill.execute(dummyContext, "minimiza todo en la pc")
        assertEquals("MINIMIZE_ALL", bridge.lastWindowAction)
        assertTrue(outputMin.speech.contains("minimizado"))

        val outputMax = skill.execute(dummyContext, "maximiza en la pc")
        assertEquals("TOGGLE_MAXIMIZE", bridge.lastWindowAction)

        val outputMon = skill.execute(dummyContext, "pasa la ventana al otro monitor en la pc")
        assertEquals("MOVE_NEXT_MONITOR", bridge.lastWindowAction)
    }

    @Test
    fun `test PcHardwareHealthSkill matching and execution`() = runBlocking {
        val bridge = MockAdvancedBridge()
        val skill = PcHardwareHealthSkill(bridge)

        val score = skill.score(dummyContext, "como esta la pc")
        assertTrue(score.confidence > 0.8f)

        val output = skill.execute(dummyContext, "como esta la pc")
        assertNotNull(output)
        assertTrue(output.speech.contains("20 por ciento de CPU"))
        assertTrue(output.speech.contains("RTX 4070"))
        assertTrue(output.speech.contains("55 grados"))
    }
}

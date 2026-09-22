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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcHardwareWatchdogSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockHardwareWatchdogBridge : PcWorkspaceBridge {
        var startWatchdogProcess: String? = null
        var startWatchdogAutoSuspend: Boolean? = null
        var hardwareTelemetryRequested = false

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

        override suspend fun queryHardwareTelemetry(): PcHardwareTelemetry? {
            hardwareTelemetryRequested = true
            return PcHardwareTelemetry(
                gpuName = "NVIDIA GeForce RTX 3080",
                gpuUsagePercent = 85.0f,
                gpuTempCelsius = 72,
                vramUsedMb = 8192L,
                vramTotalMb = 10240L,
                cpuUsagePercent = 45.0f,
                ramUsedMb = 16384L,
                ramTotalMb = 32768L,
                activeHeavyProcess = "blender.exe"
            )
        }

        override suspend fun startRenderWatchdog(
            processName: String,
            autoSuspend: Boolean
        ): Boolean {
            startWatchdogProcess = processName
            startWatchdogAutoSuspend = autoSuspend
            return true
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcHardwareWatchdogSkill()

        assertTrue(skill.score(dummyContext, "como esta la gpu").isMatch)
        assertTrue(skill.score(dummyContext, "temperatura de la gpu").isMatch)
        assertTrue(skill.score(dummyContext, "uso de vram").isMatch)
        assertTrue(skill.score(dummyContext, "vigila el render").isMatch)
        assertTrue(skill.score(dummyContext, "centinela de render").isMatch)
        assertTrue(skill.score(dummyContext, "avisame cuando termine el render").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "cual es la capital de francia").isMatch)
        assertFalse(skill.score(dummyContext, "reproduce una cancion").isMatch)
    }

    @Test
    fun testHardwareTelemetryQuery() = runBlocking {
        val bridge = MockHardwareWatchdogBridge()
        val skill = PcHardwareWatchdogSkill(bridge)

        val score = skill.score(dummyContext, "como esta la gpu")
        val output = skill.execute(dummyContext, "como esta la gpu", score)

        assertTrue(bridge.hardwareTelemetryRequested)
        assertTrue(output.speech.contains("NVIDIA GeForce RTX 3080"))
        assertTrue(output.speech.contains("85% de uso a 72 grados"))
        assertTrue(output.speech.contains("blender.exe"))
    }

    @Test
    fun testStartRenderWatchdog() = runBlocking {
        val bridge = MockHardwareWatchdogBridge()
        val skill = PcHardwareWatchdogSkill(bridge)

        val score = skill.score(dummyContext, "vigila el render de blender")
        val output = skill.execute(dummyContext, "vigila el render de blender", score)

        assertEquals("blender", bridge.startWatchdogProcess)
        assertEquals(true, bridge.startWatchdogAutoSuspend)
        assertTrue(output.speech.contains("Centinela de render activado para blender"))
        assertTrue(output.speech.contains("auto-suspensión activada"))
    }

    @Test
    fun testStartRenderWatchdogNoSuspend() = runBlocking {
        val bridge = MockHardwareWatchdogBridge()
        val skill = PcHardwareWatchdogSkill(bridge)

        val score = skill.score(dummyContext, "vigila el render sin suspender")
        val output = skill.execute(dummyContext, "vigila el render sin suspender", score)

        assertEquals("blender", bridge.startWatchdogProcess)
        assertEquals(false, bridge.startWatchdogAutoSuspend)
        assertFalse(output.speech.contains("auto-suspensión activada"))
    }

    @Test
    fun testDisconnectedBridge() = runBlocking {
        val skill = PcHardwareWatchdogSkill(null)
        val score = skill.score(dummyContext, "como esta la gpu")
        val output = skill.execute(dummyContext, "como esta la gpu", score)

        assertTrue(output.speech.contains("No hay conexión"))
    }
}

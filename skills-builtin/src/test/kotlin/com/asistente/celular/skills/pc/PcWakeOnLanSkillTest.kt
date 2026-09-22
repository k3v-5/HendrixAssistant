package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcWakeOnLanSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockWakeOnLanPcBridge(
        private val shouldSucceed: Boolean = true
    ) : PcWorkspaceBridge {
        var wolInvoked: Boolean = false
        var capturedMac: String? = null
        var capturedBroadcastIp: String? = null

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(false)

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

        override suspend fun wakeOnLan(macAddress: String?, broadcastIp: String?): Boolean {
            wolInvoked = true
            capturedMac = macAddress
            capturedBroadcastIp = broadcastIp
            return shouldSucceed
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcWakeOnLanSkill()

        assertTrue(skill.score(dummyContext, "enciende la computadora").isMatch)
        assertTrue(skill.score(dummyContext, "prende la pc").isMatch)
        assertTrue(skill.score(dummyContext, "despierta mi computadora").isMatch)
        assertTrue(skill.score(dummyContext, "arranca el ordenador").isMatch)
        assertTrue(skill.score(dummyContext, "wake on lan").isMatch)
        assertTrue(skill.score(dummyContext, "enciende mi pc").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "apaga la luz").isMatch)
        assertFalse(skill.score(dummyContext, "abre spotify en el celular").isMatch)
    }

    @Test
    fun testWakeOnLanExecutionSuccess() = runBlocking {
        val bridge = MockWakeOnLanPcBridge(shouldSucceed = true)
        val skill = PcWakeOnLanSkill(bridge)

        val score = skill.score(dummyContext, "enciende la computadora")
        val output = skill.execute(dummyContext, "enciende la computadora", score)

        assertTrue(bridge.wolInvoked)
        assertTrue(output.speech.contains("Paquete mágico Wake-on-LAN emitido"))
    }

    @Test
    fun testWakeOnLanExecutionFailure() = runBlocking {
        val bridge = MockWakeOnLanPcBridge(shouldSucceed = false)
        val skill = PcWakeOnLanSkill(bridge)

        val score = skill.score(dummyContext, "despierta la pc")
        val output = skill.execute(dummyContext, "despierta la pc", score)

        assertTrue(bridge.wolInvoked)
        assertTrue(output.speech.contains("No se pudo enviar el paquete Wake-on-LAN"))
    }

    @Test
    fun testWakeOnLanExecutionWithoutBridge() = runBlocking {
        val skill = PcWakeOnLanSkill(null)

        val score = skill.score(dummyContext, "wake on lan")
        val output = skill.execute(dummyContext, "wake on lan", score)

        assertTrue(output.speech.contains("No se pudo enviar el paquete Wake-on-LAN"))
    }
}

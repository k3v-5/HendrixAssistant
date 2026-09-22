package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.audio.PcAudioStreamState
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcWirelessAudioSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockAudioBridge : PcWorkspaceBridge {
        var startCalled = false
        var stopCalled = false
        var returnSuccess = true

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
        override val audioStreamState: StateFlow<PcAudioStreamState> = MutableStateFlow(PcAudioStreamState())

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

        override suspend fun startAudioMonitoring(sampleRate: Int): Boolean {
            startCalled = true
            return returnSuccess
        }

        override suspend fun stopAudioMonitoring(): Boolean {
            stopCalled = true
            return returnSuccess
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcWirelessAudioSkill()

        assertTrue(skill.score(dummyContext, "iniciar monitoreo de audio").isMatch)
        assertTrue(skill.score(dummyContext, "escuchar audio de la pc").isMatch)
        assertTrue(skill.score(dummyContext, "monitorea el audio de la computadora").isMatch)
        assertTrue(skill.score(dummyContext, "detener monitoreo de audio").isMatch)
        assertTrue(skill.score(dummyContext, "apaga el monitor de audio").isMatch)
        assertTrue(skill.score(dummyContext, "dejar de escuchar la pc").isMatch)
        assertTrue(skill.score(dummyContext, "transmite el audio al celular").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "que hora es").isMatch)
        assertFalse(skill.score(dummyContext, "crea una nota").isMatch)
    }

    @Test
    fun testStartAudioMonitoring() = runBlocking {
        val bridge = MockAudioBridge()
        val skill = PcWirelessAudioSkill(bridge)

        val score = skill.score(dummyContext, "iniciar monitoreo de audio de la pc")
        val output = skill.execute(dummyContext, "iniciar monitoreo de audio de la pc", score)

        assertTrue(bridge.startCalled)
        assertFalse(bridge.stopCalled)
        assertTrue(output.speech.contains("iniciado"))
        assertTrue(output.speech.contains("tiempo real"))
    }

    @Test
    fun testStopAudioMonitoring() = runBlocking {
        val bridge = MockAudioBridge()
        val skill = PcWirelessAudioSkill(bridge)

        val score = skill.score(dummyContext, "detener monitoreo de audio")
        val output = skill.execute(dummyContext, "detener monitoreo de audio", score)

        assertFalse(bridge.startCalled)
        assertTrue(bridge.stopCalled)
        assertTrue(output.speech.contains("detenido"))
    }

    @Test
    fun testDisconnectedBridge() = runBlocking {
        val skill = PcWirelessAudioSkill(null)
        val score = skill.score(dummyContext, "escuchar audio de la pc")
        val output = skill.execute(dummyContext, "escuchar audio de la pc", score)

        assertTrue(output.speech.contains("No hay conexión"))
    }
}

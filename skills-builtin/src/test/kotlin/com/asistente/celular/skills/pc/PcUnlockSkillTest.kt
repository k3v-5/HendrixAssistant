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

class PcUnlockSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockUnlockBridge(
        private val successReturn: Boolean = true
    ) : PcWorkspaceBridge {
        var unlockInvoked = false
        var capturedPin: String = ""

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

        override suspend fun unlockSession(pin: String): Boolean {
            unlockInvoked = true
            capturedPin = pin
            return successReturn
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcUnlockSkill()

        assertTrue(skill.score(dummyContext, "desbloquea la pc").isMatch)
        assertTrue(skill.score(dummyContext, "desbloquea la computadora").isMatch)
        assertTrue(skill.score(dummyContext, "inicia sesion en la pc").isMatch)
        assertTrue(skill.score(dummyContext, "desbloquear windows").isMatch)
        assertTrue(skill.score(dummyContext, "desbloquea el ordenador").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "enciende la luz").isMatch)
        assertFalse(skill.score(dummyContext, "que hora es").isMatch)
    }

    @Test
    fun testUnlockExecutionSuccess() = runBlocking {
        val bridge = MockUnlockBridge(successReturn = true)
        val skill = PcUnlockSkill(bridge)

        val score = skill.score(dummyContext, "desbloquea la pc con pin 1234")
        val output = skill.execute(dummyContext, "desbloquea la pc con pin 1234", score)

        assertTrue(bridge.unlockInvoked)
        assertEquals("1234", bridge.capturedPin)
        assertTrue(output.speech.contains("desbloqueada con éxito"))
    }

    @Test
    fun testUnlockExecutionFailure() = runBlocking {
        val bridge = MockUnlockBridge(successReturn = false)
        val skill = PcUnlockSkill(bridge)

        val score = skill.score(dummyContext, "desbloquea la computadora")
        val output = skill.execute(dummyContext, "desbloquea la computadora", score)

        assertTrue(bridge.unlockInvoked)
        assertTrue(output.speech.contains("Se envió el comando de desbloqueo"))
    }
}

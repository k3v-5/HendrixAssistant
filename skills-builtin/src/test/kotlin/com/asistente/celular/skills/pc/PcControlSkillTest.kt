package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.InteractionPlan
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.PcWorkspaceUiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcControlSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockPcBridge : PcWorkspaceBridge {
        val quickCommandsExecuted = mutableListOf<String>()
        val typedTexts = mutableListOf<String>()

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(
            PcSystemTelemetry(hostname = "Test-Rig", cpuPercent = 25f, ramPercent = 50f)
        )
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(byteArrayOf(1, 2, 3))
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = byteArrayOf(1, 2, 3)
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean {
            typedTexts.add(text)
            return true
        }
        override suspend fun executeQuickCommand(command: String): Boolean {
            quickCommandsExecuted.add(command)
            return true
        }
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan {
            return AutonomousTaskPlan(planId = "test_plan", userGoal = goalPrompt, steps = emptyList())
        }
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = telemetry.value
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcControlSkill()

        assertTrue(skill.score(dummyContext, "apaga la pc").isMatch)
        assertTrue(skill.score(dummyContext, "bloquea la pc").isMatch)
        assertTrue(skill.score(dummyContext, "suspende la computadora").isMatch)
        assertTrue(skill.score(dummyContext, "sube el volumen de la pc").isMatch)
        assertTrue(skill.score(dummyContext, "silencia la pc").isMatch)
        assertTrue(skill.score(dummyContext, "muestrame la pantalla de la pc").isMatch)
        assertTrue(skill.score(dummyContext, "ver pantalla de la computadora").isMatch)
        assertTrue(skill.score(dummyContext, "escribe en la pc codigo limpio").isMatch)
    }

    @Test
    fun testPowerShutdownRequiresConfirmation() = runBlocking {
        val mockBridge = MockPcBridge()
        val skill = PcControlSkill(mockBridge)

        val score = skill.score(dummyContext, "apaga la pc")
        val output = skill.execute(dummyContext, "apaga la pc", score)

        assertTrue(output.interactionPlan is InteractionPlan.RequestConfirmation)
        val plan = output.interactionPlan as InteractionPlan.RequestConfirmation
        val confirmedOutput = plan.onConfirm()

        assertEquals("Apagando la computadora.", confirmedOutput.speech)
        assertTrue(mockBridge.quickCommandsExecuted.contains("shutdown"))
    }

    @Test
    fun testVolumeAndMediaCommands() = runBlocking {
        val mockBridge = MockPcBridge()
        val skill = PcControlSkill(mockBridge)

        val score1 = skill.score(dummyContext, "sube el volumen de la pc")
        skill.execute(dummyContext, "sube el volumen de la pc", score1)
        assertTrue(mockBridge.quickCommandsExecuted.contains("volume_up"))

        val score2 = skill.score(dummyContext, "silencia la pc")
        skill.execute(dummyContext, "silencia la pc", score2)
        assertTrue(mockBridge.quickCommandsExecuted.contains("volume_mute"))

        val score3 = skill.score(dummyContext, "pausa la musica en la pc")
        skill.execute(dummyContext, "pausa la musica en la pc", score3)
        assertTrue(mockBridge.quickCommandsExecuted.contains("media_play_pause"))
    }

    @Test
    fun testDirectTypingCommand() = runBlocking {
        val mockBridge = MockPcBridge()
        val skill = PcControlSkill(mockBridge)

        val score = skill.score(dummyContext, "escribe en la pc Hola Hendrix")
        skill.execute(dummyContext, "escribe en la pc Hola Hendrix", score)

        assertTrue(mockBridge.typedTexts.contains("Hola Hendrix"))
    }

    @Test
    fun testWorkspaceSnapshotPayload() = runBlocking {
        val mockBridge = MockPcBridge()
        val skill = PcControlSkill(mockBridge)

        val score = skill.score(dummyContext, "muestrame la pantalla de la pc")
        val output = skill.execute(dummyContext, "muestrame la pantalla de la pc", score)

        assertNotNull(output.payload)
        assertTrue(output.payload is PcWorkspaceUiPayload)
        val payload = output.payload as PcWorkspaceUiPayload
        assertEquals("Test-Rig", payload.hostname)
        assertTrue(payload.isConnected)
        assertNotNull(payload.latestSnapshotPreview)
    }
}

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

class PcAudioMixerSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockAudioMixerBridge : PcWorkspaceBridge {
        var setAppVolumeInvoked = false
        var setAppMuteInvoked = false
        var targetApp: String = ""
        var targetVolume: Int = -1
        var targetMuted: Boolean = false

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

        override suspend fun setAppVolume(processName: String, volumePercent: Int): Boolean {
            setAppVolumeInvoked = true
            targetApp = processName
            targetVolume = volumePercent
            return true
        }

        override suspend fun setAppMute(processName: String, isMuted: Boolean): Boolean {
            setAppMuteInvoked = true
            targetApp = processName
            targetMuted = isMuted
            return true
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcAudioMixerSkill()

        assertTrue(skill.score(dummyContext, "pon el volumen de ableton al 80%").isMatch)
        assertTrue(skill.score(dummyContext, "silencia spotify en la pc").isMatch)
        assertTrue(skill.score(dummyContext, "silencia chrome").isMatch)
        assertTrue(skill.score(dummyContext, "sube el volumen de fl studio al 100").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "pon una alarma a las 7").isMatch)
        assertFalse(skill.score(dummyContext, "cuanto es 2 mas 2").isMatch)
    }

    @Test
    fun testSetAppVolumeExecution() = runBlocking {
        val bridge = MockAudioMixerBridge()
        val skill = PcAudioMixerSkill(bridge)

        val score = skill.score(dummyContext, "pon el volumen de ableton al 80%")
        val output = skill.execute(dummyContext, "pon el volumen de ableton al 80%", score)

        assertTrue(bridge.setAppVolumeInvoked)
        assertEquals("Ableton", bridge.targetApp)
        assertEquals(80, bridge.targetVolume)
        assertTrue(output.speech.contains("ajustado al 80%"))
    }

    @Test
    fun testMuteAppExecution() = runBlocking {
        val bridge = MockAudioMixerBridge()
        val skill = PcAudioMixerSkill(bridge)

        val score = skill.score(dummyContext, "silencia spotify")
        val output = skill.execute(dummyContext, "silencia spotify", score)

        assertTrue(bridge.setAppMuteInvoked)
        assertEquals("Spotify", bridge.targetApp)
        assertTrue(bridge.targetMuted)
        assertTrue(output.speech.contains("Se ha silenciado Spotify"))
    }
}

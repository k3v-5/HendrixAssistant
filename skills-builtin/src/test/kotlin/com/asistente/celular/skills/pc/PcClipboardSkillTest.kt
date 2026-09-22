package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.clipboard.PcClipboardPayload
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcClipboardSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockClipboardBridge : PcWorkspaceBridge {
        var setClipboardInvoked = false
        var getClipboardInvoked = false
        var lastSetText: String = ""
        var lastPasteImmediately: Boolean = false
        var currentClipboard: PcClipboardPayload? = PcClipboardPayload.fromText("Texto en Windows")

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

        override suspend fun getClipboard(): PcClipboardPayload? {
            getClipboardInvoked = true
            return currentClipboard
        }

        override suspend fun setClipboard(text: String, pasteImmediately: Boolean): PcClipboardPayload? {
            setClipboardInvoked = true
            lastSetText = text
            lastPasteImmediately = pasteImmediately
            val p = PcClipboardPayload.fromText(text)
            currentClipboard = p
            return p
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcClipboardSkill()

        assertTrue(skill.score(dummyContext, "copia en la pc https://youtube.com").isMatch)
        assertTrue(skill.score(dummyContext, "pega en la pc este prompt para blender").isMatch)
        assertTrue(skill.score(dummyContext, "que hay en el portapapeles de la pc").isMatch)
        assertTrue(skill.score(dummyContext, "leer portapapeles de la pc").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "como esta el clima hoy").isMatch)
        assertFalse(skill.score(dummyContext, "abre la camara").isMatch)
    }

    @Test
    fun testCopyExecution() = runBlocking {
        val bridge = MockClipboardBridge()
        val skill = PcClipboardSkill(bridge)

        val score = skill.score(dummyContext, "copia en la pc mi texto de prueba")
        val output = skill.execute(dummyContext, "copia en la pc mi texto de prueba", score)

        assertTrue(bridge.setClipboardInvoked)
        assertFalse(bridge.lastPasteImmediately)
        assertEquals("mi texto de prueba", bridge.lastSetText)
        assertTrue(output.speech.contains("Texto copiado con éxito"))
        assertTrue(output.speech.contains("SHA-256 verificado"))
    }

    @Test
    fun testPasteExecution() = runBlocking {
        val bridge = MockClipboardBridge()
        val skill = PcClipboardSkill(bridge)

        val score = skill.score(dummyContext, "pega en la pc prompt fotorrealista")
        val output = skill.execute(dummyContext, "pega en la pc prompt fotorrealista", score)

        assertTrue(bridge.setClipboardInvoked)
        assertTrue(bridge.lastPasteImmediately)
        assertEquals("prompt fotorrealista", bridge.lastSetText)
        assertTrue(output.speech.contains("pegado en la ventana activa"))
    }

    @Test
    fun testQueryClipboardExecution() = runBlocking {
        val bridge = MockClipboardBridge()
        val skill = PcClipboardSkill(bridge)

        val score = skill.score(dummyContext, "que hay en el portapapeles de la pc")
        val output = skill.execute(dummyContext, "que hay en el portapapeles de la pc", score)

        assertTrue(bridge.getClipboardInvoked)
        assertTrue(output.speech.contains("Texto en Windows"))
    }
}

package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.copilot.PcScreenAnalysisResult
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcScreenCopilotSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private open class MockScreenCopilotBridge(
        var shouldSucceed: Boolean = true
    ) : PcWorkspaceBridge {
        var lastPromptReceived: String? = null
        var lastCropSetting: Boolean? = null

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

        override suspend fun analyzeScreenWithAi(
            prompt: String,
            cropToActiveWindow: Boolean
        ): PcScreenAnalysisResult {
            lastPromptReceived = prompt
            lastCropSetting = cropToActiveWindow
            return if (shouldSucceed) {
                PcScreenAnalysisResult(
                    success = true,
                    analysisMarkdown = "En la ventana de Visual Studio Code se detectó un error de sintaxis en la línea 45: falta una coma.",
                    detectedWindow = "Code.exe - HendrixAssistant",
                    rawImageBytes = byteArrayOf(1, 2, 3)
                )
            } else {
                PcScreenAnalysisResult(
                    success = false,
                    analysisMarkdown = "Error de red al invocar API de visión.",
                    errorSummary = "Timeout de conexión"
                )
            }
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcScreenCopilotSkill()

        assertTrue(skill.score(dummyContext, "analiza la pantalla").isMatch)
        assertTrue(skill.score(dummyContext, "analiza mi pantalla").isMatch)
        assertTrue(skill.score(dummyContext, "que ves en la pantalla").isMatch)
        assertTrue(skill.score(dummyContext, "diagnostica la pantalla").isMatch)
        assertTrue(skill.score(dummyContext, "copilot de pantalla").isMatch)
        assertTrue(skill.score(dummyContext, "screen copilot").isMatch)
        assertTrue(skill.score(dummyContext, "que error hay en la pantalla").isMatch)
        assertTrue(skill.score(dummyContext, "revisa la pantalla de la pc").isMatch)
        assertTrue(skill.score(dummyContext, "que dice la terminal de la pc").isMatch)

        // Action Guidance Patterns
        assertTrue(skill.score(dummyContext, "mira lo que tengo en pantalla y dime qué botón debo presionar para exportar la textura").isMatch)
        assertTrue(skill.score(dummyContext, "mira mi pantalla y dime qué botón debo presionar").isMatch)
        assertTrue(skill.score(dummyContext, "mira la pantalla y dime dónde hago click").isMatch)
        assertTrue(skill.score(dummyContext, "qué botón debo presionar para guardar").isMatch)
        assertTrue(skill.score(dummyContext, "dónde hago clic para compilar el proyecto").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "pon una alarma a las 8").isMatch)
        assertFalse(skill.score(dummyContext, "como esta el clima hoy").isMatch)
    }

    @Test
    fun testScreenAnalysisExecutionSuccess() = runBlocking {
        val bridge = MockScreenCopilotBridge(shouldSucceed = true)
        val skill = PcScreenCopilotSkill(bridge)

        val score = skill.score(dummyContext, "analiza la pantalla")
        val output = skill.execute(dummyContext, "analiza la pantalla", score)

        assertTrue(bridge.lastCropSetting == true)
        assertTrue(output.speech.contains("Visual Studio Code"))
        assertTrue(output.speech.contains("línea 45"))
        assertTrue(output.displayText?.contains("Code.exe - HendrixAssistant") == true)
    }

    @Test
    fun testActionGuidanceInteractiveFlow() = runBlocking {
        val customBridge = object : MockScreenCopilotBridge(shouldSucceed = true) {
            override suspend fun analyzeScreenWithAi(
                prompt: String,
                cropToActiveWindow: Boolean
            ): PcScreenAnalysisResult {
                lastPromptReceived = prompt
                lastCropSetting = cropToActiveWindow
                return PcScreenAnalysisResult(
                    success = true,
                    analysisMarkdown = "Para exportar la textura en Unreal Engine:\n1. Haz clic en el menú File en la barra superior.\n2. Selecciona Export Selected.\nTambién puedes usar el atajo Ctrl+E en la esquina superior izquierda.",
                    detectedWindow = "UnrealEditor.exe - MyLevel",
                    suggestedActions = listOf("Haz clic en el menú File", "Selecciona Export Selected"),
                    rawImageBytes = byteArrayOf(4, 5, 6)
                )
            }
        }

        val skill = PcScreenCopilotSkill(customBridge)
        val input = "mira lo que tengo en pantalla y dime qué botón debo presionar para exportar la textura"
        val score = skill.score(dummyContext, input)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, input, score)
        assertTrue(output.success)

        val payload = output.payload as? com.asistente.celular.nlu.ui.ScreenCopilotGuideUiPayload
        org.junit.Assert.assertNotNull(payload)
        assertEquals("UnrealEditor.exe - MyLevel", payload?.activeWindow)
        assertTrue(payload?.guidanceTitle?.contains("exportar la textura") == true)
        assertEquals(2, payload?.guidanceSteps?.size)
        assertEquals("CTRL+E", payload?.recommendedShortcut)
        org.junit.Assert.assertArrayEquals(byteArrayOf(4, 5, 6), payload?.screenshotBytes)

        assertTrue(output.speech.contains("exportar la textura"))
        assertTrue(output.speech.contains("CTRL+E") || output.speech.contains("Ctrl+E", ignoreCase = true))
    }

    @Test
    fun testScreenAnalysisExecutionWithSpecificQuery() = runBlocking {
        val bridge = MockScreenCopilotBridge(shouldSucceed = true)
        val skill = PcScreenCopilotSkill(bridge)

        val query = "¿Por qué falló la compilación de gradle?"
        val score = skill.score(dummyContext, "analiza la pantalla")
        skill.execute(dummyContext, query, score)

        assertEquals(query, bridge.lastPromptReceived)
    }

    @Test
    fun testScreenAnalysisExecutionFailure() = runBlocking {
        val bridge = MockScreenCopilotBridge(shouldSucceed = false)
        val skill = PcScreenCopilotSkill(bridge)

        val score = skill.score(dummyContext, "analiza la pantalla")
        val output = skill.execute(dummyContext, "analiza la pantalla", score)

        assertTrue(output.speech.contains("inconveniente"))
        assertTrue(output.speech.contains("Timeout de conexión"))
    }

    @Test
    fun testScreenAnalysisDisconnected() = runBlocking {
        val skill = PcScreenCopilotSkill(pcBridge = null)
        val score = skill.score(dummyContext, "analiza la pantalla")
        val output = skill.execute(dummyContext, "analiza la pantalla", score)

        assertTrue(output.speech.contains("No hay conexión con la PC"))
        assertEquals("PC Desconectada", output.displayText)
    }
}

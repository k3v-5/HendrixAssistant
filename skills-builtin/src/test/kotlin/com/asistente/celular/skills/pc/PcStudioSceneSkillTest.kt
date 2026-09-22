package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.scene.PcSceneExecutionResult
import com.asistente.celular.nlu.pc.scene.PcStudioSceneRegistry
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcStudioSceneSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockStudioSceneBridge : PcWorkspaceBridge {
        var lastExecutedSceneId: String? = null

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

        override suspend fun executeStudioScene(sceneId: String): PcSceneExecutionResult {
            lastExecutedSceneId = sceneId
            return PcSceneExecutionResult(
                sceneId = sceneId,
                success = true,
                stepsExecuted = 3,
                totalSteps = 3,
                message = "Escena $sceneId completada exitosamente"
            )
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcStudioSceneSkill()

        assertTrue(skill.score(dummyContext, "activa modo produccion").isMatch)
        assertTrue(skill.score(dummyContext, "inicia el modo produccion musical").isMatch)
        assertTrue(skill.score(dummyContext, "modo render nocturno").isMatch)
        assertTrue(skill.score(dummyContext, "inicia modo streaming").isMatch)
        assertTrue(skill.score(dummyContext, "cierra el estudio").isMatch)
        assertTrue(skill.score(dummyContext, "apagar estudio").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "que hora es").isMatch)
        assertFalse(skill.score(dummyContext, "clima en buenos aires").isMatch)
    }

    @Test
    fun testExecuteMusicProductionScene() = runBlocking {
        val bridge = MockStudioSceneBridge()
        val skill = PcStudioSceneSkill(bridge)

        val score = skill.score(dummyContext, "activa el modo produccion musical")
        val output = skill.execute(dummyContext, "activa el modo produccion musical", score)

        assertEquals(PcStudioSceneRegistry.SCENE_MUSIC_PRODUCTION, bridge.lastExecutedSceneId)
        assertTrue(output.speech.contains("activado"))
        assertTrue(output.displayText.contains("ejecutada con éxito"))
    }

    @Test
    fun testExecuteRenderNightScene() = runBlocking {
        val bridge = MockStudioSceneBridge()
        val skill = PcStudioSceneSkill(bridge)

        val score = skill.score(dummyContext, "inicia render nocturno")
        val output = skill.execute(dummyContext, "inicia render nocturno", score)

        assertEquals(PcStudioSceneRegistry.SCENE_RENDER_NIGHT, bridge.lastExecutedSceneId)
        assertTrue(output.speech.contains("activado"))
    }

    @Test
    fun testExecuteCloseStudioScene() = runBlocking {
        val bridge = MockStudioSceneBridge()
        val skill = PcStudioSceneSkill(bridge)

        val score = skill.score(dummyContext, "cierra el estudio")
        val output = skill.execute(dummyContext, "cierra el estudio", score)

        assertEquals(PcStudioSceneRegistry.SCENE_CLOSE, bridge.lastExecutedSceneId)
        assertTrue(output.speech.contains("activado"))
    }

    @Test
    fun testDisconnectedBridge() = runBlocking {
        val skill = PcStudioSceneSkill(null)
        val score = skill.score(dummyContext, "activa modo streaming")
        val output = skill.execute(dummyContext, "activa modo streaming", score)

        assertTrue(output.speech.contains("No hay conexión"))
    }
}

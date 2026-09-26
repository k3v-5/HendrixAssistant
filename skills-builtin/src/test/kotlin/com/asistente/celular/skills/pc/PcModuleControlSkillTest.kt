package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.module.PcModuleActionRequest
import com.asistente.celular.nlu.pc.module.PcModuleActionResult
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para PcModuleControlSkill.
 * Valida el reconocimiento de comandos y la ejecución para FL Studio, Blender, Unreal Engine y Adobe Suite.
 */
class PcModuleControlSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockModuleBridge : PcWorkspaceBridge {
        var lastModuleRequest: PcModuleActionRequest? = null

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.COMMAND_ONLY)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(PcSystemTelemetry(hostname = "Studio-PC"))
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = null
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean = true
        override suspend fun executeQuickCommand(command: String): Boolean = true
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan =
            AutonomousTaskPlan(planId = "p1", userGoal = goalPrompt, steps = emptyList())
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = telemetry.value
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun executeModuleAction(request: PcModuleActionRequest): PcModuleActionResult {
            lastModuleRequest = request
            return PcModuleActionResult(success = true, message = "Ejecutado con éxito")
        }
    }

    @Test
    fun testScoring() {
        val skill = PcModuleControlSkill()

        assertTrue(skill.score(dummyContext, "reproduce en fl studio").isMatch)
        assertTrue(skill.score(dummyContext, "graba en fl studio").isMatch)
        assertTrue(skill.score(dummyContext, "renderiza en blender").isMatch)
        assertTrue(skill.score(dummyContext, "render de animacion en blender").isMatch)
        assertTrue(skill.score(dummyContext, "play en unreal").isMatch)
        assertTrue(skill.score(dummyContext, "simular en unreal").isMatch)
        assertTrue(skill.score(dummyContext, "cortar en premiere").isMatch)
        assertTrue(skill.score(dummyContext, "pincel en photoshop").isMatch)

        assertFalse(skill.score(dummyContext, "como esta el clima").isMatch)
    }

    @Test
    fun testFLStudioExecution() = runBlocking {
        val bridge = MockModuleBridge()
        val skill = PcModuleControlSkill(bridge)

        // 1. Play / Pause
        val scorePlay = skill.score(dummyContext, "play en fl studio")
        val outPlay = skill.execute(dummyContext, "play en fl studio", scorePlay)
        assertEquals(PcModuleId.FL_STUDIO, bridge.lastModuleRequest?.moduleId)
        assertEquals("PLAY_PAUSE", bridge.lastModuleRequest?.actionId)
        assertTrue(outPlay.speech.contains("FL Studio"))

        // 2. Grabar
        val scoreRec = skill.score(dummyContext, "graba en fl studio")
        skill.execute(dummyContext, "graba en fl studio", scoreRec)
        assertEquals("RECORD", bridge.lastModuleRequest?.actionId)

        // 3. Mezclador
        val scoreMix = skill.score(dummyContext, "abre el mixer en fl studio")
        skill.execute(dummyContext, "abre el mixer en fl studio", scoreMix)
        assertEquals("VIEW_MIXER", bridge.lastModuleRequest?.actionId)
    }

    @Test
    fun testBlenderExecution() = runBlocking {
        val bridge = MockModuleBridge()
        val skill = PcModuleControlSkill(bridge)

        // 1. Render Imagen
        val scoreImg = skill.score(dummyContext, "renderiza la imagen en blender")
        val outImg = skill.execute(dummyContext, "renderiza la imagen en blender", scoreImg)
        assertEquals(PcModuleId.BLENDER, bridge.lastModuleRequest?.moduleId)
        assertEquals("RENDER_IMAGE", bridge.lastModuleRequest?.actionId)
        assertTrue(outImg.speech.contains("imagen"))

        // 2. Render Animacion
        val scoreAnim = skill.score(dummyContext, "renderiza la animacion en blender")
        skill.execute(dummyContext, "renderiza la animacion en blender", scoreAnim)
        assertEquals("RENDER_ANIM", bridge.lastModuleRequest?.actionId)

        // 3. Camara
        val scoreCam = skill.score(dummyContext, "vista de camara en blender")
        skill.execute(dummyContext, "vista de camara en blender", scoreCam)
        assertEquals("VIEW_CAMERA", bridge.lastModuleRequest?.actionId)
    }

    @Test
    fun testUnrealEngineExecution() = runBlocking {
        val bridge = MockModuleBridge()
        val skill = PcModuleControlSkill(bridge)

        // 1. Play in editor
        val scorePlay = skill.score(dummyContext, "play en unreal")
        val outPlay = skill.execute(dummyContext, "play en unreal", scorePlay)
        assertEquals(PcModuleId.UNREAL_ENGINE, bridge.lastModuleRequest?.moduleId)
        assertEquals("PLAY_IN_EDITOR", bridge.lastModuleRequest?.actionId)
        assertTrue(outPlay.speech.contains("Play In Editor"))

        // 2. Simular
        val scoreSim = skill.score(dummyContext, "simular en unreal")
        // 3. Detener
        val scoreStop = skill.score(dummyContext, "deten la simulacion en unreal")
        skill.execute(dummyContext, "deten la simulacion en unreal", scoreStop)
        assertEquals("STOP_SIMULATION", bridge.lastModuleRequest?.actionId)

        // 4. Lanzar Unreal Engine
        val scoreLaunch = skill.score(dummyContext, "lanza unreal")
        val outLaunch = skill.execute(dummyContext, "lanza unreal", scoreLaunch)
        assertEquals(PcModuleId.UNREAL_ENGINE, bridge.lastModuleRequest?.moduleId)
        assertEquals("LAUNCH_UNREAL", bridge.lastModuleRequest?.actionId)
        assertTrue(outLaunch.speech.contains("Unreal Engine 5"))
    }

    @Test
    fun testAdobeSuiteExecution() = runBlocking {
        val bridge = MockModuleBridge()
        val skill = PcModuleControlSkill(bridge)

        // 1. Cuchilla en Premiere
        val scoreCut = skill.score(dummyContext, "cortar en premiere")
        val outCut = skill.execute(dummyContext, "cortar en premiere", scoreCut)
        assertEquals(PcModuleId.ADOBE_CREATIVE, bridge.lastModuleRequest?.moduleId)
        assertEquals("RAZOR_TOOL", bridge.lastModuleRequest?.actionId)
        assertTrue(outCut.speech.contains("Cuchilla"))

        // 2. Pincel en Photoshop
        val scoreBrush = skill.score(dummyContext, "pincel en photoshop")
        skill.execute(dummyContext, "pincel en photoshop", scoreBrush)
        assertEquals("BRUSH_TOOL", bridge.lastModuleRequest?.actionId)
    }
}

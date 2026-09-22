package com.asistente.celular.nlu.pc.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcStudioSceneModelsTest {

    @Test
    fun testPreconfiguredScenesRegistry() {
        val scenes = PcStudioSceneRegistry.ALL_SCENES
        assertEquals(4, scenes.size)

        val musicScene = PcStudioSceneRegistry.findById("STUDIO_MUSIC_MODE")
        assertNotNull(musicScene)
        assertEquals("🎹", musicScene?.iconEmoji)
        assertTrue(musicScene!!.steps.any { it is PcSceneStep.WakeOnLan })
        assertTrue(musicScene.steps.any { it is PcSceneStep.UnlockSession })
        assertTrue(musicScene.steps.any { it is PcSceneStep.LaunchAppOrProject })

        val renderScene = PcStudioSceneRegistry.findById("STUDIO_RENDER_NIGHT_MODE")
        assertNotNull(renderScene)
        assertEquals("🌙", renderScene?.iconEmoji)
        assertTrue(renderScene!!.steps.any { it is PcSceneStep.StartHardwareWatchdog })

        val closeScene = PcStudioSceneRegistry.findById("STUDIO_CLOSE_MODE")
        assertNotNull(closeScene)
        assertEquals("🔒", closeScene?.iconEmoji)
        assertTrue(closeScene!!.steps.any { it is PcSceneStep.PowerAction })
    }

    @Test
    fun testSceneExecutionResult() {
        val res = PcSceneExecutionResult(
            sceneId = "STUDIO_MUSIC_MODE",
            success = true,
            stepsExecuted = 4,
            totalSteps = 4,
            message = "Estudio configurado con éxito"
        )
        assertTrue(res.success)
        assertEquals(4, res.stepsExecuted)
        assertEquals(4, res.totalSteps)
    }
}

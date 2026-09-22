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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite completa de pruebas unitarias para WebBrowserControlSkill.
 * Valida reconocimiento de intenciones por voz, extracción de parámetros (navegador, plataforma, consulta)
 * y despacho de solicitudes hacia la PC a través de PcWorkspaceBridge.
 */
class WebBrowserControlSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockBrowserBridge : PcWorkspaceBridge {
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
        val skill = WebBrowserControlSkill()

        assertTrue(skill.score(dummyContext, "abrir brave y buscar en google plugins de produccion en facebook").isMatch)
        assertTrue(skill.score(dummyContext, "abrir chrome y buscar en youtube como realizar una cancion de cero a 100").isMatch)
        assertTrue(skill.score(dummyContext, "abre opera y busca tutoriales de blender en youtube").isMatch)
        assertTrue(skill.score(dummyContext, "abrir chrome").isMatch)
        assertTrue(skill.score(dummyContext, "nueva pestaña en brave").isMatch)
        assertTrue(skill.score(dummyContext, "cerrar pestaña en chrome").isMatch)
        assertTrue(skill.score(dummyContext, "recargar pagina en opera").isMatch)

        assertFalse(skill.score(dummyContext, "como esta el clima de manana").isMatch)
    }

    @Test
    fun testBraveSearchOnGoogleForFacebookPlugins() = runBlocking {
        val bridge = MockBrowserBridge()
        val skill = WebBrowserControlSkill(bridge)
        val command = "abrir brave y buscar en google plugins de produccion en facebook"

        val score = skill.score(dummyContext, command)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, command, score)
        assertNotNull(bridge.lastModuleRequest)
        val req = bridge.lastModuleRequest!!

        assertEquals(PcModuleId.WEB_BROWSERS, req.moduleId)
        assertEquals("NAVIGATE_OR_SEARCH", req.actionId)
        assertEquals("brave", req.params["browser"])
        assertEquals("google", req.params["platform"])
        assertEquals("plugins de produccion en facebook", req.params["query"])
        assertTrue(output.speech.contains("plugins de produccion en facebook"))
        assertTrue(output.speech.contains("Google"))
        assertTrue(output.speech.contains("Brave"))
    }

    @Test
    fun testChromeSearchOnYouTubeForSongCreation() = runBlocking {
        val bridge = MockBrowserBridge()
        val skill = WebBrowserControlSkill(bridge)
        val command = "abrir chrome y buscar en youtube como realizar una cancion de cero a 100"

        val score = skill.score(dummyContext, command)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, command, score)
        assertNotNull(bridge.lastModuleRequest)
        val req = bridge.lastModuleRequest!!

        assertEquals(PcModuleId.WEB_BROWSERS, req.moduleId)
        assertEquals("NAVIGATE_OR_SEARCH", req.actionId)
        assertEquals("chrome", req.params["browser"])
        assertEquals("youtube", req.params["platform"])
        assertEquals("como realizar una cancion de cero a 100", req.params["query"])
        assertTrue(output.speech.contains("como realizar una cancion de cero a 100"))
        assertTrue(output.speech.contains("YouTube"))
    }

    @Test
    fun testOperaSearchOnYouTubeWithTrailingPlatform() = runBlocking {
        val bridge = MockBrowserBridge()
        val skill = WebBrowserControlSkill(bridge)
        val command = "abre opera y busca tutoriales de blender en youtube"

        val score = skill.score(dummyContext, command)
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, command, score)
        assertNotNull(bridge.lastModuleRequest)
        val req = bridge.lastModuleRequest!!

        assertEquals(PcModuleId.WEB_BROWSERS, req.moduleId)
        assertEquals("NAVIGATE_OR_SEARCH", req.actionId)
        assertEquals("opera", req.params["browser"])
        assertEquals("youtube", req.params["platform"])
        assertEquals("tutoriales de blender", req.params["query"])
        assertTrue(output.speech.contains("tutoriales de blender"))
        assertTrue(output.speech.contains("YouTube"))
        assertTrue(output.speech.contains("Opera"))
    }

    @Test
    fun testOpenBrowserDirectly() = runBlocking {
        val bridge = MockBrowserBridge()
        val skill = WebBrowserControlSkill(bridge)

        val score = skill.score(dummyContext, "abrir chrome")
        val output = skill.execute(dummyContext, "abrir chrome", score)

        assertEquals(PcModuleId.WEB_BROWSERS, bridge.lastModuleRequest?.moduleId)
        assertEquals("OPEN_CHROME", bridge.lastModuleRequest?.actionId)
        assertEquals("chrome", bridge.lastModuleRequest?.params?.get("browser"))
        assertTrue(output.speech.contains("Google Chrome"))
    }

    @Test
    fun testTabOperations() = runBlocking {
        val bridge = MockBrowserBridge()
        val skill = WebBrowserControlSkill(bridge)

        // 1. Nueva pestaña en Brave
        val scoreNew = skill.score(dummyContext, "nueva pestaña en brave")
        skill.execute(dummyContext, "nueva pestaña en brave", scoreNew)
        assertEquals("NEW_TAB", bridge.lastModuleRequest?.actionId)
        assertEquals("brave", bridge.lastModuleRequest?.params?.get("browser"))

        // 2. Cerrar pestaña en Chrome
        val scoreClose = skill.score(dummyContext, "cerrar pestaña en chrome")
        skill.execute(dummyContext, "cerrar pestaña en chrome", scoreClose)
        assertEquals("CLOSE_TAB", bridge.lastModuleRequest?.actionId)
        assertEquals("chrome", bridge.lastModuleRequest?.params?.get("browser"))

        // 3. Recargar página en Opera
        val scoreReload = skill.score(dummyContext, "recargar pagina en opera")
        skill.execute(dummyContext, "recargar pagina en opera", scoreReload)
        assertEquals("RELOAD_PAGE", bridge.lastModuleRequest?.actionId)
        assertEquals("opera", bridge.lastModuleRequest?.params?.get("browser"))
    }
}

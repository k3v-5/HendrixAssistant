package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.plugin.PcPluginAction
import com.asistente.celular.nlu.pc.plugin.PcPluginActionResult
import com.asistente.celular.nlu.pc.plugin.PcPluginDefinition
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcCustomPluginSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockPluginBridge(
        var actionSuccess: Boolean = true
    ) : PcWorkspaceBridge {
        var lastPluginExecuted: String? = null
        var lastActionExecuted: String? = null

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

        override suspend fun queryCustomPlugins(): List<PcPluginDefinition> {
            return listOf(
                PcPluginDefinition(
                    id = "backup_projects",
                    name = "Respaldo Automático de Proyectos",
                    version = "1.0.0",
                    description = "Crea copias de seguridad de proyectos de audio, 3D y dropzone.",
                    actions = listOf(
                        PcPluginAction(id = "backup_now", label = "Respaldar Ahora")
                    )
                ),
                PcPluginDefinition(
                    id = "temp_cleaner",
                    name = "Limpiador de Temporales",
                    version = "1.0.0",
                    description = "Elimina archivos temporales de Windows y caches.",
                    actions = listOf(
                        PcPluginAction(id = "clean_now", label = "Limpiar Temporales", dangerous = true)
                    )
                ),
                PcPluginDefinition(
                    id = "command_runner",
                    name = "Ejecutor de Diagnósticos",
                    version = "1.0.0",
                    description = "Comandos de red y utilidades de sistema.",
                    actions = listOf(
                        PcPluginAction(id = "flush_dns", label = "Vaciar Caché DNS"),
                        PcPluginAction(id = "check_disk", label = "Espacio en Disco")
                    )
                )
            )
        }

        override suspend fun executeCustomPluginAction(
            pluginId: String,
            actionId: String,
            params: Map<String, Any>
        ): PcPluginActionResult {
            lastPluginExecuted = pluginId
            lastActionExecuted = actionId

            return if (actionSuccess) {
                PcPluginActionResult(
                    pluginId = pluginId,
                    actionId = actionId,
                    success = true,
                    message = "Operación completada exitosamente en 120ms.",
                    output = "Se liberaron 450 MB de archivos temporales.",
                    elapsedMs = 120L
                )
            } else {
                PcPluginActionResult(
                    pluginId = pluginId,
                    actionId = actionId,
                    success = false,
                    message = "Permiso denegado al intentar limpiar archivos en uso.",
                    elapsedMs = 45L
                )
            }
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val skill = PcCustomPluginSkill()

        assertTrue(skill.score(dummyContext, "plugins de la pc").isMatch)
        assertTrue(skill.score(dummyContext, "que plugins hay").isMatch)
        assertTrue(skill.score(dummyContext, "plugins instalados").isMatch)
        assertTrue(skill.score(dummyContext, "limpia los temporales").isMatch)
        assertTrue(skill.score(dummyContext, "limpiar cache").isMatch)
        assertTrue(skill.score(dummyContext, "backup de proyectos").isMatch)
        assertTrue(skill.score(dummyContext, "flush dns").isMatch)
        assertTrue(skill.score(dummyContext, "espacio en disco").isMatch)

        // Negative cases
        assertFalse(skill.score(dummyContext, "sube el volumen al 50").isMatch)
        assertFalse(skill.score(dummyContext, "recuerdame comprar pan").isMatch)
    }

    @Test
    fun testListCustomPlugins() = runBlocking {
        val bridge = MockPluginBridge()
        val skill = PcCustomPluginSkill(bridge)

        val score = skill.score(dummyContext, "que plugins hay")
        val output = skill.execute(dummyContext, "que plugins hay", score)

        assertTrue(output.speech.contains("3 plugins disponibles"))
        assertTrue(output.speech.contains("Respaldo Automático de Proyectos"))
        assertTrue(output.speech.contains("Limpiador de Temporales"))
    }

    @Test
    fun testExecutePluginActionSuccess() = runBlocking {
        val bridge = MockPluginBridge(actionSuccess = true)
        val skill = PcCustomPluginSkill(bridge)

        val score = skill.score(dummyContext, "limpia los temporales")
        val output = skill.execute(dummyContext, "limpia los temporales", score)

        assertEquals("temp_cleaner", bridge.lastPluginExecuted)
        assertEquals("clean_now", bridge.lastActionExecuted)
        assertTrue(output.speech.contains("Limpiar Temporales completada con éxito"))
        assertTrue(output.displayText?.contains("450 MB") == true)
    }

    @Test
    fun testExecutePluginActionFailure() = runBlocking {
        val bridge = MockPluginBridge(actionSuccess = false)
        val skill = PcCustomPluginSkill(bridge)

        val score = skill.score(dummyContext, "limpia los temporales")
        val output = skill.execute(dummyContext, "limpia los temporales", score)

        assertEquals("temp_cleaner", bridge.lastPluginExecuted)
        assertEquals("clean_now", bridge.lastActionExecuted)
        assertTrue(output.speech.contains("no se pudo completar"))
        assertTrue(output.speech.contains("Permiso denegado"))
    }

    @Test
    fun testExecuteBackupPluginAction() = runBlocking {
        val bridge = MockPluginBridge(actionSuccess = true)
        val skill = PcCustomPluginSkill(bridge)

        val score = skill.score(dummyContext, "backup de proyectos")
        skill.execute(dummyContext, "backup de proyectos", score)

        assertEquals("backup_projects", bridge.lastPluginExecuted)
        assertEquals("backup_now", bridge.lastActionExecuted)
    }

    @Test
    fun testDisconnected() = runBlocking {
        val skill = PcCustomPluginSkill(pcBridge = null)
        val score = skill.score(dummyContext, "que plugins hay")
        val output = skill.execute(dummyContext, "que plugins hay", score)

        assertTrue(output.speech.contains("No hay conexión con la PC"))
        assertEquals("PC Desconectada", output.displayText)
    }
}

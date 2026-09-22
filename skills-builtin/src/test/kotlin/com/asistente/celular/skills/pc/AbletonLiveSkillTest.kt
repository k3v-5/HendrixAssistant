package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.daw.DawAction
import com.asistente.celular.nlu.pc.daw.DawActionRequest
import com.asistente.celular.nlu.pc.daw.DawActionResult
import com.asistente.celular.nlu.pc.daw.DawProjectInfo
import com.asistente.celular.nlu.pc.daw.DawType
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.DawControlUiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite exhaustiva de pruebas unitarias para AbletonLiveSkill.
 * Valida el reconocimiento sintáctico de intenciones, la extracción de proyectos,
 * el envío de acciones de producción musical y la gestión preventiva de diálogos modales.
 */
class AbletonLiveSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockDawBridge : PcWorkspaceBridge {
        var lastDawRequest: DawActionRequest? = null
        var dawActionResultToReturn: DawActionResult = DawActionResult(success = true)

        val sampleProjects = listOf(
            DawProjectInfo("Synthwave Beat", "D:/Musica/Synthwave Beat.als", 1000L, DawType.ABLETON_LIVE, 204800L),
            DawProjectInfo("Vocal Mixdown", "D:/Musica/Vocal Mixdown.als", 900L, DawType.ABLETON_LIVE, 409600L)
        )

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.COMMAND_ONLY)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(
            PcSystemTelemetry(hostname = "Studio-Workstation")
        )
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

        override suspend fun executeDawAction(request: DawActionRequest): DawActionResult {
            lastDawRequest = request
            return dawActionResultToReturn
        }

        override suspend fun queryDawProjects(dawType: DawType): List<DawProjectInfo> = sampleProjects
    }

    @Test
    fun testGrammarMatching() {
        val skill = AbletonLiveSkill()

        assertTrue(skill.score(dummyContext, "abre ableton").isMatch)
        assertTrue(skill.score(dummyContext, "abrir ableton live").isMatch)
        assertTrue(skill.score(dummyContext, "guarda el proyecto en ableton").isMatch)
        assertTrue(skill.score(dummyContext, "nuevo proyecto en ableton").isMatch)
        assertTrue(skill.score(dummyContext, "carga el proyecto en ableton").isMatch)
        assertTrue(skill.score(dummyContext, "exporta el audio en ableton").isMatch)
        assertTrue(skill.score(dummyContext, "reproduce ableton").isMatch)
        assertTrue(skill.score(dummyContext, "graba en ableton").isMatch)

        // Consultas no relacionadas
        assertFalse(skill.score(dummyContext, "que hora es").isMatch)
        assertFalse(skill.score(dummyContext, "pon una alarma").isMatch)
    }

    @Test
    fun testLaunchOrFocusFlow() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "abre ableton live"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.LAUNCH_OR_FOCUS, bridge.lastDawRequest?.action)
        assertEquals(DawType.ABLETON_LIVE, bridge.lastDawRequest?.dawType)

        assertNotNull(output.payload)
        assertTrue(output.payload is DawControlUiPayload)
        val payload = output.payload as DawControlUiPayload
        assertEquals(2, payload.recentProjects.size)
        assertTrue(output.speech.contains("Abriendo Ableton Live"))
    }

    @Test
    fun testSaveProjectFlow() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "guarda el proyecto en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.SAVE_PROJECT, bridge.lastDawRequest?.action)
        assertTrue(output.speech.contains("guardado correctamente"))
        val payload = output.payload as DawControlUiPayload
        assertTrue(payload.statusMessage.contains("Ctrl+S"))
    }

    @Test
    fun testNewProjectFlow_Clean() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "crear nuevo proyecto en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.NEW_PROJECT, bridge.lastDawRequest?.action)
        assertEquals(null, bridge.lastDawRequest?.saveCurrentFirst)
        assertTrue(output.speech.contains("Nuevo Live Set creado"))
        val payload = output.payload as DawControlUiPayload
        assertTrue(payload.statusMessage.contains("Ctrl+N"))
        assertFalse(payload.isWaitingSaveConfirmation)
    }

    @Test
    fun testNewProjectFlow_RequiresSaveConfirmation() = runBlocking {
        val bridge = MockDawBridge().apply {
            dawActionResultToReturn = DawActionResult(
                success = true,
                requiresConfirmation = true,
                confirmationTitle = "¿Guardar cambios en Live Set actual?"
            )
        }
        val skill = AbletonLiveSkill(bridge)

        val query = "nuevo proyecto en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.NEW_PROJECT, bridge.lastDawRequest?.action)
        assertTrue(output.speech.contains("pregunta si deseas guardar los cambios"))
        val payload = output.payload as DawControlUiPayload
        assertTrue(payload.isWaitingSaveConfirmation)
        assertEquals("¿Guardar cambios en Live Set actual?", payload.confirmationDialogTitle)
        assertTrue(payload.statusMessage.contains("Confirmación requerida"))
    }

    @Test
    fun testNewProjectFlow_ProactiveSaveFirst() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "nuevo proyecto en ableton guardando el actual"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.NEW_PROJECT, bridge.lastDawRequest?.action)
        assertEquals(true, bridge.lastDawRequest?.saveCurrentFirst)
        assertTrue(output.speech.contains("Guardando proyecto actual"))
        val payload = output.payload as DawControlUiPayload
        assertFalse(payload.isWaitingSaveConfirmation)
    }

    @Test
    fun testNewProjectFlow_ProactiveDiscardFirst() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "crear un nuevo proyecto en ableton sin guardar"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.NEW_PROJECT, bridge.lastDawRequest?.action)
        assertEquals(false, bridge.lastDawRequest?.saveCurrentFirst)
        assertTrue(output.speech.contains("sin guardar"))
        val payload = output.payload as DawControlUiPayload
        assertFalse(payload.isWaitingSaveConfirmation)
    }

    @Test
    fun testModalConfirmationResolution_Save() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "si guardar en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.CONFIRM_SAVE_BEFORE_ACTION, bridge.lastDawRequest?.action)
        assertTrue(output.speech.contains("Guardando cambios"))
        val payload = output.payload as DawControlUiPayload
        assertFalse(payload.isWaitingSaveConfirmation)
    }

    @Test
    fun testModalConfirmationResolution_Discard() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "descartar en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.DISCARD_AND_CONTINUE, bridge.lastDawRequest?.action)
        assertTrue(output.speech.contains("Descartando cambios"))
        val payload = output.payload as DawControlUiPayload
        assertFalse(payload.isWaitingSaveConfirmation)
    }

    @Test
    fun testModalConfirmationResolution_Cancel() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "cancelar en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.CANCEL_ACTION, bridge.lastDawRequest?.action)
        assertTrue(output.speech.contains("Operación cancelada"))
        val payload = output.payload as DawControlUiPayload
        assertFalse(payload.isWaitingSaveConfirmation)
    }

    @Test
    fun testContextualScoring_WhenWaitingConfirmation() {
        val skill = AbletonLiveSkill()
        val waitingPayload = DawControlUiPayload(
            dawType = DawType.ABLETON_LIVE,
            isWaitingSaveConfirmation = true
        )
        val contextWithWaitingModal = object : SkillContext {
            override val androidContext: android.content.Context get() = error("Dummy")
            override val isConnectedToInternet: Boolean = true
            override val previousOutput: SkillOutput = SkillOutput(
                speech = "¿Deseas guardar?",
                displayText = "¿Deseas guardar?",
                payload = waitingPayload
            )
        }

        assertTrue(skill.score(contextWithWaitingModal, "descartar").isMatch)
        assertTrue(skill.score(contextWithWaitingModal, "guardar cambios").isMatch)
        assertTrue(skill.score(contextWithWaitingModal, "cancelar").isMatch)
    }

    @Test
    fun testOpenNamedProjectFlow() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "carga el proyecto Synthwave Beat en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.OPEN_PROJECT, bridge.lastDawRequest?.action)
        assertEquals("Synthwave Beat", bridge.lastDawRequest?.targetProjectNameOrPath)
        assertTrue(output.speech.contains("Cargando el proyecto Synthwave Beat"))
        val payload = output.payload as DawControlUiPayload
        assertEquals("Synthwave Beat", payload.activeProjectName)
    }

    @Test
    fun testExportAudioFlow() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val query = "exporta el audio en ableton"
        val score = skill.score(dummyContext, query)
        val output = skill.execute(dummyContext, query, score)

        assertEquals(DawAction.EXPORT_AUDIO, bridge.lastDawRequest?.action)
        assertTrue(output.speech.contains("exportación de audio"))
        val payload = output.payload as DawControlUiPayload
        assertTrue(payload.statusMessage.contains("Ctrl+Shift+R"))
    }

    @Test
    fun testTransportControlsFlow() = runBlocking {
        val bridge = MockDawBridge()
        val skill = AbletonLiveSkill(bridge)

        val playScore = skill.score(dummyContext, "reproduce ableton")
        val playOutput = skill.execute(dummyContext, "reproduce ableton", playScore)
        assertEquals(DawAction.PLAY_PAUSE, bridge.lastDawRequest?.action)
        assertTrue((playOutput.payload as DawControlUiPayload).isPlaying)

        val recScore = skill.score(dummyContext, "graba en ableton")
        val recOutput = skill.execute(dummyContext, "graba en ableton", recScore)
        assertEquals(DawAction.RECORD, bridge.lastDawRequest?.action)
        assertTrue((recOutput.payload as DawControlUiPayload).isRecording)
    }
}

package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.automation.AutomatedRoutine
import com.asistente.celular.nlu.automation.AutomatedRoutineAction
import com.asistente.celular.nlu.automation.AutomatedRoutineEngine
import com.asistente.celular.nlu.automation.AutomatedRoutineRepository
import com.asistente.celular.nlu.automation.AutomatedRoutineTrigger
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcAutomatedRoutineSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockAutomatedRoutineRepository : AutomatedRoutineRepository {
        override val engine = AutomatedRoutineEngine()
        private val _routines = MutableStateFlow<List<AutomatedRoutine>>(emptyList())
        override val routines: StateFlow<List<AutomatedRoutine>> = _routines.asStateFlow()

        init {
            val sampleRoutines = listOf(
                AutomatedRoutine(
                    id = "routine_arrival",
                    name = "Llegada al Estudio",
                    description = "Enciende la PC y saluda.",
                    iconEmoji = "🏡",
                    isEnabled = true,
                    triggers = listOf(
                        AutomatedRoutineTrigger.VoicePhraseTrigger(listOf("llegada al estudio", "buenos dias estudio"))
                    ),
                    actions = listOf(
                        AutomatedRoutineAction.PcQuickCommandAction("wake_on_lan"),
                        AutomatedRoutineAction.PcQuickCommandAction("unlock"),
                        AutomatedRoutineAction.SpeakTtsAction("Bienvenido al estudio.")
                    )
                ),
                AutomatedRoutine(
                    id = "routine_leave",
                    name = "Salida de Casa",
                    description = "Suspende la PC.",
                    iconEmoji = "🚗",
                    isEnabled = true,
                    triggers = emptyList(),
                    actions = listOf(
                        AutomatedRoutineAction.PcQuickCommandAction("sleep")
                    )
                )
            )
            sampleRoutines.forEach { engine.registerRoutine(it) }
            _routines.value = sampleRoutines
        }

        override suspend fun saveRoutine(routine: AutomatedRoutine) {
            engine.registerRoutine(routine)
            _routines.value = engine.getRoutines()
        }

        override suspend fun deleteRoutine(id: String) {
            engine.deleteRoutine(id)
            _routines.value = engine.getRoutines()
        }

        override suspend fun toggleRoutine(id: String): Boolean {
            val newState = engine.toggleRoutine(id)
            _routines.value = engine.getRoutines()
            return newState
        }
    }

    private class MockRoutineBridge : PcWorkspaceBridge {
        var wolCalled = false
        var unlockCalled = false

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

        override suspend fun wakeOnLan(macAddress: String?, broadcastIp: String?): Boolean {
            wolCalled = true
            return true
        }

        override suspend fun unlockSession(pin: String): Boolean {
            unlockCalled = true
            return true
        }
    }

    @Test
    fun testVoiceGrammarMatching() {
        val repo = MockAutomatedRoutineRepository()
        val skill = PcAutomatedRoutineSkill(repo)

        assertTrue(skill.score(dummyContext, "ejecutar rutina llegada al estudio").isMatch)
        assertTrue(skill.score(dummyContext, "inicia la rutina salida de casa").isMatch)
        assertTrue(skill.score(dummyContext, "cuales son mis rutinas").isMatch)
        assertTrue(skill.score(dummyContext, "listar rutinas").isMatch)
        assertTrue(skill.score(dummyContext, "desactivar rutina salida de casa").isMatch)
        assertTrue(skill.score(dummyContext, "llegada al estudio").isMatch) // Voice phrase trigger

        // Negative cases
        assertFalse(skill.score(dummyContext, "que tiempo hace hoy").isMatch)
        assertFalse(skill.score(dummyContext, "reproducir metallica").isMatch)
    }

    @Test
    fun testExecuteRoutine() = runBlocking {
        val repo = MockAutomatedRoutineRepository()
        val bridge = MockRoutineBridge()
        val skill = PcAutomatedRoutineSkill(repo, bridge)

        val score = skill.score(dummyContext, "ejecuta la rutina llegada al estudio")
        val output = skill.execute(dummyContext, "ejecuta la rutina llegada al estudio", score)

        assertTrue(bridge.wolCalled)
        assertTrue(bridge.unlockCalled)
        assertEquals("Bienvenido al estudio.", output.speech)
    }

    @Test
    fun testListRoutines() = runBlocking {
        val repo = MockAutomatedRoutineRepository()
        val skill = PcAutomatedRoutineSkill(repo)

        val score = skill.score(dummyContext, "cuales son mis rutinas")
        val output = skill.execute(dummyContext, "cuales son mis rutinas", score)

        assertTrue(output.speech.contains("Tienes 2 rutinas"))
        assertTrue(output.speech.contains("Llegada al Estudio"))
        assertTrue(output.speech.contains("Salida de Casa"))
    }

    @Test
    fun testToggleRoutine() = runBlocking {
        val repo = MockAutomatedRoutineRepository()
        val skill = PcAutomatedRoutineSkill(repo)

        val score = skill.score(dummyContext, "desactiva la rutina llegada al estudio")
        val output = skill.execute(dummyContext, "desactiva la rutina llegada al estudio", score)

        assertTrue(output.speech.contains("desactivada"))
        val routine = repo.engine.getRoutine("routine_arrival")
        assertEquals(false, routine?.isEnabled)
    }

    @Test
    fun testUnknownRoutine() = runBlocking {
        val repo = MockAutomatedRoutineRepository()
        val skill = PcAutomatedRoutineSkill(repo)

        val score = skill.score(dummyContext, "ejecutar rutina fantasma que no existe")
        val output = skill.execute(dummyContext, "ejecutar rutina fantasma que no existe", score)

        assertTrue(output.speech.contains("No encontré"))
    }
}

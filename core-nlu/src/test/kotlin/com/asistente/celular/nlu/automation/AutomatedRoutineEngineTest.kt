package com.asistente.celular.nlu.automation

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomatedRoutineEngineTest {

    @Test
    fun testRegisterToggleAndDeleteRoutine() {
        val engine = AutomatedRoutineEngine()
        val routine = AutomatedRoutine(
            id = "test_routine_1",
            name = "Test Routine",
            isEnabled = true,
            triggers = listOf(AutomatedRoutineTrigger.WifiSsidTrigger("MyWifi", WifiTransition.CONNECTED)),
            actions = listOf(AutomatedRoutineAction.SpeakTtsAction("Hola"))
        )

        engine.registerRoutine(routine)
        assertEquals(1, engine.getRoutines().size)
        assertNotNull(engine.getRoutine("test_routine_1"))

        // Toggle to disabled
        val newState = engine.toggleRoutine("test_routine_1")
        assertFalse(newState)
        assertEquals(false, engine.getRoutine("test_routine_1")?.isEnabled)

        // Toggle to enabled
        val backToActive = engine.toggleRoutine("test_routine_1")
        assertTrue(backToActive)
        assertEquals(true, engine.getRoutine("test_routine_1")?.isEnabled)

        // Delete
        val deleted = engine.deleteRoutine("test_routine_1")
        assertTrue(deleted)
        assertEquals(0, engine.getRoutines().size)
        assertNull(engine.getRoutine("test_routine_1"))
    }

    @Test
    fun testWifiTriggerEvaluation() {
        val engine = AutomatedRoutineEngine()
        val routineHome = AutomatedRoutine(
            id = "r_home",
            name = "Llegada Home",
            triggers = listOf(AutomatedRoutineTrigger.WifiSsidTrigger("Studio_5G", WifiTransition.CONNECTED))
        )
        val routineLeave = AutomatedRoutine(
            id = "r_leave",
            name = "Salida Home",
            triggers = listOf(AutomatedRoutineTrigger.WifiSsidTrigger("Studio_5G", WifiTransition.DISCONNECTED))
        )

        engine.registerRoutine(routineHome)
        engine.registerRoutine(routineLeave)

        val connectedMatches = engine.evaluateWifiEvent("Studio_5G", WifiTransition.CONNECTED)
        assertEquals(1, connectedMatches.size)
        assertEquals("r_home", connectedMatches.first().id)

        val disconnectedMatches = engine.evaluateWifiEvent("Studio_5G", WifiTransition.DISCONNECTED)
        assertEquals(1, disconnectedMatches.size)
        assertEquals("r_leave", disconnectedMatches.first().id)

        val wrongSsidMatches = engine.evaluateWifiEvent("Other_WiFi", WifiTransition.CONNECTED)
        assertTrue(wrongSsidMatches.isEmpty())
    }

    @Test
    fun testGeofenceAndPcEventTriggers() {
        val engine = AutomatedRoutineEngine()
        val routineGeofence = AutomatedRoutine(
            id = "r_geo",
            name = "Zona Estudio",
            triggers = listOf(AutomatedRoutineTrigger.GeofenceTrigger("Estudio", GeofenceTransition.ENTER))
        )
        val routineRender = AutomatedRoutine(
            id = "r_render",
            name = "Render Alert",
            triggers = listOf(AutomatedRoutineTrigger.PcEventTrigger("RENDER_COMPLETED"))
        )

        engine.registerRoutine(routineGeofence)
        engine.registerRoutine(routineRender)

        val geoMatches = engine.evaluateGeofenceEvent("Estudio", GeofenceTransition.ENTER)
        assertEquals(1, geoMatches.size)
        assertEquals("r_geo", geoMatches.first().id)

        val exitMatches = engine.evaluateGeofenceEvent("Estudio", GeofenceTransition.EXIT)
        assertTrue(exitMatches.isEmpty())

        val pcMatches = engine.evaluatePcEvent("RENDER_COMPLETED")
        assertEquals(1, pcMatches.size)
        assertEquals("r_render", pcMatches.first().id)
    }

    @Test
    fun testExecuteRoutineChain() = runBlocking {
        val engine = AutomatedRoutineEngine()
        val actionsExecuted = mutableListOf<String>()

        val routine = AutomatedRoutine(
            id = "r_exec",
            name = "Cadena de Acciones",
            actions = listOf(
                AutomatedRoutineAction.PcQuickCommandAction("wake_on_lan"),
                AutomatedRoutineAction.DelayAction(50),
                AutomatedRoutineAction.SpeakTtsAction("Listo")
            )
        )
        engine.registerRoutine(routine)

        val count = engine.executeRoutine(routine) { action ->
            when (action) {
                is AutomatedRoutineAction.PcQuickCommandAction -> {
                    actionsExecuted.add(action.command)
                    true
                }
                is AutomatedRoutineAction.SpeakTtsAction -> {
                    actionsExecuted.add(action.text)
                    true
                }
                else -> false
            }
        }

        assertEquals(3, count) // 2 custom + 1 delay
        assertEquals(listOf("wake_on_lan", "Listo"), actionsExecuted)
        val updated = engine.getRoutine("r_exec")
        assertTrue(updated!!.lastTriggeredEpoch > 0)
    }
}

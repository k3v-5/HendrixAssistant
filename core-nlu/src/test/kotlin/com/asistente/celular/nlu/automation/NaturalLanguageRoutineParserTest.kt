package com.asistente.celular.nlu.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalLanguageRoutineParserTest {

    @Test
    fun testParseAppLaunchAndPcActions() {
        val input = "Cada vez que abra Unreal, cierra los navegadores y pon el ventilador al máximo"
        val routine = NaturalLanguageRoutineParser.parse(input)

        assertNotNull(routine)
        assertTrue(routine!!.isEnabled)
        assertEquals(1, routine.triggers.size)

        val trigger = routine.triggers.first() as? AutomatedRoutineTrigger.PcEventTrigger
        assertNotNull(trigger)
        assertEquals("FOREGROUND_APP:unreal", trigger?.eventType)

        assertEquals(2, routine.actions.size)
        val action1 = routine.actions[0] as? AutomatedRoutineAction.AssistantCommandAction
        assertNotNull(action1)
        assertTrue(action1!!.commandText.contains("cierra"))

        val action2 = routine.actions[1] as? AutomatedRoutineAction.PcPluginAction
        assertNotNull(action2)
        assertEquals("fan_control", action2!!.pluginId)
        assertEquals("set_speed", action2.actionId)
        assertEquals(100, action2.params["speed"])
    }

    @Test
    fun testParseWifiTriggerAndSceneAction() {
        val input = "Cuando me conecte al wifi Oficina, pon la escena Unreal"
        val routine = NaturalLanguageRoutineParser.parse(input)

        assertNotNull(routine)
        assertEquals(1, routine!!.triggers.size)

        val trigger = routine.triggers.first() as? AutomatedRoutineTrigger.WifiSsidTrigger
        assertNotNull(trigger)
        assertEquals("Oficina", trigger?.ssid)

        assertEquals(1, routine.actions.size)
        val action = routine.actions.first() as? AutomatedRoutineAction.PcStudioSceneAction
        assertNotNull(action)
        assertTrue(action!!.sceneId.contains("Unreal"))
    }

    @Test
    fun testParseNonRoutineReturnsNull() {
        val input = "busca en internet cómo compilar shaders en unreal"
        val routine = NaturalLanguageRoutineParser.parse(input)
        assertNull(routine)
    }
}

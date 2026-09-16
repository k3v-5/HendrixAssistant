package com.asistente.celular.nlu

import com.asistente.celular.nlu.routines.RoutineAction
import com.asistente.celular.nlu.routines.RoutineCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineCompilerTest {

    @Test
    fun testCompileRoutineWithMultipleActions() {
        val input = "cuando diga modo cine, apaga las luces, pon el volumen al 30 y dime a disfrutar"
        val routine = RoutineCompiler.compile(input)

        assertNotNull(routine)
        assertEquals("modo cine", routine!!.triggerPhrases.first())
        assertEquals(3, routine.actions.size)

        val first = routine.actions[0] as RoutineAction.ExecuteCommandAction
        assertEquals("apaga las luces", first.commandText)

        val second = routine.actions[1] as RoutineAction.ExecuteCommandAction
        assertEquals("pon el volumen al 30", second.commandText)

        val third = routine.actions[2] as RoutineAction.SpeakAction
        assertEquals("a disfrutar", third.text)
    }

    @Test
    fun testCompileRoutineWithDelay() {
        val input = "cuando diga fiesta, pon la luz roja, espera 5 segundos y pon la luz azul"
        val routine = RoutineCompiler.compile(input)

        assertNotNull(routine)
        assertEquals("fiesta", routine!!.triggerPhrases.first())
        assertEquals(3, routine.actions.size)

        val delay = routine.actions[1] as RoutineAction.DelayAction
        assertEquals(5000L, delay.delayMillis)
    }

    @Test
    fun testCompileRoutineWithAlDecirPrefix() {
        val input = "crea una rutina llamada buenas noches que apague el foco y dime que descanses"
        val routine = RoutineCompiler.compile(input)

        assertNotNull(routine)
        assertEquals("buenas noches", routine!!.triggerPhrases.first())
        assertEquals(2, routine.actions.size)
        assertTrue(routine.actions[0] is RoutineAction.ExecuteCommandAction)
        assertTrue(routine.actions[1] is RoutineAction.SpeakAction)
    }
}

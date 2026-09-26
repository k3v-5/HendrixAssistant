package com.asistente.celular.skills.timer

import android.content.Context
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.timer.ActiveTimer
import com.asistente.celular.nlu.timer.TimerCoordinator
import com.asistente.celular.nlu.timer.TimerState
import com.asistente.celular.nlu.ui.TimerStatusUiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSkillReflexTest {

    private class FakeTimerCoordinator : TimerCoordinator {
        private val _timers = MutableStateFlow<List<ActiveTimer>>(emptyList())
        override val activeTimers: StateFlow<List<ActiveTimer>> = _timers.asStateFlow()

        var isCancelled = false
        var isPaused = false
        var isResumed = false
        var isDismissed = false

        override fun startTimer(durationSeconds: Long, label: String): ActiveTimer {
            val timer = ActiveTimer(
                id = "test_timer_1",
                label = label,
                totalDurationSeconds = durationSeconds,
                remainingSeconds = durationSeconds,
                state = TimerState.RUNNING
            )
            _timers.value = _timers.value + timer
            return timer
        }

        override fun pauseTimer(timerId: String?): Boolean {
            isPaused = true
            _timers.value = _timers.value.map { it.copy(state = TimerState.PAUSED) }
            return true
        }

        override fun resumeTimer(timerId: String?): Boolean {
            isResumed = true
            _timers.value = _timers.value.map { it.copy(state = TimerState.RUNNING) }
            return true
        }

        override fun cancelTimer(timerId: String?): Boolean {
            isCancelled = true
            _timers.value = emptyList()
            return true
        }

        override fun getPrimaryTimer(): ActiveTimer? = _timers.value.firstOrNull()

        override fun dismissAlarm(timerId: String?) {
            isDismissed = true
            _timers.value = emptyList()
        }
    }

    private fun createDummyContext(): SkillContext {
        return object : SkillContext {
            override val androidContext: Context get() = error("Dummy Context")
            override val isConnectedToInternet: Boolean = true
            override val previousOutput: com.asistente.celular.nlu.skill.SkillOutput? = null
        }
    }

    @Test
    fun testStartTimerReflexFlow() = runBlocking {
        val fakeCoordinator = FakeTimerCoordinator()
        val skill = TimerSkill(fakeCoordinator)
        val context = createDummyContext()

        val input = "pon un temporizador de 5 minutos para la pizza"
        val score = skill.score(context, input)
        assertTrue(score.confidence > 0.6f)

        val output = skill.execute(context, input, score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("pizza"))
        assertTrue(output.speech.contains("5 minutos"))

        val payload = output.payload as? TimerStatusUiPayload
        assertNotNull(payload)
        assertEquals("pizza", payload?.label)
        assertEquals(300L, payload?.totalDurationSeconds)
    }

    @Test
    fun testQueryTimerRemainingReflexFlow() = runBlocking {
        val fakeCoordinator = FakeTimerCoordinator()
        fakeCoordinator.startTimer(180, "pasta")
        val skill = TimerSkill(fakeCoordinator)
        val context = createDummyContext()

        val input = "¿cuánto tiempo le queda al temporizador?"
        val score = skill.score(context, input)
        assertEquals(1.0f, score.confidence)

        val output = skill.execute(context, input, score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("pasta"))
        assertTrue(output.speech.contains("3 minutos"))
        assertTrue(output.payload is TimerStatusUiPayload)
    }

    @Test
    fun testCancelAndPauseTimerReflexFlow() = runBlocking {
        val fakeCoordinator = FakeTimerCoordinator()
        fakeCoordinator.startTimer(180, "render")
        val skill = TimerSkill(fakeCoordinator)
        val context = createDummyContext()

        // 1. Pausa
        val pauseScore = skill.score(context, "pausa el temporizador")
        val pauseOut = skill.execute(context, "pausa el temporizador", pauseScore)
        assertTrue(pauseOut.success)
        assertTrue(fakeCoordinator.isPaused)

        // 2. Reanudar
        val resumeScore = skill.score(context, "reanuda el temporizador")
        val resumeOut = skill.execute(context, "reanuda el temporizador", resumeScore)
        assertTrue(resumeOut.success)
        assertTrue(fakeCoordinator.isResumed)

        // 3. Cancelar
        val cancelScore = skill.score(context, "cancela el temporizador")
        val cancelOut = skill.execute(context, "cancela el temporizador", cancelScore)
        assertTrue(cancelOut.success)
        assertTrue(fakeCoordinator.isCancelled)
        assertTrue(cancelOut.speech.contains("cancelado"))
    }
}

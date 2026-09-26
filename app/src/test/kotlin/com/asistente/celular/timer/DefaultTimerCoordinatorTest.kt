package com.asistente.celular.timer

import com.asistente.celular.nlu.timer.TimerState
import com.asistente.celular.voice.earcon.EarconEngine
import com.asistente.celular.voice.earcon.EarconType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTimerCoordinatorTest {

    private class FakeEarconEngine : EarconEngine {
        val playedEarcons = mutableListOf<EarconType>()
        var isAlarmStopped = false

        override fun playEarcon(type: EarconType) {
            playedEarcons.add(type)
        }

        override fun stopAlarm() {
            isAlarmStopped = true
        }

        override fun release() {}
    }

    @Test
    fun testStartPauseResumeAndCancelTimer() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEarcon = FakeEarconEngine()

        val coordinator = DefaultTimerCoordinator(
            earconEngine = fakeEarcon,
            scope = testScope
        )

        // 1. Iniciar temporizador
        val timer = coordinator.startTimer(durationSeconds = 120, label = "pasta")
        testScope.testScheduler.runCurrent()

        assertEquals("pasta", timer.label)
        assertEquals(120L, timer.totalDurationSeconds)
        assertEquals(1, coordinator.activeTimers.value.size)

        val primary = coordinator.getPrimaryTimer()
        assertNotNull(primary)
        assertEquals(timer.id, primary?.id)

        // 2. Pausar temporizador
        coordinator.pauseTimer(timer.id)
        testScope.testScheduler.runCurrent()
        assertEquals(TimerState.PAUSED, coordinator.activeTimers.value.first().state)

        // 3. Reanudar temporizador
        coordinator.resumeTimer(timer.id)
        testScope.testScheduler.runCurrent()
        assertEquals(TimerState.RUNNING, coordinator.activeTimers.value.first().state)

        // 4. Cancelar temporizador
        coordinator.cancelTimer(timer.id)
        testScope.testScheduler.runCurrent()
        assertTrue(coordinator.activeTimers.value.isEmpty())
        assertNull(coordinator.getPrimaryTimer())
    }

    @Test
    fun testTimerCountdownAndExpirationAlarm() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEarcon = FakeEarconEngine()

        val coordinator = DefaultTimerCoordinator(
            earconEngine = fakeEarcon,
            scope = testScope
        )

        coordinator.startTimer(durationSeconds = 3, label = "huevo")
        testScope.testScheduler.runCurrent()

        // Avanzar 1 segundo
        testScope.testScheduler.advanceTimeBy(1050)
        testScope.testScheduler.runCurrent()
        assertEquals(2L, coordinator.activeTimers.value.first().remainingSeconds)

        // Avanzar 2 segundos más para expirar
        testScope.testScheduler.advanceTimeBy(2100)
        testScope.testScheduler.runCurrent()

        val expired = coordinator.activeTimers.value.first()
        assertEquals(0L, expired.remainingSeconds)
        assertEquals(TimerState.COMPLETED, expired.state)
        assertTrue(expired.isRinging)
        assertTrue(fakeEarcon.playedEarcons.contains(EarconType.TIMER_ALARM))

        // Descartar alarma
        coordinator.dismissAlarm(expired.id)
        testScope.testScheduler.runCurrent()
        assertTrue(fakeEarcon.isAlarmStopped)
        assertTrue(coordinator.activeTimers.value.isEmpty())
    }
}

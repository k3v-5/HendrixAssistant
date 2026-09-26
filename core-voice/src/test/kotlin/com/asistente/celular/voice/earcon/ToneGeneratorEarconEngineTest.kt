package com.asistente.celular.voice.earcon

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToneGeneratorEarconEngineTest {

    @Test
    fun testEarconEngineLifecycleAndNoCrashInHeadless() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val engine = ToneGeneratorEarconEngine(scope = testScope)
        assertNotNull(engine)

        // Verifica que reproducir diferentes earcons no lance excepciones
        engine.playEarcon(EarconType.WAKE_WORD_PING)
        engine.playEarcon(EarconType.SUCCESS_CONFIRMATION)
        engine.playEarcon(EarconType.ERROR_ALERT)
        engine.playEarcon(EarconType.DISMISS_PROMPT)
        engine.playEarcon(EarconType.TIMER_ALARM)

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(1200)
        testScheduler.runCurrent()

        engine.stopAlarm()
        testScheduler.runCurrent()

        engine.release()
        assertTrue(true)
    }
}

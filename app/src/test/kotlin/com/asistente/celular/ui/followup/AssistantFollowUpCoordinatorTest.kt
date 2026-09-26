package com.asistente.celular.ui.followup

import com.asistente.celular.voice.earcon.EarconEngine
import com.asistente.celular.voice.earcon.EarconType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantFollowUpCoordinatorTest {

    private class FakeEarconEngine : EarconEngine {
        val playedEarcons = mutableListOf<EarconType>()
        override fun playEarcon(type: EarconType) {
            playedEarcons.add(type)
        }
        override fun stopAlarm() {}
        override fun release() {}
    }

    @Test
    fun testDetectClosingPhrases() {
        val coordinator = AssistantFollowUpCoordinator()

        assertTrue(coordinator.isClosingPhrase("gracias"))
        assertTrue(coordinator.isClosingPhrase("¡Muchas gracias!"))
        assertTrue(coordinator.isClosingPhrase("listo"))
        assertTrue(coordinator.isClosingPhrase("adiós"))
        assertTrue(coordinator.isClosingPhrase("eso es todo"))
        assertTrue(coordinator.isClosingPhrase("nada más"))

        assertFalse(coordinator.isClosingPhrase("enciende la luz"))
        assertFalse(coordinator.isClosingPhrase("pon un temporizador"))
        assertFalse(coordinator.isClosingPhrase("cómo está el clima"))
    }

    @Test
    fun testFollowUpWindowTimeoutDismiss() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEarcon = FakeEarconEngine()

        val coordinator = AssistantFollowUpCoordinator(
            scope = testScope,
            earconEngine = fakeEarcon,
            windowDurationMillis = 6000L
        )

        var listenAgainCalled = false
        var dismissCalled = false

        coordinator.startFollowUpWindow(
            onListenAgain = { listenAgainCalled = true },
            onDismiss = { dismissCalled = true }
        )

        assertTrue(listenAgainCalled)
        assertEquals(FollowUpState.LISTENING, coordinator.state.value)
        assertFalse(dismissCalled)

        // Avanzar el tiempo 6 segundos
        testScope.advanceTimeBy(6050L)
        testScope.advanceUntilIdle()

        assertTrue(dismissCalled)
        assertEquals(FollowUpState.COMPLETED, coordinator.state.value)
        assertTrue(fakeEarcon.playedEarcons.contains(EarconType.DISMISS_PROMPT))
    }

    @Test
    fun testFollowUpCancel() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val coordinator = AssistantFollowUpCoordinator(
            scope = testScope,
            windowDurationMillis = 6000L
        )

        var dismissCalled = false

        coordinator.startFollowUpWindow(
            onListenAgain = {},
            onDismiss = { dismissCalled = true }
        )

        assertEquals(FollowUpState.LISTENING, coordinator.state.value)
        coordinator.cancel()

        assertEquals(FollowUpState.INACTIVE, coordinator.state.value)
        testScope.advanceTimeBy(7000L)
        testScope.advanceUntilIdle()
        assertFalse(dismissCalled)
    }
}

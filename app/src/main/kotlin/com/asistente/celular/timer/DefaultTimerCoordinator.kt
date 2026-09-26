package com.asistente.celular.timer

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.timer.ActiveTimer
import com.asistente.celular.nlu.timer.TimerCoordinator
import com.asistente.celular.nlu.timer.TimerState
import com.asistente.celular.voice.earcon.EarconEngine
import com.asistente.celular.voice.earcon.EarconType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Coordinador nativo de temporizadores en la aplicación Hendrix.
 * Mantiene temporizadores activos con cuenta regresiva en vivo, alarma sonora mediante EarconEngine
 * y soporte para consulta, pausa, reanudación y cancelación.
 */
class DefaultTimerCoordinator(
    private val context: Context? = null,
    private val earconEngine: EarconEngine? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : TimerCoordinator {

    companion object {
        private const val TAG = "DefaultTimerCoord"
    }

    private val _activeTimers = MutableStateFlow<List<ActiveTimer>>(emptyList())
    override val activeTimers: StateFlow<List<ActiveTimer>> = _activeTimers.asStateFlow()

    private var countdownJob: Job? = null

    private fun startTickerLoop() {
        if (countdownJob?.isActive == true) return
        countdownJob = scope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    private fun checkStopTicker() {
        val hasRunning = _activeTimers.value.any { it.state == TimerState.RUNNING }
        if (!hasRunning) {
            countdownJob?.cancel()
            countdownJob = null
        }
    }

    private fun tick() {
        val currentList = _activeTimers.value
        if (currentList.isEmpty()) {
            checkStopTicker()
            return
        }

        var updated = false
        val newList = currentList.map { timer ->
            if (timer.state == TimerState.RUNNING) {
                val newRemaining = timer.remainingSeconds - 1
                if (newRemaining <= 0) {
                    updated = true
                    Log.i(TAG, "⏰ Temporizador finalizado: ${timer.id} (${timer.label})")
                    earconEngine?.playEarcon(EarconType.TIMER_ALARM)
                    timer.copy(
                        remainingSeconds = 0,
                        state = TimerState.COMPLETED,
                        isRinging = true
                    )
                } else {
                    updated = true
                    timer.copy(remainingSeconds = newRemaining)
                }
            } else {
                timer
            }
        }

        if (updated) {
            _activeTimers.value = newList
            checkStopTicker()
        }
    }

    override fun startTimer(durationSeconds: Long, label: String): ActiveTimer {
        val cleanLabel = label.trim().ifEmpty { "Temporizador" }
        val timer = ActiveTimer(
            id = "timer_${UUID.randomUUID().toString().take(8)}",
            label = cleanLabel,
            totalDurationSeconds = durationSeconds,
            startedEpochMillis = System.currentTimeMillis(),
            remainingSeconds = durationSeconds,
            state = TimerState.RUNNING,
            isRinging = false
        )

        synchronized(this) {
            _activeTimers.value = _activeTimers.value + timer
        }
        startTickerLoop()
        return timer
    }

    override fun pauseTimer(timerId: String?): Boolean {
        var found = false
        synchronized(this) {
            val targetId = timerId ?: getPrimaryTimerInternal()?.id ?: return false
            val updated = _activeTimers.value.map {
                if (it.id == targetId && it.state == TimerState.RUNNING) {
                    found = true
                    it.copy(state = TimerState.PAUSED)
                } else it
            }
            if (found) {
                _activeTimers.value = updated
                checkStopTicker()
            }
        }
        return found
    }

    override fun resumeTimer(timerId: String?): Boolean {
        var found = false
        synchronized(this) {
            val targetId = timerId ?: _activeTimers.value.firstOrNull { it.state == TimerState.PAUSED }?.id ?: return false
            val updated = _activeTimers.value.map {
                if (it.id == targetId && it.state == TimerState.PAUSED) {
                    found = true
                    it.copy(state = TimerState.RUNNING)
                } else it
            }
            if (found) {
                _activeTimers.value = updated
                startTickerLoop()
            }
        }
        return found
    }

    override fun cancelTimer(timerId: String?): Boolean {
        var found = false
        synchronized(this) {
            val targetId = timerId ?: getPrimaryTimerInternal()?.id ?: return false
            val timerToCancel = _activeTimers.value.find { it.id == targetId }
            if (timerToCancel != null) {
                found = true
                if (timerToCancel.isRinging) {
                    earconEngine?.stopAlarm()
                }
                _activeTimers.value = _activeTimers.value.filterNot { it.id == targetId }
                checkStopTicker()
            }
        }
        return found
    }

    override fun getPrimaryTimer(): ActiveTimer? {
        val list = _activeTimers.value
        return list.filter { it.state == TimerState.RUNNING || it.state == TimerState.PAUSED }
            .minByOrNull { it.remainingSeconds }
            ?: list.firstOrNull { it.isRinging }
    }

    private fun getPrimaryTimerInternal(): ActiveTimer? {
        val list = _activeTimers.value
        return list.filter { it.state == TimerState.RUNNING || it.state == TimerState.PAUSED }
            .minByOrNull { it.remainingSeconds }
            ?: list.firstOrNull { it.isRinging }
    }

    override fun dismissAlarm(timerId: String?) {
        synchronized(this) {
            earconEngine?.stopAlarm()
            val targetId = timerId ?: _activeTimers.value.firstOrNull { it.isRinging }?.id
            if (targetId != null) {
                _activeTimers.value = _activeTimers.value.filterNot { it.id == targetId }
                checkStopTicker()
            }
        }
    }
}

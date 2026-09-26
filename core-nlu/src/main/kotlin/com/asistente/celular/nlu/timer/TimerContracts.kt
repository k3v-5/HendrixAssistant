package com.asistente.celular.nlu.timer

import kotlinx.coroutines.flow.StateFlow

/**
 * Estados del ciclo de vida de un temporizador en Hendrix.
 */
enum class TimerState {
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * Representa un temporizador activo o en cuenta regresiva.
 */
data class ActiveTimer(
    val id: String,
    val label: String = "",
    val totalDurationSeconds: Long,
    val startedEpochMillis: Long = System.currentTimeMillis(),
    val remainingSeconds: Long,
    val state: TimerState = TimerState.RUNNING,
    val isRinging: Boolean = false
) {
    val formattedRemaining: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            val hours = mins / 60
            val remMins = mins % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, remMins, secs)
            } else {
                String.format("%02d:%02d", remMins, secs)
            }
        }
}

/**
 * Orquestador desacoplado de temporizadores nativos en app.
 */
interface TimerCoordinator {
    val activeTimers: StateFlow<List<ActiveTimer>>

    /**
     * Inicia un nuevo temporizador.
     */
    fun startTimer(durationSeconds: Long, label: String = ""): ActiveTimer

    /**
     * Pausa un temporizador por id o el primario si no se especifica.
     */
    fun pauseTimer(timerId: String? = null): Boolean

    /**
     * Reanuda un temporizador en pausa.
     */
    fun resumeTimer(timerId: String? = null): Boolean

    /**
     * Cancela un temporizador por id o el primario.
     */
    fun cancelTimer(timerId: String? = null): Boolean

    /**
     * Consulta el temporizador activo primario (el más próximo a finalizar).
     */
    fun getPrimaryTimer(): ActiveTimer?

    /**
     * Detiene la alarma sonora de un temporizador que ha finalizado.
     */
    fun dismissAlarm(timerId: String? = null)
}

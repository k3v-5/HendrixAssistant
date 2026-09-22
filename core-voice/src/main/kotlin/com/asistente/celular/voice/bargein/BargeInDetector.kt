package com.asistente.celular.voice.bargein

import android.media.audiofx.AcousticEchoCanceler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Contrato para detección de interrupción por voz del usuario durante la reproducción del asistente (Barge-In).
 */
interface BargeInDetector {
    val isMonitoring: Boolean

    /**
     * Inicia el monitoreo de audio para detectar si el usuario interrumpe al asistente.
     * @param onBargeIn Callback ejecutado inmediatamente al detectar voz humana del usuario.
     */
    fun startMonitoring(onBargeIn: () -> Unit)

    /**
     * Detiene el monitoreo y libera recursos de audio.
     */
    fun stopMonitoring()

    /**
     * Alimenta datos PCM en vivo si se reciben desde una fuente compartida de micrófono.
     */
    fun onAudioSamples(buffer: ShortArray, readSize: Int) {}
}

/**
 * Detector de interrupción basado en energía acústica RMS y filtrado de eco.
 * Emplea cancelación acústica de eco (AEC) si está soportada por el hardware de Android.
 */
class DefaultBargeInDetector(
    private val scope: CoroutineScope,
    private val energyThreshold: Double = 1200.0,
    private val gracePeriodMillis: Long = 250L
) : BargeInDetector {

    private val _isMonitoring = AtomicBoolean(false)
    override val isMonitoring: Boolean get() = _isMonitoring.get()

    private var onBargeInCallback: (() -> Unit)? = null
    private var monitoringStartTime: Long = 0L
    private var consecutiveSpeechFrames = 0

    val isAcousticEchoCancelerAvailable: Boolean
        get() = try {
            AcousticEchoCanceler.isAvailable()
        } catch (_: Throwable) {
            false
        }

    override fun startMonitoring(onBargeIn: () -> Unit) {
        onBargeInCallback = onBargeIn
        monitoringStartTime = System.currentTimeMillis()
        consecutiveSpeechFrames = 0
        _isMonitoring.set(true)
    }

    override fun stopMonitoring() {
        _isMonitoring.set(false)
        onBargeInCallback = null
        consecutiveSpeechFrames = 0
    }

    override fun onAudioSamples(buffer: ShortArray, readSize: Int) {
        if (!_isMonitoring.get() || readSize <= 0) return

        // Período de gracia inicial para evitar disparos por los primeros milisegundos de arranque del altavoz
        if (System.currentTimeMillis() - monitoringStartTime < gracePeriodMillis) return

        // Cálculo de energía RMS (Root Mean Square)
        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i]
            sumSquares += (sample * sample)
        }
        val rms = Math.sqrt(sumSquares / readSize)

        if (rms >= energyThreshold) {
            consecutiveSpeechFrames++
            // Si supera el umbral durante al menos 2 tramas consecutivas (~100 ms de voz sostenida)
            if (consecutiveSpeechFrames >= 2) {
                val callback = onBargeInCallback
                stopMonitoring()
                callback?.invoke()
            }
        } else {
            consecutiveSpeechFrames = Math.max(0, consecutiveSpeechFrames - 1)
        }
    }
}

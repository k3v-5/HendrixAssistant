package com.asistente.celular.voice.earcon

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Motor de Earcons acústicos inmediatos basado en ToneGenerator nativo de Android.
 * Ofrece latencia cero (<10ms) y cero consumo de assets o archivos en disco.
 */
class ToneGeneratorEarconEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val streamType: Int = AudioManager.STREAM_NOTIFICATION,
    private val volume: Int = 85
) : EarconEngine {

    companion object {
        private const val TAG = "ToneGenEarconEngine"
    }

    private var toneGenerator: ToneGenerator? = null
    private val isAlarmRinging = AtomicBoolean(false)
    private var alarmJob: Job? = null

    init {
        try {
            toneGenerator = ToneGenerator(streamType, volume)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo inicializar ToneGenerator nativo (posible entorno de pruebas): ${e.message}")
        }
    }

    override fun playEarcon(type: EarconType) {
        scope.launch {
            try {
                when (type) {
                    EarconType.WAKE_WORD_PING -> {
                        // Tono nítido de activación ascendente
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 75)
                    }
                    EarconType.SUCCESS_CONFIRMATION -> {
                        // Tono suave de confirmación
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 110)
                    }
                    EarconType.ERROR_ALERT -> {
                        // Tono de nack/error
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
                    }
                    EarconType.DISMISS_PROMPT -> {
                        // Tono suave de despedida
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 90)
                    }
                    EarconType.TIMER_ALARM -> {
                        startContinuousAlarm()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reproduciendo earcon $type: ${e.message}")
            }
        }
    }

    private fun startContinuousAlarm() {
        if (isAlarmRinging.getAndSet(true)) return

        alarmJob?.cancel()
        alarmJob = scope.launch {
            try {
                while (isActive && isAlarmRinging.get()) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
                    delay(350)
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
                    delay(800)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción en bucle de alarma acústica: ${e.message}")
            } finally {
                isAlarmRinging.set(false)
            }
        }
    }

    override fun stopAlarm() {
        isAlarmRinging.set(false)
        alarmJob?.cancel()
        alarmJob = null
        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            Log.w(TAG, "Error al detener tono: ${e.message}")
        }
    }

    override fun release() {
        stopAlarm()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando ToneGenerator: ${e.message}")
        }
    }
}

package com.asistente.celular.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.asistente.celular.voice.gesture.GestureEvent
import com.asistente.celular.voice.gesture.HardwareGestureDetector
import com.asistente.celular.voice.gesture.HardwareGestureType
import kotlin.math.abs

/**
 * Detector de gestos físicos de hardware utilizando acelerómetro del dispositivo.
 * Detecta doble toque en la tapa trasera (Quick Tap) y giro boca abajo (Flip to Shush).
 */
class SensorHardwareGestureDetector(
    context: Context
) : HardwareGestureDetector, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var gestureCallback: ((GestureEvent) -> Unit)? = null
    private var isListeningState = false
    private var sensitivity = 1.0f

    // Variables de detección de toque trasero
    private var lastSpikeTime = 0L
    private var spikeCount = 0

    // Variables de detección de volteo boca abajo
    private var wasFaceUp = true
    private var lastFlipTime = 0L

    override fun startListening(onGesture: (GestureEvent) -> Unit) {
        gestureCallback = onGesture
        if (!isListeningState && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListeningState = true
        }
    }

    override fun stopListening() {
        if (isListeningState) {
            sensorManager?.unregisterListener(this)
            isListeningState = false
            gestureCallback = null
        }
    }

    override fun isListening(): Boolean = isListeningState

    override fun setSensitivity(sensitivityMultiplier: Float) {
        sensitivity = sensitivityMultiplier.coerceIn(0.5f, 2.5f)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val z = event.values[2]
        val now = System.currentTimeMillis()

        // 1. Detección de Voltear Boca Abajo (Flip Face Down)
        if (z > 6.0f) {
            wasFaceUp = true
        } else if (z < -7.0f && wasFaceUp && (now - lastFlipTime > 1500L)) {
            wasFaceUp = false
            lastFlipTime = now
            gestureCallback?.invoke(
                GestureEvent(
                    gestureType = HardwareGestureType.FLIP_FACE_DOWN,
                    timestamp = now,
                    confidence = 0.95f
                )
            )
        }

        // 2. Detección de Doble Toque Trasero (Quick Tap Back)
        val zDelta = abs(z - 9.8f)
        val threshold = 7.0f / sensitivity
        if (zDelta > threshold) {
            if (now - lastSpikeTime in 120L..500L) {
                spikeCount++
                if (spikeCount >= 2) {
                    spikeCount = 0
                    lastSpikeTime = 0L
                    gestureCallback?.invoke(
                        GestureEvent(
                            gestureType = HardwareGestureType.QUICK_TAP_BACK,
                            timestamp = now,
                            confidence = 0.88f
                        )
                    )
                }
            } else if (now - lastSpikeTime > 600L) {
                spikeCount = 1
                lastSpikeTime = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

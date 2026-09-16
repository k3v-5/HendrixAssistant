package com.asistente.celular.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Coordinador de sensores de hardware para Hendrix Assistant.
 * Procesa señales de Acelerómetro y Proximidad en tiempo real para:
 * 1. Shake to Wake: Despertar agitando el dispositivo.
 * 2. Pocket Silence: Pausar escucha continua cuando el teléfono está en el bolsillo para ahorrar batería.
 * 3. Flip to Mute: Silenciar voz o alarmas al voltear el teléfono boca abajo.
 */
class HardwareSensorCoordinator(
    private val context: Context,
    var isShakeToWakeEnabled: Boolean = false,
    var isPocketSilenceEnabled: Boolean = true,
    var isFlipToMuteEnabled: Boolean = true,
    private val onShakeDetected: () -> Unit = {},
    private val onFlipToMute: () -> Unit = {},
    private val onPocketStateChanged: (isPocketed: Boolean) -> Unit = {}
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val _isPocketed = MutableStateFlow(false)
    val isPocketed: StateFlow<Boolean> = _isPocketed.asStateFlow()

    // Parámetros para Shake Detection
    private var lastAccelerationMagnitude = 0f
    private var accelerationDelta = 0f
    private var shakeCount = 0
    private var lastShakeTimestamp = 0L
    private var lastShakeTriggerTime = 0L

    // Parámetros para Flip to Mute
    private var isCurrentlyFaceDown = false

    private var isListening = false

    fun startListening() {
        if (isListening || sensorManager == null) return

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        isListening = true
        Log.i(TAG, "HardwareSensorCoordinator iniciado (Shake=$isShakeToWakeEnabled, Pocket=$isPocketSilenceEnabled, Flip=$isFlipToMuteEnabled)")
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
        Log.i(TAG, "HardwareSensorCoordinator detenido")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometer(event.values)
            Sensor.TYPE_PROXIMITY -> processProximity(event.values)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun processAccelerometer(values: FloatArray) {
        if (values.size < 3) return
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val now = System.currentTimeMillis()

        // 1. Detección de Flip to Mute (Boca abajo sobre superficie)
        if (isFlipToMuteEnabled) {
            val isFaceDown = z < -7.5f && sqrt((x * x + y * y).toDouble()) < 4.0
            if (isFaceDown && !isCurrentlyFaceDown) {
                isCurrentlyFaceDown = true
                Log.i(TAG, "Gesto detectado: Volteado boca abajo (Flip to Mute)")
                onFlipToMute()
            } else if (!isFaceDown && isCurrentlyFaceDown) {
                isCurrentlyFaceDown = false
            }
        }

        // 2. Detección de Shake to Wake (Sacudida intencional)
        if (isShakeToWakeEnabled && !_isPocketed.value) {
            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            if (lastAccelerationMagnitude != 0f) {
                val delta = abs(magnitude - lastAccelerationMagnitude)
                accelerationDelta = accelerationDelta * 0.7f + delta

                if (accelerationDelta > SHAKE_THRESHOLD) {
                    if (now - lastShakeTimestamp < 500) {
                        shakeCount++
                        if (shakeCount >= REQUIRED_SHAKES && now - lastShakeTriggerTime > SHAKE_DEBOUNCE_MILLIS) {
                            lastShakeTriggerTime = now
                            shakeCount = 0
                            Log.i(TAG, "Gesto detectado: Sacudida (Shake to Wake)")
                            onShakeDetected()
                        }
                    } else {
                        shakeCount = 1
                    }
                    lastShakeTimestamp = now
                }
            }
            lastAccelerationMagnitude = magnitude
        }
    }

    private fun processProximity(values: FloatArray) {
        if (!isPocketSilenceEnabled || values.isEmpty()) return
        val distance = values[0]
        val maxRange = proximitySensor?.maximumRange ?: 5f
        val pocketDetected = distance < 3.0f || distance < maxRange * 0.5f

        if (_isPocketed.value != pocketDetected) {
            _isPocketed.value = pocketDetected
            Log.i(TAG, "Estado de bolsillo (Pocket Silence) cambiado: en_bolsillo=$pocketDetected (dist=$distance)")
            onPocketStateChanged(pocketDetected)
        }
    }

    companion object {
        private const val TAG = "HardwareSensorCoord"
        private const val SHAKE_THRESHOLD = 11.0f
        private const val REQUIRED_SHAKES = 2
        private const val SHAKE_DEBOUNCE_MILLIS = 2000L
    }
}

package com.asistente.celular.emergency

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.telephony.SmsManager
import android.util.Log
import com.asistente.celular.nlu.emergency.EmergencySosController
import com.asistente.celular.nlu.emergency.EmergencyTriggerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Coordinador central de Emergencia y Alerta SOS Manos Libres.
 * Soporta activación automática por 5 sacudidas bruscas (acelerómetro) o comandos por voz.
 * Ejecuta destellos de linterna en código Morse SOS (... --- ...), sirena sonora y notificación SMS con GPS.
 */
class EmergencySosCoordinator(
    private val context: Context
) : EmergencySosController, SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var emergencyJob: Job? = null
    @Volatile
    private var isEmergencyActive = false

    // Detección de sacudidas (Shake)
    private var shakeCount = 0
    private var lastShakeTimestamp = 0L

    // Contactos de emergencia preconfigurados (persistibles en SharedPreferences)
    var emergencyContacts: List<String> = listOf()

    fun startListeningSensors() {
        if (accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.i(TAG, "Detector de sacudidas para emergencia SOS activo.")
        }
    }

    fun stopListeningSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
        if (gForce > 2.7) { // Sacudida brusca
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp < 600) {
                shakeCount++
                if (shakeCount >= 5 && !isEmergencyActive) {
                    Log.w(TAG, "¡5 sacudidas rápidas detectadas! Disparando SOS de emergencia.")
                    shakeCount = 0
                    scope.launch { triggerEmergencySos() }
                }
            } else {
                shakeCount = 1
            }
            lastShakeTimestamp = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override suspend fun triggerEmergencySos(): EmergencyTriggerResult {
        if (isEmergencyActive) {
            return EmergencyTriggerResult(
                success = true,
                locationUrl = getDeviceLocationUrl(),
                notifiedContacts = emergencyContacts,
                sirenStarted = true,
                flashlightSosStarted = true
            )
        }

        isEmergencyActive = true
        val locationUrl = getDeviceLocationUrl()
        val notified = mutableListOf<String>()

        // 1. Enviar SMS con GPS a contactos
        for (phone in emergencyContacts) {
            val message = "¡ALERTA SOS! Necesito ayuda urgente. Mi ubicación actual es: $locationUrl"
            val sent = sendSms(phone, message)
            if (sent) notified.add(phone)
        }

        // 2. Iniciar bucle de linterna SOS Morse y sirena en corrutina
        emergencyJob?.cancel()
        emergencyJob = scope.launch {
            val cameraId = getTorchCameraId()
            val toneGen = try { ToneGenerator(AudioManager.STREAM_ALARM, 100) } catch (_: Exception) { null }

            try {
                while (isActive && isEmergencyActive) {
                    // Morse SOS: ... --- ...
                    // Tres cortos (.)
                    repeat(3) {
                        setTorch(cameraId, true)
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 180)
                        delay(200)
                        setTorch(cameraId, false)
                        delay(150)
                    }
                    delay(300)

                    // Tres largos (-)
                    repeat(3) {
                        setTorch(cameraId, true)
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
                        delay(600)
                        setTorch(cameraId, false)
                        delay(200)
                    }
                    delay(300)

                    // Tres cortos (.)
                    repeat(3) {
                        setTorch(cameraId, true)
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 180)
                        delay(200)
                        setTorch(cameraId, false)
                        delay(150)
                    }

                    // Pausa entre secuencias de SOS
                    delay(2000)
                }
            } finally {
                setTorch(cameraId, false)
                toneGen?.release()
            }
        }

        return EmergencyTriggerResult(
            success = true,
            locationUrl = locationUrl,
            notifiedContacts = if (notified.isNotEmpty()) notified else emergencyContacts,
            sirenStarted = true,
            flashlightSosStarted = true
        )
    }

    override fun cancelEmergencySos(): Boolean {
        isEmergencyActive = false
        emergencyJob?.cancel()
        emergencyJob = null
        val cameraId = getTorchCameraId()
        setTorch(cameraId, false)
        Log.i(TAG, "Emergencia SOS cancelada.")
        return true
    }

    override fun isEmergencyActive(): Boolean = isEmergencyActive

    private fun getDeviceLocationUrl(): String {
        return try {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val loc: Location? = try {
                locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: SecurityException) {
                null
            }

            if (loc != null) {
                "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
            } else {
                "https://maps.google.com/?q=0,0"
            }
        } catch (e: Exception) {
            "https://maps.google.com"
        }
    }

    private fun sendSms(phoneNumber: String, text: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, text, null, null)
            Log.i(TAG, "SMS de emergencia enviado con éxito a $phoneNumber")
            true
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo enviar SMS a $phoneNumber (falta permiso o SIM ausente)")
            false
        }
    }

    private fun getTorchCameraId(): String? {
        return try {
            val mgr = cameraManager ?: return null
            mgr.cameraIdList.firstOrNull { id ->
                val chars = mgr.getCameraCharacteristics(id)
                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                flash && facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setTorch(cameraId: String?, enable: Boolean) {
        if (cameraId == null) return
        try {
            cameraManager?.setTorchMode(cameraId, enable)
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "EmergencyCoordinator"
    }
}

package com.asistente.celular.rhythm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import com.asistente.celular.nlu.rhythm.SleepWakeController
import com.asistente.celular.skills.smarthome.YeelightLanDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.SmartHomeRepository

/**
 * Coordinador proactivo de ritmo circadiano y hábitos de sueño y despertar.
 * Detecta conexión de cargador nocturno (> 10 PM) y apagado de alarma matutina
 * para orquestar automáticamente el ambiente del hogar, no molestar y briefings.
 */
class SleepWakeCoordinator(
    private val context: Context,
    private val smartHomeRepository: SmartHomeRepository? = null
) : SleepWakeController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isReceiverRegistered = false

    private val powerAndAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    // Si se conecta a cargar entre 10:00 PM y 4:00 AM -> sugerir o activar rutina de dormir
                    if (hour >= 22 || hour < 4) {
                        Log.i(TAG, "Cargador nocturno conectado a las $hour hrs. Activando buenas noches.")
                        scope.launch { executeGoodNightRoutine() }
                    }
                }
                "android.intent.action.ALARM_DISMISSED",
                "com.google.android.deskclock.ALARM_DISMISSED" -> {
                    // Si se descarta la alarma entre 5:00 AM y 11:00 AM -> activar buenos días
                    if (hour in 5..11) {
                        Log.i(TAG, "Alarma matutina apagada a las $hour hrs. Activando buenos días.")
                        scope.launch { executeGoodMorningRoutine() }
                    }
                }
            }
        }
    }

    fun startListening() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction("android.intent.action.ALARM_DISMISSED")
            addAction("com.google.android.deskclock.ALARM_DISMISSED")
        }
        try {
            context.registerReceiver(powerAndAlarmReceiver, filter)
            isReceiverRegistered = true
            Log.i(TAG, "SleepWakeCoordinator monitoreando eventos de carga y alarma.")
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando receiver para SleepWakeCoordinator", e)
        }
    }

    fun stopListening() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(powerAndAlarmReceiver)
            isReceiverRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Error desregistrando receiver de SleepWakeCoordinator", e)
        }
    }

    override suspend fun executeGoodNightRoutine(): String {
        Log.i(TAG, "Ejecutando rutina de Buenas Noches...")
        // 1. Apagar o atenuar foco inteligente Xiaomi / Yeelight
        try {
            smartHomeRepository?.executeAction(null, DeviceAction.TurnOff)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo comunicar con foco inteligente: ${e.message}")
        }

        // 2. Silenciar notificaciones / volumen multimedia
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Ajuste de volumen nocturno restringido por permisos")
        }

        return "Foco apagado y modo silencioso nocturno activado. Que descanses."
    }

    override suspend fun executeGoodMorningRoutine(): String {
        Log.i(TAG, "Ejecutando rutina de Buenos Días...")
        // 1. Encender foco inteligente con luz cálida al 80%
        try {
            smartHomeRepository?.executeAction(null, DeviceAction.TurnOn)
            smartHomeRepository?.executeAction(null, DeviceAction.SetBrightness(80))
            smartHomeRepository?.executeAction(null, DeviceAction.SetColorTemperature(3500))
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo comunicar con foco inteligente al despertar: ${e.message}")
        }

        // 2. Restablecer volumen de notificaciones
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        try {
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION) ?: 10
            audioManager?.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxVol / 2, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Restablecimiento de volumen matutino restringido por permisos")
        }

        return "Foco encendido en tono cálido para despertar. Dispositivo listo para el día."
    }

    companion object {
        private const val TAG = "SleepWakeCoordinator"
    }
}

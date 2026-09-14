package com.asistente.celular

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Clase principal de la aplicación.
 * Configura canales de notificación para el servicio de voz en segundo plano.
 */
class AsistenteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                VOICE_SERVICE_CHANNEL_ID,
                "Servicio de Escucha del Asistente",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activo el detector de activación ('Oye Asistente') en segundo plano"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val VOICE_SERVICE_CHANNEL_ID = "voice_service_channel"
    }
}

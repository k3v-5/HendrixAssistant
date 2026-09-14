package com.asistente.celular.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.asistente.celular.AsistenteApplication
import com.asistente.celular.MainActivity
import com.asistente.celular.R
import com.asistente.celular.util.HapticFeedbackManager
import com.asistente.celular.util.MicCoordinator
import com.asistente.celular.voice.kws.AndroidContinuousWakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano para mantener activa la escucha de palabra clave (Wake Word)
 * en segundo plano incluso si la app no está en pantalla.
 */
class AssistantVoiceService : Service() {

    private var wakeWordEngine: AndroidContinuousWakeWordEngine? = null
    private lateinit var hapticManager: HapticFeedbackManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        hapticManager = HapticFeedbackManager(this)
        wakeWordEngine = AndroidContinuousWakeWordEngine(this)

        // Observar si una pantalla interactiva (MainActivity o AssistantDialogActivity)
        // toma el control del micrófono para pausar la escucha de fondo y evitar colisiones de audio.
        serviceScope.launch {
            MicCoordinator.isMicLockedByUi.collect { isLocked ->
                if (isLocked) {
                    wakeWordEngine?.pause()
                } else {
                    wakeWordEngine?.resume()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        wakeWordEngine?.startListening { keyword ->
            // Feedback táctil inmediato: el usuario sabe que fue escuchado al instante
            hapticManager.vibrateStartListening()

            // Lanzar el panel flotante sobre la app actual
            val dialogIntent = Intent(this, com.asistente.celular.ui.AssistantDialogActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            try {
                startActivity(dialogIntent)
            } catch (e: Exception) {
                // Si el sistema restringe el inicio directo de actividades desde segundo plano (Android 10+)
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    this,
                    2,
                    dialogIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val alertNotification = NotificationCompat.Builder(this, AsistenteApplication.VOICE_SERVICE_CHANNEL_ID)
                    .setContentTitle("Oye Hendrix detectado")
                    .setContentText("Toca para responder")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setAutoCancel(true)
                    .build()
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.notify(WAKE_NOTIFICATION_ID, alertNotification)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        wakeWordEngine?.stopListening()
        wakeWordEngine?.release()
        wakeWordEngine = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dialogIntent = Intent(this, com.asistente.celular.ui.AssistantDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val dialogPendingIntent = PendingIntent.getActivity(
            this,
            1,
            dialogIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AsistenteApplication.VOICE_SERVICE_CHANNEL_ID)
            .setContentTitle("Asistente Hendrix Activo")
            .setContentText("Escuchando palabra de activación (\"Oye Hendrix\")")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "🎙️ Hablar ahora", dialogPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val WAKE_NOTIFICATION_ID = 1002
        const val EXTRA_TRIGGERED_BY_WAKE_WORD = "extra_triggered_by_wake_word"
    }
}

package com.asistente.celular.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.asistente.celular.AsistenteApplication
import com.asistente.celular.MainActivity
import com.asistente.celular.R
import com.asistente.celular.data.SettingsRepository
import com.asistente.celular.di.AssistantSkillFactory
import com.asistente.celular.nlu.evaluator.SkillEvaluator
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.ui.AssistantDialogActivity
import com.asistente.celular.util.HapticFeedbackManager
import com.asistente.celular.util.MicCoordinator
import com.asistente.celular.voice.kws.AndroidContinuousWakeWordEngine
import com.asistente.celular.voice.tts.AndroidNativeTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano para mantener activa la escucha de palabra clave (Wake Word)
 * en segundo plano incluso si la app no está en pantalla.
 */
class AssistantVoiceService : Service() {

    private var wakeWordEngine: AndroidContinuousWakeWordEngine? = null
    private lateinit var hapticManager: HapticFeedbackManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ttsEngine: AndroidNativeTtsEngine? = null
    private var evaluator: SkillEvaluator? = null

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

        wakeWordEngine?.startListeningWithEvent { event ->
            Log.i(TAG, "Evento de voz detectado en servicio: keyword='${event.keyword}', command='${event.command}'")
            // Feedback táctil inmediato: el usuario sabe que fue escuchado al instante
            hapticManager.vibrateStartListening()

            val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else {
                true
            }

            val dialogIntent = Intent(this, AssistantDialogActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AssistantDialogActivity.EXTRA_INITIAL_COMMAND, event.command)
                putExtra(EXTRA_TRIGGERED_BY_WAKE_WORD, true)
            }

            if (canDrawOverlays) {
                try {
                    startActivity(dialogIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Fallo al iniciar AssistantDialogActivity desde segundo plano: ${e.message}")
                    handleBackgroundFallback(event, dialogIntent)
                }
            } else {
                handleBackgroundFallback(event, dialogIntent)
            }
        }

        return START_STICKY
    }

    private fun handleBackgroundFallback(event: com.asistente.celular.voice.WakeWordEvent, dialogIntent: Intent) {
        val command = event.command
        if (!command.isNullOrBlank()) {
            // El usuario pronunció el comando completo ("Hendrix prende mi foco").
            // Lo ejecutamos directamente en segundo plano sin requerir ventana visual.
            Log.i(TAG, "Ejecutando comando one-shot directamente en segundo plano: '$command'")
            executeCommandInBackground(command)
        } else {
            // Solo se detectó la palabra de activación ("Hendrix"). Notificar al usuario para que toque e interactúe.
            showWakeNotification(dialogIntent)
        }
    }

    private fun executeCommandInBackground(command: String) {
        serviceScope.launch {
            wakeWordEngine?.pause()
            try {
                val eval = getOrCreateEvaluator()
                val output = eval.processInput(command)
                if (output.success) {
                    hapticManager.vibrateSuccess()
                } else {
                    hapticManager.vibrateError()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ejecutando comando en segundo plano: ${e.message}", e)
                hapticManager.vibrateError()
            } finally {
                // Esperar a que el motor TTS termine la locución antes de reanudar el micrófono
                val tts = ttsEngine
                if (tts != null) {
                    while (tts.isSpeaking) {
                        delay(150)
                    }
                }
                delay(600)
                wakeWordEngine?.resume()
            }
        }
    }

    private fun getOrCreateEvaluator(): SkillEvaluator {
        evaluator?.let { return it }

        val tts = ttsEngine ?: AndroidNativeTtsEngine(this).also { ttsEngine = it }
        val factory = AssistantSkillFactory(this, serviceScope)
        val settingsRepo = SettingsRepository(this)
        val activeLlmConfig = settingsRepo.loadLlmConfig()
        var lastOutput: SkillOutput? = null

        val skillContext = object : SkillContext {
            override val androidContext: Context get() = this@AssistantVoiceService
            override val isConnectedToInternet: Boolean get() = checkInternet()
            override val previousOutput: SkillOutput? get() = lastOutput
        }

        val eval = factory.createSkillEvaluator(
            skillContext = skillContext,
            localModelManager = com.asistente.celular.ai.local.DefaultLocalModelManager(this),
            localInferenceEngine = com.asistente.celular.ai.local.LlamaCppInferenceEngine(),
            configProvider = { activeLlmConfig },
            onSpeak = { text ->
                tts.speak(text)
            }
        )
        evaluator = eval
        return eval
    }

    private fun checkInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showWakeNotification(dialogIntent: Intent) {
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

    override fun onDestroy() {
        serviceScope.cancel()
        wakeWordEngine?.stopListening()
        wakeWordEngine?.release()
        wakeWordEngine = null
        ttsEngine?.release()
        ttsEngine = null
        evaluator = null
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
        private const val TAG = "AssistantVoiceService"
        const val NOTIFICATION_ID = 1001
        const val WAKE_NOTIFICATION_ID = 1002
        const val EXTRA_TRIGGERED_BY_WAKE_WORD = "extra_triggered_by_wake_word"
    }
}

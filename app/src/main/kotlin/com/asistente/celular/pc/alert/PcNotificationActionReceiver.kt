package com.asistente.celular.pc.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.asistente.celular.MainActivity
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.lang.ref.WeakReference

/**
 * Receptor de difusión de Android que intercepta los clics en los botones de acción
 * de las notificaciones proactivas de la PC, ejecutando comandos de inmediato de forma asíncrona segura.
 */
class PcNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PcNotificationActionReceiver"
        private var bridgeRef: WeakReference<PcWorkspaceBridge>? = null

        var activeBridge: PcWorkspaceBridge?
            get() = bridgeRef?.get()
            set(value) {
                bridgeRef = if (value != null) WeakReference(value) else null
            }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PcProactiveAlertNotificationHelper.ACTION_ALERT_BUTTON) return

        val actionId = intent.getStringExtra(PcProactiveAlertNotificationHelper.EXTRA_ACTION_ID) ?: return
        val alertId = intent.getStringExtra(PcProactiveAlertNotificationHelper.EXTRA_ALERT_ID) ?: ""
        Log.i(TAG, "Acción de notificación pulsada: $actionId para alerta $alertId")

        // Descartar notificación al pulsar cualquier botón
        PcProactiveAlertNotificationHelper.dismissNotification(context)

        val bridge = activeBridge
        val pendingResult = goAsync()

        when (actionId) {
            "suspend_pc" -> {
                Toast.makeText(context, "💤 Solicitando suspender PC...", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bridge?.executeQuickCommand("sleep")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error enviando sleep: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "pause_process" -> {
                Toast.makeText(context, "⏸️ Solicitando pausar proceso...", Toast.LENGTH_SHORT).show()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bridge?.executeQuickCommand("media_play_pause")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error enviando media_play_pause: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "view_copilot" -> {
                try {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("open_screen", "pc_screen_copilot")
                    }
                    context.startActivity(openIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error abriendo MainActivity copilot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
            else -> {
                Log.d(TAG, "Notificación descartada o acción no reconocida: $actionId")
                pendingResult.finish()
            }
        }
    }
}

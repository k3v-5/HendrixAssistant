package com.asistente.celular.pc.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.asistente.celular.MainActivity
import com.asistente.celular.nlu.pc.alert.PcAlertCategory
import com.asistente.celular.nlu.pc.alert.PcProactiveAlert

/**
 * Gestor de notificaciones interactivas proactivas de alta prioridad para eventos de la PC
 * (Sobrecalentamiento térmico, Renders concluidos, Fallos de compilación).
 */
object PcProactiveAlertNotificationHelper {

    const val CHANNEL_ID = "hendrix_pc_alerts"
    private const val CHANNEL_NAME = "Alertas Proactivas de PC"
    private const val CHANNEL_DESC = "Notificaciones interactivas para renders, salud de hardware y estado de la PC"
    const val NOTIFICATION_ID = 8900

    const val ACTION_ALERT_BUTTON = "com.asistente.celular.ACTION_PC_ALERT_BUTTON"
    const val EXTRA_ACTION_ID = "extra_action_id"
    const val EXTRA_ALERT_ID = "extra_alert_id"
    const val EXTRA_CATEGORY = "extra_category"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showAlertNotification(context: Context, alert: PcProactiveAlert) {
        createNotificationChannel(context)

        // Intent principal al tocar el cuerpo de la notificación (abre la app)
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingAppIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = when (alert.category) {
            PcAlertCategory.GPU_OVERHEAT -> "⚠️"
            PcAlertCategory.RENDER_COMPLETED -> "🎨"
            PcAlertCategory.RENDER_CRASHED -> "💥"
            PcAlertCategory.BUILD_FAILED -> "❌"
            PcAlertCategory.SECURITY -> "🔒"
            PcAlertCategory.OTHER -> "🔔"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("$emoji ${alert.title}")
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingAppIntent)

        // Agregar botones de acción rápida
        alert.actions.take(3).forEachIndexed { index, alertAction ->
            val actionIntent = Intent(context, PcNotificationActionReceiver::class.java).apply {
                action = ACTION_ALERT_BUTTON
                putExtra(EXTRA_ACTION_ID, alertAction.id)
                putExtra(EXTRA_ALERT_ID, alert.alertId)
                putExtra(EXTRA_CATEGORY, alert.category.name)
            }
            val pendingActionIntent = PendingIntent.getBroadcast(
                context,
                (alert.alertId.hashCode() + index),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, alertAction.label, pendingActionIntent)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }
}

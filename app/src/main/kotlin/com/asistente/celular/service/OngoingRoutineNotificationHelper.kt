package com.asistente.celular.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.asistente.celular.MainActivity
import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import com.asistente.celular.ui.components.findOngoingRoutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor de notificaciones de Android para la rutina o actividad en curso.
 * Proporciona una notificación discreta y contextual en la barra de estado cuando el usuario
 * tiene una rutina activa según su horario personal.
 *
 * Al pulsar la notificación, abre directamente MainActivity enfocando la sección de HORARIO
 * y la rutina específica correspondiente mediante extras en el Intent.
 */
object OngoingRoutineNotificationHelper {

    const val CHANNEL_ID = "ongoing_routine_channel"
    const val CHANNEL_NAME = "Rutina en curso"
    const val CHANNEL_DESC = "Notificación con la actividad o bloque de horario activo en este momento"
    const val NOTIFICATION_ID = 9050

    const val EXTRA_OPEN_TAB = "OPEN_TAB"
    const val EXTRA_ROUTINE_ID = "ROUTINE_ID"
    const val TAB_SCHEDULE = "SCHEDULE"

    /**
     * Crea o actualiza el canal de notificaciones en Android 8.0+ (API 26+).
     * Usa IMPORTANCE_LOW para no emitir sonidos ni vibraciones invasivas en cada actualización,
     * manteniéndose como un estado en vivo en la barra de notificaciones.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(false)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Construye la notificación para la rutina activa.
     */
    fun buildNotification(context: Context, activity: ScheduleActivityItem): Notification {
        createNotificationChannel(context)

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TAB, TAB_SCHEDULE)
            putExtra(EXTRA_ROUTINE_ID, activity.id)
        }

        val pendingAppIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parsedColor = runCatching {
            Color.parseColor(activity.colorHex)
        }.getOrDefault(Color.parseColor("#00F0FF"))

        val contentTitle = "Rutina en curso: ${activity.title}"
        val contentText = "${activity.startTime} - ${activity.endTime} • ${activity.category}"

        val bigText = buildString {
            append("Actividad activa: ${activity.title}\n")
            append("Horario: ${activity.startTime} - ${activity.endTime}\n")
            append("Categoría: ${activity.category}")
            if (activity.location.isNotBlank()) {
                append("\nUbicación: ${activity.location}")
            }
            if (activity.description.isNotBlank()) {
                append("\nDescripción: ${activity.description}")
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setColor(parsedColor)
            .setColorized(false)
            .setContentIntent(pendingAppIntent)
            .addAction(android.R.drawable.ic_menu_view, "Ver Rutina", pendingAppIntent)
            .build()
    }

    /**
     * Muestra o actualiza la notificación de la rutina en curso.
     */
    fun showOngoingRoutineNotification(context: Context, activity: ScheduleActivityItem) {
        val notification = buildNotification(context, activity)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancela y remueve la notificación de rutina en curso.
     */
    fun cancelOngoingRoutineNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }

    /**
     * Evalúa la rutina activa en un día y hora dados de manera pura y desacoplada.
     */
    fun evaluateOngoingRoutine(
        activities: List<ScheduleActivityItem>,
        currentDay: ScheduleDay = ScheduleDay.currentDay(),
        currentTimeStr: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    ): ScheduleActivityItem? {
        return findOngoingRoutine(activities, currentDay, currentTimeStr)
    }

    /**
     * Evalúa las actividades provistas contra el día y hora actual.
     * Si hay una rutina en curso, publica o actualiza la notificación y la retorna.
     * Si no hay rutina en curso, cancela la notificación y retorna null.
     */
    fun updateNotification(
        context: Context,
        activities: List<ScheduleActivityItem>,
        currentDay: ScheduleDay = ScheduleDay.currentDay(),
        currentTimeStr: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    ): ScheduleActivityItem? {
        val ongoing = evaluateOngoingRoutine(activities, currentDay, currentTimeStr)
        if (ongoing != null) {
            showOngoingRoutineNotification(context, ongoing)
        } else {
            cancelOngoingRoutineNotification(context)
        }
        return ongoing
    }
}

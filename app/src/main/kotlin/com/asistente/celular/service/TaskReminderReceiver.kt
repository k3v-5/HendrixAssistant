package com.asistente.celular.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.asistente.celular.MainActivity
import com.asistente.celular.data.JsonTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receptor de difusiones para disparar recordatorios de tareas y manejar
 * acciones directas e interactivas desde la barra de notificaciones ("Completar" y "Posponer 15 min").
 */
class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        when (action) {
            ACTION_TRIGGER_REMINDER -> {
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Recordatorio de Tarea"
                val description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""
                showReminderNotification(context, taskId, title, description)
            }
            ACTION_COMPLETE_TASK -> {
                completeTaskAsync(context, taskId)
            }
            ACTION_SNOOZE_TASK -> {
                snoozeTaskAsync(context, taskId)
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        taskId: String,
        title: String,
        description: String
    ) {
        createNotificationChannel(context)

        // Intento principal al tocar la notificación (abre la app)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "TASKS")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción: Completar tarea directamente
        val completeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción: Posponer 15 minutos
        val snoozeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ $title")
            .setContentText(if (description.isNotBlank()) description else "Recordatorio de tarea de Hendrix")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Completar", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Posponer 15 min", snoozePendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(taskId.hashCode(), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("TaskReminderReceiver", "Permiso POST_NOTIFICATIONS no concedido: ${e.message}")
        }
    }

    private fun completeTaskAsync(context: Context, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = TaskReminderScheduler(context)
                val repository = JsonTaskRepository(context, scheduler)
                repository.toggleTaskCompletion(taskId)

                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(taskId.hashCode())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun snoozeTaskAsync(context: Context, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = TaskReminderScheduler(context)
                val repository = JsonTaskRepository(context, scheduler)
                val task = repository.getTaskById(taskId)
                if (task != null) {
                    val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000 // 15 minutos
                    repository.updateTask(task.copy(reminderMillis = snoozeTime, isCompleted = false))
                }
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(taskId.hashCode())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de vencimiento y alertas de tareas"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "channel_task_reminders"
        const val ACTION_TRIGGER_REMINDER = "com.asistente.celular.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.asistente.celular.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.asistente.celular.ACTION_SNOOZE_TASK"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    }
}

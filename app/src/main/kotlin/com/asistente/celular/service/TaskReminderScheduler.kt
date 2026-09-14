package com.asistente.celular.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.tasks.TaskScheduler

/**
 * Implementación de TaskScheduler utilizando Android AlarmManager con alarmas exactas
 * ('setExactAndAllowWhileIdle') para sonar puntualmente incluso en modo Doze.
 */
class TaskReminderScheduler(private val context: Context) : TaskScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun scheduleReminder(task: TaskItem) {
        val reminderTime = task.reminderMillis ?: return
        if (task.isCompleted || reminderTime <= System.currentTimeMillis()) return
        if (alarmManager == null) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(TaskReminderReceiver.EXTRA_TASK_DESCRIPTION, task.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            android.util.Log.e("TaskReminderScheduler", "Permiso de alarma exacta no concedido: ${e.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    override fun cancelReminder(taskId: String) {
        if (alarmManager == null) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

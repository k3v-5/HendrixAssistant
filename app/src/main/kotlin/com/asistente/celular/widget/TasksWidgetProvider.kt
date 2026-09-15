package com.asistente.celular.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.asistente.celular.MainActivity
import com.asistente.celular.R

/**
 * Widget de pantalla de inicio para visualizar y gestionar tareas pendientes de Hendrix.
 */
class TasksWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TASKS_CHANGED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TasksWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_tasks_list)
        }
    }

    companion object {
        const val ACTION_TASKS_CHANGED = "com.asistente.celular.widget.ACTION_TASKS_CHANGED"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val serviceIntent = Intent(context, TasksWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_tasks).apply {
                setRemoteAdapter(R.id.widget_tasks_list, serviceIntent)
                setEmptyView(R.id.widget_tasks_list, R.id.widget_tasks_empty)

                // Botón "+" para añadir tarea abre la app en la pestaña de tareas
                val addIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("OPEN_TAB", "TASKS")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val addPendingIntent = PendingIntent.getActivity(
                    context,
                    101,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.btn_widget_add_task, addPendingIntent)

                // Template de clic para los ítems de la lista
                val itemIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("OPEN_TAB", "TASKS")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val itemPendingIntent = PendingIntent.getActivity(
                    context,
                    102,
                    itemIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                setPendingIntentTemplate(R.id.widget_tasks_list, itemPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_tasks_list)
        }

        fun notifyDataChanged(context: Context) {
            val intent = Intent(context, TasksWidgetProvider::class.java).apply {
                action = ACTION_TASKS_CHANGED
            }
            context.sendBroadcast(intent)
        }
    }
}

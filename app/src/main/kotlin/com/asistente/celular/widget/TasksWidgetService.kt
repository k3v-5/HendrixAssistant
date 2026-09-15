package com.asistente.celular.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.asistente.celular.R
import com.asistente.celular.nlu.tasks.TaskItem
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Servicio de vistas remotas para poblar la lista del TasksWidget.
 */
class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TasksRemoteViewsFactory(applicationContext)
    }
}

private class TasksRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val json = Json { ignoreUnknownKeys = true }
    private val tasksFile = File(context.filesDir, "tasks.json")
    private val items = mutableListOf<TaskItem>()
    private val formatter = DateTimeFormatter.ofPattern("d/MM HH:mm", Locale("es", "ES"))

    override fun onCreate() {
        loadTasks()
    }

    override fun onDataSetChanged() {
        loadTasks()
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null
        val task = items[position]

        val views = RemoteViews(context.packageName, R.layout.widget_task_item)
        views.setTextViewText(R.id.widget_task_title, task.title)

        val now = System.currentTimeMillis()
        val dueStr = task.dueDateMillis?.let { due ->
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(due), ZoneId.systemDefault())
            if (due < now) "⚠️ Vencida: ${dt.format(formatter)}"
            else "🕒 Vence: ${dt.format(formatter)}"
        } ?: ""

        views.setTextViewText(R.id.widget_task_due, dueStr)
        views.setViewVisibility(R.id.widget_task_due, if (dueStr.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE)

        // FillInIntent para interactuar al hacer clic
        val fillInIntent = Intent().apply {
            putExtra("OPEN_TAB", "TASKS")
            putExtra("TASK_ID", task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_task_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadTasks() {
        items.clear()
        if (tasksFile.exists()) {
            try {
                val content = tasksFile.readText()
                val list = json.decodeFromString<List<TaskItem>>(content)
                val pending = list.filter { !it.isCompleted }
                    .sortedBy { it.dueDateMillis ?: Long.MAX_VALUE }
                items.addAll(pending)
            } catch (_: Exception) {}
        }
    }
}

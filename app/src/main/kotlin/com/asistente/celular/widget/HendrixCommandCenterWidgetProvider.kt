package com.asistente.celular.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.asistente.celular.R
import com.asistente.celular.emergency.EmergencySosCoordinator
import com.asistente.celular.skills.smarthome.YeelightLanDriver
import com.asistente.celular.ui.AssistantDialogActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Proveedor de widget de escritorio interactivo Material You "Hendrix Command Center".
 * Permite control rápido de voz, foco inteligente, notas y SOS sin abrir la app completa.
 */
class HendrixCommandCenterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE_BULB -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = com.asistente.celular.data.JsonSmartHomeRepository(context, this)
                        repo.executeAction(null, com.asistente.celular.nlu.smarthome.DeviceAction.Toggle)
                    } catch (_: Exception) {}
                }
                Toast.makeText(context, "💡 Foco inteligente alternado", Toast.LENGTH_SHORT).show()
            }
            ACTION_TRIGGER_SOS -> {
                val coordinator = EmergencySosCoordinator(context)
                CoroutineScope(Dispatchers.IO).launch {
                    coordinator.triggerEmergencySos()
                }
                Toast.makeText(context, "🚨 ¡ALERTA SOS ACTIVADA!", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_BULB = "com.asistente.celular.widget.ACTION_TOGGLE_BULB"
        const val ACTION_TRIGGER_SOS = "com.asistente.celular.widget.ACTION_TRIGGER_SOS"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // 1. Micrófono Hendrix
            val micIntent = Intent(context, AssistantDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val micPendingIntent = PendingIntent.getActivity(
                context, 101, micIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2. Foco inteligente
            val bulbIntent = Intent(context, HendrixCommandCenterWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_BULB
            }
            val bulbPendingIntent = PendingIntent.getBroadcast(
                context, 102, bulbIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 3. Nota rápida
            val noteIntent = Intent(context, AssistantDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("auto_command", "toma una nota")
            }
            val notePendingIntent = PendingIntent.getActivity(
                context, 103, noteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 4. SOS
            val sosIntent = Intent(context, HendrixCommandCenterWidgetProvider::class.java).apply {
                action = ACTION_TRIGGER_SOS
            }
            val sosPendingIntent = PendingIntent.getBroadcast(
                context, 104, sosIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_hendrix_command_center).apply {
                setOnClickPendingIntent(R.id.widget_btn_mic, micPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_bulb, bulbPendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_note, notePendingIntent)
                setOnClickPendingIntent(R.id.widget_btn_sos, sosPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

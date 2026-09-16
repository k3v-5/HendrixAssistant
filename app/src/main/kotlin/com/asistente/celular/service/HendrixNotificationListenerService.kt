package com.asistente.celular.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.asistente.celular.nlu.ui.WhatsAppQuickReplyPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio de escucha de notificaciones para WhatsApp.
 * Captura mensajes entrantes de WhatsApp y permite enviar respuestas directas
 * mediante la API de [RemoteInput] sin necesidad de abrir la aplicación.
 */
class HendrixNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "HendrixNotificationListenerService conectado con éxito.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg != "com.whatsapp") return

        val extras = sbn.notification.extras ?: return
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        if (sender.isBlank() || message.isBlank()) return

        // Buscar acción con RemoteInput para respuesta directa
        val actions = sbn.notification.actions ?: return
        var replyAction: Notification.Action? = null

        for (action in actions) {
            val remoteInputs = action.remoteInputs
            if (!remoteInputs.isNullOrEmpty()) {
                replyAction = action
                break
            }
        }

        if (replyAction != null) {
            activeReplyActions[sbn.key] = replyAction
        }

        // Generar 3 sugerencias contextuales rápidas
        val suggestions = generateSmartReplies(message)
        val payload = WhatsAppQuickReplyPayload(
            senderName = sender,
            messageSnippet = message,
            suggestedReplies = suggestions,
            notificationKey = sbn.key
        )

        _lastWhatsAppNotification.value = payload
        Log.i(TAG, "Notificación WhatsApp de '$sender': \"$message\" con ${suggestions.size} sugerencias.")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let { activeReplyActions.remove(it.key) }
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HendrixNotifListener"
        @Volatile
        var instance: HendrixNotificationListenerService? = null

        private val activeReplyActions = mutableMapOf<String, Notification.Action>()

        private val _lastWhatsAppNotification = MutableStateFlow<WhatsAppQuickReplyPayload?>(null)
        val lastWhatsAppNotification: StateFlow<WhatsAppQuickReplyPayload?> = _lastWhatsAppNotification.asStateFlow()

        fun isEnabled(): Boolean = instance != null

        fun requestPermissionIntent(): Intent {
            return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        /**
         * Envía una respuesta de texto directa al chat de WhatsApp usando el RemoteInput de la notificación.
         */
        fun sendReply(context: Context, notificationKey: String, replyText: String): Boolean {
            val action = activeReplyActions[notificationKey] ?: return false
            val remoteInputs = action.remoteInputs ?: return false
            val remoteInput = remoteInputs.firstOrNull() ?: return false

            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyText)
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

            return try {
                action.actionIntent.send(context, 0, intent)
                Log.i(TAG, "Respuesta rápida enviada a WhatsApp: \"$replyText\"")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando respuesta rápida a WhatsApp", e)
                false
            }
        }

        /**
         * Generador heurístico rápido de respuestas inteligentes para WhatsApp (Ghostwriter).
         */
        fun generateSmartReplies(incomingMessage: String): List<String> {
            val lower = incomingMessage.lowercase().trim()
            return when {
                lower.contains("dónde estás") || lower.contains("donde estas") || lower.contains("ya vienes") -> {
                    listOf("Ya voy en camino", "Estoy en casa", "Llego en 15 minutos")
                }
                lower.contains("hola") || lower.contains("buenos días") || lower.contains("buenas tardes") -> {
                    listOf("¡Hola! ¿Cómo estás?", "Hola, ¿qué tal todo?", "Buenas, dime")
                }
                lower.contains("gracias") -> {
                    listOf("¡Con gusto!", "De nada, para servirte", "A la orden")
                }
                lower.contains("puedes") || lower.contains("podrías") || lower.contains("te parece") -> {
                    listOf("¡Claro que sí!", "Ahora no puedo, te aviso en un rato", "Me parece perfecto")
                }
                lower.endsWith("?") -> {
                    listOf("Sí, claro", "No, no creo", "Déjame revisarlo y te aviso")
                }
                else -> {
                    listOf("Entendido 👍", "¡Perfecto, muchas gracias!", "Hablamos más al rato")
                }
            }
        }
    }
}

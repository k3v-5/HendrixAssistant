package com.asistente.celular.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.asistente.celular.MainActivity
import com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile

/**
 * Gestor de notificaciones del sistema Android para entregables de Hendrix Studio
 * (Renders de Blender, canciones de Ableton/FL Studio, videos de Premiere).
 */
object DropzoneNotificationHelper {

    const val CHANNEL_ID = "hendrix_studio_deliverables"
    private const val CHANNEL_NAME = "Entregables de Hendrix Studio"
    private const val CHANNEL_DESC = "Notificaciones de renders 3D, exportaciones de audio y video completadas en la PC"
    private const val NOTIFICATION_ID_BASE = 8800

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

    fun notifyDeliverable(context: Context, file: PcDropzoneFile) {
        createNotificationChannel(context)

        val (title, iconEmoji) = when {
            file.category.contains("blender", ignoreCase = true) -> "🎨 Render de Blender Terminado" to "🧊"
            file.category.contains("audio", ignoreCase = true) -> "🎵 Canción Exportada a Drive" to "🎹"
            file.category.contains("video", ignoreCase = true) -> "🎬 Video Renderizado en Adobe" to "🎥"
            else -> "📁 Nuevo Entregable en Drive" to "☁️"
        }

        val sizeFormatted = formatFileSize(file.sizeBytes)
        val contentText = "$iconEmoji ${file.fileName} ($sizeFormatted) - Ya disponible en tu nube"

        // Intent para abrir MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingAppIntent = PendingIntent.getActivity(
            context,
            file.fileName.hashCode(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para abrir Google Drive
        val driveIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://drive.google.com")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingDriveIntent = PendingIntent.getActivity(
            context,
            (file.fileName + "_drive").hashCode(),
            driveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingAppIntent)
            .addAction(android.R.drawable.ic_menu_view, "Abrir Drive", pendingDriveIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notifId = NOTIFICATION_ID_BASE + (file.fileName.hashCode() % 100)
        manager?.notify(notifId, notification)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.0f KB", bytes.toDouble() / 1024)
            bytes > 0 -> "$bytes B"
            else -> "0 B"
        }
    }
}

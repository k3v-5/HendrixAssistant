package com.asistente.celular.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.asistente.celular.R
import com.asistente.celular.data.SettingsRepository
import com.asistente.celular.ui.AssistantDialogActivity

/**
 * Servicio de Quick Settings Tile (Mosaico de Ajustes Rápidos) de Android.
 * Permite invocar a Hendrix Assistant directamente desde el panel de notificaciones superior
 * o desde la pantalla de bloqueo de Android con un solo toque sin abrir la aplicación completa.
 */
class HendrixQuickSettingsTileService : TileService() {

    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        Log.i(TAG, "Quick Settings Tile pulsado por el usuario")

        val dialogIntent = Intent(this, AssistantDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AssistantVoiceService.EXTRA_TRIGGERED_BY_WAKE_WORD, true)
        }

        if (isLocked) {
            unlockAndRun {
                launchDialog(dialogIntent)
            }
        } else {
            launchDialog(dialogIntent)
        }
    }

    private fun launchDialog(dialogIntent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    dialogIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(dialogIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar AssistantDialogActivity desde Quick Settings: ${e.message}")
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(dialogIntent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isWakeActive = settingsRepo.isWakeWordActive

        tile.icon = Icon.createWithResource(this, R.drawable.ic_quick_tile_assistant)
        tile.label = "Hendrix Asistente"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isWakeActive) "Escucha activa" else "Toca para hablar"
        }

        tile.state = if (isWakeActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    companion object {
        private const val TAG = "HendrixTileService"
    }
}

package com.asistente.celular.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Gestor de retroalimentación háptica (vibraciones táctiles de confirmación).
 */
class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Vibración corta y nítida al iniciar la escucha ("tap" de activación).
     */
    fun vibrateStartListening() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(45)
        }
    }

    /**
     * Doble pulsación suave de confirmación al completar una acción con éxito.
     */
    fun vibrateSuccess() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 35, 60, 40)
            val amplitudes = intArrayOf(0, 180, 0, 220)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 35, 60, 40), -1)
        }
    }

    /**
     * Vibración en caso de error o cuando no se entiende la frase.
     */
    fun vibrateError() {
        if (vibrator?.hasVibrator() != true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 80, 50, 80)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 80, 50, 80), -1)
        }
    }
}

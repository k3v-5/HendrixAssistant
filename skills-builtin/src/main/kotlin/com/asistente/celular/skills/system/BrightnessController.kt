package com.asistente.celular.skills.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlin.math.roundToInt

/**
 * Contrato desacoplado para el control de brillo de la pantalla del dispositivo (OCP).
 * Permite mockear el control de brillo en tests y aislar la interacción con Settings.System.
 */
interface BrightnessController {
    /**
     * Establece el brillo en un porcentaje entre 1 y 100%.
     * @return true si se aplicó con éxito, false si no se tiene permiso de escritura.
     */
    fun setBrightness(percent: Int): Boolean

    /**
     * Obtiene el brillo actual en porcentaje (1-100%).
     */
    fun getBrightness(): Int

    /**
     * Ajusta el brillo sumando o restando un delta porcentual (ej. +20 o -20).
     */
    fun adjustBrightness(deltaPercent: Int): Int

    /**
     * Verifica si la app cuenta con permiso especial Settings.System.canWrite.
     */
    fun hasWritePermission(): Boolean

    /**
     * Retorna el Intent para abrir la pantalla de concesión del permiso WRITE_SETTINGS.
     */
    fun requestWritePermissionIntent(): Intent
}

/**
 * Implementación nativa para Android de BrightnessController.
 * Lee y escribe en Settings.System.SCREEN_BRIGHTNESS (escala 0-255) y gestiona
 * el cambio al modo manual cuando se ajusta un valor explícito.
 */
class AndroidBrightnessController(
    private val context: Context
) : BrightnessController {

    override fun hasWritePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    override fun requestWritePermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    override fun getBrightness(): Int {
        return try {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            val percent = (raw.toFloat() / 255f * 100f).roundToInt()
            percent.coerceIn(1, 100)
        } catch (_: Exception) {
            50
        }
    }

    override fun setBrightness(percent: Int): Boolean {
        if (!hasWritePermission()) return false

        val clamped = percent.coerceIn(1, 100)
        val rawValue = ((clamped.toFloat() / 100f) * 255f).roundToInt().coerceIn(1, 255)

        return try {
            // 1. Asegurar modo manual para que el ajuste no sea sobreescrito por auto-brillo
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )

            // 2. Establecer el valor de brillo
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                rawValue
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun adjustBrightness(deltaPercent: Int): Int {
        val current = getBrightness()
        val target = (current + deltaPercent).coerceIn(1, 100)
        setBrightness(target)
        return target
    }
}

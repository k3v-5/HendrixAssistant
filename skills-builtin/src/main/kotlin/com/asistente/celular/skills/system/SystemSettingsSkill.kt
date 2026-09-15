package com.asistente.celular.skills.system

import android.content.Intent
import android.provider.Settings
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad offline para acceder a los ajustes del sistema (Wi-Fi, Bluetooth, Sonido, Pantalla).
 */
class SystemSettingsSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "system_settings_skill",
        name = "Ajustes del Sistema",
        description = "Abre rápidamente los paneles de configuración de Wi-Fi, Bluetooth, Sonido y Pantalla."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // "abre los ajustes de [wifi/bluetooth/sonido/pantalla]", "configurar wifi"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "abre", "abreme", "abrir",
                    "ve", "vamos", "ir",
                    "muestra", "muestrame", "mostrar",
                    "configura", "configurar"
                )
            ),
            OptionalConstruct(WordConstruct("a", "en", "los", "las", "el", "la")),
            OptionalConstruct(WordConstruct("ajustes", "configuracion", "opciones", "panel")),
            OptionalConstruct(WordConstruct("de", "del")),
            WordConstruct("wifi", "wi-fi", "bluetooth", "pantalla", "sonido", "bateria", "red", "ajustes", "configuracion")
        ),
        // "modo no molestar"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("activa", "activar", "pon", "poner", "abre", "abrir")),
            OptionalConstruct(WordConstruct("el")),
            OptionalConstruct(WordConstruct("modo")),
            WordConstruct("no"),
            WordConstruct("molestar")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase().trim()

        val (action, name) = when {
            lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("red") -> {
                Settings.ACTION_WIFI_SETTINGS to "Wi-Fi"
            }
            lower.contains("bluetooth") -> {
                Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth"
            }
            lower.contains("no molestar") -> {
                Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS to "No Molestar"
            }
            lower.contains("sonido") || lower.contains("audio") -> {
                Settings.ACTION_SOUND_SETTINGS to "Sonido"
            }
            lower.contains("pantalla") || lower.contains("brillo") -> {
                Settings.ACTION_DISPLAY_SETTINGS to "Pantalla"
            }
            lower.contains("bateria") -> {
                Settings.ACTION_BATTERY_SAVER_SETTINGS to "Batería"
            }
            else -> {
                Settings.ACTION_SETTINGS to "Ajustes del Sistema"
            }
        }

        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)
            val msg = "Abriendo ajustes de $name."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } catch (e: Exception) {
            // Fallback a los ajustes principales si la pantalla específica no está soportada
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(fallbackIntent)
                val msg = "Abriendo los ajustes del dispositivo."
                SkillOutput(speech = msg, displayText = msg, success = true)
            } catch (ex: Exception) {
                val err = "No se pudieron abrir los ajustes: ${ex.message}"
                SkillOutput(speech = err, displayText = err, success = false)
            }
        }
    }
}

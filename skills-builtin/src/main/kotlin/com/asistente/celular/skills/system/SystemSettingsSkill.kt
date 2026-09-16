package com.asistente.celular.skills.system

import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.parser.SpanishNumberParser
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.BrightnessUiPayload

/**
 * Habilidad offline para control de brillo de pantalla y acceso directo
 * a paneles de conectividad (Wi-Fi, Bluetooth, Punto de acceso / Hotspot, Modo Avión).
 */
class SystemSettingsSkill(
    private val brightnessController: BrightnessController? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "system_settings_skill",
        name = "Ajustes y Conectividad",
        description = "Controla el brillo de pantalla y abre rápidamente los paneles de Wi-Fi, Bluetooth, Hotspot y Sonido."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Control de brillo por porcentaje o nivel: "pon el brillo al 50", "brillo al maximo"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("pon", "ponme", "ajusta", "ajustame", "sube", "baja", "cambia")),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("brillo"),
            OptionalConstruct(WordConstruct("de", "la", "pantalla")),
            OptionalConstruct(WordConstruct("al", "a", "en")),
            CapturingConstruct("brightness_level")
        ),
        // 2. Comandos relativos de brillo: "sube el brillo", "baja el brillo"
        SequenceConstruct(
            WordConstruct("sube", "subir", "aumenta", "baja", "bajar", "disminuye"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("brillo"),
            OptionalConstruct(WordConstruct("de", "la", "pantalla"))
        ),
        // 3. Paneles de conectividad y ajustes: "abrir wifi", "configurar bluetooth", "ajustes de sonido"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("abrir", "abre", "mostrar", "muestra", "ir", "ve", "configurar", "configura", "activar", "activa", "pon", "conectar")),
            OptionalConstruct(WordConstruct("a", "los", "las", "el", "la", "mi")),
            OptionalConstruct(WordConstruct("ajustes", "configuracion", "opciones", "panel")),
            OptionalConstruct(WordConstruct("de", "del")),
            WordConstruct("wifi", "wi-fi", "bluetooth", "sonido", "audio", "pantalla", "bateria", "red", "notificaciones", "sistema", "hotspot", "punto de acceso", "zona wifi", "compartir internet")
        ),
        // 4. Modos especiales de sistema: "modo avion", "modo no molestar", "modo silencio"
        SequenceConstruct(
            WordConstruct("modo"),
            WordConstruct("avion", "avión", "no molestar", "silencio")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        // 1. Detección directa de brillo
        if (lower.contains("brillo")) {
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "brightness")
            )
        }

        // 2. Detección directa de Hotspot / Punto de acceso
        if (lower.contains("hotspot") || lower.contains("punto de acceso") || lower.contains("zona wifi") || lower.contains("compartir internet")) {
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "hotspot")
            )
        }

        // 3. Detección directa de Modo Avión
        if (lower.contains("modo avion") || lower.contains("modo avión")) {
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "airplane")
            )
        }

        // 4. Detección directa de Modo No Molestar
        if (lower.contains("no molestar")) {
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "dnd")
            )
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase().trim()
        val controller = brightnessController ?: AndroidBrightnessController(context.androidContext)

        // 1. Manejar brillo de pantalla
        if (score.capturedSlots["action"] == "brightness" || lower.contains("brillo")) {
            return handleBrightness(context, lower, controller)
        }

        // 2. Manejar Hotspot / Zona Wi-Fi
        if (score.capturedSlots["action"] == "hotspot" || lower.contains("hotspot") || lower.contains("punto de acceso") || lower.contains("compartir internet") || lower.contains("zona wifi")) {
            return openSettingsIntent(
                context,
                Settings.ACTION_WIRELESS_SETTINGS,
                "Punto de acceso (Zona Wi-Fi)"
            )
        }

        // 3. Manejar Modo Avión
        if (score.capturedSlots["action"] == "airplane" || lower.contains("avion") || lower.contains("avión")) {
            return openSettingsIntent(
                context,
                Settings.ACTION_AIRPLANE_MODE_SETTINGS,
                "Modo Avión"
            )
        }

        // 4. Manejar Modo No Molestar
        if (score.capturedSlots["action"] == "dnd" || lower.contains("no molestar")) {
            return openSettingsIntent(
                context,
                Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
                "No Molestar"
            )
        }

        // 5. Manejar Sonido
        if (lower.contains("sonido") || lower.contains("audio")) {
            return openSettingsIntent(
                context,
                Settings.ACTION_SOUND_SETTINGS,
                "Sonido"
            )
        }

        // 6. Manejar Batería
        if (lower.contains("bateria") || lower.contains("batería")) {
            return openSettingsIntent(
                context,
                Settings.ACTION_BATTERY_SAVER_SETTINGS,
                "Batería"
            )
        }

        // 7. Manejar Wi-Fi (Panel flotante en Android 10+)
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("red")) {
            val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Panel.ACTION_WIFI
            } else {
                Settings.ACTION_WIFI_SETTINGS
            }
            return openSettingsIntent(context, action, "Wi-Fi")
        }

        // 8. Manejar Bluetooth
        if (lower.contains("bluetooth")) {
            return openSettingsIntent(context, Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth")
        }

        // 9. Pantalla general
        if (lower.contains("pantalla")) {
            return openSettingsIntent(context, Settings.ACTION_DISPLAY_SETTINGS, "Pantalla")
        }

        // Fallback
        return openSettingsIntent(context, Settings.ACTION_SETTINGS, "Ajustes del Sistema")
    }

    private fun handleBrightness(context: SkillContext, lower: String, controller: BrightnessController): SkillOutput {
        if (!controller.hasWritePermission()) {
            val intent = controller.requestWritePermissionIntent()
            try {
                context.androidContext.startActivity(intent)
            } catch (_: Exception) {}
            val speech = "Necesito permiso para ajustar el brillo de la pantalla. Por favor actívalo en la pantalla de ajustes."
            val display = "⚙️ **Permiso requerido:** Habilita el permiso de Modificar Ajustes del Sistema para controlar el brillo."
            val payload = BrightnessUiPayload(percent = controller.getBrightness(), hasPermission = false)
            return SkillOutput(speech = speech, displayText = display, success = false, payload = payload)
        }

        val current = controller.getBrightness()
        val numberInInput = Regex("\\b(\\d{1,3})\\b").find(lower)?.groupValues?.get(1)?.toIntOrNull()

        // Nivel explícito: "brillo al 50", "brillo al maximo", "brillo al minimo"
        val targetPercent = when {
            lower.contains("maximo") || lower.contains("tope") || lower.contains("todo") || lower.contains("cien") -> 100
            lower.contains("minimo") -> 5
            lower.contains("mitad") || lower.contains("medio") -> 50
            lower.contains("sube") || lower.contains("aumenta") || lower.contains("mas") -> (current + 20).coerceIn(1, 100)
            lower.contains("baja") || lower.contains("disminuye") || lower.contains("menos") -> (current - 20).coerceIn(1, 100)
            numberInInput != null -> numberInInput.coerceIn(1, 100)
            else -> {
                val clean = lower.replace("brillo", "").replace("pantalla", "").replace("pon", "").replace("el", "").replace("al", "").replace("a", "").replace("%", "").trim()
                SpanishNumberParser.parseNumber(clean)
            }
        }

        return if (targetPercent != null) {
            val success = controller.setBrightness(targetPercent)
            if (success) {
                val msg = "Brillo de pantalla ajustado al $targetPercent%."
                val payload = BrightnessUiPayload(percent = targetPercent, hasPermission = true)
                SkillOutput(speech = msg, displayText = "☀️ $msg", success = true, payload = payload)
            } else {
                val msg = "No se pudo cambiar el brillo de la pantalla."
                SkillOutput(speech = msg, displayText = "⚠️ $msg", success = false)
            }
        } else {
            val msg = "El brillo actual de la pantalla está al $current%."
            val payload = BrightnessUiPayload(percent = current, hasPermission = true)
            SkillOutput(speech = msg, displayText = "☀️ $msg", success = true, payload = payload)
        }
    }

    private fun openSettingsIntent(context: SkillContext, action: String, name: String): SkillOutput {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)
            val msg = "Abriendo panel de $name."
            SkillOutput(speech = msg, displayText = "⚙️ $msg", success = true)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(fallbackIntent)
                val msg = "Abriendo ajustes del dispositivo."
                SkillOutput(speech = msg, displayText = "⚙️ $msg", success = true)
            } catch (ex: Exception) {
                val err = "No se pudieron abrir los ajustes: ${ex.message}"
                SkillOutput(speech = err, displayText = "⚠️ $err", success = false)
            }
        }
    }
}

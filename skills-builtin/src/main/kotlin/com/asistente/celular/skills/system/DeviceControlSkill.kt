package com.asistente.celular.skills.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Habilidad para diagnóstico y control de estado del dispositivo:
 * batería, almacenamiento, perfiles de audio (silencio/vibración) y modo No Molestar.
 */
class DeviceControlSkill : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "device_control_skill",
        name = "Diagnóstico y Ajustes del Dispositivo",
        description = "Consulta la batería y espacio libre, o controla el modo No Molestar y perfiles de sonido."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val format1Dec = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale("es", "MX")))

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.isBlank()) return SkillScore.NO_MATCH

        // 1. Batería
        if (lower.contains("bateria") || lower.contains("batería") || lower.contains("cargando")) {
            if (lower.contains("cuanta") || lower.contains("cuánta") || lower.contains("nivel") ||
                lower.contains("porcentaje") || lower.contains("queda") || lower.contains("tengo") || lower.contains("esta") || lower.contains("está")) {
                return SkillScore(
                    confidence = 0.95f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf("action" to "battery")
                )
            }
        }

        // 2. Almacenamiento / Memoria
        if (lower.contains("espacio") || lower.contains("almacenamiento") || lower.contains("memoria libre") || lower.contains("memoria disponible")) {
            if (lower.contains("libre") || lower.contains("disponible") || lower.contains("cuanto") || lower.contains("cuánto") || lower.contains("queda")) {
                return SkillScore(
                    confidence = 0.94f,
                    specificity = Specificity.HIGH,
                    capturedSlots = mapOf("action" to "storage")
                )
            }
        }

        // 3. No Molestar
        if (lower.contains("no molestar")) {
            val isDisable = lower.contains("desactiva") || lower.contains("quita") || lower.contains("apaga") || lower.contains("desactivar")
            return SkillScore(
                confidence = 0.96f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf(
                    "action" to "dnd",
                    "enable" to (!isDisable).toString()
                )
            )
        }

        // 4. Perfiles de sonido (Silencio / Vibración / Normal)
        if (lower.contains("silencio") || lower.contains("silenciar") || lower.contains("modo silencio")) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "ringer", "mode" to "silent")
            )
        }
        if (lower.contains("vibracion") || lower.contains("vibración") || lower.contains("vibrar") || lower.contains("modo vibrar")) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "ringer", "mode" to "vibrate")
            )
        }
        if (lower.contains("sonido normal") || lower.contains("modo normal") || lower.contains("activa el sonido") || lower.contains("quita el silencio")) {
            return SkillScore(
                confidence = 0.95f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "ringer", "mode" to "normal")
            )
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        return when (score.capturedSlots["action"]) {
            "battery" -> handleBattery(context)
            "storage" -> handleStorage(context)
            "dnd" -> {
                val enable = score.capturedSlots["enable"]?.toBooleanStrictOrNull() ?: true
                handleDnd(context, enable)
            }
            "ringer" -> {
                val mode = score.capturedSlots["mode"] ?: "silent"
                handleRingerMode(context, mode)
            }
            else -> {
                val msg = "No pude determinar la acción del dispositivo."
                SkillOutput(speech = msg, displayText = msg, success = false)
            }
        }
    }

    private fun handleBattery(context: SkillContext): SkillOutput {
        val appContext = context.androidContext.applicationContext
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = appContext.registerReceiver(null, intentFilter)

        if (batteryStatus == null) {
            val msg = "No se pudo obtener la información de la batería."
            return SkillOutput(speech = msg, displayText = msg, success = false)
        }

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "por cargador de pared"
            BatteryManager.BATTERY_PLUGGED_USB -> "por puerto USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "de forma inalámbrica"
            else -> ""
        }

        val speech = buildString {
            append("Tienes $pct% de batería.")
            if (isFull) {
                append(" La batería está completamente cargada.")
            } else if (isCharging) {
                append(" El teléfono se está cargando $plugType.")
            }
        }.trim()

        val display = buildString {
            append("🔋 **Nivel de Batería:** $pct%\n")
            append("⚡ **Estado:** ")
            if (isFull) append("Cargada (100%)")
            else if (isCharging) append("Cargando $plugType")
            else append("En descarga")
        }

        val payload = com.asistente.celular.nlu.ui.BatteryUiPayload(
            percent = pct,
            isCharging = isCharging,
            chargeSource = plugType.ifBlank { null }
        )

        return SkillOutput(speech = speech, displayText = display, success = true, payload = payload)
    }

    private fun handleStorage(context: SkillContext): SkillOutput {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val availableBytes = stat.availableBytes
            val totalBytes = stat.totalBytes

            val gigabyte = 1024.0 * 1024.0 * 1024.0
            val freeGb = availableBytes / gigabyte
            val totalGb = totalBytes / gigabyte
            val usedGb = totalGb - freeGb

            val speech = "Tienes ${format1Dec.format(freeGb)} gigabytes libres de ${format1Dec.format(totalGb)} gigabytes."
            val display = """
                💾 **Almacenamiento Interno:**
                
                • Espacio libre: **${format1Dec.format(freeGb)} GB**
                • Espacio ocupado: **${format1Dec.format(usedGb)} GB**
                • Total: **${format1Dec.format(totalGb)} GB**
            """.trimIndent()

            SkillOutput(speech = speech, displayText = display, success = true)
        } catch (e: Exception) {
            val err = "No se pudo leer el almacenamiento del teléfono: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }

    private fun handleDnd(context: SkillContext, enable: Boolean): SkillOutput {
        val nm = context.androidContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm == null) {
            val err = "Servicio de notificaciones no disponible."
            return SkillOutput(speech = err, displayText = err, success = false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!nm.isNotificationPolicyAccessGranted) {
                // Solicitar permiso de acceso a políticas de no molestar
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(intent)
                val msg = "Para controlar el modo No Molestar directamente, concede el permiso a Hendrix en los ajustes que acabo de abrir."
                return SkillOutput(speech = msg, displayText = "⚠️ Permiso requerido: Acceso a No Molestar", success = false)
            }

            return if (enable) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                val msg = "Modo No Molestar activado."
                SkillOutput(speech = msg, displayText = "🌙 **No Molestar:** Activado", success = true)
            } else {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                val msg = "Modo No Molestar desactivado."
                SkillOutput(speech = msg, displayText = "🔔 **No Molestar:** Desactivado", success = true)
            }
        }

        val msg = "Modo No Molestar no soportado en esta versión de Android."
        return SkillOutput(speech = msg, displayText = msg, success = false)
    }

    private fun handleRingerMode(context: SkillContext, mode: String): SkillOutput {
        val audioManager = context.androidContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            val err = "Servicio de audio no disponible."
            return SkillOutput(speech = err, displayText = err, success = false)
        }

        val nm = context.androidContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm != null && !nm.isNotificationPolicyAccessGranted && mode == "silent") {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)
            val msg = "Para silenciar completamente el teléfono, concede el permiso de No Molestar a Hendrix."
            return SkillOutput(speech = msg, displayText = "⚠️ Permiso de No Molestar requerido", success = false)
        }

        return when (mode) {
            "silent" -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                val msg = "Teléfono en silencio."
                SkillOutput(speech = msg, displayText = "🔇 **Sonido:** Modo Silencio", success = true)
            }
            "vibrate" -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                val msg = "Teléfono en vibración."
                SkillOutput(speech = msg, displayText = "📳 **Sonido:** Modo Vibración", success = true)
            }
            else -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                val msg = "Sonido normal restablecido."
                SkillOutput(speech = msg, displayText = "🔔 **Sonido:** Modo Normal", success = true)
            }
        }
    }
}

package com.asistente.celular.skills.smarthome

import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.parser.SpanishNumberParser
import com.asistente.celular.nlu.skill.Skill
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.ui.SmartBulbUiPayload
import com.asistente.celular.nlu.smarthome.SmartHomeRepository

/**
 * Habilidad de Domótica y Hogar Inteligente para controlar focos, luces y dispositivos Xiaomi / Yeelight por voz.
 * Soporta encendido/apagado, brillo por %, colores RGB, temperaturas Kelvin y efectos dinámicos (Vela, Fiesta, Luz de Noche).
 * 100% Offline-first mediante protocolo local en la red WiFi.
 */
class SmartHomeSkill(
    private val smartHomeRepository: SmartHomeRepository
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "smart_home_skill",
        name = "Hogar Inteligente / Foco Xiaomi",
        description = "Controla focos, luces y dispositivos inteligentes Xiaomi/Yeelight en la red local por voz con soporte de colores y efectos."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val turnOnPatterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("se", "me", "por", "favor")),
            WordConstruct("prende", "prender", "enciende", "encender", "activa", "activar", "prendeme", "enciendeme", "pon", "poner"),
            OptionalConstruct(WordConstruct("el", "la", "los", "las", "un", "una", "mi", "mis", "tu", "este", "esta")),
            WordConstruct("foco", "luz", "luces", "lampara", "bombilla", "foco inteligente"),
            OptionalConstruct(CapturingConstruct("target_device"))
        ),
        SequenceConstruct(
            WordConstruct("luz", "foco", "luces"),
            WordConstruct("on", "encendido", "prendido")
        )
    )

    private val turnOffPatterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("se", "me", "por", "favor")),
            WordConstruct("apaga", "apagar", "desactiva", "desactivar", "apagame", "apagarme", "quita", "quitar"),
            OptionalConstruct(WordConstruct("el", "la", "los", "las", "un", "una", "mi", "mis", "tu", "este", "esta")),
            WordConstruct("foco", "luz", "luces", "lampara", "bombilla", "foco inteligente"),
            OptionalConstruct(CapturingConstruct("target_device"))
        ),
        SequenceConstruct(
            WordConstruct("luz", "foco", "luces"),
            WordConstruct("off", "apagado", "apagar")
        )
    )

    private val brightnessPatterns: List<Construct> = listOf(
        SequenceConstruct(
            OptionalConstruct(WordConstruct("pon", "sube", "baja", "ajusta", "cambia", "coloca")),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("brillo"),
            OptionalConstruct(WordConstruct("de", "del")),
            OptionalConstruct(WordConstruct("foco", "luz", "lampara")),
            OptionalConstruct(WordConstruct("a", "al", "en")),
            CapturingConstruct("brightness_value")
        ),
        SequenceConstruct(
            WordConstruct("pon", "cambia", "ajusta", "sube", "baja"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("foco", "luz", "lampara"),
            WordConstruct("a", "al", "en"),
            CapturingConstruct("brightness_value")
        )
    )

    private val colorPatterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("pon", "cambia", "cambiar", "coloca"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("foco", "luz", "lampara"),
            OptionalConstruct(WordConstruct("a", "al", "en")),
            OptionalConstruct(WordConstruct("color", "de")),
            CapturingConstruct("color_name")
        ),
        SequenceConstruct(
            WordConstruct("luz", "foco", "lampara"),
            OptionalConstruct(WordConstruct("de", "a", "en", "color")),
            OptionalConstruct(WordConstruct("color")),
            CapturingConstruct("color_name")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        // 1. Efectos Dinámicos (Detener efecto primero, luego Vela, Fiesta, Luz de Noche)
        val isStopEffect = normalized.contains("deten") || normalized.contains("detener") ||
                normalized.contains("para el efecto") || normalized.contains("parar el efecto") ||
                normalized.contains("para las luces") || normalized.contains("para la luz")
        val isCandle = normalized.contains("vela")
        val isParty = normalized.contains("fiesta") || normalized.contains("discoteca")
        val isNight = normalized.contains("luz de noche") || normalized.contains("modo noche") || normalized.contains("luz de luna")

        if (isStopEffect || isCandle || isParty || isNight) {
            val effectType = when {
                isStopEffect -> "stop_flow"
                isCandle -> "candle"
                isParty -> "party"
                else -> "night"
            }
            return SkillScore(
                confidence = 0.97f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "effect", "effect_type" to effectType)
            )
        }

        // 2. Patrones de Brillo
        for (pattern in brightnessPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val bVal = ctx.capturedSlots["brightness_value"] ?: ""
                val percent = parsePercent(bVal)
                if (percent != null) {
                    val slots = ctx.capturedSlots.toMutableMap()
                    slots["action"] = "brightness"
                    slots["parsed_percent"] = percent.toString()
                    return SkillScore(
                        confidence = 0.96f,
                        matchedWords = ctx.tokenIndex,
                        totalWords = ctx.tokens.size,
                        specificity = specificity,
                        capturedSlots = slots
                    )
                }
            }
        }

        // 3. Patrones de Color
        for (pattern in colorPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val colorVal = ctx.capturedSlots["color_name"]?.lowercase() ?: ""
                val knownColor = parseColorRgb(colorVal)
                val isKnownTemp = colorVal in listOf("calida", "calido", "fria", "frio", "blanco", "blanca", "natural", "solar")
                if (knownColor != null || isKnownTemp) {
                    val slots = ctx.capturedSlots.toMutableMap()
                    slots["action"] = "color"
                    return SkillScore(
                        confidence = 0.95f,
                        matchedWords = ctx.tokenIndex,
                        totalWords = ctx.tokens.size,
                        specificity = specificity,
                        capturedSlots = slots
                    )
                }
            }
        }

        // 4. Patrones de Encender
        for (pattern in turnOnPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val slots = ctx.capturedSlots.toMutableMap()
                slots["action"] = "turn_on"
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = slots
                )
            }
        }

        // 5. Patrones de Apagar
        for (pattern in turnOffPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val slots = ctx.capturedSlots.toMutableMap()
                slots["action"] = "turn_off"
                return SkillScore(
                    confidence = 0.95f,
                    matchedWords = ctx.tokenIndex,
                    totalWords = ctx.tokens.size,
                    specificity = specificity,
                    capturedSlots = slots
                )
            }
        }

        // 6. Palabras clave de control de foco
        if ((normalized.contains("prende") || normalized.contains("enciende")) &&
            (normalized.contains("foco") || normalized.contains("luz"))) {
            return SkillScore(
                confidence = 0.92f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "turn_on")
            )
        }

        if ((normalized.contains("apaga") || normalized.contains("apagar")) &&
            (normalized.contains("foco") || normalized.contains("luz"))) {
            return SkillScore(
                confidence = 0.92f,
                specificity = Specificity.HIGH,
                capturedSlots = mapOf("action" to "turn_off")
            )
        }

        return SkillScore.NO_MATCH
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val actionType = score.capturedSlots["action"] ?: "turn_on"
        val targetDevice = score.capturedSlots["target_device"]?.trim()

        // Auto-descubrimiento automático si no hay dispositivos registrados
        if (smartHomeRepository.devices.value.isEmpty()) {
            val discovered = smartHomeRepository.discoverDevices()
            if (discovered.isEmpty()) {
                val errorMsg = "No encontré ningún foco Xiaomi en tu red WiFi. Asegúrate de tener activada la opción 'Control en LAN' (en la app Yeelight o Xiaomi Home) o agrega la IP del foco en los Ajustes de Hendrix."
                return SkillOutput(speech = errorMsg, displayText = errorMsg, success = false)
            }
        }

        val activeDevice = smartHomeRepository.findDeviceByName(targetDevice)
        val devName = activeDevice?.name ?: "Foco Xiaomi"
        val devIp = activeDevice?.ipAddress

        return when (actionType) {
            "turn_on" -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.TurnOn)
                val speech = if (result.success) "Encendí el foco." else result.message
                val display = if (result.success) "💡 **$devName:** Encendido" else "⚠️ ${result.message}"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = true,
                    brightness = activeDevice?.brightness ?: 100,
                    ipAddress = devIp
                )
                SkillOutput(speech = speech, displayText = display, success = result.success, payload = payload)
            }
            "turn_off" -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.TurnOff)
                val speech = if (result.success) "Apagué el foco." else result.message
                val display = if (result.success) "🌑 **$devName:** Apagado" else "⚠️ ${result.message}"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = false,
                    brightness = activeDevice?.brightness ?: 100,
                    ipAddress = devIp
                )
                SkillOutput(speech = speech, displayText = display, success = result.success, payload = payload)
            }
            "brightness" -> {
                val rawVal = score.capturedSlots["brightness_value"] ?: "50"
                val percent = score.capturedSlots["parsed_percent"]?.toIntOrNull() ?: parsePercent(rawVal) ?: 50
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetBrightness(percent))
                val speech = if (result.success) "Ajusté el brillo al $percent por ciento." else result.message
                val display = if (result.success) "🔆 **Brillo:** $percent%" else "⚠️ ${result.message}"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = true,
                    brightness = percent,
                    ipAddress = devIp
                )
                SkillOutput(speech = speech, displayText = display, success = result.success, payload = payload)
            }
            "color" -> {
                val colorVal = score.capturedSlots["color_name"] ?: "blanco"
                handleColorChange(targetDevice, devName, devIp, colorVal)
            }
            "effect" -> {
                val effectType = score.capturedSlots["effect_type"] ?: "candle"
                handleEffect(targetDevice, devName, devIp, effectType)
            }
            else -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.Toggle)
                SkillOutput(speech = result.message, displayText = result.message, success = result.success)
            }
        }
    }

    private suspend fun handleEffect(targetDevice: String?, devName: String, devIp: String?, effectType: String): SkillOutput {
        return when (effectType) {
            "candle" -> {
                // Modo Vela: Flujo continuo con parpadeo entre 2400K y 2800K a intensidades variables
                val candleFlow = "800,2,2700,50, 1000,2,2600,75, 600,2,2800,45, 1200,2,2700,70"
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.StartColorFlow(0, 0, candleFlow))
                val msg = if (result.success) "Activé el modo vela en el foco." else result.message
                val display = "🕯️ **Modo Vela:** Parpadeo cálido activo"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = true,
                    brightness = 65,
                    colorTemp = 2700,
                    activeMode = "candle",
                    ipAddress = devIp
                )
                SkillOutput(speech = msg, displayText = display, success = result.success, payload = payload)
            }
            "party" -> {
                // Modo Fiesta: Ciclo fluido por colores vivos RGB
                val partyFlow = "1000,1,16711680,100, 1000,1,65280,100, 1000,1,255,100, 1000,1,16711935,100, 1000,1,16776960,100"
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.StartColorFlow(0, 0, partyFlow))
                val msg = if (result.success) "Activé el modo fiesta en las luces." else result.message
                val display = "🎉 **Modo Fiesta:** Ciclo multicolor dinámico"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = true,
                    brightness = 100,
                    activeMode = "party",
                    ipAddress = devIp
                )
                SkillOutput(speech = msg, displayText = display, success = result.success, payload = payload)
            }
            "night" -> {
                // Luz de Noche: 2200K cálido al 1% de brillo
                smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColorTemperature(2200))
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetBrightness(1))
                val msg = if (result.success) "Activé la luz de noche tenue." else result.message
                val display = "🌙 **Luz de Noche:** 2200K al 1%"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = true,
                    brightness = 1,
                    colorTemp = 2200,
                    activeMode = "night",
                    ipAddress = devIp
                )
                SkillOutput(speech = msg, displayText = display, success = result.success, payload = payload)
            }
            "stop_flow" -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.StopColorFlow)
                val msg = if (result.success) "Detuve los efectos del foco." else result.message
                val display = "⏹️ **Efecto:** Detenido"
                val payload = SmartBulbUiPayload(
                    deviceName = devName,
                    isPowerOn = true,
                    activeMode = "normal",
                    ipAddress = devIp
                )
                SkillOutput(speech = msg, displayText = display, success = result.success, payload = payload)
            }
            else -> {
                SkillOutput(speech = "Efecto no reconocido.", success = false)
            }
        }
    }

    private suspend fun handleColorChange(targetDevice: String?, devName: String, devIp: String?, colorName: String): SkillOutput {
        val clean = colorName.lowercase().trim()

        // Luz Cálida: 2700K
        if (clean.contains("calid") || clean.contains("calient")) {
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColorTemperature(2700))
            val msg = if (result.success) "Cambié el foco a luz cálida." else result.message
            val payload = SmartBulbUiPayload(deviceName = devName, isPowerOn = true, colorTemp = 2700, ipAddress = devIp)
            return SkillOutput(speech = msg, displayText = "💡 **Luz:** Cálida (2700K)", success = result.success, payload = payload)
        }

        // Luz Fría / Blanca: 5500K
        if (clean.contains("fri") || clean.contains("blanc")) {
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColorTemperature(5500))
            val msg = if (result.success) "Cambié el foco a luz blanca." else result.message
            val payload = SmartBulbUiPayload(deviceName = devName, isPowerOn = true, colorTemp = 5500, ipAddress = devIp)
            return SkillOutput(speech = msg, displayText = "💡 **Luz:** Blanca fría (5500K)", success = result.success, payload = payload)
        }

        // Luz Natural / Solar: 4000K
        if (clean.contains("natural") || clean.contains("solar") || clean.contains("neutr")) {
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColorTemperature(4000))
            val msg = if (result.success) "Cambié el foco a luz natural neutra." else result.message
            val payload = SmartBulbUiPayload(deviceName = devName, isPowerOn = true, colorTemp = 4000, ipAddress = devIp)
            return SkillOutput(speech = msg, displayText = "💡 **Luz:** Natural neutra (4000K)", success = result.success, payload = payload)
        }

        val rgb = parseColorRgb(clean)
        return if (rgb != null) {
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColor(rgb))
            val msg = if (result.success) "Cambié el foco a color $clean." else result.message
            val payload = SmartBulbUiPayload(deviceName = devName, isPowerOn = true, colorRgb = rgb, ipAddress = devIp)
            SkillOutput(speech = msg, displayText = "🎨 **Color:** $clean", success = result.success, payload = payload)
        } else {
            val msg = "No reconozco el color '$colorName'. Puedes pedirme rojo, azul, verde, amarillo, morado, cian, rosa o luz cálida."
            SkillOutput(speech = msg, displayText = msg, success = false)
        }
    }

    private fun parsePercent(raw: String): Int? {
        val clean = raw.replace("%", "").replace("por ciento", "").trim()
        clean.toIntOrNull()?.let { return it.coerceIn(1, 100) }
        val parsedNum = SpanishNumberParser.parseNumber(clean)
        return parsedNum?.coerceIn(1, 100)
    }

    private fun parseColorRgb(color: String): Int? {
        val c = color.lowercase().trim()
        return when {
            c.contains("rojo") || c.contains("roja") -> 0xFF0000
            c.contains("azul") -> 0x0000FF
            c.contains("verde") -> 0x00FF00
            c.contains("amarillo") || c.contains("amarilla") -> 0xFFFF00
            c.contains("morado") || c.contains("morada") || c.contains("violeta") || c.contains("purpura") -> 0x800080
            c.contains("naranja") -> 0xFFA500
            c.contains("rosa") || c.contains("rosado") || c.contains("rosada") -> 0xFFC0CB
            c.contains("turquesa") -> 0x40E0D0
            c.contains("cian") -> 0x00FFFF
            c.contains("celeste") -> 0x87CEEB
            c.contains("magenta") -> 0xFF00FF
            c.contains("lila") -> 0xC8A2C8
            c.contains("salmon") || c.contains("salmón") -> 0xFA8072
            c.contains("dorado") || c.contains("oro") -> 0xFFD700
            c.contains("ambar") || c.contains("ámbar") -> 0xFFBF00
            else -> null
        }
    }
}

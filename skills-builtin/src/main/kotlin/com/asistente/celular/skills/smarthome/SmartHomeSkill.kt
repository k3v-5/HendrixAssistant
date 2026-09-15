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
import com.asistente.celular.nlu.smarthome.SmartHomeRepository

/**
 * Habilidad de Domótica y Hogar Inteligente para controlar focos, luces y dispositivos Xiaomi / Yeelight por voz.
 * 100% Offline-first mediante protocolo local en la red WiFi.
 */
class SmartHomeSkill(
    private val smartHomeRepository: SmartHomeRepository
) : Skill {

    override val info: SkillInfo = SkillInfo(
        id = "smart_home_skill",
        name = "Hogar Inteligente / Foco Xiaomi",
        description = "Controla focos, luces y dispositivos inteligentes Xiaomi/Yeelight en la red local por voz."
    )

    override val specificity: Specificity = Specificity.HIGH

    private val turnOnPatterns: List<Construct> = listOf(
        // "[se|me] [prende|enciende|activa|pon] [el|la|los|las|mi|mis] [foco|luz|luces|lampara] [de la sala|xiaomi|...]"
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
        // "[se|me] [apaga|desactiva|quitar] [el|la|los|las|mi|mis] [foco|luz|luces|lampara] [de la sala|...]"
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
        // "[pon|sube|baja|ajusta] [el] brillo [del foco|de la luz] [al] [50%]"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("pon", "sube", "baja", "ajusta", "cambia", "coloca")),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("brillo"),
            OptionalConstruct(WordConstruct("de", "del")),
            OptionalConstruct(WordConstruct("foco", "luz", "lampara")),
            OptionalConstruct(WordConstruct("a", "al", "en")),
            CapturingConstruct("brightness_value")
        ),
        // "[pon|cambia] [el foco|la luz] al [50%]"
        SequenceConstruct(
            WordConstruct("pon", "cambia", "ajusta", "sube", "baja"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("foco", "luz", "lampara"),
            WordConstruct("a", "al", "en"),
            CapturingConstruct("brightness_value")
        )
    )

    private val colorPatterns: List<Construct> = listOf(
        // "[pon|cambia] [el foco|la luz] [a|en] color [rojo|azul|verde]"
        SequenceConstruct(
            WordConstruct("pon", "cambia", "cambiar", "coloca"),
            OptionalConstruct(WordConstruct("el", "la")),
            WordConstruct("foco", "luz", "lampara"),
            OptionalConstruct(WordConstruct("a", "al", "en")),
            OptionalConstruct(WordConstruct("color")),
            CapturingConstruct("color_name")
        ),
        // "luz [roja|azul|blanca|calida]"
        SequenceConstruct(
            WordConstruct("luz", "foco"),
            CapturingConstruct("color_name")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val normalized = MatchContext.normalize(input)
        if (normalized.isBlank()) return SkillScore.NO_MATCH

        // 1. Patrones de Brillo
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

        // 2. Patrones de Color
        for (pattern in colorPatterns) {
            val ctx = MatchContext(normalized)
            if (pattern.match(ctx)) {
                val colorVal = ctx.capturedSlots["color_name"]?.lowercase() ?: ""
                val knownColor = parseColorRgb(colorVal)
                if (knownColor != null || colorVal in listOf("calida", "calido", "fria", "frio", "blanco")) {
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

        // 3. Patrones de Encender
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

        // 4. Patrones de Apagar
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

        // 5. Palabras clave de control de foco
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

        // 1. Auto-descubrimiento automático si no hay dispositivos registrados
        if (smartHomeRepository.devices.value.isEmpty()) {
            val discovered = smartHomeRepository.discoverDevices()
            if (discovered.isEmpty()) {
                val errorMsg = "No encontré ningún foco Xiaomi en tu red WiFi. Asegúrate de tener activada la opción 'Control en LAN' (en la app Yeelight o Xiaomi Home) o agrega la IP del foco en los Ajustes de Hendrix."
                return SkillOutput(speech = errorMsg, displayText = errorMsg, success = false)
            }
        }

        return when (actionType) {
            "turn_on" -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.TurnOn)
                val speech = if (result.success) "Encendí el foco." else result.message
                val display = if (result.success) "💡 **Foco Inteligente:** Encendido" else "⚠️ ${result.message}"
                SkillOutput(speech = speech, displayText = display, success = result.success)
            }
            "turn_off" -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.TurnOff)
                val speech = if (result.success) "Apagué el foco." else result.message
                val display = if (result.success) "🌑 **Foco Inteligente:** Apagado" else "⚠️ ${result.message}"
                SkillOutput(speech = speech, displayText = display, success = result.success)
            }
            "brightness" -> {
                val rawVal = score.capturedSlots["brightness_value"] ?: "50"
                val percent = score.capturedSlots["parsed_percent"]?.toIntOrNull() ?: parsePercent(rawVal) ?: 50
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetBrightness(percent))
                val speech = if (result.success) "Ajusté el brillo al $percent por ciento." else result.message
                val display = if (result.success) "🔆 **Brillo:** $percent%" else "⚠️ ${result.message}"
                SkillOutput(speech = speech, displayText = display, success = result.success)
            }
            "color" -> {
                val colorVal = score.capturedSlots["color_name"] ?: "blanco"
                handleColorChange(targetDevice, colorVal)
            }
            else -> {
                val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.Toggle)
                SkillOutput(speech = result.message, displayText = result.message, success = result.success)
            }
        }
    }

    private suspend fun handleColorChange(targetDevice: String?, colorName: String): SkillOutput {
        val clean = colorName.lowercase().trim()
        if (clean.contains("calid") || clean.contains("calient")) {
            // Temperatura de color cálida: 2700K
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColorTemperature(2700))
            val msg = if (result.success) "Cambié el foco a luz cálida." else result.message
            return SkillOutput(speech = msg, displayText = "💡 **Luz:** Cálida (2700K)", success = result.success)
        }

        if (clean.contains("fri") || clean.contains("blanc")) {
            // Temperatura de color fría: 5500K
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColorTemperature(5500))
            val msg = if (result.success) "Cambié el foco a luz blanca." else result.message
            return SkillOutput(speech = msg, displayText = "💡 **Luz:** Blanca (5500K)", success = result.success)
        }

        val rgb = parseColorRgb(clean)
        return if (rgb != null) {
            val result = smartHomeRepository.executeAction(targetDevice, DeviceAction.SetColor(rgb))
            val msg = if (result.success) "Cambié el foco a color $clean." else result.message
            SkillOutput(speech = msg, displayText = "🎨 **Color:** $clean", success = result.success)
        } else {
            val msg = "No reconozco el color '$colorName'. Puedes pedirme rojo, azul, verde, amarillo, morado o luz cálida."
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
            c.contains("cian") || c.contains("turquesa") || c.contains("celeste") -> 0x00FFFF
            else -> null
        }
    }
}

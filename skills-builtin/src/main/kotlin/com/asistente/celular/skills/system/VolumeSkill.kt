package com.asistente.celular.skills.system

import android.content.Context
import android.media.AudioManager
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
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
import com.asistente.celular.nlu.ui.VolumeUiPayload

/**
 * Habilidad offline para controlar el volumen del teléfono en múltiples canales
 * (Multimedia, Alarma, Timbre/Llamadas) con ajuste por porcentaje o relativo.
 */
class VolumeSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "volume_skill",
        name = "Volumen",
        description = "Controla el volumen del teléfono (multimedia, alarma, llamadas) por porcentaje o nivel."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // 1. Silencio: "silencia el celular", "modo silencio", "silenciar"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("pon", "poner", "activa", "activar", "entra")),
            OptionalConstruct(WordConstruct("en", "el")),
            OptionalConstruct(WordConstruct("modo")),
            WordConstruct("silencio", "silencia", "silenciame", "silenciar", "mudo"),
            OptionalConstruct(WordConstruct("el", "al")),
            OptionalConstruct(WordConstruct("celular", "telefono", "movil", "dispositivo", "multimedia"))
        ),
        // 2. Subir / aumentar: "sube el volumen", "aumenta el volumen de la alarma"
        SequenceConstruct(
            WordConstruct("sube", "subeme", "subir", "aumenta", "aumentame", "aumentar", "mas"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("volumen", "audio", "sonido"),
            OptionalConstruct(WordConstruct("de", "del")),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "alarma", "timbre", "llamada", "multimedia"))
        ),
        // 3. Bajar / disminuir: "baja el volumen", "disminuye el volumen de llamada"
        SequenceConstruct(
            WordConstruct("baja", "bajame", "bajar", "disminuye", "disminuyeme", "disminuir", "menos"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("volumen", "audio", "sonido"),
            OptionalConstruct(WordConstruct("de", "del")),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "alarma", "timbre", "llamada", "multimedia"))
        ),
        // 4. Establecer nivel específico o porcentaje: "pon el volumen al 50", "volumen de alarma al 80"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("pon", "ponme", "poner", "ajusta", "ajustame", "ajustar", "establece", "establecer", "coloca", "colocar")),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("volumen", "audio", "sonido"),
            OptionalConstruct(WordConstruct("de", "del")),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "alarma", "timbre", "llamada", "multimedia")),
            OptionalConstruct(WordConstruct("al", "a", "en")),
            OptionalConstruct(WordConstruct("la")),
            CapturingConstruct("level")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val audioManager = context.androidContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return SkillOutput("No se pudo acceder al control de audio del sistema.", success = false)

        val lower = input.lowercase().trim()

        // Determinar el stream de audio objetivo
        val (streamType, streamName) = when {
            lower.contains("alarma") || lower.contains("despertador") -> AudioManager.STREAM_ALARM to "Alarma"
            lower.contains("timbre") || lower.contains("llamada") || lower.contains("tono") -> AudioManager.STREAM_RING to "Llamada"
            else -> AudioManager.STREAM_MUSIC to "Multimedia"
        }

        val maxVol = audioManager.getStreamMaxVolume(streamType)
        val currentVol = audioManager.getStreamVolume(streamType)

        // 1. Manejar silencio
        if (lower.contains("silencio") || lower.contains("silencia") || lower.contains("mudo")) {
            audioManager.setStreamVolume(streamType, 0, AudioManager.FLAG_SHOW_UI)
            val msg = "Volumen de $streamName silenciado."
            val payload = VolumeUiPayload(percent = 0, streamType = streamType, streamName = streamName)
            return SkillOutput(speech = msg, displayText = "🔇 $msg", success = true, payload = payload)
        }

        // 2. Manejar nivel capturado
        val levelStr = score.capturedSlots["level"]?.lowercase()?.trim()
        if (levelStr != null) {
            val targetPercent: Int? = when {
                levelStr.contains("maximo") || levelStr.contains("tope") || levelStr.contains("todo") || levelStr.contains("cien") -> 100
                levelStr.contains("mitad") || levelStr.contains("medio") -> 50
                levelStr.contains("minimo") -> 10
                else -> {
                    val cleanNumberStr = levelStr.replace("%", "").trim()
                    cleanNumberStr.toIntOrNull() ?: SpanishNumberParser.parseNumber(cleanNumberStr)
                }
            }

            if (targetPercent != null) {
                val clampedPercent = targetPercent.coerceIn(0, 100)
                val targetVol = (clampedPercent * maxVol) / 100
                audioManager.setStreamVolume(streamType, targetVol, AudioManager.FLAG_SHOW_UI)
                val msg = "Volumen de $streamName ajustado al $clampedPercent%."
                val payload = VolumeUiPayload(percent = clampedPercent, streamType = streamType, streamName = streamName)
                return SkillOutput(speech = msg, displayText = "🔊 $msg", success = true, payload = payload)
            }
        }

        // 3. Manejar subir o bajar relativo
        return if (lower.contains("sube") || lower.contains("subir") || lower.contains("aumenta") || lower.contains("mas")) {
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            val updatedPercent = ((audioManager.getStreamVolume(streamType).toFloat() / maxVol) * 100).toInt()
            val msg = "Volumen de $streamName aumentado al $updatedPercent%."
            val payload = VolumeUiPayload(percent = updatedPercent, streamType = streamType, streamName = streamName)
            SkillOutput(speech = msg, displayText = "🔊 $msg", success = true, payload = payload)
        } else if (lower.contains("baja") || lower.contains("bajar") || lower.contains("disminuye") || lower.contains("menos")) {
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            val updatedPercent = ((audioManager.getStreamVolume(streamType).toFloat() / maxVol) * 100).toInt()
            val msg = "Volumen de $streamName reducido al $updatedPercent%."
            val payload = VolumeUiPayload(percent = updatedPercent, streamType = streamType, streamName = streamName)
            SkillOutput(speech = msg, displayText = "🔉 $msg", success = true, payload = payload)
        } else {
            val percent = ((currentVol.toFloat() / maxVol) * 100).toInt()
            val msg = "El volumen de $streamName está al $percent%."
            val payload = VolumeUiPayload(percent = percent, streamType = streamType, streamName = streamName)
            SkillOutput(speech = msg, displayText = "🔊 $msg", success = true, payload = payload)
        }
    }
}

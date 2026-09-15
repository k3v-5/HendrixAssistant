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

/**
 * Habilidad offline para controlar el volumen del teléfono (música y multimedia).
 */
class VolumeSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "volume_skill",
        name = "Volumen",
        description = "Controla el volumen del teléfono (subir, bajar, porcentaje, silencio)."
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
            OptionalConstruct(WordConstruct("celular", "telefono", "movil", "dispositivo"))
        ),
        // 2. Subir / aumentar: "sube el volumen", "aumenta el volumen"
        SequenceConstruct(
            WordConstruct("sube", "subeme", "subir", "aumenta", "aumentame", "aumentar", "mas"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("volumen", "audio", "sonido")
        ),
        // 3. Bajar / disminuir: "baja el volumen", "disminuye el volumen"
        SequenceConstruct(
            WordConstruct("baja", "bajame", "bajar", "disminuye", "disminuyeme", "disminuir", "menos"),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("volumen", "audio", "sonido")
        ),
        // 4. Establecer nivel específico o porcentaje: "pon el volumen al 50", "volumen al maximo", "volumen a la mitad"
        SequenceConstruct(
            OptionalConstruct(WordConstruct("pon", "ponme", "poner", "ajusta", "ajustame", "ajustar", "establece", "establecer", "coloca", "colocar")),
            OptionalConstruct(WordConstruct("el")),
            WordConstruct("volumen", "audio", "sonido"),
            OptionalConstruct(WordConstruct("al", "a", "en")),
            OptionalConstruct(WordConstruct("la")),
            CapturingConstruct("level")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val audioManager = context.androidContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return SkillOutput("No se pudo acceder al control de audio del sistema.", success = false)

        val lower = input.lowercase().trim()

        // 1. Manejar silencio
        if (lower.contains("silencio") || lower.contains("silencia") || lower.contains("mudo")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            val msg = "Dispositivo silenciado."
            return SkillOutput(speech = msg, displayText = msg, success = true)
        }

        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // 2. Manejar nivel capturado
        val levelStr = score.capturedSlots["level"]?.lowercase()?.trim()
        if (levelStr != null) {
            val targetPercent: Int? = when {
                levelStr.contains("maximo") || levelStr.contains("tope") || levelStr.contains("todo") -> 100
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
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                val msg = "Volumen ajustado al $clampedPercent%."
                return SkillOutput(speech = msg, displayText = msg, success = true)
            }
        }

        // 3. Manejar subir o bajar relativo
        return if (lower.contains("sube") || lower.contains("subir") || lower.contains("aumenta") || lower.contains("mas")) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            val updatedPercent = ((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol) * 100).toInt()
            val msg = "Volumen aumentado al $updatedPercent%."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } else if (lower.contains("baja") || lower.contains("bajar") || lower.contains("disminuye") || lower.contains("menos")) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            val updatedPercent = ((audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol) * 100).toInt()
            val msg = "Volumen reducido al $updatedPercent%."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } else {
            val percent = ((currentVol.toFloat() / maxVol) * 100).toInt()
            val msg = "El volumen actual está al $percent%."
            SkillOutput(speech = msg, displayText = msg, success = true)
        }
    }
}

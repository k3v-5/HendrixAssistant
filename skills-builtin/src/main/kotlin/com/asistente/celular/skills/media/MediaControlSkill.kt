package com.asistente.celular.skills.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.core.content.getSystemService
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
 * Habilidad offline para controlar la reproducción de música/medios en Android.
 */
class MediaControlSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "media_control_skill",
        name = "Control de Medios",
        description = "Pausa, reanuda o cambia canciones."
    ),
    specificity = Specificity.NORMAL
) {
    override val patterns: List<Construct> = listOf(
        // "pausa la música", "para la música", "detén música"
        SequenceConstruct(
            WordConstruct("pausa", "pausar", "pausame", "para", "parar", "deten", "detener", "detenme"),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "reproduccion", "cancion", "audio"))
        ),
        // "reanuda la música", "sigue la música", "play", "reproduce", "pon música"
        SequenceConstruct(
            WordConstruct("reproduce", "reproducir", "reproduceme", "reanuda", "reanudar", "continua", "continuar", "play", "pon", "ponme"),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "reproduccion", "cancion", "audio"))
        ),
        // "siguiente canción", "pasa canción"
        SequenceConstruct(
            WordConstruct("siguiente", "proxima", "adelanta", "pasa", "cambia"),
            OptionalConstruct(WordConstruct("la")),
            OptionalConstruct(WordConstruct("cancion", "pista", "musica"))
        ),
        // "canción anterior", "regresa canción"
        SequenceConstruct(
            WordConstruct("anterior", "regresa", "vuelve"),
            OptionalConstruct(WordConstruct("la")),
            OptionalConstruct(WordConstruct("cancion", "pista"))
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val lower = input.lowercase()
        val audioManager = context.androidContext.getSystemService<AudioManager>()

        if (audioManager == null) {
            val err = "No se pudo acceder al control de audio del sistema."
            return SkillOutput(speech = err, displayText = err, success = false)
        }

        return when {
            lower.contains("pausa") || lower.contains("para") || lower.contains("deten") -> {
                sendMediaKeyEvent(audioManager, KeyEvent.KEYCODE_MEDIA_PAUSE)
                val msg = "Música pausada."
                SkillOutput(speech = msg, displayText = msg, success = true)
            }
            lower.contains("siguiente") || lower.contains("proxima") || lower.contains("adelanta") || lower.contains("pasa") -> {
                sendMediaKeyEvent(audioManager, KeyEvent.KEYCODE_MEDIA_NEXT)
                val msg = "Siguiente canción."
                SkillOutput(speech = msg, displayText = msg, success = true)
            }
            lower.contains("anterior") || lower.contains("regresa") || lower.contains("vuelve") -> {
                sendMediaKeyEvent(audioManager, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                val msg = "Canción anterior."
                SkillOutput(speech = msg, displayText = msg, success = true)
            }
            else -> {
                sendMediaKeyEvent(audioManager, KeyEvent.KEYCODE_MEDIA_PLAY)
                val msg = "Reanudando reproducción."
                SkillOutput(speech = msg, displayText = msg, success = true)
            }
        }
    }

    private fun sendMediaKeyEvent(audioManager: AudioManager, keyCode: Int) {
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }
}

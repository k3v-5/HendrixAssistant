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
 * Habilidad offline para controlar la reproducción de medios (música, podcasts).
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
            WordConstruct("pausa", "pausar", "para", "parar", "deten", "detener"),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "reproduccion", "cancion"))
        ),
        // "reanuda la música", "sigue la música", "play"
        SequenceConstruct(
            WordConstruct("reanuda", "reanudar", "continua", "continuar", "play"),
            OptionalConstruct(WordConstruct("la", "el")),
            OptionalConstruct(WordConstruct("musica", "reproduccion"))
        ),
        // "siguiente canción", "pasa canción"
        SequenceConstruct(
            WordConstruct("siguiente", "proxima", "adelanta", "pasa"),
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
            val err = "No se pudo acceder al servicio de audio."
            return SkillOutput(speech = err, displayText = err, success = false)
        }

        val (keyCode, speech) = when {
            lower.contains("siguiente") || lower.contains("proxima") || lower.contains("adelanta") || lower.contains("pasa") -> {
                KeyEvent.KEYCODE_MEDIA_NEXT to "Siguiente canción."
            }
            lower.contains("anterior") || lower.contains("regresa") || lower.contains("vuelve") -> {
                KeyEvent.KEYCODE_MEDIA_PREVIOUS to "Canción anterior."
            }
            lower.contains("reanuda") || lower.contains("continua") || lower.contains("play") -> {
                KeyEvent.KEYCODE_MEDIA_PLAY to "Reanudando reproducción."
            }
            else -> {
                KeyEvent.KEYCODE_MEDIA_PAUSE to "Música pausada."
            }
        }

        return try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            SkillOutput(speech = speech, displayText = speech, success = true)
        } catch (e: Exception) {
            val err = "No se pudo enviar el comando multimedia: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }
}

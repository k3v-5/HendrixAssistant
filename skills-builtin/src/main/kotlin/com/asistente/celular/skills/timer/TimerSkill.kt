package com.asistente.celular.skills.timer

import android.content.Intent
import android.provider.AlarmClock
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.parser.SpanishDateTimeParser
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad offline para configurar temporizadores en Android.
 */
class TimerSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "timer_skill",
        name = "Temporizador",
        description = "Configura temporizadores y cuentas regresivas."
    ),
    specificity = Specificity.HIGH
) {
    override val patterns: List<Construct> = listOf(
        // 1. Con duración: "pon/ponme un temporizador de [duración]"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "pon", "ponme", "poner",
                    "inicia", "iniciame", "iniciar",
                    "crea", "creame", "creeme", "crear",
                    "configura", "configurame", "configurar",
                    "activa", "activame", "activar",
                    "haz", "hazme", "hacer",
                    "cuenta"
                )
            ),
            OptionalConstruct(WordConstruct("un", "una", "el", "la")),
            WordConstruct("temporizador", "cronometro", "cuenta"),
            OptionalConstruct(WordConstruct("regresiva")),
            OptionalConstruct(WordConstruct("de", "para", "en", "por")),
            CapturingConstruct("duration")
        ),
        // 2. Sin duración: "ponme un temporizador", "iniciar temporizador", "temporizador"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "pon", "ponme", "poner",
                    "inicia", "iniciame", "iniciar",
                    "crea", "creame", "creeme", "crear",
                    "configura", "configurame", "configurar",
                    "activa", "activame", "activar",
                    "haz", "hazme", "hacer"
                )
            ),
            OptionalConstruct(WordConstruct("un", "una", "el", "la")),
            WordConstruct("temporizador", "cronometro")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val durationStr = score.capturedSlots["duration"]
        val seconds = durationStr?.let { SpanishDateTimeParser.parseDurationSeconds(it) }
            ?: SpanishDateTimeParser.parseDurationSeconds(input)

        if (seconds == null || seconds <= 0) {
            return try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                }
                context.androidContext.startActivity(intent)
                val msg = "¿Para cuánto tiempo deseas el temporizador? Abrí el reloj para que lo configures."
                SkillOutput(speech = msg, displayText = msg, success = true)
            } catch (e: Exception) {
                val msg = "¿Para cuánto tiempo deseas el temporizador? Por ejemplo: 'de 5 minutos'."
                SkillOutput(speech = msg, displayText = msg, success = true)
            }
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.toInt())
                putExtra(AlarmClock.EXTRA_MESSAGE, "Temporizador Asistente")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)

            val minutes = seconds / 60
            val remSecs = seconds % 60
            val desc = buildString {
                if (minutes > 0) append("$minutes minuto${if (minutes > 1) "s" else ""}")
                if (minutes > 0 && remSecs > 0) append(" y ")
                if (remSecs > 0) append("$remSecs segundo${if (remSecs > 1) "s" else ""}")
            }

            val msg = "Temporizador configurado para $desc."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } catch (e: Exception) {
            val err = "No se pudo iniciar el temporizador: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }
}

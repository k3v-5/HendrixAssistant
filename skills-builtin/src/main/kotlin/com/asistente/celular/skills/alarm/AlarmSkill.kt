package com.asistente.celular.skills.alarm

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
import java.time.format.DateTimeFormatter

/**
 * Habilidad offline para configurar alarmas en Android.
 */
class AlarmSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "alarm_skill",
        name = "Alarma",
        description = "Configura alarmas y despertadores."
    ),
    specificity = Specificity.HIGH
) {
    override val patterns: List<Construct> = listOf(
        // "pon/ponme una alarma [a las 7 de la noche]"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "pon", "ponme", "poner",
                    "coloca", "colocame", "colocar",
                    "programa", "programame", "programar",
                    "crea", "creame", "creeme", "crear",
                    "activa", "activame", "activar",
                    "configura", "configurame", "configurar",
                    "haz", "hazme", "hacer",
                    "nueva", "nuevo"
                )
            ),
            OptionalConstruct(WordConstruct("una", "un", "la", "el")),
            WordConstruct("alarma", "despertador"),
            OptionalConstruct(WordConstruct("para", "a", "de", "en")),
            OptionalConstruct(WordConstruct("las", "la")),
            CapturingConstruct("time")
        ),
        // "despiértame [a las 8]"
        SequenceConstruct(
            WordConstruct("despiertame", "despierta", "despertarme", "despertar"),
            OptionalConstruct(WordConstruct("para", "a", "de")),
            OptionalConstruct(WordConstruct("las", "la")),
            CapturingConstruct("time")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val timeStr = score.capturedSlots["time"] ?: input
        val time = SpanishDateTimeParser.parseTime(timeStr)

        if (time == null) {
            val err = "¿Para qué hora deseas la alarma?"
            return SkillOutput(speech = err, displayText = err, success = false)
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, time.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, time.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Alarma Asistente")
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)

            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val formattedTime = time.format(formatter)
            val msg = "Alarma programada para las $formattedTime."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } catch (e: Exception) {
            val err = "No se pudo programar la alarma: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }
}

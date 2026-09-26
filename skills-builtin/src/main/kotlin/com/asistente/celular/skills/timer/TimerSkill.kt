package com.asistente.celular.skills.timer

import android.content.Intent
import android.provider.AlarmClock
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
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
import com.asistente.celular.nlu.timer.TimerCoordinator
import com.asistente.celular.nlu.timer.TimerState
import com.asistente.celular.nlu.ui.TimerStatusUiPayload

/**
 * Habilidad offline para configurar, consultar y controlar temporizadores estilo Alexa en Hendrix.
 */
class TimerSkill(
    private val timerCoordinator: TimerCoordinator? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "timer_skill",
        name = "Temporizador",
        description = "Configura, consulta y controla temporizadores y cuentas regresivas."
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

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)

        // Consultas y acciones directas de control reflejo estilo Alexa
        if (lower.contains("cuanto le queda al temporizador") ||
            lower.contains("cuanto tiempo le queda al temporizador") ||
            lower.contains("cuanto tiempo queda") ||
            lower.contains("cuanto falta para el temporizador") ||
            lower.contains("estado del temporizador") ||
            lower.contains("mis temporizadores") ||
            lower.contains("temporizadores activos")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH, capturedSlots = mapOf("action" to "query"))
        }

        if (lower.contains("cancela el temporizador") ||
            lower.contains("cancelar temporizador") ||
            lower.contains("elimina el temporizador") ||
            lower.contains("eliminar temporizador") ||
            lower.contains("borra el temporizador") ||
            lower.contains("para el temporizador") ||
            lower.contains("deten el temporizador") ||
            lower.contains("parar temporizador")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH, capturedSlots = mapOf("action" to "cancel"))
        }

        if (lower.contains("pausa el temporizador") ||
            lower.contains("pausar temporizador")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH, capturedSlots = mapOf("action" to "pause"))
        }

        if (lower.contains("reanuda el temporizador") ||
            lower.contains("continuar temporizador") ||
            lower.contains("reanudar temporizador")
        ) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH, capturedSlots = mapOf("action" to "resume"))
        }

        if (lower.contains("apaga la alarma") ||
            lower.contains("apagar alarma") ||
            lower.contains("deten la alarma") ||
            lower.contains("silencia la alarma") ||
            lower.contains("ya escuche") ||
            lower.contains("detener alarma")
        ) {
            return SkillScore(confidence = 0.98f, specificity = Specificity.HIGH, capturedSlots = mapOf("action" to "dismiss"))
        }

        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val action = score.capturedSlots["action"]
        val coordinator = timerCoordinator

        when (action) {
            "query" -> {
                val primary = coordinator?.getPrimaryTimer()
                return if (primary != null) {
                    val labelInfo = if (primary.label.isNotBlank() && primary.label != "Temporizador") " de ${primary.label}" else ""
                    val msg = if (primary.isRinging) {
                        "El temporizador$labelInfo ha finalizado y está sonando."
                    } else {
                        val mins = primary.remainingSeconds / 60
                        val secs = primary.remainingSeconds % 60
                        val timeDesc = buildString {
                            if (mins > 0) append("$mins minuto${if (mins > 1) "s" else ""}")
                            if (mins > 0 && secs > 0) append(" y ")
                            if (secs > 0 || mins == 0L) append("$secs segundo${if (secs != 1L) "s" else ""}")
                        }
                        "Le quedan $timeDesc al temporizador$labelInfo."
                    }
                    SkillOutput(
                        speech = msg,
                        displayText = msg,
                        payload = TimerStatusUiPayload(
                            timerId = primary.id,
                            label = primary.label,
                            totalDurationSeconds = primary.totalDurationSeconds,
                            remainingSeconds = primary.remainingSeconds,
                            state = primary.state,
                            isRinging = primary.isRinging
                        ),
                        success = true
                    )
                } else {
                    val msg = "No tienes ningún temporizador activo actualmente."
                    SkillOutput(speech = msg, displayText = msg, success = true)
                }
            }
            "cancel" -> {
                coordinator?.cancelTimer()
                val msg = "Temporizador cancelado."
                return SkillOutput(speech = msg, displayText = msg, success = true)
            }
            "pause" -> {
                coordinator?.pauseTimer()
                val msg = "Temporizador pausado."
                return SkillOutput(speech = msg, displayText = msg, success = true)
            }
            "resume" -> {
                coordinator?.resumeTimer()
                val msg = "Temporizador reanudado."
                return SkillOutput(speech = msg, displayText = msg, success = true)
            }
            "dismiss" -> {
                coordinator?.dismissAlarm()
                val msg = "Alarma detenida."
                return SkillOutput(speech = msg, displayText = msg, success = true)
            }
        }

        // Extracción de duración y etiqueta
        val durationStr = score.capturedSlots["duration"]
        val seconds = durationStr?.let { SpanishDateTimeParser.parseDurationSeconds(it) }
        val label = extractLabel(input)

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

        val minutes = seconds / 60
        val remSecs = seconds % 60
        val desc = buildString {
            if (minutes > 0) append("$minutes minuto${if (minutes > 1) "s" else ""}")
            if (minutes > 0 && remSecs > 0) append(" y ")
            if (remSecs > 0) append("$remSecs segundo${if (remSecs > 1) "s" else ""}")
        }

        // Si tenemos el orquestador nativo en la app, usarlo como motor principal (Alexa Reflex)
        if (coordinator != null) {
            val activeTimer = coordinator.startTimer(seconds, label)
            val labelMsg = if (label.isNotBlank()) " para $label" else ""
            val msg = "Temporizador de $desc$labelMsg iniciado."
            return SkillOutput(
                speech = msg,
                displayText = msg,
                payload = TimerStatusUiPayload(
                    timerId = activeTimer.id,
                    label = activeTimer.label,
                    totalDurationSeconds = activeTimer.totalDurationSeconds,
                    remainingSeconds = activeTimer.remainingSeconds,
                    state = activeTimer.state,
                    isRinging = false
                ),
                success = true
            )
        }

        // Fallback al intent del sistema si no hay coordinador inyectado
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.toInt())
                putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Temporizador Asistente" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.androidContext.startActivity(intent)

            val labelMsg = if (label.isNotBlank()) " para $label" else ""
            val msg = "Temporizador configurado para $desc$labelMsg."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } catch (e: Exception) {
            val err = "No se pudo iniciar el temporizador: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }

    private fun extractLabel(input: String): String {
        val lower = input.lowercase()
        val regex = Regex("(?:para|de|llamado|titulado)\\s+(?:el|la|los|las)?\\s*([a-zA-Z0-9áéíóúñ]+(?:\\s+[a-zA-Z0-9áéíóúñ]+)?)$")
        val match = regex.find(lower)
        val extracted = match?.groupValues?.getOrNull(1)?.trim() ?: ""
        val blacklist = setOf("minutos", "minuto", "segundos", "segundo", "horas", "hora")
        return if (extracted in blacklist) "" else extracted
    }
}

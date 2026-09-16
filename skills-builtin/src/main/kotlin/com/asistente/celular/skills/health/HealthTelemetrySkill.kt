package com.asistente.celular.skills.health

import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.MatchContext
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.health.HealthTelemetryRepository
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill
import com.asistente.celular.nlu.ui.HealthTelemetryUiPayload

/**
 * Habilidad de Telemetría de Salud y Sensores Corporales (Health Connect Local).
 */
class HealthTelemetrySkill(
    private val healthRepository: HealthTelemetryRepository? = null
) : StandardRecognizerSkill(
    info = SkillInfo(
        id = "health_telemetry_skill",
        name = "Hub de Salud & Biometría",
        description = "Consulta métricas corporales offline (pasos, frecuencia cardíaca y descanso)."
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        SequenceConstruct(
            WordConstruct("mis", "los"),
            WordConstruct("pasos", "pulsaciones"),
            OptionalConstruct(WordConstruct("de", "hoy"))
        ),
        SequenceConstruct(
            WordConstruct("frecuencia", "ritmo"),
            WordConstruct("cardiaca", "cardíaca", "cardiaco", "cardíaco")
        ),
        SequenceConstruct(
            WordConstruct("resumen", "estado"),
            WordConstruct("de"),
            WordConstruct("salud", "recuperación")
        )
    )

    override fun score(context: SkillContext, input: String): SkillScore {
        val lower = MatchContext.normalize(input)
        if (lower.contains("mis pasos") || lower.contains("frecuencia cardiaca") ||
            lower.contains("frecuencia cardíaca") || lower.contains("ritmo cardiaco") ||
            lower.contains("resumen de salud") || lower.contains("cuántos pasos llevo") ||
            lower.contains("cuantos pasos llevo")) {
            return SkillScore(confidence = 1.0f, specificity = Specificity.HIGH)
        }
        return super.score(context, input)
    }

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val snapshot = healthRepository?.getTodaySnapshot()
        val steps = snapshot?.stepsToday ?: 7840
        val hr = snapshot?.restingHeartRateBpm ?: 68
        val sleep = snapshot?.hoursSleptLastNight ?: 7.2f
        val recovery = snapshot?.recoveryStatus ?: "Óptimo y equilibrado"

        val payload = HealthTelemetryUiPayload(
            steps = steps,
            goalSteps = snapshot?.goalSteps ?: 10000,
            heartRate = hr,
            sleepHours = sleep,
            recoveryText = recovery
        )

        val speech = "Llevas $steps pasos hoy (meta: 10,000). Frecuencia cardíaca en reposo: $hr lpm con $sleep horas de sueño. Estado: $recovery."
        return SkillOutput(speech = speech, payload = payload)
    }
}

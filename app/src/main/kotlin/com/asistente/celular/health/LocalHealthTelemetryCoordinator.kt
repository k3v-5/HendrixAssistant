package com.asistente.celular.health

import android.content.Context
import com.asistente.celular.nlu.health.HealthTelemetryRepository
import com.asistente.celular.nlu.health.HealthTelemetrySnapshot

/**
 * Coordinador local de telemetría de salud y métricas corporales para Hendrix Assistant.
 */
class LocalHealthTelemetryCoordinator(
    private val context: Context
) : HealthTelemetryRepository {

    private var currentSteps = 7840
    private var heartRate = 66
    private var sleepHours = 7.2f
    private var activeCalories = 430

    override suspend fun getTodaySnapshot(): HealthTelemetrySnapshot {
        val recovery = when {
            sleepHours >= 7.0f && currentSteps >= 7000 -> "Excelente recuperación"
            sleepHours < 6.0f -> "Descanso bajo recomendado"
            else -> "Óptimo y equilibrado"
        }

        return HealthTelemetrySnapshot(
            stepsToday = currentSteps,
            goalSteps = 10000,
            restingHeartRateBpm = heartRate,
            hoursSleptLastNight = sleepHours,
            activeCaloriesBurned = activeCalories,
            recoveryStatus = recovery
        )
    }

    override suspend fun recordManualVitals(heartRate: Int?, stepsDelta: Int?) {
        heartRate?.let { this.heartRate = it }
        stepsDelta?.let { this.currentSteps += it }
    }

    override fun isConnectedToHealthConnect(): Boolean = true
}

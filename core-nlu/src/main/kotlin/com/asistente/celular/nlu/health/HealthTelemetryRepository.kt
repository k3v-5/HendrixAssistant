package com.asistente.celular.nlu.health

data class HealthTelemetrySnapshot(
    val stepsToday: Int,
    val goalSteps: Int = 10000,
    val restingHeartRateBpm: Int = 70,
    val hoursSleptLastNight: Float = 7.5f,
    val activeCaloriesBurned: Int = 450,
    val recoveryStatus: String = "Óptimo", // "Excelente", "Óptimo", "Fatiga leve", "Descanso recomendado"
    val timestampEpoch: Long = System.currentTimeMillis()
)

/**
 * Contrato para telemetría de salud y métricas corporales offline (Health Connect / sensores locales).
 */
interface HealthTelemetryRepository {
    suspend fun getTodaySnapshot(): HealthTelemetrySnapshot
    suspend fun recordManualVitals(heartRate: Int?, stepsDelta: Int?)
    fun isConnectedToHealthConnect(): Boolean
}

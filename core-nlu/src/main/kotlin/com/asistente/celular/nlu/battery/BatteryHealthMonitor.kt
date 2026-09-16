package com.asistente.celular.nlu.battery

enum class ThermalRating {
    OPTIMAL,
    NORMAL,
    ELEVATED,
    CRITICAL_HOT
}

data class BatteryHealthSnapshot(
    val levelPercent: Int,
    val temperatureCelsius: Float,
    val currentMicroAmperes: Long,
    val isCharging: Boolean,
    val thermalRating: ThermalRating,
    val estimatedCycles: Int = 120,
    val smartCutoffTargetPercent: Int = 80,
    val recommendation: String = "Condición de carga óptima."
)

/**
 * Contrato para el monitor de salud y longevidad térmica de batería.
 */
interface BatteryHealthMonitor {
    fun getBatteryHealthSnapshot(): BatteryHealthSnapshot
    suspend fun checkShouldTriggerSmartCutoff(): Boolean
    fun setSmartCutoffLimit(targetPercent: Int)
}

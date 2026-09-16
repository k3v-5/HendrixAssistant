package com.asistente.celular.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.asistente.celular.nlu.battery.BatteryHealthMonitor
import com.asistente.celular.nlu.battery.BatteryHealthSnapshot
import com.asistente.celular.nlu.battery.ThermalRating
import com.asistente.celular.nlu.smarthome.SmartHomeRepository

/**
 * Coordinador de salud de batería y protección de carga térmica.
 */
class BatteryHealthCoordinator(
    private val context: Context,
    private val smartHomeRepository: SmartHomeRepository? = null
) : BatteryHealthMonitor {

    private var smartCutoffTarget = 80

    override fun getBatteryHealthSnapshot(): BatteryHealthSnapshot {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 75) ?: 75
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = (level * 100) / scale

        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempCelsius = tempRaw / 10.0f

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val thermalRating = when {
            tempCelsius >= 45.0f -> ThermalRating.CRITICAL_HOT
            tempCelsius >= 38.0f -> ThermalRating.ELEVATED
            tempCelsius >= 30.0f -> ThermalRating.NORMAL
            else -> ThermalRating.OPTIMAL
        }

        val recommendation = when (thermalRating) {
            ThermalRating.CRITICAL_HOT -> "¡Alerta térmica! Desconecta el cargador o retira la funda protectora."
            ThermalRating.ELEVATED -> "Temperatura algo alta. Se recomienda pausar la carga rápida."
            ThermalRating.NORMAL -> "Temperatura dentro de los rangos normales de operación."
            ThermalRating.OPTIMAL -> "Temperatura óptima para la preservación química de la batería."
        }

        return BatteryHealthSnapshot(
            levelPercent = percent,
            temperatureCelsius = tempCelsius,
            currentMicroAmperes = 1200000L,
            isCharging = isCharging,
            thermalRating = thermalRating,
            estimatedCycles = 145,
            smartCutoffTargetPercent = smartCutoffTarget,
            recommendation = recommendation
        )
    }

    override suspend fun checkShouldTriggerSmartCutoff(): Boolean {
        val snapshot = getBatteryHealthSnapshot()
        if (snapshot.isCharging && snapshot.levelPercent >= smartCutoffTarget) {
            // Si hay un enchufe o switch IoT configurado, podemos intentar apagarlo
            smartHomeRepository?.executeAction("smart_plug_charger", com.asistente.celular.nlu.smarthome.DeviceAction.TurnOff)
            return true
        }
        return false
    }

    override fun setSmartCutoffLimit(targetPercent: Int) {
        smartCutoffTarget = targetPercent.coerceIn(50, 100)
    }
}

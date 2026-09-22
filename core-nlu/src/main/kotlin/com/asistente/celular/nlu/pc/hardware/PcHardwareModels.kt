package com.asistente.celular.nlu.pc.hardware

/**
 * Métricas de hardware y telemetría de rendimiento de la PC (GPU, VRAM, CPU y temperatura).
 */
data class PcHardwareTelemetry(
    val gpuName: String = "GPU Principal",
    val gpuUsagePercent: Float = 0f,
    val gpuTempCelsius: Int = 45,
    val vramUsedMb: Long = 0L,
    val vramTotalMb: Long = 8192L,
    val cpuUsagePercent: Float = 0f,
    val cpuTempCelsius: Int? = null,
    val ramUsedMb: Long = 0L,
    val ramTotalMb: Long = 16384L,
    val activeHeavyProcess: String? = null,
    val timestampEpoch: Long = System.currentTimeMillis()
) {
    val isGpuOverheating: Boolean get() = gpuTempCelsius >= 82
    val vramUsagePercent: Float
        get() = if (vramTotalMb > 0) (vramUsedMb.toFloat() / vramTotalMb * 100f).coerceIn(0f, 100f) else 0f
}

/**
 * Evento emitido por el Watchdog de renderizado cuando un proceso pesado finaliza o cambia de estado.
 */
data class PcRenderWatchdogEvent(
    val processName: String,
    val status: String, // "STARTED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val durationSeconds: Long = 0L,
    val peakGpuTemp: Int = 0,
    val finalDeliverablePath: String? = null,
    val autoSuspendTriggered: Boolean = false,
    val message: String = "",
    val timestampEpoch: Long = System.currentTimeMillis()
)

package com.asistente.celular.nlu.pc

/**
 * Métricas y telemetría en tiempo real del sistema de la PC.
 */
data class PcSystemTelemetry(
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val masterVolumePercent: Int = 50,
    val isVolumeMuted: Boolean = false,
    val activeWindowTitle: String = "",
    val activeProcessName: String = "",
    val isBatteryPresent: Boolean = false,
    val batteryPercent: Int? = null,
    val isBatteryCharging: Boolean = false,
    val hostname: String = "PC-Host",
    val osName: String = "Windows",
    val activeTransportType: TransportType = TransportType.LAN_DIRECT,
    val roundTripLatencyMs: Long = 12,
    val isWanRelayActive: Boolean = false,
    val activeWindowBounds: WindowBounds? = null,
    val isSessionLocked: Boolean = false,
    val timestampEpoch: Long = System.currentTimeMillis()
)


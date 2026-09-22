package com.asistente.celular.nlu.pc.alert

enum class PcAlertCategory {
    GPU_OVERHEAT,
    RENDER_COMPLETED,
    RENDER_CRASHED,
    BUILD_FAILED,
    SECURITY,
    OTHER
}

enum class PcAlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class PcAlertAction(
    val id: String,
    val label: String,
    val dangerous: Boolean = false
)

data class PcProactiveAlert(
    val alertId: String,
    val category: PcAlertCategory,
    val title: String,
    val message: String,
    val severity: PcAlertSeverity = PcAlertSeverity.INFO,
    val actions: List<PcAlertAction> = emptyList(),
    val timestampEpoch: Long = System.currentTimeMillis()
)

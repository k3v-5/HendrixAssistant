package com.asistente.celular.pc

/**
 * Estados reactivos del ciclo de vida de la conexión entre el móvil y la PC.
 */
sealed interface PcConnectionState {
    data object Disconnected : PcConnectionState

    data class Connecting(
        val host: String,
        val port: Int
    ) : PcConnectionState

    data class Connected(
        val host: String,
        val port: Int,
        val latencyMs: Long = 0,
        val hostname: String = "PC-Host",
        val osName: String = "Windows",
        val transportType: com.asistente.celular.nlu.pc.TransportType = com.asistente.celular.nlu.pc.TransportType.LAN_DIRECT
    ) : PcConnectionState


    data class Reconnecting(
        val attempt: Int,
        val host: String,
        val port: Int
    ) : PcConnectionState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : PcConnectionState
}

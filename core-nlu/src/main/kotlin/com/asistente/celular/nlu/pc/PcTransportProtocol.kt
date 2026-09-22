package com.asistente.celular.nlu.pc

/**
 * Tipo de transporte físico o lógico para la conexión con la PC.
 */
enum class TransportType {
    /** Enlace directo en la misma red de área local WiFi (latencia 2-5 ms). */
    LAN_DIRECT,

    /** Enlace seguro cifrado de extremo a extremo a través de internet (4G/5G/WAN). */
    GLOBAL_TUNNEL_WAN,

    /** Simulación o pruebas locales en memoria. */
    MOCK_SIMULATION
}

/**
 * Configuración persistente de enlace con la computadora.
 * Permite emparejar una sola vez ('Enroll Once, Connect Anywhere')
 * almacenando tanto la ruta LAN como el túnel seguro para acceso fuera de casa.
 */
data class PcEndpointConfig(
    val hostname: String = "PC-Host",
    val localIp: String = "192.168.1.100",
    val port: Int = 8899,
    val remoteTunnelUrl: String? = null,
    val deviceToken: String = "",
    val pin: String = "",
    val isPaired: Boolean = false,
    val lastConnectedEpoch: Long = 0,
    val macAddress: String? = null,
    val airSyncPort: Int = 8900,
    val otaPort: Int = 8901,
    val snapshotQuality: Int = 75,
    val telemetryIntervalMs: Long = 3000L,
    val autoPasteDefault: Boolean = true
)

/**
 * Rectángulo de coordenadas absolutas de una ventana en el monitor de Windows.
 */
data class WindowBounds(
    val left: Int = 0,
    val top: Int = 0,
    val width: Int = 1920,
    val height: Int = 1080
)

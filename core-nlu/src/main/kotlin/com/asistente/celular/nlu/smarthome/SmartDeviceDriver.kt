package com.asistente.celular.nlu.smarthome

/**
 * Acción a ejecutar sobre un dispositivo inteligente.
 */
sealed interface DeviceAction {
    data object TurnOn : DeviceAction
    data object TurnOff : DeviceAction
    data object Toggle : DeviceAction
    data class SetBrightness(val percent: Int) : DeviceAction
    data class SetColor(val colorRgb: Int) : DeviceAction
    data class SetColorTemperature(val kelvin: Int) : DeviceAction
    data class Custom(val command: String, val params: List<Any> = emptyList()) : DeviceAction
}

/**
 * Resultado de ejecutar una acción sobre un dispositivo.
 */
data class DeviceActionResult(
    val success: Boolean,
    val message: String,
    val updatedProperties: Map<String, String> = emptyMap()
)

/**
 * Contrato extensible para drivers de dispositivos inteligentes (OCP).
 * Permite soportar diferentes marcas o protocolos (Yeelight LAN, miIO, Home Assistant, etc.).
 */
interface SmartDeviceDriver {
    val supportedProtocol: SmartProtocol

    /**
     * Ejecuta una acción sobre el dispositivo dado de forma asíncrona.
     */
    suspend fun executeAction(device: SmartDevice, action: DeviceAction): DeviceActionResult

    /**
     * Consulta las propiedades en tiempo real del dispositivo (power, bright, rgb, etc.).
     */
    suspend fun queryStatus(device: SmartDevice): Map<String, String>?
}

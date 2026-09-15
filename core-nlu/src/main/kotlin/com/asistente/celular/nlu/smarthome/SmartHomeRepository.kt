package com.asistente.celular.nlu.smarthome

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato del repositorio y orquestador central de domótica y dispositivos inteligentes.
 */
interface SmartHomeRepository {
    /**
     * Flujo reactivo con la lista de dispositivos inteligentes registrados o recordados.
     */
    val devices: StateFlow<List<SmartDevice>>

    /**
     * Añade o actualiza un dispositivo en el repositorio.
     */
    suspend fun addOrUpdateDevice(device: SmartDevice)

    /**
     * Elimina un dispositivo por su identificador.
     */
    suspend fun removeDevice(id: String)

    /**
     * Obtiene un dispositivo por su ID.
     */
    suspend fun getDeviceById(id: String): SmartDevice?

    /**
     * Busca el dispositivo que mejor coincida con el nombre o alias dado.
     * Si no se especifica nombre y solo hay un foco configurado, devuelve ese único foco.
     */
    suspend fun findDeviceByName(nameOrAlias: String?): SmartDevice?

    /**
     * Realiza un escaneo activo de red WiFi local para descubrir dispositivos automáticamente.
     */
    suspend fun discoverDevices(): List<SmartDevice>

    /**
     * Ejecuta una acción sobre un dispositivo específico por nombre o sobre el dispositivo predeterminado.
     */
    suspend fun executeAction(targetName: String?, action: DeviceAction): DeviceActionResult
}

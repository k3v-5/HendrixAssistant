package com.asistente.celular.data

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartDeviceDriver
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.nlu.smarthome.SmartProtocol
import com.asistente.celular.skills.smarthome.YeelightLanDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Implementación de SmartHomeRepository con persistencia JSON local segura y soporte para drivers plug-and-play.
 */
class JsonSmartHomeRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val drivers: List<SmartDeviceDriver> = listOf(YeelightLanDriver())
) : SmartHomeRepository {

    private val file = File(context.filesDir, "smart_devices.json")
    private val mutex = Mutex()
    private val _devices = MutableStateFlow<List<SmartDevice>>(emptyList())
    override val devices: StateFlow<List<SmartDevice>> = _devices.asStateFlow()

    private val driverMap: Map<SmartProtocol, SmartDeviceDriver> = drivers.associateBy { it.supportedProtocol }

    init {
        scope.launch {
            loadDevices()
        }
    }

    override suspend fun addOrUpdateDevice(device: SmartDevice) {
        mutex.withLock {
            val current = _devices.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.id == device.id || it.ipAddress == device.ipAddress }
            if (existingIndex >= 0) {
                current[existingIndex] = device
            } else {
                current.add(device)
            }
            _devices.value = current
            saveDevicesLocked(current)
        }
    }

    override suspend fun removeDevice(id: String) {
        mutex.withLock {
            val updated = _devices.value.filter { it.id != id }
            _devices.value = updated
            saveDevicesLocked(updated)
        }
    }

    override suspend fun getDeviceById(id: String): SmartDevice? {
        return _devices.value.find { it.id == id }
    }

    override suspend fun findDeviceByName(nameOrAlias: String?): SmartDevice? {
        val currentList = _devices.value
        if (currentList.isEmpty()) return null

        if (nameOrAlias.isNullOrBlank()) {
            return currentList.firstOrNull()
        }

        val clean = nameOrAlias.lowercase().trim()
        return currentList.firstOrNull { it.matchesName(clean) }
            ?: currentList.firstOrNull { it.type == DeviceType.LIGHT }
            ?: currentList.firstOrNull()
    }

    override suspend fun discoverDevices(): List<SmartDevice> {
        val yeelightDriver = driverMap[SmartProtocol.YEELIGHT_LAN] as? YeelightLanDriver
            ?: YeelightLanDriver()

        val discovered = yeelightDriver.discoverDevices(timeoutMillis = 2000)

        if (discovered.isNotEmpty()) {
            for (dev in discovered) {
                addOrUpdateDevice(dev)
            }
        }
        return discovered
    }

    override suspend fun executeAction(targetName: String?, action: DeviceAction): DeviceActionResult {
        var device = findDeviceByName(targetName)

        // Si no hay dispositivos guardados, intentar descubrir automáticamente en el momento
        if (device == null && _devices.value.isEmpty()) {
            val newlyDiscovered = discoverDevices()
            device = newlyDiscovered.firstOrNull()
        }

        if (device == null) {
            val nameInfo = if (!targetName.isNullOrBlank()) "con el nombre '$targetName'" else "registrado"
            return DeviceActionResult(
                success = false,
                message = "No encontré ningún foco inteligente Xiaomi $nameInfo en la red WiFi local."
            )
        }

        val driver = driverMap[device.protocol]
            ?: return DeviceActionResult(
                success = false,
                message = "Protocolo no soportado: ${device.protocol.displayName}"
            )

        val result = driver.executeAction(device, action)

        if (result.success && result.updatedProperties.isNotEmpty()) {
            val updatedProps = device.properties.toMutableMap()
            updatedProps.putAll(result.updatedProperties)
            val updatedDevice = device.copy(properties = updatedProps)
            addOrUpdateDevice(updatedDevice)
        }

        return result
    }

    private suspend fun loadDevices() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!file.exists()) {
                _devices.value = emptyList()
                return@withLock
            }

            try {
                val jsonStr = file.readText()
                val array = JSONArray(jsonStr)
                val list = mutableListOf<SmartDevice>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val aliasesArray = obj.optJSONArray("aliases") ?: JSONArray()
                    val aliases = (0 until aliasesArray.length()).map { aliasesArray.getString(it) }

                    val propsObj = obj.optJSONObject("properties") ?: JSONObject()
                    val props = mutableMapOf<String, String>()
                    val keys = propsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        props[key] = propsObj.getString(key)
                    }

                    val protocolName = obj.optString("protocol", SmartProtocol.YEELIGHT_LAN.name)
                    val protocol = try { SmartProtocol.valueOf(protocolName) } catch (_: Exception) { SmartProtocol.YEELIGHT_LAN }

                    val typeName = obj.optString("type", DeviceType.LIGHT.name)
                    val type = try { DeviceType.valueOf(typeName) } catch (_: Exception) { DeviceType.LIGHT }

                    list.add(
                        SmartDevice(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            aliases = aliases,
                            type = type,
                            ipAddress = obj.getString("ipAddress"),
                            port = obj.optInt("port", 55443),
                            protocol = protocol,
                            isOnline = obj.optBoolean("isOnline", true),
                            properties = props
                        )
                    )
                }

                _devices.value = list
                Log.d(TAG, "Cargados ${list.size} dispositivos inteligentes desde almacenamiento.")
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando dispositivos inteligentes: ${e.message}", e)
                _devices.value = emptyList()
            }
        }
    }

    private fun saveDevicesLocked(list: List<SmartDevice>) {
        try {
            val array = JSONArray()
            for (dev in list) {
                val obj = JSONObject().apply {
                    put("id", dev.id)
                    put("name", dev.name)
                    put("aliases", JSONArray(dev.aliases))
                    put("type", dev.type.name)
                    put("ipAddress", dev.ipAddress)
                    put("port", dev.port)
                    put("protocol", dev.protocol.name)
                    put("isOnline", dev.isOnline)
                    val propsObj = JSONObject()
                    dev.properties.forEach { (k, v) -> propsObj.put(k, v) }
                    put("properties", propsObj)
                }
                array.put(obj)
            }

            val tempFile = File(context.filesDir, "smart_devices.json.tmp")
            tempFile.writeText(array.toString(2))
            if (tempFile.renameTo(file)) {
                Log.d(TAG, "Dispositivos inteligentes guardados atómicamente.")
            } else {
                file.writeText(array.toString(2))
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando dispositivos inteligentes: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "JsonSmartHomeRepo"
    }
}

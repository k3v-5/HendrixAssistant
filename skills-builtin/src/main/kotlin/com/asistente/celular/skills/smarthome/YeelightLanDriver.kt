package com.asistente.celular.skills.smarthome

import android.util.Log
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartDeviceDriver
import com.asistente.celular.nlu.smarthome.SmartProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Driver para focos y lámparas inteligentes Xiaomi / Yeelight que admiten el protocolo "LAN Control".
 * Se comunica directamente en la red local mediante sockets TCP (puerto 55443) y descubrimiento SSDP UDP.
 * 100% Offline-first, sin nubes ni tokens de autenticación externos.
 */
class YeelightLanDriver(
    private val socketTimeoutMillis: Int = 2500
) : SmartDeviceDriver {

    override val supportedProtocol: SmartProtocol = SmartProtocol.YEELIGHT_LAN
    private val commandIdCounter = AtomicInteger(1)

    override suspend fun executeAction(device: SmartDevice, action: DeviceAction): DeviceActionResult = withContext(Dispatchers.IO) {
        val (method, params) = when (action) {
            is DeviceAction.TurnOn -> "set_power" to listOf("on", "smooth", 500)
            is DeviceAction.TurnOff -> "set_power" to listOf("off", "smooth", 500)
            is DeviceAction.Toggle -> "toggle" to emptyList<Any>()
            is DeviceAction.SetBrightness -> "set_bright" to listOf(action.percent.coerceIn(1, 100), "smooth", 500)
            is DeviceAction.SetColor -> "set_rgb" to listOf(action.colorRgb and 0xFFFFFF, "smooth", 500)
            is DeviceAction.SetColorTemperature -> "set_ct_abx" to listOf(action.kelvin.coerceIn(1700, 6500), "smooth", 500)
            is DeviceAction.Custom -> action.command to action.params
        }

        try {
            val response = sendJsonRpcCommand(device.ipAddress, device.port, method, params)
            if (response != null && response.has("result")) {
                val updatedProps = when (action) {
                    is DeviceAction.TurnOn -> mapOf("power" to "on")
                    is DeviceAction.TurnOff -> mapOf("power" to "off")
                    is DeviceAction.SetBrightness -> mapOf("bright" to action.percent.coerceIn(1, 100).toString())
                    else -> emptyMap()
                }
                DeviceActionResult(
                    success = true,
                    message = "Comando ejecutado exitosamente en '${device.name}'.",
                    updatedProperties = updatedProps
                )
            } else {
                val errorMsg = response?.optJSONObject("error")?.optString("message") ?: "Sin respuesta del foco"
                DeviceActionResult(
                    success = false,
                    message = "Fallo al enviar comando a '${device.name}': $errorMsg"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comunicándose con foco en ${device.ipAddress}:${device.port} -> ${e.message}")
            DeviceActionResult(
                success = false,
                message = "No se pudo conectar con '${device.name}' en ${device.ipAddress}. Verifica que esté encendido desde la pared y conectado al WiFi."
            )
        }
    }

    override suspend fun queryStatus(device: SmartDevice): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            val response = sendJsonRpcCommand(device.ipAddress, device.port, "get_prop", listOf("power", "bright", "rgb", "ct", "name"))
            val results = response?.optJSONArray("result") ?: return@withContext null
            val props = mutableMapOf<String, String>()
            if (results.length() > 0) props["power"] = results.optString(0, "off")
            if (results.length() > 1) props["bright"] = results.optString(1, "100")
            if (results.length() > 2) props["rgb"] = results.optString(2, "0")
            if (results.length() > 3) props["ct"] = results.optString(3, "4000")
            if (results.length() > 4) props["name"] = results.optString(4, "")
            props
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo consultar estado de '${device.name}': ${e.message}")
            null
        }
    }

    /**
     * Descubre bombillas Xiaomi/Yeelight en la red local WiFi mediante SSDP Multicast UDP (puerto 1982).
     */
    suspend fun discoverDevices(timeoutMillis: Long = 2000): List<SmartDevice> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<SmartDevice>()
        val seenIps = mutableSetOf<String>()

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 800

            val message = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1982\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "ST: wifi_bulb\r\n"

            val sendData = message.toByteArray()
            val groupAddress = InetAddress.getByName("239.255.255.250")
            val sendPacket = DatagramPacket(sendData, sendData.size, groupAddress, 1982)

            // Enviar dos ráfagas para mayor confiabilidad en redes WiFi con interferencia
            socket.send(sendPacket)
            socket.send(sendPacket)

            val startTime = System.currentTimeMillis()
            val receiveBuffer = ByteArray(2048)

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)

                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val device = parseSsdpResponse(response)
                    if (device != null && seenIps.add(device.ipAddress)) {
                        discovered.add(device)
                        Log.i(TAG, "Dispositivo Yeelight/Xiaomi descubierto: '${device.name}' en ${device.ipAddress}")
                    }
                } catch (timeoutEx: java.net.SocketTimeoutException) {
                    // Continuar hasta agotar timeoutMillis
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Aviso durante descubrimiento SSDP Yeelight: ${e.message}")
        } finally {
            socket?.close()
        }

        discovered
    }

    private fun parseSsdpResponse(rawHeader: String): SmartDevice? {
        val lines = rawHeader.split("\r\n")
        val headers = mutableMapOf<String, String>()
        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex != -1) {
                val key = line.substring(0, colonIndex).trim().lowercase()
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }

        val location = headers["location"] ?: return null
        // Formato: yeelight://192.168.1.50:55443
        val uri = location.removePrefix("yeelight://")
        val parts = uri.split(":")
        val ip = parts.getOrNull(0) ?: return null
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 55443

        val id = headers["id"] ?: "yeelight_$ip"
        val model = headers["model"] ?: "color"
        val customName = headers["name"]?.takeIf { it.isNotBlank() } ?: "Foco Xiaomi ($model)"
        val power = headers["power"] ?: "off"
        val bright = headers["bright"] ?: "100"

        val properties = mutableMapOf(
            "power" to power,
            "bright" to bright,
            "model" to model
        )

        return SmartDevice(
            id = id,
            name = customName,
            aliases = listOf("foco", "luz", "foco xiaomi", "bombilla", "foco de la sala", "luz de la sala"),
            type = DeviceType.LIGHT,
            ipAddress = ip,
            port = port,
            protocol = SmartProtocol.YEELIGHT_LAN,
            isOnline = true,
            properties = properties
        )
    }

    private fun sendJsonRpcCommand(
        ip: String,
        port: Int,
        method: String,
        params: List<Any>
    ): JSONObject? {
        val id = commandIdCounter.incrementAndGet()
        val request = JSONObject().apply {
            put("id", id)
            put("method", method)
            put("params", JSONArray(params))
        }
        val requestStr = request.toString() + "\r\n"

        val socket = Socket()
        socket.soTimeout = socketTimeoutMillis
        socket.connect(InetSocketAddress(ip, port), socketTimeoutMillis)

        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        writer.write(requestStr)
        writer.flush()

        val responseStr = reader.readLine()
        socket.close()

        return if (!responseStr.isNullOrBlank()) {
            JSONObject(responseStr)
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "YeelightLanDriver"
    }
}

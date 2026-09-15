package com.asistente.celular.skills.smarthome

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartDeviceDriver
import com.asistente.celular.nlu.smarthome.SmartProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

/**
 * Driver para focos y lámparas inteligentes Xiaomi / Yeelight que admiten el protocolo "LAN Control".
 * Se comunica directamente en la red local mediante sockets TCP (puerto 55443) y descubrimiento SSDP UDP.
 * 100% Offline-first, sin nubes ni tokens de autenticación externos.
 */
class YeelightLanDriver(
    private val context: Context? = null,
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
            val hasResult = response?.has("result") == true
            val isPropsNotification = response?.optString("method") == "props"
            val hasError = response?.has("error") == true

            // Si el foco respondió con result, props o no reportó error, el comando fue ejecutado con éxito
            if (response != null && !hasError) {
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
                val errorMsg = response?.optJSONObject("error")?.optString("message") ?: "Sin confirmación del foco"
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
     * Descubre bombillas Xiaomi/Yeelight en la red local WiFi mediante SSDP Multicast UDP (puerto 1982)
     * con fallback automático a escaneo activo de subred en el puerto 55443.
     */
    suspend fun discoverDevices(timeoutMillis: Long = 2000): List<SmartDevice> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<SmartDevice>()
        val seenIps = mutableSetOf<String>()

        val wifiManager = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = try {
            wifiManager?.createMulticastLock("yeelight_ssdp_lock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo adquirir MulticastLock: ${e.message}")
            null
        }

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

            // Enviar ráfagas para mayor confiabilidad en redes WiFi
            socket.send(sendPacket)
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
                        Log.i(TAG, "Dispositivo Yeelight/Xiaomi descubierto vía SSDP: '${device.name}' en ${device.ipAddress}")
                    }
                } catch (timeoutEx: java.net.SocketTimeoutException) {
                    // Continuar hasta agotar timeoutMillis
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Aviso durante descubrimiento SSDP Yeelight: ${e.message}")
        } finally {
            socket?.close()
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }

        // Si el descubrimiento SSDP no arrojó resultados (frecuente en routers que bloquean multicast IGMP entre dispositivos WiFi),
        // realizamos un barrido TCP concurrente en el puerto 55443 sobre la subred local (/24).
        if (discovered.isEmpty()) {
            val subnetPrefix = getLocalSubnetPrefix()
            if (subnetPrefix != null) {
                Log.i(TAG, "SSDP sin resultados. Iniciando escaneo TCP de subred: ${subnetPrefix}0/24 en puerto 55443...")
                val probedDevices = probeSubnetForYeelight(subnetPrefix, seenIps)
                discovered.addAll(probedDevices)
            }
        }

        discovered
    }

    private fun getLocalSubnetPrefix(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = intf.inetAddresses ?: continue
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val lastDot = host.lastIndexOf('.')
                            if (lastDot > 0) {
                                return host.substring(0, lastDot + 1)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error detectando subred local: ${e.message}")
        }
        return null
    }

    private suspend fun probeSubnetForYeelight(prefix: String, seenIps: MutableSet<String>): List<SmartDevice> = withContext(Dispatchers.IO) {
        val found = mutableListOf<SmartDevice>()
        coroutineScope {
            val deferreds = (1..254).map { hostIndex ->
                async {
                    val ip = "$prefix$hostIndex"
                    if (seenIps.contains(ip)) return@async null
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(ip, 55443), 200)
                        socket.close()
                        ip
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val responsiveIps = deferreds.awaitAll().filterNotNull()
            for (ip in responsiveIps) {
                if (seenIps.add(ip)) {
                    val initialDevice = SmartDevice(
                        id = "yeelight_${ip.replace('.', '_')}",
                        name = "Foco Xiaomi",
                        aliases = listOf("foco", "luz", "bombilla", "foco xiaomi", "foco del cuarto", "luz del cuarto", "cuarto"),
                        type = DeviceType.LIGHT,
                        ipAddress = ip,
                        port = 55443,
                        protocol = SmartProtocol.YEELIGHT_LAN,
                        isOnline = true
                    )
                    val status = queryStatus(initialDevice)
                    val customName = status?.get("name")?.takeIf { it.isNotBlank() } ?: "Foco Xiaomi ($ip)"
                    val finalDevice = initialDevice.copy(
                        name = customName,
                        properties = status ?: emptyMap()
                    )
                    found.add(finalDevice)
                    Log.i(TAG, "Foco Yeelight detectado en subred local por sondeo TCP: $ip ($customName)")
                }
            }
        }
        found
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
            aliases = listOf("foco", "luz", "foco xiaomi", "bombilla", "foco de la sala", "luz de la sala", "foco del cuarto", "luz del cuarto", "cuarto", "sala"),
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

        var finalJson: JSONObject? = null
        try {
            Socket().use { socket ->
                socket.soTimeout = socketTimeoutMillis
                socket.connect(InetSocketAddress(ip, port), socketTimeoutMillis)

                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                writer.write(requestStr)
                writer.flush()

                // Leer respuestas: Yeelight suele enviar notificaciones "props" antes del "result"
                val deadline = System.currentTimeMillis() + socketTimeoutMillis
                while (System.currentTimeMillis() < deadline) {
                    val line = try {
                        reader.readLine()
                    } catch (_: Exception) {
                        null
                    } ?: break

                    if (line.isBlank()) continue
                    val parsed = try { JSONObject(line) } catch (_: Exception) { null } ?: continue

                    // Si encontramos la respuesta al id solicitado o con result, la priorizamos
                    if (parsed.has("result") || parsed.optInt("id") == id) {
                        finalJson = parsed
                        break
                    }

                    // Notificación de estado cambiado
                    if (parsed.optString("method") == "props") {
                        finalJson = parsed
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error comunicándose por JSON-RPC a $ip:$port -> ${e.message}")
        }

        return finalJson
    }

    companion object {
        private const val TAG = "YeelightLanDriver"
    }
}

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
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Driver universal para conectar Hendrix con un servidor local o remoto de Home Assistant vía REST API.
 * Cumple con OCP implementando SmartDeviceDriver para soporte plug-and-play de domótica abierta.
 */
class HomeAssistantDriver(
    private val defaultBaseUrl: String = "http://homeassistant.local:8123",
    private val defaultAccessToken: String? = null,
    private val connectionTimeoutMillis: Int = 3000,
    private val readTimeoutMillis: Int = 4000
) : SmartDeviceDriver {

    override val supportedProtocol: SmartProtocol = SmartProtocol.HOME_ASSISTANT

    override suspend fun executeAction(device: SmartDevice, action: DeviceAction): DeviceActionResult = withContext(Dispatchers.IO) {
        val baseUrl = device.metadata["ha_base_url"]?.takeIf { it.isNotBlank() } ?: defaultBaseUrl
        val token = device.metadata["ha_token"]?.takeIf { it.isNotBlank() } ?: defaultAccessToken

        val entityId = device.id.ifBlank { device.metadata["entity_id"] ?: "" }
        if (entityId.isBlank()) {
            return@withContext DeviceActionResult(
                success = false,
                message = "El dispositivo '${device.name}' no tiene configurado un entity_id de Home Assistant."
            )
        }

        val domain = entityId.substringBefore('.', "light")
        val (service, serviceData) = mapActionToService(domain, entityId, action)

        try {
            val endpoint = "$baseUrl/api/services/$domain/$service"
            val response = executeHttpRequest(
                method = "POST",
                urlString = endpoint,
                token = token,
                body = serviceData.toString()
            )

            if (response.statusCode in 200..299) {
                val updatedProps = when (action) {
                    is DeviceAction.TurnOn -> mapOf("power" to "on")
                    is DeviceAction.TurnOff -> mapOf("power" to "off")
                    is DeviceAction.SetBrightness -> mapOf("bright" to action.percent.coerceIn(1, 100).toString(), "power" to "on")
                    else -> emptyMap()
                }
                DeviceActionResult(
                    success = true,
                    message = "Acción ejecutada en '${device.name}' vía Home Assistant.",
                    updatedProperties = updatedProps
                )
            } else {
                DeviceActionResult(
                    success = false,
                    message = "Home Assistant devolvió código HTTP ${response.statusCode}: ${response.body.take(100)}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comunicándose con Home Assistant en $baseUrl: ${e.message}", e)
            DeviceActionResult(
                success = false,
                message = "No se pudo conectar con Home Assistant en $baseUrl (${e.localizedMessage ?: "error de red"})."
            )
        }
    }

    override suspend fun queryStatus(device: SmartDevice): Map<String, String>? = withContext(Dispatchers.IO) {
        val baseUrl = device.metadata["ha_base_url"]?.takeIf { it.isNotBlank() } ?: defaultBaseUrl
        val token = device.metadata["ha_token"]?.takeIf { it.isNotBlank() } ?: defaultAccessToken
        val entityId = device.id.ifBlank { device.metadata["entity_id"] ?: "" }
        if (entityId.isBlank()) return@withContext null

        try {
            val endpoint = "$baseUrl/api/states/$entityId"
            val response = executeHttpRequest(method = "GET", urlString = endpoint, token = token)
            if (response.statusCode in 200..299 && response.body.isNotBlank()) {
                val json = JSONObject(response.body)
                val state = json.optString("state", "off")
                val attributes = json.optJSONObject("attributes") ?: JSONObject()

                val props = mutableMapOf<String, String>()
                props["power"] = if (state.equals("on", ignoreCase = true)) "on" else "off"

                val haBrightness = attributes.optInt("brightness", -1)
                if (haBrightness >= 0) {
                    val percent = (haBrightness * 100) / 255
                    props["bright"] = percent.coerceIn(1, 100).toString()
                }

                attributes.optString("friendly_name", "").takeIf { it.isNotBlank() }?.let {
                    props["name"] = it
                }
                return@withContext props
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando estado de $entityId en HA: ${e.message}")
        }
        null
    }

    /**
     * Descubre y lista todas las entidades compatibles (luces, switches) expuestas por la instancia de Home Assistant.
     */
    suspend fun discoverEntities(
        baseUrl: String = defaultBaseUrl,
        token: String? = defaultAccessToken
    ): List<SmartDevice> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<SmartDevice>()
        try {
            val endpoint = "$baseUrl/api/states"
            val response = executeHttpRequest(method = "GET", urlString = endpoint, token = token)
            if (response.statusCode in 200..299 && response.body.isNotBlank()) {
                val jsonArray = JSONArray(response.body)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val entityId = item.optString("entity_id")
                    if (entityId.startsWith("light.") || entityId.startsWith("switch.")) {
                        val state = item.optString("state", "off")
                        val attrs = item.optJSONObject("attributes") ?: JSONObject()
                        val friendlyName = attrs.optString("friendly_name", entityId)
                        val devType = if (entityId.startsWith("light.")) DeviceType.LIGHT else DeviceType.SWITCH

                        val props = mutableMapOf(
                            "power" to if (state.equals("on", ignoreCase = true)) "on" else "off"
                        )
                        val haBright = attrs.optInt("brightness", -1)
                        if (haBright >= 0) {
                            props["bright"] = ((haBright * 100) / 255).coerceIn(1, 100).toString()
                        }

                        discovered.add(
                            SmartDevice(
                                id = entityId,
                                name = friendlyName,
                                aliases = listOf(friendlyName.lowercase(), entityId.substringAfter('.').lowercase()),
                                type = devType,
                                ipAddress = baseUrl.substringAfter("://").substringBefore(':').substringBefore('/'),
                                port = baseUrl.substringAfterLast(':', "8123").substringBefore('/').toIntOrNull() ?: 8123,
                                protocol = SmartProtocol.HOME_ASSISTANT,
                                isOnline = state != "unavailable",
                                properties = props,
                                metadata = mapOf(
                                    "entity_id" to entityId,
                                    "ha_base_url" to baseUrl
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error descubriendo entidades en Home Assistant: ${e.message}")
        }
        discovered
    }

    private fun mapActionToService(domain: String, entityId: String, action: DeviceAction): Pair<String, JSONObject> {
        val json = JSONObject().apply {
            put("entity_id", entityId)
        }

        return when (action) {
            is DeviceAction.TurnOn -> "turn_on" to json
            is DeviceAction.TurnOff -> "turn_off" to json
            is DeviceAction.Toggle -> "toggle" to json
            is DeviceAction.SetBrightness -> {
                json.put("brightness_pct", action.percent.coerceIn(1, 100))
                "turn_on" to json
            }
            is DeviceAction.SetColor -> {
                val r = (action.colorRgb shr 16) and 0xFF
                val g = (action.colorRgb shr 8) and 0xFF
                val b = action.colorRgb and 0xFF
                val rgbArray = JSONArray().apply {
                    put(r)
                    put(g)
                    put(b)
                }
                json.put("rgb_color", rgbArray)
                "turn_on" to json
            }
            is DeviceAction.SetColorTemperature -> {
                json.put("kelvin", action.kelvin.coerceIn(2000, 6500))
                "turn_on" to json
            }
            is DeviceAction.StartColorFlow -> {
                json.put("effect", "colorloop")
                "turn_on" to json
            }
            is DeviceAction.StopColorFlow -> {
                json.put("effect", "none")
                "turn_on" to json
            }
            is DeviceAction.Custom -> {
                val servicePart = if (action.command.contains('.')) action.command.substringAfter('.') else action.command
                servicePart to json
            }
        }
    }

    private fun executeHttpRequest(
        method: String,
        urlString: String,
        token: String?,
        body: String? = null
    ): HttpResponse {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = connectionTimeoutMillis
            conn.readTimeout = readTimeoutMillis
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }

            if (body != null && (method == "POST" || method == "PUT")) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val statusCode = conn.responseCode
            val inputStream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val content = inputStream?.let { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            } ?: ""

            return HttpResponse(statusCode, content)
        } finally {
            conn.disconnect()
        }
    }

    private data class HttpResponse(val statusCode: Int, val body: String)

    companion object {
        private const val TAG = "HomeAssistantDriver"
    }
}

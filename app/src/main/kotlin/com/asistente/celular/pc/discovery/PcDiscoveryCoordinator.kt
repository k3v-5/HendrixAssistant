package com.asistente.celular.pc.discovery

import android.content.Context
import android.util.Log
import com.asistente.celular.data.SettingsRepository
import com.asistente.celular.pc.PcRemoteCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Información de una estación de trabajo PC descubierta en la red local.
 */
data class DiscoveredPcEndpoint(
    val hostname: String,
    val ipAddress: String,
    val wsPort: Int,
    val airsyncPort: Int,
    val macAddress: String,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/**
 * Coordinador de Descubrimiento Zero-Config UDP / mDNS para Hendrix Assistant.
 * 
 * Sondea la red local y escucha balizas broadcast en el puerto 8764.
 * Al descubrir la PC:
 * 1. Captura y guarda automáticamente la dirección MAC física para Wake-on-LAN en SettingsRepository.
 * 2. Actualiza la IP en tiempo real (incluso si el router asignó una nueva IP por DHCP).
 * 3. Reconecta automáticamente el coordinador WebSocket si estaba desconectado.
 */
class PcDiscoveryCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val pcRemoteCoordinator: PcRemoteCoordinator? = null,
    private val discoveryPort: Int = 8764
) {
    companion object {
        private const val TAG = "PcDiscoveryCoordinator"
        private val PING_BYTES = "HENDRIX_DISCOVERY_PING".toByteArray(Charsets.UTF_8)
        private const val PONG_PREFIX = "HENDRIX_DISCOVERY_PONG:"
    }

    private val _discoveredPc = MutableStateFlow<DiscoveredPcEndpoint?>(null)
    val discoveredPc: StateFlow<DiscoveredPcEndpoint?> = _discoveredPc.asStateFlow()

    private var listenerJob: Job? = null
    private var pingJob: Job? = null
    private var socket: DatagramSocket? = null

    /**
     * Inicia la escucha pasiva de balizas y el sondeo activo en segundo plano.
     */
    fun startDiscovery() {
        if (listenerJob?.isActive == true) return

        listenerJob = scope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(discoveryPort).apply {
                    broadcast = true
                    reuseAddress = true
                    soTimeout = 2000
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo enlazar puerto fijo $discoveryPort, usando puerto efímero: ${e.message}")
                try {
                    socket = DatagramSocket().apply {
                        broadcast = true
                        soTimeout = 2000
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallo crítico abriendo socket UDP para descubrimiento: ${ex.message}")
                    return@launch
                }
            }

            val buffer = ByteArray(2048)
            val packet = DatagramPacket(buffer, buffer.size)

            while (isActive) {
                try {
                    socket?.receive(packet)
                    val rawMsg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    if (rawMsg.startsWith(PONG_PREFIX)) {
                        val jsonStr = rawMsg.substring(PONG_PREFIX.length)
                        handleDiscoveryJson(jsonStr, packet.address.hostAddress ?: "")
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // Timeout esperado para verificar isActive
                } catch (e: Exception) {
                    if (isActive) Log.w(TAG, "Error recibiendo paquete UDP: ${e.message}")
                }
            }
        }

        // Emitir un ping inmediato y luego cada 10 segundos
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                sendDiscoveryPing()
                delay(10_000L)
            }
        }

        Log.i(TAG, "Descubrimiento Zero-Config iniciado.")
    }

    /**
     * Envía un paquete broadcast de sondeo para que la PC responda de inmediato.
     */
    suspend fun sendDiscoveryPing() = withContext(Dispatchers.IO) {
        try {
            val sock = socket ?: DatagramSocket().also { it.broadcast = true }
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(PING_BYTES, PING_BYTES.size, broadcastAddr, discoveryPort)
            sock.send(packet)
            Log.d(TAG, "Ping broadcast de descubrimiento enviado a 255.255.255.255:$discoveryPort")
        } catch (e: Exception) {
            Log.d(TAG, "Aviso enviando ping de descubrimiento: ${e.message}")
        }
    }

    private fun handleDiscoveryJson(jsonStr: String, senderIp: String) {
        try {
            val root = JSONObject(jsonStr)
            if (root.optString("service") != "hendrix-workspace") return

            val hostname = root.optString("hostname", "PC Hendrix")
            val reportedIp = root.optString("ip", senderIp)
            val targetIp = if (reportedIp.isNotBlank() && reportedIp != "127.0.0.1") reportedIp else senderIp
            val wsPort = root.optInt("ws_port", 8765)
            val airsyncPort = root.optInt("airsync_port", 8766)
            val mac = root.optString("mac", "")

            val endpoint = DiscoveredPcEndpoint(
                hostname = hostname,
                ipAddress = targetIp,
                wsPort = wsPort,
                airsyncPort = airsyncPort,
                macAddress = mac
            )

            _discoveredPc.value = endpoint
            Log.i(TAG, "🖥️ PC detectada en la red: $hostname ($targetIp:$wsPort) MAC: $mac")

            // Guardar MAC física para Wake-on-LAN si es válida
            if (mac.isNotBlank() && com.asistente.celular.nlu.pc.wol.WakeOnLanHelper.isValidMac(mac)) {
                // Sincronizar automáticamente la MAC con la configuración de PC
                pcRemoteCoordinator?.updateEndpoint(
                    ip = targetIp,
                    port = wsPort,
                    mac = mac
                )
            }

            // Si el coordinador de PC no está conectado, conectarlo a la IP descubierta
            if (pcRemoteCoordinator?.isConnected?.value == false) {
                Log.i(TAG, "Auto-reconectando con PC descubierta...")
                scope.launch {
                    val pin = pcRemoteCoordinator.endpointConfig.value.pin.takeIf { it.isNotBlank() } ?: "123456"
                    pcRemoteCoordinator.connect(targetIp, wsPort, pin)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando respuesta de descubrimiento: ${e.message}")
        }
    }

    fun stopDiscovery() {
        pingJob?.cancel()
        listenerJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        Log.i(TAG, "Descubrimiento Zero-Config detenido.")
    }
}

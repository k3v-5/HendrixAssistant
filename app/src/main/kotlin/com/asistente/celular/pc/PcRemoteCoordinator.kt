package com.asistente.celular.pc

import android.content.Context
import android.util.Log
import com.asistente.celular.nlu.pc.AntigravityChat
import com.asistente.celular.nlu.pc.AntigravityProject
import com.asistente.celular.nlu.pc.AntigravityTargetMode
import com.asistente.celular.nlu.pc.daw.DawAction
import com.asistente.celular.nlu.pc.daw.DawActionRequest
import com.asistente.celular.nlu.pc.daw.DawActionResult
import com.asistente.celular.nlu.pc.daw.DawProjectInfo
import com.asistente.celular.nlu.pc.daw.DawType
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.AutonomousTaskStep
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcEndpointConfig
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWindowInfo
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.TaskStepStatus
import com.asistente.celular.nlu.pc.TransportType
import com.asistente.celular.nlu.pc.clipboard.PcClipboardPayload
import com.asistente.celular.nlu.pc.audio.PcAudioAppSession
import com.asistente.celular.nlu.pc.audio.PcAudioMixerState
import com.asistente.celular.nlu.pc.scene.PcStudioScene
import com.asistente.celular.nlu.pc.scene.PcStudioSceneRegistry
import com.asistente.celular.nlu.pc.scene.PcSceneExecutionResult
import com.asistente.celular.nlu.pc.project.PcProjectItem
import com.asistente.celular.nlu.pc.project.PcProjectCategory
import com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry
import com.asistente.celular.nlu.pc.hardware.PcRenderWatchdogEvent
import com.asistente.celular.nlu.pc.copilot.PcScreenAnalysisResult
import com.asistente.celular.nlu.pc.plugin.PcPluginAction
import com.asistente.celular.nlu.pc.plugin.PcPluginActionResult
import com.asistente.celular.nlu.pc.plugin.PcPluginDefinition
import com.asistente.celular.nlu.pc.plugin.PcPluginParam
import com.asistente.celular.nlu.pc.workspace.PcWorkspaceContext
import com.asistente.celular.nlu.pc.workspace.PcGitRepositoryStatus
import com.asistente.celular.nlu.pc.workspace.PcRunningCreativeProcess
import com.asistente.celular.nlu.pc.workspace.PcTerminalErrorAlert
import com.asistente.celular.nlu.pc.airsync.AirSyncSharedFile
import com.asistente.celular.nlu.pc.airsync.AirSyncTransfer
import com.asistente.celular.nlu.pc.airsync.AirSyncTransferState
import com.asistente.celular.nlu.pc.airsync.AirSyncDirection
import com.asistente.celular.pc.airsync.AirSyncClient
import com.asistente.celular.ai.client.LlmClient

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Coordinador de cliente de red y transporte de alta velocidad para el Hendrix Remote PC Workspace.
 * Implementa [PcWorkspaceBridge] gestionando la conexión WebSocket bidireccional con la PC
 * con soporte de reconexión automática, tramas binarias de imágenes WebP y telemetría reactiva.
 */
class PcRemoteCoordinator(
    private val context: Context,
    private val scope: CoroutineScope
) : PcWorkspaceBridge {

    companion object {
        private const val TAG = "PcRemoteCoordinator"
        private const val DEFAULT_PORT = 8899
        private const val DEFAULT_TIMEOUT_MS = 5000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket no debe expirar por inactividad
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var telemetryPollingJob: Job? = null
    private var isUserExplicitDisconnect = false
    private var reconnectJob: Job? = null
    private val snapshotMutex = kotlinx.coroutines.sync.Mutex()
    private var inFlightSnapshotDeferred: CompletableDeferred<ByteArray?>? = null

    private var currentHost: String = "192.168.100.159"
    private var currentPort: Int = DEFAULT_PORT
    private var authToken: String = ""

    private val prefs = context.getSharedPreferences("pc_remote_prefs", Context.MODE_PRIVATE)

    private val _endpointConfig = MutableStateFlow(loadSavedConfig())
    val endpointConfig: StateFlow<PcEndpointConfig> = _endpointConfig.asStateFlow()

    private val _activeTransport = MutableStateFlow(TransportType.LAN_DIRECT)
    val activeTransport: StateFlow<TransportType> = _activeTransport.asStateFlow()

    val focusActiveWindow = MutableStateFlow(false)
    var llmClientProvider: (() -> LlmClient?)? = null

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    private var pendingSnapshotDeferred: CompletableDeferred<ByteArray?>? = null

    private val _currentMode = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
    override val currentMode: StateFlow<PcOperationMode> = _currentMode.asStateFlow()

    private val _telemetry = MutableStateFlow<PcSystemTelemetry?>(null)
    override val telemetry: StateFlow<PcSystemTelemetry?> = _telemetry.asStateFlow()

    private val _latestSnapshot = MutableStateFlow<ByteArray?>(null)
    override val latestSnapshot: StateFlow<ByteArray?> = _latestSnapshot.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _dropzoneEvents = MutableStateFlow<com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile?>(null)
    override val dropzoneEvents: StateFlow<com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile?> = _dropzoneEvents.asStateFlow()

    private val _clipboardState = MutableStateFlow<PcClipboardPayload?>(null)
    override val clipboardState: StateFlow<PcClipboardPayload?> = _clipboardState.asStateFlow()

    private val _audioMixerState = MutableStateFlow<PcAudioMixerState?>(null)
    override val audioMixerState: StateFlow<PcAudioMixerState?> = _audioMixerState.asStateFlow()

    private val _hardwareTelemetry = MutableStateFlow<PcHardwareTelemetry?>(null)
    override val hardwareTelemetry: StateFlow<PcHardwareTelemetry?> = _hardwareTelemetry.asStateFlow()

    private val _watchdogEvents = MutableStateFlow<PcRenderWatchdogEvent?>(null)
    override val watchdogEvents: StateFlow<PcRenderWatchdogEvent?> = _watchdogEvents.asStateFlow()

    val audioStreamPlayer = com.asistente.celular.pc.audio.PcAudioStreamPlayer()

    private val _proactiveAlerts = MutableStateFlow<com.asistente.celular.nlu.pc.alert.PcProactiveAlert?>(null)
    override val proactiveAlerts: StateFlow<com.asistente.celular.nlu.pc.alert.PcProactiveAlert?> = _proactiveAlerts.asStateFlow()

    private val _audioStreamState = MutableStateFlow(com.asistente.celular.nlu.pc.audio.PcAudioStreamState())
    override val audioStreamState: StateFlow<com.asistente.celular.nlu.pc.audio.PcAudioStreamState> = _audioStreamState.asStateFlow()

    private val _connectionState = MutableStateFlow<PcConnectionState>(PcConnectionState.Disconnected)
    val connectionState: StateFlow<PcConnectionState> = _connectionState.asStateFlow()

    private val _foregroundApp = MutableStateFlow("")
    override val foregroundApp: StateFlow<String> = _foregroundApp.asStateFlow()

    private val _terminalErrorAlerts = MutableStateFlow<PcTerminalErrorAlert?>(null)
    override val terminalErrorAlerts: StateFlow<PcTerminalErrorAlert?> = _terminalErrorAlerts.asStateFlow()

    private val _airSyncTransfers = MutableStateFlow<List<AirSyncTransfer>>(emptyList())
    override val airSyncTransfers: StateFlow<List<AirSyncTransfer>> = _airSyncTransfers.asStateFlow()

    private val _airSyncSharedFiles = MutableStateFlow<List<AirSyncSharedFile>>(emptyList())
    override val airSyncSharedFiles: StateFlow<List<AirSyncSharedFile>> = _airSyncSharedFiles.asStateFlow()

    private val _openWindows = MutableStateFlow<List<PcWindowInfo>>(emptyList())
    val openWindows: StateFlow<List<PcWindowInfo>> = _openWindows.asStateFlow()

    private val airSyncClient = AirSyncClient()
    private var airSyncPort: Int = 8900

    private val requestSigner: com.asistente.celular.nlu.security.RemoteRequestSigner =
        com.asistente.celular.nlu.security.HmacSha256Signer()

    fun sendSignedPayload(payload: JSONObject, ws: WebSocket? = activeWebSocket): Boolean {
        val socket = ws ?: return false
        if (authToken.isNotBlank()) {
            requestSigner.sign(payload, authToken)
        }
        return socket.send(payload.toString())
    }

    init {
        authToken = _endpointConfig.value.deviceToken
        airSyncPort = _endpointConfig.value.airSyncPort
        com.asistente.celular.pc.alert.PcNotificationActionReceiver.activeBridge = this
        // Inicializar con telemetría base por defecto para pruebas y arranque limpio
        _telemetry.value = PcSystemTelemetry(
            cpuPercent = 14.5f,
            ramPercent = 42.0f,
            ramUsedGb = 6.7f,
            ramTotalGb = 16.0f,
            masterVolumePercent = 65,
            activeWindowTitle = "Visual Studio Code - HendrixAssistant",
            activeProcessName = "Code.exe",
            hostname = _endpointConfig.value.hostname,
            osName = "Windows 11 Pro",
            activeTransportType = _activeTransport.value
        )
    }

    private fun loadSavedConfig(): PcEndpointConfig {
        val host = prefs.getString("pc_host", "192.168.100.159") ?: "192.168.100.159"
        val port = prefs.getInt("pc_port", DEFAULT_PORT)
        val tunnelUrl = prefs.getString("pc_tunnel_url", null)
        val token = prefs.getString("pc_token", "") ?: ""
        val pin = prefs.getString("pc_pin", "123456") ?: "123456"
        val hostname = prefs.getString("pc_hostname", "PC-Workstation") ?: "PC-Workstation"
        val isPaired = prefs.getBoolean("pc_is_paired", false)
        val mac = prefs.getString("pc_mac_address", null)
        val airSyncPort = prefs.getInt("pc_airsync_port", 8900)
        val snapshotQuality = prefs.getInt("pc_snapshot_quality", 75)
        val telemetryIntervalMs = prefs.getLong("pc_telemetry_interval_ms", 3000L)
        val autoPasteDefault = prefs.getBoolean("pc_auto_paste_default", true)

        return PcEndpointConfig(
            hostname = hostname,
            localIp = host,
            port = port,
            remoteTunnelUrl = tunnelUrl,
            deviceToken = token,
            pin = pin,
            isPaired = isPaired,
            macAddress = mac,
            airSyncPort = airSyncPort,
            snapshotQuality = snapshotQuality,
            telemetryIntervalMs = telemetryIntervalMs,
            autoPasteDefault = autoPasteDefault
        )
    }

    fun saveConfig(newConfig: PcEndpointConfig) {
        _endpointConfig.value = newConfig
        airSyncPort = newConfig.airSyncPort
        prefs.edit()
            .putString("pc_host", newConfig.localIp)
            .putInt("pc_port", newConfig.port)
            .putString("pc_tunnel_url", newConfig.remoteTunnelUrl)
            .putString("pc_token", newConfig.deviceToken)
            .putString("pc_pin", newConfig.pin)
            .putString("pc_hostname", newConfig.hostname)
            .putBoolean("pc_is_paired", newConfig.isPaired)
            .putString("pc_mac_address", newConfig.macAddress)
            .putInt("pc_airsync_port", newConfig.airSyncPort)
            .putInt("pc_snapshot_quality", newConfig.snapshotQuality)
            .putLong("pc_telemetry_interval_ms", newConfig.telemetryIntervalMs)
            .putBoolean("pc_auto_paste_default", newConfig.autoPasteDefault)
            .apply()
    }

    fun updateEndpoint(ip: String, port: Int, pin: String? = null, mac: String? = null) {
        val current = _endpointConfig.value
        val updated = current.copy(
            localIp = ip,
            port = port,
            pin = pin ?: current.pin,
            macAddress = mac ?: current.macAddress
        )
        saveConfig(updated)
    }

    /**
     * Conecta de forma inteligente: primero prueba LAN directa (< 4s),
     * y si no está en la misma red y cuenta con túnel WAN, conmuta automáticamente.
     */
    suspend fun connectAuto(): Boolean = withContext(Dispatchers.IO) {
        val conf = _endpointConfig.value
        Log.i(TAG, "Iniciando conexión automática híbrida con ${conf.hostname}")

        // 1. Intentar por LAN directa primero (4s de gracia para ARP y handshake)
        val lanSuccess = withTimeoutOrNull(4000L) {
            connect(conf.localIp, conf.port, conf.pin.takeIf { it.isNotBlank() })
        } ?: false

        if (lanSuccess) {
            _activeTransport.value = TransportType.LAN_DIRECT
            return@withContext true
        }

        // 2. Si falló la red local y tenemos túnel WAN (ej. estamos con datos 4G/5G)
        val tunnel = conf.remoteTunnelUrl
        if (!tunnel.isNullOrBlank()) {
            Log.i(TAG, "Conmutando automáticamente al túnel WAN global seguro: $tunnel")
            val wanSuccess = connect(tunnel, conf.port, conf.pin.takeIf { it.isNotBlank() })
            if (wanSuccess) {
                _activeTransport.value = TransportType.GLOBAL_TUNNEL_WAN
                return@withContext true
            }
        }

        false
    }

    override suspend fun connect(host: String, port: Int, pin: String?): Boolean = withContext(Dispatchers.IO) {
        currentHost = host
        currentPort = port
        isUserExplicitDisconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        _connectionState.value = PcConnectionState.Connecting(host, port)

        val cleanHost = host.trim()
        val isDirectUrl = cleanHost.startsWith("ws://", ignoreCase = true) || cleanHost.startsWith("wss://", ignoreCase = true)

        val wsUrl = if (isDirectUrl) {
            cleanHost
        } else {
            val stripped = cleanHost.removePrefix("http://").removePrefix("https://").substringBefore("/")
            val parsedHost = if (stripped.contains(":") && !stripped.contains("[")) stripped.substringBefore(":") else stripped
            val parsedPort = if (stripped.contains(":") && !stripped.contains("[")) stripped.substringAfter(":").toIntOrNull() ?: port else port
            "ws://$parsedHost:$parsedPort/ws"
        }

        val isWan = wsUrl.startsWith("wss://", ignoreCase = true) ||
            wsUrl.contains("trycloudflare.com") ||
            (!wsUrl.contains("192.168.") && !wsUrl.contains("10.") && !wsUrl.contains("127.0.0.1") && !wsUrl.contains("localhost"))

        _activeTransport.value = if (isWan) TransportType.GLOBAL_TUNNEL_WAN else TransportType.LAN_DIRECT

        val request = Request.Builder().url(wsUrl).build()

        val connectSignal = CompletableDeferred<Boolean>()

        activeWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket conectado con éxito vía ${_activeTransport.value} a $wsUrl")
                reconnectJob?.cancel()
                reconnectJob = null
                _isConnected.value = true
                _connectionState.value = PcConnectionState.Connected(
                    host = host,
                    port = port,
                    latencyMs = if (isWan) 45 else 12,
                    hostname = _telemetry.value?.hostname ?: "PC-Host",
                    transportType = _activeTransport.value
                )

                // Enviar saludo de emparejamiento con PIN o token persistente
                val helloPayload = JSONObject().apply {
                    put("type", "HELLO_PAIR")
                    put("pin", pin ?: _endpointConfig.value.pin)
                    put("token", _endpointConfig.value.deviceToken)
                    put("deviceName", "Hendrix Android Client")
                    put("clientVersion", "1.0.0")
                }
                webSocket.send(helloPayload.toString())
                startTelemetryPolling()
                connectSignal.complete(true)
            }


            override fun onMessage(webSocket: WebSocket, text: String) {
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val byteArray = bytes.toByteArray()
                if (byteArray.isNotEmpty() && byteArray[0] == 0x02.toByte()) {
                    if (byteArray.size > 5) {
                        // Trama binaria de Audio Inalámbrico:
                        // Byte 0: 0x02, Bytes 1..4: sequence number, Bytes 5+: PCM payload
                        audioStreamPlayer.writePcmChunk(byteArray, 5, byteArray.size - 5)
                    }
                } else if (byteArray.size >= 12 && byteArray[0] == 'R'.code.toByte() && byteArray[1] == 'I'.code.toByte()) {
                    // Trama binaria recibida: Snapshot WebP de la pantalla (Encabezado RIFF WebP)
                    _latestSnapshot.value = byteArray
                    pendingSnapshotDeferred?.complete(byteArray)
                    pendingSnapshotDeferred = null
                    inFlightSnapshotDeferred?.complete(byteArray)
                    inFlightSnapshotDeferred = null
                } else if (byteArray.isNotEmpty() && byteArray[0] == 0x01.toByte()) {
                    val payload = byteArray.copyOfRange(1, byteArray.size)
                    _latestSnapshot.value = payload
                    pendingSnapshotDeferred?.complete(payload)
                    pendingSnapshotDeferred = null
                    inFlightSnapshotDeferred?.complete(payload)
                    inFlightSnapshotDeferred = null
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket cerrándose: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket cerrado ($code: $reason)")
                handleDisconnect(unexpected = true)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Error en conexión WebSocket: ${t.message}")
                _connectionState.value = PcConnectionState.Error(t.localizedMessage ?: "Error de conexión con la PC.")
                handleDisconnect(unexpected = true)
                if (!connectSignal.isCompleted) {
                    connectSignal.complete(false)
                }
            }
        })

        val success = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { connectSignal.await() } ?: false
        if (!success && _connectionState.value is PcConnectionState.Connecting) {
            _connectionState.value = PcConnectionState.Error("Tiempo de espera agotado al conectar con $host:$port")
        }
        success
    }

    override suspend fun disconnect() {
        isUserExplicitDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        telemetryPollingJob?.cancel()
        activeWebSocket?.close(1000, "Desconectado por el usuario")
        activeWebSocket = null
        handleDisconnect(unexpected = false)
    }

    private fun handleDisconnect(unexpected: Boolean = false) {
        _isConnected.value = false
        telemetryPollingJob?.cancel()
        audioStreamPlayer.stop()
        _audioStreamState.value = com.asistente.celular.nlu.pc.audio.PcAudioStreamState(isStreaming = false)

        if (unexpected && !isUserExplicitDisconnect) {
            scheduleAutoReconnect()
        } else {
            _connectionState.value = PcConnectionState.Disconnected
        }
    }

    private fun scheduleAutoReconnect() {
        if (isUserExplicitDisconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            val host = currentHost
            val port = currentPort
            var delayMs = 1500L
            var attempt = 1

            while (!isUserExplicitDisconnect && !_isConnected.value && attempt <= 5) {
                Log.i(TAG, "Reconexión automática ($attempt/5) a $host:$port en ${delayMs}ms...")
                _connectionState.value = PcConnectionState.Reconnecting(attempt, host, port)
                delay(delayMs)
                if (isUserExplicitDisconnect || _isConnected.value) break

                val reconnected = connect(host, port)
                if (reconnected) {
                    Log.i(TAG, "Reconexión automática exitosa con $host:$port")
                    requestSnapshot()
                    break
                }
                attempt++
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(5000L)
            }
        }
    }

    private fun handleTextMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")
            val reqId = json.optString("requestId", "")

            if (reqId.isNotBlank() && pendingRequests.containsKey(reqId)) {
                pendingRequests.remove(reqId)?.complete(json)
            }

            when (type) {
                "SECURITY_ERROR" -> {
                    val error = json.optString("error", "UNKNOWN")
                    val message = json.optString("message", "")
                    val action = json.optString("action", "")
                    Log.w(TAG, "🛡️ Alerta de Seguridad recibida del Desktop: [$action] $error - $message")
                }
                "TELEMETRY_DATA" -> {
                    val tJson = json.optJSONObject("telemetry")
                    if (tJson != null) {
                        val newTelemetry = PcSystemTelemetry(
                            cpuPercent = tJson.optDouble("cpuPercent", 0.0).toFloat(),
                            ramPercent = tJson.optDouble("ramPercent", 0.0).toFloat(),
                            ramUsedGb = tJson.optDouble("ramUsedGb", 0.0).toFloat(),
                            ramTotalGb = tJson.optDouble("ramTotalGb", 16.0).toFloat(),
                            masterVolumePercent = tJson.optInt("volumePercent", 50),
                            isVolumeMuted = tJson.optBoolean("isMuted", false),
                            activeWindowTitle = tJson.optString("activeWindowTitle", ""),
                            activeProcessName = tJson.optString("activeProcessName", ""),
                            isBatteryPresent = tJson.optBoolean("isBatteryPresent", false),
                            batteryPercent = if (tJson.has("batteryPercent")) tJson.optInt("batteryPercent") else null,
                            isBatteryCharging = tJson.optBoolean("isBatteryCharging", false),
                            hostname = tJson.optString("hostname", "PC-Host"),
                            osName = tJson.optString("osName", "Windows"),
                            activeTransportType = _activeTransport.value,
                            roundTripLatencyMs = if (_activeTransport.value == TransportType.GLOBAL_TUNNEL_WAN) 45L else 12L,
                            isSessionLocked = tJson.optBoolean("isSessionLocked", false)
                        )
                        _telemetry.value = newTelemetry
                        val reportedMac = tJson.optString("macAddress", "").takeIf { it.isNotBlank() }
                        if (reportedMac != null && _endpointConfig.value.macAddress != reportedMac) {
                            saveConfig(_endpointConfig.value.copy(macAddress = reportedMac))
                        }
                    }
                }
                "DROPZONE_FILE_READY" -> {
                    val fObj = json.optJSONObject("file")
                    if (fObj != null) {
                        val file = com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile(
                            fileName = fObj.optString("fileName", "archivo"),
                            category = fObj.optString("category", "general"),
                            sizeBytes = fObj.optLong("sizeBytes", 0L),
                            relativePath = fObj.optString("relativePath", ""),
                            timestamp = fObj.optLong("timestamp", System.currentTimeMillis())
                        )
                        _dropzoneEvents.value = file
                        com.asistente.celular.service.DropzoneNotificationHelper.notifyDeliverable(context, file)
                        Log.i(TAG, "📁 Nuevo archivo en Dropzone: ${file.fileName} (${file.category}, ${file.sizeBytes} bytes)")
                    }
                }
                "CLIPBOARD_SET_ACK", "CLIPBOARD_GET_ACK" -> {
                    val text = json.optString("text", "")
                    val charCount = json.optInt("charCount", text.length)
                    val sha = json.optString("sha256", "")
                    if (text.isNotBlank() || json.optString("status") == "ok") {
                        _clipboardState.value = PcClipboardPayload(
                            text = text,
                            charCount = charCount,
                            checksumSha256 = sha
                        )
                    }
                }
                "OPEN_WINDOWS_LIST" -> {
                    val wArr = json.optJSONArray("windows") ?: JSONArray()
                    val list = mutableListOf<PcWindowInfo>()
                    for (i in 0 until wArr.length()) {
                        val obj = wArr.optJSONObject(i) ?: continue
                        list.add(
                            PcWindowInfo(
                                hwnd = obj.optLong("hwnd", 0L),
                                title = obj.optString("title", ""),
                                process = obj.optString("process", ""),
                                pid = obj.optInt("pid", 0),
                                isForeground = obj.optBoolean("isForeground", false)
                            )
                        )
                    }
                    _openWindows.value = list
                }
                "AUDIO_MIXER_SESSIONS" -> {
                    val mObj = json.optJSONObject("mixer")
                    if (mObj != null) {
                        val sArr = mObj.optJSONArray("sessions") ?: JSONArray()
                        val sessionList = mutableListOf<PcAudioAppSession>()
                        for (i in 0 until sArr.length()) {
                            val so = sArr.getJSONObject(i)
                            val proc = so.optString("processName", "")
                            sessionList.add(
                                PcAudioAppSession(
                                    id = so.optString("id", proc),
                                    processName = proc,
                                    displayName = so.optString("displayName", proc),
                                    volumePercent = so.optInt("volumePercent", 100),
                                    isMuted = so.optBoolean("isMuted", false)
                                )
                            )
                        }
                        _audioMixerState.value = PcAudioMixerState(
                            masterVolumePercent = mObj.optInt("masterVolumePercent", 50),
                            isMasterMuted = mObj.optBoolean("isMasterMuted", false),
                            sessions = sessionList
                        )
                    }
                }
                "RENDER_WATCHDOG_EVENT" -> {
                    val evObj = json.optJSONObject("event")
                    if (evObj != null) {
                        val event = PcRenderWatchdogEvent(
                            processName = evObj.optString("processName", "blender"),
                            status = evObj.optString("status", "COMPLETED"),
                            durationSeconds = evObj.optLong("durationSeconds", 0L),
                            peakGpuTemp = evObj.optInt("peakGpuTemp", 0),
                            autoSuspendTriggered = evObj.optBoolean("autoSuspendTriggered", false),
                            message = evObj.optString("message", "")
                        )
                        _watchdogEvents.value = event
                        Log.i(TAG, "🔔 Watchdog de render completado: ${event.message}")
                    }
                }
                "HARDWARE_TELEMETRY_RESP" -> {
                    val hObj = json.optJSONObject("telemetry")
                    if (hObj != null) {
                        _hardwareTelemetry.value = PcHardwareTelemetry(
                            gpuName = hObj.optString("gpuName", "GPU Principal"),
                            gpuUsagePercent = hObj.optDouble("gpuUsagePercent", 0.0).toFloat(),
                            gpuTempCelsius = hObj.optInt("gpuTempCelsius", 45),
                            vramUsedMb = hObj.optLong("vramUsedMb", 0L),
                            vramTotalMb = hObj.optLong("vramTotalMb", 8192L),
                            cpuUsagePercent = hObj.optDouble("cpuUsagePercent", 0.0).toFloat(),
                            ramUsedMb = hObj.optLong("ramUsedMb", 0L),
                            ramTotalMb = hObj.optLong("ramTotalMb", 16384L),
                            activeHeavyProcess = hObj.optString("activeHeavyProcess").takeIf { it.isNotBlank() }
                        )
                    }
                }
                "PROACTIVE_ALERT" -> {
                    val alertId = json.optString("alertId", "alert_${System.currentTimeMillis()}")
                    val catStr = json.optString("category", "OTHER")
                    val cat = try {
                        com.asistente.celular.nlu.pc.alert.PcAlertCategory.valueOf(catStr)
                    } catch (e: Exception) {
                        com.asistente.celular.nlu.pc.alert.PcAlertCategory.OTHER
                    }
                    val sevStr = json.optString("severity", "INFO")
                    val sev = try {
                        com.asistente.celular.nlu.pc.alert.PcAlertSeverity.valueOf(sevStr)
                    } catch (e: Exception) {
                        com.asistente.celular.nlu.pc.alert.PcAlertSeverity.INFO
                    }
                    val actionsArr = json.optJSONArray("actions") ?: org.json.JSONArray()
                    val actionsList = mutableListOf<com.asistente.celular.nlu.pc.alert.PcAlertAction>()
                    for (i in 0 until actionsArr.length()) {
                        val ao = actionsArr.getJSONObject(i)
                        actionsList.add(
                            com.asistente.celular.nlu.pc.alert.PcAlertAction(
                                id = ao.optString("id", ""),
                                label = ao.optString("label", ""),
                                dangerous = ao.optBoolean("dangerous", false)
                            )
                        )
                    }
                    val alert = com.asistente.celular.nlu.pc.alert.PcProactiveAlert(
                        alertId = alertId,
                        category = cat,
                        title = json.optString("title", "Alerta de PC"),
                        message = json.optString("message", ""),
                        severity = sev,
                        actions = actionsList,
                        timestampEpoch = json.optLong("timestampEpoch", System.currentTimeMillis())
                    )
                    _proactiveAlerts.value = alert
                    com.asistente.celular.pc.alert.PcProactiveAlertNotificationHelper.showAlertNotification(context, alert)
                    Log.i(TAG, "🚨 Alerta Proactiva de PC recibida: ${alert.title}")
                }
                "AUDIO_STREAM_START_RESP", "AUDIO_STREAM_STOP_RESP", "AUDIO_STREAM_TELEMETRY_RESP" -> {
                    val tObj = json.optJSONObject("telemetry")
                    if (tObj != null) {
                        _audioStreamState.value = com.asistente.celular.nlu.pc.audio.PcAudioStreamState(
                            isStreaming = tObj.optBoolean("isStreaming", false),
                            sampleRate = tObj.optInt("sampleRate", 24000),
                            channels = tObj.optInt("channels", 1),
                            currentRms = tObj.optDouble("currentRms", 0.0).toFloat(),
                            currentPeak = tObj.optDouble("currentPeak", 0.0).toFloat(),
                            driverName = tObj.optString("driver", "wasapi")
                        )
                    }
                }
                "HELLO_ACK" -> {
                    val status = json.optString("status", "AUTHORIZED")
                    if (status.equals("UNAUTHORIZED", ignoreCase = true)) {
                        val msg = json.optString("message", "PIN incorrecto o dispositivo no autorizado")
                        Log.w(TAG, "Conexión rechazada por el servidor de PC: $msg")
                        authToken = ""
                        val resetConfig = _endpointConfig.value.copy(
                            deviceToken = "",
                            isPaired = false
                        )
                        saveConfig(resetConfig)
                        _isConnected.value = false
                        _connectionState.value = PcConnectionState.Error(msg, canRetry = true)
                        activeWebSocket?.close(1008, msg)
                        return
                    }

                    authToken = json.optString("token", "")
                    val hostName = json.optString("hostname", "PC-Host")
                    val remoteTunnel = json.optString("remoteTunnelUrl", _endpointConfig.value.remoteTunnelUrl ?: "")
                    val reportedMac = json.optString("macAddress", "").takeIf { it.isNotBlank() } ?: _endpointConfig.value.macAddress
                    val updatedConfig = _endpointConfig.value.copy(
                        hostname = hostName,
                        localIp = currentHost,
                        port = currentPort,
                        remoteTunnelUrl = remoteTunnel.takeIf { it.isNotBlank() },
                        deviceToken = authToken,
                        isPaired = true,
                        lastConnectedEpoch = System.currentTimeMillis(),
                        macAddress = reportedMac
                    )
                    saveConfig(updatedConfig)
                    _isConnected.value = true
                    _connectionState.value = PcConnectionState.Connected(
                        host = currentHost,
                        port = currentPort,
                        latencyMs = if (_activeTransport.value == TransportType.GLOBAL_TUNNEL_WAN) 45L else 12L,
                        hostname = hostName,
                        transportType = _activeTransport.value
                    )

                    // Solicitar snapshot inicial de inmediato para que la pantalla esté disponible al instante
                    scope.launch(Dispatchers.IO) {
                        delay(250)
                        requestSnapshot()
                    }
                }
                "FOREGROUND_APP_CHANGED" -> {
                    val proc = json.optString("processName", "")
                    _foregroundApp.value = proc
                    Log.i(TAG, "Foreground app changed on PC: $proc (${json.optString("windowTitle")})")
                }
                "TERMINAL_BUILD_ERROR" -> {
                    val errObj = json.optJSONObject("error")
                    if (errObj != null) {
                        val alert = PcTerminalErrorAlert(
                            errorId = errObj.optString("errorId", "err_${System.currentTimeMillis()}"),
                            source = errObj.optString("source", "terminal"),
                            command = errObj.optString("command", ""),
                            errorMessage = errObj.optString("errorMessage", ""),
                            failedFile = errObj.optString("failedFile").takeIf { it.isNotBlank() },
                            failedLine = if (errObj.has("failedLine")) errObj.optInt("failedLine") else null,
                            aiDiagnosisPrompt = errObj.optString("aiDiagnosisPrompt", ""),
                            timestamp = errObj.optLong("timestamp", System.currentTimeMillis())
                        )
                        _terminalErrorAlerts.value = alert
                        Log.w(TAG, "🚨 Terminal build error detected on PC: ${alert.command} - ${alert.errorMessage}")
                    }
                }
                "AIRSYNC_LIST_RESP" -> {
                    val port = json.optInt("port", 8900)
                    if (port > 0) {
                        airSyncPort = port
                    }
                    val filesArr = json.optJSONArray("files") ?: JSONArray()
                    val list = mutableListOf<AirSyncSharedFile>()
                    for (i in 0 until filesArr.length()) {
                        val fo = filesArr.getJSONObject(i)
                        list.add(
                            AirSyncSharedFile(
                                fileId = fo.optString("fileId", ""),
                                fileName = fo.optString("fileName", ""),
                                filePath = fo.optString("filePath", ""),
                                fileSizeBytes = fo.optLong("fileSizeBytes", 0L),
                                sha256 = fo.optString("sha256", ""),
                                totalChunks = fo.optInt("totalChunks", 0),
                                chunkSizeBytes = fo.optInt("chunkSizeBytes", 262144),
                                readyForDownload = fo.optBoolean("readyForDownload", true)
                            )
                        )
                    }
                    _airSyncSharedFiles.value = list
                }
                "AIRSYNC_FILE_RECEIVED" -> {
                    Log.i(TAG, "AirSync file received on PC: ${json.optString("fileName")}")
                    scope.launch { queryAirSyncSharedFiles() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando mensaje WebSocket: ${e.message}", e)
        }
    }

    private fun startTelemetryPolling() {
        telemetryPollingJob?.cancel()
        telemetryPollingJob = scope.launch(Dispatchers.IO) {
            while (_isConnected.value) {
                queryTelemetry()
                delay(_endpointConfig.value.telemetryIntervalMs)
            }
        }
    }

    override suspend fun setOperationMode(mode: PcOperationMode): Boolean = withContext(Dispatchers.IO) {
        _currentMode.value = mode
        val ws = activeWebSocket ?: return@withContext true
        val msg = JSONObject().apply {
            put("type", "SET_MODE")
            put("mode", mode.name)
        }
        sendSignedPayload(msg, ws)
    }

    override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext _latestSnapshot.value

        var shouldSend = false
        val deferred = snapshotMutex.withLock {
            val existing = inFlightSnapshotDeferred
            if (existing != null && !existing.isCompleted) {
                existing
            } else {
                val newDeferred = CompletableDeferred<ByteArray?>()
                inFlightSnapshotDeferred = newDeferred
                pendingSnapshotDeferred = newDeferred
                shouldSend = true
                newDeferred
            }
        }

        if (shouldSend) {
            val req = JSONObject().apply {
                put("type", "SNAPSHOT_REQUEST")
                put("cropToActiveWindow", cropToActiveWindow)
                put("quality", _endpointConfig.value.snapshotQuality)
            }
            sendSignedPayload(req, ws)
        }

        val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
            deferred.await()
        } ?: _latestSnapshot.value

        snapshotMutex.withLock {
            if (inFlightSnapshotDeferred == deferred) {
                inFlightSnapshotDeferred = null
            }
        }
        result
    }

    override suspend fun sendInteraction(action: PcInteractionAction): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val x = action.xRatio
        val y = action.yRatio
        val text = action.textPayload
        val req = JSONObject().apply {
            put("type", "INPUT_ACTION")
            put("actionType", action.type.name)
            if (x != null) put("xRatio", x.toDouble())
            if (y != null) put("yRatio", y.toDouble())
            put("scrollDeltaX", action.scrollDeltaX.toDouble())
            put("scrollDeltaY", action.scrollDeltaY.toDouble())
            if (text != null) put("textPayload", text)
            if (action.keyCodes.isNotEmpty()) {
                val arr = JSONArray()
                action.keyCodes.forEach { arr.put(it) }
                put("keyCodes", arr)
            }
            put("timestamp", action.timestampEpoch)
        }

        val sent = sendSignedPayload(req, ws)

        // En modo Snapshot Interactivo, SOLO acciones que alteran el contenido de la pantalla (clics, teclas, scroll)
        // solicitan refresco visual. MOUSE_MOVE nunca solicita un snapshot para no saturar la red ni crear lag.
        if (_currentMode.value == PcOperationMode.INTERACTIVE_SNAPSHOT && action.type != PcActionType.MOUSE_MOVE) {
            scope.launch(Dispatchers.IO) {
                delay(120) // Breve pausa para permitir que la UI de la PC reaccione
                requestSnapshot()
            }
        }
        sent
    }

    override suspend fun typeTextDirectly(text: String): Boolean = withContext(Dispatchers.IO) {
        val action = PcInteractionAction(
            type = PcActionType.TYPE_TEXT,
            textPayload = text
        )
        sendInteraction(action)
    }

    override suspend fun executeQuickCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            Log.w(TAG, "Ejecución simulada de comando rápido: $command")
            return@withContext true
        }

        val req = JSONObject().apply {
            put("type", "QUICK_COMMAND")
            put("command", command)
        }
        val sent = sendSignedPayload(req, ws)
        delay(200)
        queryTelemetry()
        sent
    }

    override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        val planId = UUID.randomUUID().toString().take(8)

        if (ws == null) {
            // Plan local de fallback / simulación determinista
            val lower = goalPrompt.lowercase()
            val steps = mutableListOf<AutonomousTaskStep>()

            if (lower.contains("abre") || lower.contains("abrir")) {
                val app = goalPrompt.substringAfter("abre", "").trim()
                steps.add(AutonomousTaskStep("step_1", "Lanzar aplicación $app", "LAUNCH", targetAppOrElement = app))
                steps.add(AutonomousTaskStep("step_2", "Enfocar ventana activa de $app", "FOCUS", targetAppOrElement = app))
            }
            if (lower.contains("escribe") || lower.contains("escribir")) {
                val text = goalPrompt.substringAfter("escribe", "").trim()
                steps.add(AutonomousTaskStep("step_3", "Escribir texto en el campo activo: \"$text\"", "TYPE", textArgument = text))
            }
            if (steps.isEmpty()) {
                steps.add(AutonomousTaskStep("step_1", "Ejecutar objetivo: $goalPrompt", "EXECUTE_GOAL", textArgument = goalPrompt))
            }

            return@withContext AutonomousTaskPlan(
                planId = planId,
                userGoal = goalPrompt,
                steps = steps,
                requiresUserApproval = steps.any { it.isDestructiveOrSensitive },
                statusSummary = "Plan generado (${steps.size} pasos identificados)."
            )
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "RPA_PLAN")
            put("requestId", reqId)
            put("goalPrompt", goalPrompt)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val stepsArray = resp.optJSONArray("steps") ?: JSONArray()
            val stepsList = mutableListOf<AutonomousTaskStep>()
            for (i in 0 until stepsArray.length()) {
                val s = stepsArray.getJSONObject(i)
                stepsList.add(
                    AutonomousTaskStep(
                        stepId = s.optString("stepId", "step_$i"),
                        description = s.optString("description", ""),
                        actionType = s.optString("actionType", "CUSTOM"),
                        targetAppOrElement = s.optString("targetAppOrElement").takeIf { it.isNotBlank() },
                        textArgument = s.optString("textArgument").takeIf { it.isNotBlank() },
                        status = TaskStepStatus.PENDING,
                        isDestructiveOrSensitive = s.optBoolean("isDestructive", false)
                    )
                )
            }
            AutonomousTaskPlan(
                planId = resp.optString("planId", planId),
                userGoal = goalPrompt,
                steps = stepsList,
                requiresUserApproval = resp.optBoolean("requiresApproval", false),
                statusSummary = resp.optString("summary", "Plan listo")
            )
        } else {
            // Plan de contingencia
            AutonomousTaskPlan(
                planId = planId,
                userGoal = goalPrompt,
                steps = listOf(AutonomousTaskStep("step_1", "Ejecutar: $goalPrompt", "EXECUTE_GOAL")),
                requiresUserApproval = false
            )
        }
    }

    override suspend fun executeApprovedPlan(planId: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext true
        val req = JSONObject().apply {
            put("type", "RPA_EXECUTE")
            put("planId", planId)
        }
        sendSignedPayload(req, ws)
    }

    override suspend fun queryTelemetry(): PcSystemTelemetry? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext _telemetry.value
        val req = JSONObject().apply {
            put("type", "TELEMETRY_REQUEST")
        }
        sendSignedPayload(req, ws)
        _telemetry.value
    }

    override suspend fun queryAntigravityProjects(): List<AntigravityProject> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            return@withContext listOf(
                AntigravityProject(
                    id = "d:/Proyectos/TEST/HendrixAssistant-main",
                    name = "HendrixAssistant-main",
                    workspaceUri = "file:///d:/Proyectos/TEST/HendrixAssistant-main",
                    lastActiveEpoch = System.currentTimeMillis(),
                    totalConversations = 3
                ),
                AntigravityProject(
                    id = "d:/Proyectos/Servicios",
                    name = "Servicios",
                    workspaceUri = "file:///d:/Proyectos/Servicios",
                    lastActiveEpoch = System.currentTimeMillis() - 3600000,
                    totalConversations = 5
                )
            )
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AG_LIST_PROJECTS")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val arr = resp.optJSONArray("projects") ?: JSONArray()
            val list = mutableListOf<AntigravityProject>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lastActiveRaw = obj.opt("lastActive")
                val lastActiveEpoch = when (lastActiveRaw) {
                    is Number -> lastActiveRaw.toLong()
                    is String -> lastActiveRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                list.add(
                    AntigravityProject(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", "Proyecto"),
                        workspaceUri = obj.optString("workspaceUri", ""),
                        lastActiveEpoch = lastActiveEpoch,
                        totalConversations = obj.optInt("totalConversations", 0)
                    )
                )
            }
            list
        } else {
            emptyList()
        }
    }

    override suspend fun queryAntigravityChats(projectId: String?): List<AntigravityChat> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            return@withContext listOf(
                AntigravityChat(
                    conversationId = "mock-conv-1",
                    title = "Integración Antigravity Hendrix",
                    preview = "Control de proyectos, chats y automatización remota",
                    lastModifiedEpoch = System.currentTimeMillis(),
                    stepCount = 10,
                    projectId = projectId ?: "d:/Proyectos/TEST/HendrixAssistant-main"
                ),
                AntigravityChat(
                    conversationId = "mock-conv-2",
                    title = "Optimización de WebSocket y Baja Latencia",
                    preview = "Pruebas de latencia en LAN y compresión WebP",
                    lastModifiedEpoch = System.currentTimeMillis() - 7200000,
                    stepCount = 6,
                    projectId = projectId ?: "d:/Proyectos/TEST/HendrixAssistant-main"
                )
            )
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AG_LIST_CHATS")
            put("requestId", reqId)
            if (projectId != null) {
                put("projectId", projectId)
            }
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val arr = resp.optJSONArray("chats") ?: JSONArray()
            val list = mutableListOf<AntigravityChat>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lastModRaw = obj.opt("lastModified")
                val lastModEpoch = when (lastModRaw) {
                    is Number -> lastModRaw.toLong()
                    is String -> lastModRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                list.add(
                    AntigravityChat(
                        conversationId = obj.optString("conversationId", ""),
                        title = obj.optString("title", "Conversación"),
                        preview = obj.optString("preview", ""),
                        lastModifiedEpoch = lastModEpoch,
                        stepCount = obj.optInt("stepCount", 0),
                        projectId = obj.optString("projectId", ""),
                        workspaceUri = obj.optString("workspaceUri", "")
                    )
                )
            }
            list
        } else {
            emptyList()
        }
    }

    override suspend fun executeAntigravityAction(
        mode: AntigravityTargetMode,
        projectId: String?,
        conversationId: String?,
        prompt: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            Log.w(TAG, "Simulación de acción Antigravity en modo desconectado: $mode")
            return@withContext true
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AG_ACTION")
            put("requestId", reqId)
            put("mode", mode.name)
            if (projectId != null) put("workspaceUri", projectId)
            if (conversationId != null) put("conversationId", conversationId)
            if (prompt != null) put("prompt", prompt)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun executeDawAction(request: DawActionRequest): DawActionResult = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            Log.w(TAG, "Simulación de acción DAW en modo desconectado: ${request.action} (${request.dawType})")
            return@withContext DawActionResult(success = true, message = "Acción simulada sin conexión")
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "DAW_ACTION")
            put("requestId", reqId)
            put("dawType", request.dawType.name)
            put("action", request.action.name)
            if (request.targetProjectNameOrPath != null) {
                put("targetProject", request.targetProjectNameOrPath)
            }
            if (request.exportPreset != null) {
                put("exportPreset", request.exportPreset)
            }
            if (request.saveCurrentFirst != null) {
                put("saveCurrentFirst", request.saveCurrentFirst)
            }
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            DawActionResult(
                success = resp.optBoolean("success", false),
                requiresConfirmation = resp.optBoolean("requiresConfirmation", false),
                confirmationTitle = if (resp.has("confirmationTitle") && !resp.isNull("confirmationTitle")) resp.getString("confirmationTitle") else null,
                message = resp.optString("message", "")
            )
        } else {
            DawActionResult(success = false, message = "Timeout al comunicarse con la computadora")
        }
    }

    override suspend fun queryDawProjects(dawType: DawType): List<DawProjectInfo> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            return@withContext listOf(
                DawProjectInfo(
                    name = "Synthwave Neon Beat",
                    absolutePath = "D:/Musica/Ableton/Synthwave Neon Beat.als",
                    lastModifiedEpoch = System.currentTimeMillis() - 3600000,
                    dawType = dawType,
                    sizeBytes = 245760
                ),
                DawProjectInfo(
                    name = "Ambient Chill Vocal Mix",
                    absolutePath = "D:/Musica/Ableton/Ambient Chill Vocal Mix.als",
                    lastModifiedEpoch = System.currentTimeMillis() - 86400000,
                    dawType = dawType,
                    sizeBytes = 512000
                )
            )
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "DAW_LIST_PROJECTS")
            put("requestId", reqId)
            put("dawType", dawType.name)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val arr = resp.optJSONArray("projects") ?: JSONArray()
            val list = mutableListOf<DawProjectInfo>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val lastModRaw = obj.opt("lastModifiedEpoch")
                val lastModEpoch = when (lastModRaw) {
                    is Number -> lastModRaw.toLong()
                    is String -> lastModRaw.toLongOrNull() ?: 0L
                    else -> 0L
                }
                list.add(
                    DawProjectInfo(
                        name = obj.optString("name", "Proyecto"),
                        absolutePath = obj.optString("absolutePath", ""),
                        lastModifiedEpoch = lastModEpoch,
                        dawType = dawType,
                        sizeBytes = obj.optLong("sizeBytes", 0L)
                    )
                )
            }
            list
        } else {
            emptyList()
        }
    }

    override suspend fun executeModuleAction(
        request: com.asistente.celular.nlu.pc.module.PcModuleActionRequest
    ): com.asistente.celular.nlu.pc.module.PcModuleActionResult = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            Log.w(TAG, "Simulación de acción de módulo en modo desconectado: ${request.moduleId} -> ${request.actionId}")
            return@withContext com.asistente.celular.nlu.pc.module.PcModuleActionResult(
                success = true,
                message = "Acción simulada en modo desconectado"
            )
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "MODULE_ACTION")
            put("requestId", reqId)
            put("moduleId", request.moduleId.name)
            put("actionId", request.actionId)
            val paramsObj = JSONObject()
            for ((k, v) in request.params) {
                paramsObj.put(k, v)
            }
            put("params", paramsObj)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            com.asistente.celular.nlu.pc.module.PcModuleActionResult(
                success = resp.optBoolean("success", true),
                message = resp.optString("message", "Acción ejecutada")
            )
        } else {
            com.asistente.celular.nlu.pc.module.PcModuleActionResult(
                success = false,
                message = "Timeout al comunicarse con la computadora"
            )
        }
    }

    override suspend fun queryDropzoneInfo(): com.asistente.celular.nlu.pc.dropzone.PcDropzoneInfo? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext com.asistente.celular.nlu.pc.dropzone.PcDropzoneInfo(
            rootPath = "C:\\Users\\Workstation\\Google Drive\\HendrixStudio",
            cloudProvider = "Google Drive (Simulado)",
            isCloudSynced = true
        )

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "DROPZONE_GET_INFO")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val infoObj = resp?.optJSONObject("info") ?: return@withContext null

        val cats = mutableMapOf<String, String>()
        val catsObj = infoObj.optJSONObject("categories")
        if (catsObj != null) {
            for (key in catsObj.keys()) {
                cats[key] = catsObj.optString(key)
            }
        }

        val recent = mutableListOf<com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile>()
        val filesArr = infoObj.optJSONArray("recentFiles")
        if (filesArr != null) {
            for (i in 0 until filesArr.length()) {
                val fo = filesArr.optJSONObject(i) ?: continue
                recent.add(
                    com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile(
                        fileName = fo.optString("fileName"),
                        category = fo.optString("category"),
                        sizeBytes = fo.optLong("sizeBytes"),
                        relativePath = fo.optString("relativePath"),
                        timestamp = fo.optLong("timestamp")
                    )
                )
            }
        }

        com.asistente.celular.nlu.pc.dropzone.PcDropzoneInfo(
            rootPath = infoObj.optString("rootPath"),
            cloudProvider = infoObj.optString("cloudProvider", "Local"),
            isCloudSynced = infoObj.optBoolean("isCloudSynced", false),
            categories = cats,
            recentFiles = recent
        )
    }

    override suspend fun setDropzonePath(path: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext true
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "DROPZONE_SET_PATH")
            put("requestId", reqId)
            put("path", path)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", true) ?: false
    }

    override suspend fun wakeOnLan(macAddress: String?, broadcastIp: String?): Boolean = withContext(Dispatchers.IO) {
        val targetMac = macAddress ?: _endpointConfig.value.macAddress
        if (targetMac.isNullOrBlank() || !com.asistente.celular.nlu.pc.wol.WakeOnLanHelper.isValidMac(targetMac)) {
            Log.w(TAG, "No se puede emitir Wake-on-LAN: dirección MAC no configurada o inválida: $targetMac")
            return@withContext false
        }
        val ip = broadcastIp ?: "255.255.255.255"
        val res = com.asistente.celular.nlu.pc.wol.WakeOnLanHelper.sendMagicPacket(targetMac, ip)
        if (res.isSuccess) {
            Log.i(TAG, "⚡ Paquete mágico Wake-on-LAN enviado con éxito hacia $targetMac (IP: $ip)")
            true
        } else {
            Log.e(TAG, "Error enviando Wake-on-LAN: ${res.exceptionOrNull()?.message}")
            false
        }
    }

    override suspend fun unlockSession(pin: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "SESSION_UNLOCK")
            put("requestId", reqId)
            put("pin", pin)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val success = resp?.optBoolean("success", false) ?: false
        if (resp != null && resp.has("isSessionLocked")) {
            _telemetry.value = _telemetry.value?.copy(isSessionLocked = resp.optBoolean("isSessionLocked"))
        }
        success
    }

    override suspend fun getClipboard(): PcClipboardPayload? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext _clipboardState.value
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "CLIPBOARD_GET")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null && resp.optString("status") == "ok") {
            val payload = PcClipboardPayload(
                text = resp.optString("text", ""),
                charCount = resp.optInt("charCount", 0),
                checksumSha256 = resp.optString("sha256", "")
            )
            _clipboardState.value = payload
            payload
        } else {
            _clipboardState.value
        }
    }

    override suspend fun setClipboard(text: String, pasteImmediately: Boolean): PcClipboardPayload? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: run {
            val local = PcClipboardPayload.fromText(text)
            _clipboardState.value = local
            return@withContext local
        }
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "CLIPBOARD_SET")
            put("requestId", reqId)
            put("text", text)
            put("pasteImmediately", pasteImmediately)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null && resp.optString("status") == "ok") {
            val payload = PcClipboardPayload(
                text = text,
                charCount = resp.optInt("charCount", text.length),
                checksumSha256 = resp.optString("sha256", PcClipboardPayload.computeSha256(text))
            )
            _clipboardState.value = payload
            payload
        } else {
            null
        }
    }

    override suspend fun queryAudioMixer(): PcAudioMixerState? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext _audioMixerState.value
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AUDIO_MIXER_GET")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val mObj = resp.optJSONObject("mixer")
            if (mObj != null) {
                val sArr = mObj.optJSONArray("sessions") ?: JSONArray()
                val sessionList = mutableListOf<PcAudioAppSession>()
                for (i in 0 until sArr.length()) {
                    val so = sArr.getJSONObject(i)
                    val proc = so.optString("processName", "")
                    sessionList.add(
                        PcAudioAppSession(
                            id = so.optString("id", proc),
                            processName = proc,
                            displayName = so.optString("displayName", proc),
                            volumePercent = so.optInt("volumePercent", 100),
                            isMuted = so.optBoolean("isMuted", false)
                        )
                    )
                }
                val mixerState = PcAudioMixerState(
                    masterVolumePercent = mObj.optInt("masterVolumePercent", 50),
                    isMasterMuted = mObj.optBoolean("isMasterMuted", false),
                    sessions = sessionList
                )
                _audioMixerState.value = mixerState
                return@withContext mixerState
            }
        }
        _audioMixerState.value
    }

    override suspend fun setAppVolume(processName: String, volumePercent: Int): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val req = JSONObject().apply {
            put("type", "AUDIO_MIXER_SET_VOLUME")
            put("requestId", reqId)
            put("processName", processName)
            put("volumePercent", volumePercent)
            put("isMaster", false)
        }
        sendSignedPayload(req, ws)
        true
    }

    override suspend fun setAppMute(processName: String, isMuted: Boolean): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val req = JSONObject().apply {
            put("type", "AUDIO_MIXER_SET_MUTE")
            put("requestId", reqId)
            put("processName", processName)
            put("isMuted", isMuted)
            put("isMaster", false)
        }
        sendSignedPayload(req, ws)
        true
    }

    override suspend fun setMasterVolume(volumePercent: Int): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val req = JSONObject().apply {
            put("type", "AUDIO_MIXER_SET_VOLUME")
            put("requestId", reqId)
            put("isMaster", true)
            put("volumePercent", volumePercent)
        }
        sendSignedPayload(req, ws)
        _telemetry.value = _telemetry.value?.copy(masterVolumePercent = volumePercent)
        true
    }

    override suspend fun setMasterMute(isMuted: Boolean): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val req = JSONObject().apply {
            put("type", "AUDIO_MIXER_SET_MUTE")
            put("requestId", reqId)
            put("isMaster", true)
            put("isMuted", isMuted)
        }
        sendSignedPayload(req, ws)
        _telemetry.value = _telemetry.value?.copy(isVolumeMuted = isMuted)
        true
    }

    fun getSavedUnlockPin(): String = prefs.getString("pc_unlock_pin", "") ?: ""

    fun saveUnlockPin(pin: String) {
        prefs.edit().putString("pc_unlock_pin", pin).apply()
    }

    override suspend fun executeStudioScene(
        sceneId: String
    ): PcSceneExecutionResult = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext PcSceneExecutionResult(
            sceneId = sceneId,
            success = false,
            stepsExecuted = 0,
            totalSteps = 0,
            message = "Sin conexión activa con la PC"
        )
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "STUDIO_SCENE_EXECUTE")
            put("requestId", reqId)
            put("sceneId", sceneId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(30000L) { deferred.await() }
        if (resp != null) {
            val sObj = resp.optJSONObject("sceneResult")
            if (sObj != null) {
                return@withContext PcSceneExecutionResult(
                    sceneId = sObj.optString("sceneId", sceneId),
                    success = sObj.optBoolean("success", false),
                    stepsExecuted = sObj.optInt("stepsExecuted", 0),
                    totalSteps = sObj.optInt("totalSteps", 0),
                    message = sObj.optString("message", "")
                )
            }
        }
        PcSceneExecutionResult(
            sceneId = sceneId,
            success = false,
            stepsExecuted = 0,
            totalSteps = 0,
            message = "Tiempo de espera agotado al ejecutar escena"
        )
    }

    override suspend fun queryRemoteProjects(
        category: PcProjectCategory?,
        query: String?
    ): List<PcProjectItem> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext emptyList()
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PROJECTS_QUERY")
            put("requestId", reqId)
            if (category != null) put("category", category.name)
            if (!query.isNullOrBlank()) put("query", query)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val arr = resp.optJSONArray("projects") ?: JSONArray()
            val list = mutableListOf<PcProjectItem>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val catStr = p.optString("category", "OTHER")
                val cat = try {
                    PcProjectCategory.valueOf(catStr)
                } catch (e: Exception) {
                    PcProjectCategory.OTHER
                }
                val path = p.optString("path", "")
                list.add(
                    PcProjectItem(
                        id = p.optString("id", path),
                        name = p.optString("name", "Proyecto"),
                        path = path,
                        category = cat,
                        extension = p.optString("extension", ""),
                        sizeBytes = p.optLong("sizeBytes", 0L),
                        lastModifiedEpoch = p.optLong("lastModifiedEpoch", p.optLong("lastModified", 0L))
                    )
                )
            }
            return@withContext list
        }
        emptyList()
    }

    override suspend fun launchRemoteProject(projectPath: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PROJECT_LAUNCH")
            put("requestId", reqId)
            put("projectPath", projectPath)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun queryHardwareTelemetry(): PcHardwareTelemetry? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext _hardwareTelemetry.value
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "HARDWARE_TELEMETRY_GET")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val hObj = resp.optJSONObject("telemetry")
            if (hObj != null) {
                val telemetry = PcHardwareTelemetry(
                    gpuName = hObj.optString("gpuName", "GPU Principal"),
                    gpuUsagePercent = hObj.optDouble("gpuUsagePercent", 0.0).toFloat(),
                    gpuTempCelsius = hObj.optInt("gpuTempCelsius", 45),
                    vramUsedMb = hObj.optLong("vramUsedMb", 0L),
                    vramTotalMb = hObj.optLong("vramTotalMb", 8192L),
                    cpuUsagePercent = hObj.optDouble("cpuUsagePercent", 0.0).toFloat(),
                    ramUsedMb = hObj.optLong("ramUsedMb", 0L),
                    ramTotalMb = hObj.optLong("ramTotalMb", 16384L),
                    activeHeavyProcess = hObj.optString("activeHeavyProcess").takeIf { it.isNotBlank() }
                )
                _hardwareTelemetry.value = telemetry
                return@withContext telemetry
            }
        }
        _hardwareTelemetry.value
    }

    override suspend fun startRenderWatchdog(
        processName: String,
        autoSuspend: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "WATCHDOG_START")
            put("requestId", reqId)
            put("processName", processName)
            put("autoSuspend", autoSuspend)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun analyzeScreenWithAi(
        prompt: String,
        cropToActiveWindow: Boolean
    ): PcScreenAnalysisResult = withContext(Dispatchers.IO) {
        val activeWin = _telemetry.value?.activeWindowTitle
        val activeProc = _telemetry.value?.activeProcessName

        val imageBytes = requestSnapshot(cropToActiveWindow = cropToActiveWindow)
        if (imageBytes == null || imageBytes.isEmpty()) {
            return@withContext PcScreenAnalysisResult(
                success = false,
                analysisMarkdown = "No se pudo obtener la captura de pantalla de la computadora. Asegúrate de que la PC esté conectada y con la sesión iniciada.",
                detectedWindow = activeWin,
                errorSummary = "Fallo al solicitar snapshot a la PC."
            )
        }

        val client = llmClientProvider?.invoke()
        if (client == null) {
            val winInfo = if (!activeWin.isNullOrBlank()) "Ventana activa: **$activeWin** ($activeProc)" else "Sin ventana identificada"
            return@withContext PcScreenAnalysisResult(
                success = true,
                analysisMarkdown = "Captura de pantalla recibida correctamente (${imageBytes.size / 1024} KB).\n\n" +
                        "$winInfo.\n\n" +
                        "*Nota: Configura un proveedor de IA (Gemini / OpenAI) en los Ajustes de Hendrix para obtener el análisis visual multimodal completo por IA.*",
                detectedWindow = activeWin,
                rawImageBytes = imageBytes
            )
        }

        val contextualPrompt = buildString {
            appendLine("Eres Hendrix AI Screen Copilot, un asistente experto en diagnóstico visual de pantallas de PC.")
            if (!activeWin.isNullOrBlank()) {
                appendLine("Ventana en primer plano: '$activeWin' (Proceso: $activeProc)")
            }
            appendLine("Analiza la imagen adjunta de la pantalla y responde a la siguiente consulta del usuario:")
            appendLine(prompt)
            appendLine()
            appendLine("Estructura tu respuesta en Markdown claro:")
            appendLine("1. Estado actual y lo que se observa")
            appendLine("2. Diagnóstico de errores, excepciones, avisos o estado del proceso/render")
            appendLine("3. Solución o pasos recomendados a seguir")
        }

        val responseResult = client.generateMultimodalResponse(
            prompt = contextualPrompt,
            imageBytes = imageBytes,
            mimeType = "image/webp"
        )

        if (responseResult.isSuccess) {
            PcScreenAnalysisResult(
                success = true,
                analysisMarkdown = responseResult.getOrThrow(),
                detectedWindow = activeWin,
                rawImageBytes = imageBytes
            )
        } else {
            val ex = responseResult.exceptionOrNull()
            Log.e(TAG, "Error en análisis multimodal con LLM: ${ex?.message}", ex)
            PcScreenAnalysisResult(
                success = false,
                analysisMarkdown = "Error al comunicarse con el modelo de visión: ${ex?.localizedMessage ?: ex?.message}",
                detectedWindow = activeWin,
                errorSummary = ex?.localizedMessage ?: "Error en proveedor de IA",
                rawImageBytes = imageBytes
            )
        }
    }

    override suspend fun queryCustomPlugins(): List<PcPluginDefinition> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext emptyList()
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PLUGINS_QUERY")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val arr = resp.optJSONArray("plugins") ?: JSONArray()
            val list = mutableListOf<PcPluginDefinition>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val actionsArr = p.optJSONArray("actions") ?: JSONArray()
                val actionsList = mutableListOf<PcPluginAction>()
                for (j in 0 until actionsArr.length()) {
                    val a = actionsArr.getJSONObject(j)
                    val paramsArr = a.optJSONArray("params") ?: JSONArray()
                    val paramsList = mutableListOf<PcPluginParam>()
                    for (k in 0 until paramsArr.length()) {
                        val pr = paramsArr.getJSONObject(k)
                        paramsList.add(
                            PcPluginParam(
                                name = pr.optString("name", ""),
                                type = pr.optString("type", "string"),
                                default = if (pr.has("default")) pr.opt("default") else null,
                                description = pr.optString("description", "")
                            )
                        )
                    }
                    actionsList.add(
                        PcPluginAction(
                            id = a.optString("id", ""),
                            label = a.optString("label", ""),
                            description = a.optString("description", ""),
                            dangerous = a.optBoolean("dangerous", false),
                            params = paramsList
                        )
                    )
                }
                list.add(
                    PcPluginDefinition(
                        id = p.optString("id", ""),
                        name = p.optString("name", ""),
                        version = p.optString("version", "1.0.0"),
                        description = p.optString("description", ""),
                        author = p.optString("author", ""),
                        icon = p.optString("icon", "code"),
                        actions = actionsList
                    )
                )
            }
            return@withContext list
        }
        emptyList()
    }

    override suspend fun executeCustomPluginAction(
        pluginId: String,
        actionId: String,
        params: Map<String, Any>
    ): PcPluginActionResult = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext PcPluginActionResult(
            pluginId = pluginId,
            actionId = actionId,
            success = false,
            message = "Sin conexión con la PC"
        )
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PLUGIN_EXECUTE")
            put("requestId", reqId)
            put("pluginId", pluginId)
            put("actionId", actionId)
            val paramsObj = JSONObject()
            params.forEach { (k, v) -> paramsObj.put(k, v) }
            put("params", paramsObj)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(10000L) { deferred.await() }
        if (resp != null) {
            return@withContext PcPluginActionResult(
                pluginId = resp.optString("pluginId", pluginId),
                actionId = resp.optString("actionId", actionId),
                success = resp.optBoolean("success", false),
                message = resp.optString("message", ""),
                output = resp.optString("output", ""),
                elapsedMs = resp.optLong("elapsedMs", 0L)
            )
        }
        PcPluginActionResult(
            pluginId = pluginId,
            actionId = actionId,
            success = false,
            message = "Tiempo de espera agotado al ejecutar acción del plugin"
        )
    }

    override suspend fun startAudioMonitoring(sampleRate: Int): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            audioStreamPlayer.start(sampleRate = sampleRate)
            _audioStreamState.value = _audioStreamState.value.copy(isStreaming = true, sampleRate = sampleRate)
            return@withContext true
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AUDIO_STREAM_START")
            put("requestId", reqId)
            put("sampleRate", sampleRate)
            put("channels", 1)
        }
        sendSignedPayload(req, ws)

        audioStreamPlayer.start(sampleRate = sampleRate)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val ok = resp?.optBoolean("success", true) ?: true
        _audioStreamState.value = _audioStreamState.value.copy(isStreaming = ok, sampleRate = sampleRate)
        ok
    }

    override suspend fun stopAudioMonitoring(): Boolean = withContext(Dispatchers.IO) {
        audioStreamPlayer.stop()
        _audioStreamState.value = _audioStreamState.value.copy(isStreaming = false)

        val ws = activeWebSocket ?: return@withContext true
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AUDIO_STREAM_STOP")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", true) ?: true
    }

    override suspend fun triggerTestAlert(category: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            val testAlert = com.asistente.celular.nlu.pc.alert.PcProactiveAlert(
                alertId = "test_alert_${System.currentTimeMillis()}",
                category = com.asistente.celular.nlu.pc.alert.PcAlertCategory.GPU_OVERHEAT,
                title = "⚠️ Alerta de Prueba (Modo Desconectado)",
                message = "Simulación local de sobrecalentamiento térmico.",
                severity = com.asistente.celular.nlu.pc.alert.PcAlertSeverity.WARNING,
                actions = listOf(
                    com.asistente.celular.nlu.pc.alert.PcAlertAction("suspend_pc", "Suspender PC"),
                    com.asistente.celular.nlu.pc.alert.PcAlertAction("dismiss", "Descartar")
                )
            )
            _proactiveAlerts.value = testAlert
            com.asistente.celular.pc.alert.PcProactiveAlertNotificationHelper.showAlertNotification(context, testAlert)
            return@withContext true
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "TRIGGER_TEST_ALERT")
            put("requestId", reqId)
            put("category", category)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun queryWorkspaceContext(): PcWorkspaceContext? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            return@withContext PcWorkspaceContext(
                foregroundProcess = _foregroundApp.value.ifBlank { "Code.exe" },
                foregroundTitle = "Visual Studio Code - HendrixAssistant",
                activeGitRepos = listOf(
                    PcGitRepositoryStatus(
                        repoName = "HendrixAssistant",
                        branch = "main",
                        path = "D:\\Proyectos\\TEST\\HendrixAssistant-main",
                        hasUncommittedChanges = true,
                        uncommittedFilesCount = 3,
                        lastCommitMessage = "feat: add macro deck and airsync core",
                        lastCommitAuthor = "Developer",
                        lastCommitHash = "a1b2c3d"
                    )
                ),
                runningCreativeProcesses = listOf(
                    PcRunningCreativeProcess(
                        pid = 1234,
                        name = "Code.exe",
                        title = "Visual Studio Code",
                        category = "DEVELOPMENT",
                        cpuPercent = 1.2,
                        memoryMb = 450.0
                    )
                ),
                recentTerminalErrors = _terminalErrorAlerts.value?.let { listOf(it) } ?: emptyList()
            )
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "WORKSPACE_CONTEXT_GET")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() } ?: return@withContext null
        val cObj = resp.optJSONObject("context") ?: return@withContext null

        val fgObj = cObj.optJSONObject("foreground")
        val fgProcess = fgObj?.optString("processName", "") ?: ""
        val fgTitle = fgObj?.optString("windowTitle", "") ?: ""

        val reposArr = cObj.optJSONArray("gitRepos") ?: JSONArray()
        val gitRepos = mutableListOf<PcGitRepositoryStatus>()
        for (i in 0 until reposArr.length()) {
            val ro = reposArr.getJSONObject(i)
            gitRepos.add(
                PcGitRepositoryStatus(
                    repoName = ro.optString("repoName", ""),
                    branch = ro.optString("branch", ""),
                    path = ro.optString("path", ""),
                    hasUncommittedChanges = ro.optBoolean("hasUncommittedChanges", false),
                    uncommittedFilesCount = ro.optInt("uncommittedFilesCount", 0),
                    lastCommitMessage = ro.optString("lastCommitMessage", ""),
                    lastCommitAuthor = ro.optString("lastCommitAuthor", ""),
                    lastCommitHash = ro.optString("lastCommitHash", ""),
                    lastCommitTimestamp = ro.optString("lastCommitTimestamp", "")
                )
            )
        }

        val procArr = cObj.optJSONArray("creativeProcesses") ?: JSONArray()
        val creativeProcs = mutableListOf<PcRunningCreativeProcess>()
        for (i in 0 until procArr.length()) {
            val po = procArr.getJSONObject(i)
            creativeProcs.add(
                PcRunningCreativeProcess(
                    pid = po.optInt("pid", 0),
                    name = po.optString("name", ""),
                    title = po.optString("title", ""),
                    category = po.optString("category", "OTHER"),
                    cpuPercent = po.optDouble("cpuPercent", 0.0),
                    memoryMb = po.optDouble("memoryMb", 0.0)
                )
            )
        }

        val errArr = cObj.optJSONArray("terminalErrors") ?: JSONArray()
        val termErrors = mutableListOf<PcTerminalErrorAlert>()
        for (i in 0 until errArr.length()) {
            val eo = errArr.getJSONObject(i)
            termErrors.add(
                PcTerminalErrorAlert(
                    errorId = eo.optString("errorId", ""),
                    source = eo.optString("source", ""),
                    command = eo.optString("command", ""),
                    errorMessage = eo.optString("errorMessage", ""),
                    failedFile = eo.optString("failedFile").takeIf { it.isNotBlank() },
                    failedLine = if (eo.has("failedLine")) eo.optInt("failedLine") else null,
                    aiDiagnosisPrompt = eo.optString("aiDiagnosisPrompt", ""),
                    timestamp = eo.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        PcWorkspaceContext(
            foregroundProcess = fgProcess,
            foregroundTitle = fgTitle,
            activeGitRepos = gitRepos,
            runningCreativeProcesses = creativeProcs,
            recentTerminalErrors = termErrors,
            timestamp = cObj.optLong("timestamp", System.currentTimeMillis())
        )
    }

    override suspend fun queryAirSyncSharedFiles(): List<AirSyncSharedFile> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            return@withContext _airSyncSharedFiles.value
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AIRSYNC_LIST_FILES")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        if (resp != null) {
            val port = resp.optInt("port", 8900)
            if (port > 0) {
                airSyncPort = port
            }
            val filesArr = resp.optJSONArray("files") ?: JSONArray()
            val list = mutableListOf<AirSyncSharedFile>()
            for (i in 0 until filesArr.length()) {
                val fo = filesArr.getJSONObject(i)
                list.add(
                    AirSyncSharedFile(
                        fileId = fo.optString("fileId", ""),
                        fileName = fo.optString("fileName", ""),
                        filePath = fo.optString("filePath", ""),
                        fileSizeBytes = fo.optLong("fileSizeBytes", 0L),
                        sha256 = fo.optString("sha256", ""),
                        totalChunks = fo.optInt("totalChunks", 0),
                        chunkSizeBytes = fo.optInt("chunkSizeBytes", 262144),
                        readyForDownload = fo.optBoolean("readyForDownload", true)
                    )
                )
            }
            _airSyncSharedFiles.value = list
            list
        } else {
            _airSyncSharedFiles.value
        }
    }

    override suspend fun shareAirSyncFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "AIRSYNC_SHARE_FILE")
            put("requestId", reqId)
            put("filePath", filePath)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val ok = resp?.optString("status") == "ok"
        if (ok) {
            queryAirSyncSharedFiles()
        }
        ok
    }

    override suspend fun downloadAirSyncFile(fileId: String, destinationPath: String): Boolean = withContext(Dispatchers.IO) {
        val destFile = java.io.File(destinationPath)
        val transferId = UUID.randomUUID().toString().take(8)

        val success = airSyncClient.downloadFile(
            host = currentHost,
            port = airSyncPort,
            fileId = fileId,
            destinationFile = destFile,
            transferId = transferId,
            onProgress = { update ->
                val currentList = _airSyncTransfers.value.toMutableList()
                val idx = currentList.indexOfFirst { it.transferId == update.transferId }
                if (idx >= 0) {
                    currentList[idx] = update
                } else {
                    currentList.add(0, update)
                }
                _airSyncTransfers.value = currentList
            }
        )
        success
    }

    override suspend fun uploadAirSyncFile(
        file: java.io.File,
        options: com.asistente.celular.nlu.pc.airsync.AirSyncUploadOptions
    ): Boolean = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString().take(8)

        val success = airSyncClient.uploadFile(
            host = currentHost,
            port = airSyncPort,
            sourceFile = file,
            options = options,
            transferId = transferId,
            onProgress = { update ->
                val currentList = _airSyncTransfers.value.toMutableList()
                val idx = currentList.indexOfFirst { it.transferId == update.transferId }
                if (idx >= 0) {
                    currentList[idx] = update
                } else {
                    currentList.add(0, update)
                }
                _airSyncTransfers.value = currentList
            }
        )
        if (success) {
            queryAirSyncSharedFiles()
        }
        success
    }

    override suspend fun simulateTerminalError(command: String, errorMessage: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket
        if (ws == null) {
            val alert = PcTerminalErrorAlert(
                errorId = "sim_${System.currentTimeMillis()}",
                source = "gradle",
                command = command,
                errorMessage = errorMessage,
                failedFile = "MainActivity.kt",
                failedLine = 42,
                aiDiagnosisPrompt = "Error de compilación en Kotlin: $errorMessage"
            )
            _terminalErrorAlerts.value = alert
            return@withContext true
        }

        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "TERMINAL_SIMULATE_ERROR")
            put("requestId", reqId)
            put("command", command)
            put("errorMessage", errorMessage)
        }
        sendSignedPayload(req, ws)

        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", true) ?: true
    }

    override suspend fun killProcess(processName: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PC_KILL_PROCESS")
            put("requestId", reqId)
            put("processName", processName)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun executeWindowCommand(action: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PC_WINDOW_COMMAND")
            put("requestId", reqId)
            put("action", action)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun queryHardwareHealth(): com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext _hardwareTelemetry.value
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "PC_HARDWARE_HEALTH")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val tObj = resp?.optJSONObject("telemetry")
        if (tObj != null) {
            val telemetry = com.asistente.celular.nlu.pc.hardware.PcHardwareTelemetry(
                gpuName = tObj.optString("gpuName", "GPU Principal"),
                gpuUsagePercent = tObj.optDouble("gpuUsagePercent", 0.0).toFloat(),
                gpuTempCelsius = tObj.optInt("gpuTempCelsius", 45),
                vramUsedMb = tObj.optLong("vramUsedMb", 0L),
                vramTotalMb = tObj.optLong("vramTotalMb", 8192L),
                cpuUsagePercent = tObj.optDouble("cpuUsagePercent", 0.0).toFloat(),
                ramUsedMb = tObj.optLong("ramUsedMb", 0L),
                ramTotalMb = tObj.optLong("ramTotalMb", 16384L),
                activeHeavyProcess = tObj.optString("activeHeavyProcess").takeIf { it.isNotBlank() }
            )
            _hardwareTelemetry.value = telemetry
            telemetry
        } else {
            _hardwareTelemetry.value
        }
    }

    override suspend fun backupVault(vaultJson: String): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "VAULT_BACKUP_PUSH")
            put("requestId", reqId)
            put("vaultData", vaultJson)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        resp?.optBoolean("success", false) ?: false
    }

    override suspend fun listVaultBackups(): List<String> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext emptyList()
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "VAULT_BACKUP_LIST")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val arr = resp?.optJSONArray("backups") ?: org.json.JSONArray()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            list.add(item.optString("filename", ""))
        }
        list
    }

    override suspend fun restoreVault(filename: String?): String? = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext null
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "VAULT_BACKUP_PULL")
            put("requestId", reqId)
            if (filename != null) put("filename", filename)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val vObj = resp?.opt("vault")
        vObj?.toString()
    }

    override suspend fun getOpenWindows(): List<PcWindowInfo> = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext emptyList()
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "GET_OPEN_WINDOWS")
            put("requestId", reqId)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) { deferred.await() }
        val wArr = resp?.optJSONArray("windows") ?: org.json.JSONArray()
        val list = mutableListOf<PcWindowInfo>()
        for (i in 0 until wArr.length()) {
            val obj = wArr.optJSONObject(i) ?: continue
            list.add(
                PcWindowInfo(
                    hwnd = obj.optLong("hwnd", 0L),
                    title = obj.optString("title", ""),
                    process = obj.optString("process", ""),
                    pid = obj.optInt("pid", 0),
                    isForeground = obj.optBoolean("isForeground", false)
                )
            )
        }
        if (list.isNotEmpty()) {
            _openWindows.value = list
        }
        list
    }

    override suspend fun focusWindow(hwnd: Long): Boolean = withContext(Dispatchers.IO) {
        val ws = activeWebSocket ?: return@withContext false
        val reqId = UUID.randomUUID().toString().take(8)
        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[reqId] = deferred

        val req = JSONObject().apply {
            put("type", "FOCUS_WINDOW")
            put("requestId", reqId)
            put("hwnd", hwnd)
        }
        sendSignedPayload(req, ws)
        val resp = withTimeoutOrNull(2500L) { deferred.await() }
        val success = resp?.optBoolean("success", true) ?: true

        // Disparar refresco visual automático de la pantalla de Windows
        scope.launch(Dispatchers.IO) {
            delay(150)
            requestSnapshot()
        }
        success
    }
}


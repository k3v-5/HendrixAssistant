package com.asistente.celular.ui.pc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcEndpointConfig
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.pc.PcRemoteCoordinator
import kotlinx.coroutines.launch

/**
 * Pantalla completa del Espacio de Trabajo Remoto (Hendrix PC Workspace).
 * Integra el canvas visual con soporte de gestos táctiles, la barra de atajos ergonómica,
 * el Muelle Flotante de Acciones (Dynamic Island), lupa HD y conexión híbrida LAN/WAN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcRemoteWorkspaceScreen(
    pcBridge: PcWorkspaceBridge,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isConnected by pcBridge.isConnected.collectAsState()
    val telemetry by pcBridge.telemetry.collectAsState()
    val currentMode by pcBridge.currentMode.collectAsState()
    val snapshotBytes by pcBridge.latestSnapshot.collectAsState()

    val coordinator = pcBridge as? PcRemoteCoordinator
    val savedConfig = coordinator?.endpointConfig?.collectAsState()?.value
    val openWindows by coordinator?.openWindows?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    var showConfigDialog by remember { mutableStateOf(false) }
    var hostInput by remember(savedConfig) { mutableStateOf(savedConfig?.localIp?.takeIf { it.isNotBlank() } ?: "192.168.100.159") }
    var portInput by remember(savedConfig) { mutableStateOf((savedConfig?.port ?: 8899).toString()) }
    var airSyncPortInput by remember(savedConfig) { mutableStateOf((savedConfig?.airSyncPort ?: 8900).toString()) }
    var tunnelInput by remember(savedConfig) { mutableStateOf(savedConfig?.remoteTunnelUrl ?: "") }
    var pinInput by remember(savedConfig) { mutableStateOf(savedConfig?.pin?.takeIf { it.isNotBlank() } ?: "123456") }
    var snapshotQualitySelected by remember(savedConfig) { mutableStateOf(savedConfig?.snapshotQuality ?: 75) }
    var telemetryIntervalSelected by remember(savedConfig) { mutableStateOf(savedConfig?.telemetryIntervalMs ?: 3000L) }

    var isFocusWindowActive by remember { mutableStateOf(false) }
    var isLoupeActive by remember { mutableStateOf(false) }

    var dictationText by remember { mutableStateOf("") }
    var showDictationDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PC Workspace Remoto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes de PC", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cabecera con telemetría en tiempo real
                PcStatusHeaderAtom(
                    isConnected = isConnected,
                    telemetry = telemetry,
                    currentMode = currentMode,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Lienzo interactivo central con zoom, paneo, trackpad y lupa HD
                Box(modifier = Modifier.weight(1f)) {
                    PcSnapshotCanvasOrganism(
                        snapshotBytes = snapshotBytes,
                        onSendAction = { action ->
                            scope.launch { pcBridge.sendInteraction(action) }
                        },
                        onRequestRefresh = {
                            scope.launch { pcBridge.requestSnapshot(cropToActiveWindow = isFocusWindowActive) }
                        },
                        isLoupeEnabled = isLoupeActive,
                        isWindowFocusActive = isFocusWindowActive,
                        activeWindowBounds = telemetry?.activeWindowBounds,
                        openWindows = openWindows,
                        onRequestWindows = { pcBridge.getOpenWindows() },
                        onFocusWindow = { hwnd -> scope.launch { pcBridge.focusWindow(hwnd) } }
                    )
                }

                // Muelle de Acciones Flotante Ergonómico (Dynamic Island)
                PcFloatingActionDock(
                    isFocusWindowActive = isFocusWindowActive,
                    onToggleFocusWindow = {
                        isFocusWindowActive = !isFocusWindowActive
                        scope.launch { pcBridge.requestSnapshot(cropToActiveWindow = isFocusWindowActive) }
                    },
                    isLoupeActive = isLoupeActive,
                    onToggleLoupe = { isLoupeActive = !isLoupeActive },
                    onSendAction = { action ->
                        scope.launch { pcBridge.sendInteraction(action) }
                    },
                    onTypeText = { text ->
                        scope.launch { pcBridge.typeTextDirectly(text) }
                    },
                    onStartVoiceDictation = { showDictationDialog = true }
                )
            }
        }
    }

    // Diálogo de configuración y emparejamiento único ("Enroll Once, Connect Anywhere")
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(
                    text = "Ajustes de Enlace Hendrix PC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Configura el acceso para conectarte tanto en tu red local WiFi como fuera de casa por WAN/Internet:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("IP Local WiFi (LAN)") },
                        placeholder = { Text("ej. 192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Puerto WS") },
                            placeholder = { Text("8899") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = airSyncPortInput,
                            onValueChange = { airSyncPortInput = it },
                            label = { Text("AirSync") },
                            placeholder = { Text("8900") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = tunnelInput,
                        onValueChange = { tunnelInput = it },
                        label = { Text("Túnel Seguro WAN (Fuera de Casa)") },
                        placeholder = { Text("ej. https://xyz.trycloudflare.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN de Seguridad") },
                        placeholder = { Text("PIN de 6 dígitos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calidad de streaming WebP: $snapshotQualitySelected%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50 to "50% Rápido", 75 to "75% Equilibrado", 90 to "90% HD").forEach { (q, label) ->
                            FilterChip(
                                selected = snapshotQualitySelected == q,
                                onClick = { snapshotQualitySelected = q },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text(
                        text = "Frecuencia de telemetría: ${telemetryIntervalSelected / 1000}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000L to "1s Vivo", 3000L to "3s Normal", 5000L to "5s Eco").forEach { (ms, label) ->
                            FilterChip(
                                selected = telemetryIntervalSelected == ms,
                                onClick = { telemetryIntervalSelected = ms },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showConfigDialog = false
                            val parsedPort = portInput.trim().toIntOrNull() ?: 8899
                            val parsedAirSyncPort = airSyncPortInput.trim().toIntOrNull() ?: 8900
                            if (coordinator != null) {
                                val current = coordinator.endpointConfig.value
                                val newCfg = current.copy(
                                    localIp = hostInput.trim(),
                                    port = parsedPort,
                                    remoteTunnelUrl = tunnelInput.trim().takeIf { it.isNotBlank() },
                                    pin = pinInput.trim(),
                                    airSyncPort = parsedAirSyncPort,
                                    snapshotQuality = snapshotQualitySelected,
                                    telemetryIntervalMs = telemetryIntervalSelected
                                )
                                coordinator.saveConfig(newCfg)
                                scope.launch {
                                    coordinator.connectAuto()
                                }
                            } else {
                                scope.launch {
                                    pcBridge.connect(hostInput.trim(), parsedPort, pinInput.trim())
                                }
                            }
                        }
                    ) {
                        Text("Conectar Auto")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de dictado rápido por voz / texto
    if (showDictationDialog) {
        AlertDialog(
            onDismissRequest = { showDictationDialog = false },
            title = { Text(text = "Escribir en la PC", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Escribe el texto que deseas inyectar en el campo o ventana activa de la computadora:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = dictationText,
                        onValueChange = { dictationText = it },
                        label = { Text("Texto a escribir") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDictationDialog = false
                        if (dictationText.isNotBlank()) {
                            scope.launch {
                                pcBridge.typeTextDirectly(dictationText)
                                dictationText = ""
                            }
                        }
                    }
                ) {
                    Text("Enviar al PC")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDictationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

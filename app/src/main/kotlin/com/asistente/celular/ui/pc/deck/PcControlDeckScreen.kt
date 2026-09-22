package com.asistente.celular.ui.pc.deck

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.platform.LocalContext
import com.asistente.celular.ui.pc.deck.airsync.PcAirSyncCard
import com.asistente.celular.ui.pc.deck.terminal.PcWorkspaceContextCard
import com.asistente.celular.ui.pc.deck.macro.PcDynamicMacroDeckScreen
import com.asistente.celular.ui.automation.designer.VisualRoutineDesignerScreen

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile
import com.asistente.celular.nlu.pc.module.PcModuleActionRequest
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.pc.module.PcModuleManager
import kotlinx.coroutines.launch

import com.asistente.celular.ui.pc.PcSnapshotCanvasOrganism
import com.asistente.celular.ui.pc.PcFloatingActionDock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap

/**
 * Pestañas principales para navegación segmentada de la workstation de PC.
 * Prioriza la visualización de la pantalla en vivo y atajos esenciales sin scroll infinito.
 */
enum class PcDeckTab(val title: String, val icon: String) {
    SCREEN("Pantalla", "🖥️"),
    DECK("Atajos", "🎛️"),
    AIRSYNC("Archivos", "📁"),
    TELEMETRY("Sistema", "📊")
}

/**
 * Pantalla principal del Centro de Módulos y Plugins de PC (Quick Command Deck).
 * Diseñada para máxima ergonomía táctil, cero consumo de streaming de video
 * y organización por pestañas funcionales sin scroll infinito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcControlDeckScreen(
    pcBridge: PcWorkspaceBridge,
    moduleManager: PcModuleManager,
    routineRepository: com.asistente.celular.data.JsonAutomatedRoutineRepository? = null,
    macroDeckProfileRepository: com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRepository? = null,
    onEnterDeskStandby: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isConnected by pcBridge.isConnected.collectAsState()
    val telemetry by pcBridge.telemetry.collectAsState()

    val allModules by moduleManager.modules.collectAsState()
    val enabledModules by moduleManager.enabledModules.collectAsState()

    var selectedTab by remember { mutableStateOf(PcDeckTab.SCREEN) }
    var showSelectorDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var unlockPinInput by remember { mutableStateOf("") }
    var showMacroDeckScreen by remember { mutableStateOf(false) }
    var showRoutineDesignerScreen by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    val coordinator = pcBridge as? com.asistente.celular.pc.PcRemoteCoordinator
    val savedConfig by coordinator?.endpointConfig?.collectAsState() ?: remember { mutableStateOf(null) }
    var manualIpInput by remember(savedConfig) { mutableStateOf(savedConfig?.localIp?.takeIf { it.isNotBlank() } ?: "192.168.100.159") }
    var manualPortInput by remember(savedConfig) { mutableStateOf((savedConfig?.port ?: 8899).toString()) }
    var manualPinInput by remember(savedConfig) { mutableStateOf(savedConfig?.pin?.takeIf { it.isNotBlank() } ?: "123456") }

    val context = LocalContext.current
    val effectiveRoutineRepo = remember(routineRepository) {
        routineRepository ?: com.asistente.celular.data.JsonAutomatedRoutineRepository(context, scope)
    }
    val effectiveMacroRepo = remember(macroDeckProfileRepository) {
        macroDeckProfileRepository ?: com.asistente.celular.data.JsonMacroDeckProfileRepository(context, scope)
    }

    if (showMacroDeckScreen) {
        PcDynamicMacroDeckScreen(
            pcBridge = pcBridge,
            profileRepository = effectiveMacroRepo,
            onNavigateBack = { showMacroDeckScreen = false },
            onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
        return
    }

    if (showRoutineDesignerScreen) {
        VisualRoutineDesignerScreen(
            routineRepository = effectiveRoutineRepo,
            pcBridge = pcBridge,
            onNavigateBack = { showRoutineDesignerScreen = false },
            onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
        return
    }

    val latestDropzoneFile by pcBridge.dropzoneEvents.collectAsState()
    var activeDeliverable by remember { mutableStateOf<PcDropzoneFile?>(null) }

    LaunchedEffect(latestDropzoneFile) {
        if (latestDropzoneFile != null) {
            activeDeliverable = latestDropzoneFile
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected && activeDeliverable == null) {
            try {
                val info = pcBridge.queryDropzoneInfo()
                if (info != null && info.recentFiles.isNotEmpty()) {
                    activeDeliverable = info.recentFiles.first()
                }
            } catch (_: Exception) {
                // Silently ignore
            }
        }
    }

    val isModuleEnabled: (PcModuleId) -> Boolean = { id -> enabledModules.any { it.id == id } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B1120),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Workstation PC",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Indicador de conexión
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) (telemetry?.hostname ?: "Conectado") else "Desconectado",
                            fontSize = 11.sp,
                            color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showConnectDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurar Conexión PC",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val ok = pcBridge.wakeOnLan()
                            if (ok) {
                                snackbarHostState.showSnackbar("⚡ Paquete Wake-on-LAN emitido a la red local")
                            } else {
                                snackbarHostState.showSnackbar("⚠️ No se pudo enviar Wake-on-LAN (Verifica MAC registrada)")
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            contentDescription = "Despertar PC (Wake-on-LAN)",
                            tint = Color(0xFFFBBF24)
                        )
                    }
                    IconButton(onClick = { showSelectorDialog = true }) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = "Gestor de Módulos",
                            tint = Color(0xFF8B5CF6)
                        )
                    }
                    IconButton(onClick = { showMacroDeckScreen = true }) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = "Macro Deck Táctil",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    if (onEnterDeskStandby != null) {
                        IconButton(onClick = onEnterDeskStandby) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = "Modo Desk Standby",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de Pestañas Segmentadas
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8)
            ) {
                PcDeckTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = "${tab.icon} ${tab.title}",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) Color(0xFF38BDF8) else Color.LightGray,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // Contenido según Pestaña Activa
            when (selectedTab) {
                PcDeckTab.SCREEN -> {
                    ScreenLiveTabContent(
                        isConnected = isConnected,
                        pcBridge = pcBridge,
                        telemetry = telemetry,
                        isSessionLocked = telemetry?.isSessionLocked == true,
                        onConnectClick = { showConnectDialog = true },
                        onWakeClick = {
                            scope.launch {
                                val sent = pcBridge.wakeOnLan()
                                snackbarHostState.showSnackbar(
                                    if (sent) "⚡ Paquete Wake-on-LAN emitido a la red local"
                                    else "⚠️ No hay dirección MAC guardada para esta PC"
                                )
                            }
                        },
                        onUnlockClick = { showUnlockDialog = true },
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                }
                PcDeckTab.DECK -> {
                    DeckMacrosTabContent(
                        isConnected = isConnected,
                        isSessionLocked = telemetry?.isSessionLocked == true,
                        activeDeliverable = activeDeliverable,
                        pcBridge = pcBridge,
                        telemetry = telemetry,
                        onOpenMacroDeck = { showMacroDeckScreen = true },
                        onOpenDesigner = { showRoutineDesignerScreen = true },
                        onManageModules = { showSelectorDialog = true },
                        onEnterDeskStandby = onEnterDeskStandby,
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                }
                PcDeckTab.AIRSYNC -> {
                    AirSyncTabContent(
                        activeDeliverable = activeDeliverable,
                        isModuleEnabled = isModuleEnabled,
                        pcBridge = pcBridge,
                        onDismissDeliverable = { activeDeliverable = null },
                        onManageModules = { showSelectorDialog = true },
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                }
                PcDeckTab.TELEMETRY -> {
                    TelemetryTabContent(
                        telemetry = telemetry,
                        isModuleEnabled = isModuleEnabled,
                        enabledModules = enabledModules,
                        pcBridge = pcBridge,
                        onManageModules = { showSelectorDialog = true },
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                }
            }
        }
    }

    if (showSelectorDialog) {
        PcModuleSelectorDialog(
            allModules = allModules,
            onToggleModule = { id -> moduleManager.toggleModule(id) },
            onDismiss = { showSelectorDialog = false }
        )
    }

    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = {
                Text(
                    text = "🔓 Desbloquear Sesión de Windows",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ingresa tu PIN o contraseña de Windows para iniciar sesión remotamente:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = unlockPinInput,
                        onValueChange = { unlockPinInput = it },
                        label = { Text("PIN o Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pinToSend = unlockPinInput
                        showUnlockDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("🔓 Enviando comando de desbloqueo a Windows...")
                            val ok = pcBridge.unlockSession(pinToSend)
                            if (ok) {
                                snackbarHostState.showSnackbar("✅ Sesión de Windows desbloqueada")
                            } else {
                                snackbarHostState.showSnackbar("⚠️ La sesión aún continúa bloqueada")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Desbloquear", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Text(
                    text = "🔗 Conectar con Hendrix PC",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configura la IP y PIN de tu computadora para conectar directamente:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        label = { Text("IP de la PC (LAN)") },
                        placeholder = { Text("192.168.100.159") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualPortInput,
                            onValueChange = { manualPortInput = it },
                            label = { Text("Puerto") },
                            placeholder = { Text("8899") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualPinInput,
                            onValueChange = { manualPinInput = it },
                            label = { Text("PIN") },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ip = manualIpInput.trim()
                        val port = manualPortInput.toIntOrNull() ?: 8899
                        val pin = manualPinInput.trim()
                        showConnectDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Conectando a ws://$ip:$port/ws...")
                            coordinator?.updateEndpoint(ip, port, pin)
                            val ok = pcBridge.connect(ip, port, pin)
                            if (ok) {
                                snackbarHostState.showSnackbar("✅ Conectado con éxito a la PC")
                            } else {
                                snackbarHostState.showSnackbar("❌ No se pudo conectar. Verifica que el servidor de PC esté abierto y el firewall permitido.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Conectar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ---------------------------------------------------------------------------
// SUB-VISTAS DE PESTAÑAS ERGONÓMICAS
// ---------------------------------------------------------------------------

@Composable
private fun ScreenLiveTabContent(
    isConnected: Boolean,
    pcBridge: PcWorkspaceBridge,
    telemetry: com.asistente.celular.nlu.pc.PcSystemTelemetry?,
    isSessionLocked: Boolean,
    onConnectClick: () -> Unit,
    onWakeClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snapshotBytes by pcBridge.latestSnapshot.collectAsState()

    var isFocusWindowActive by remember { mutableStateOf(false) }
    var isLoupeActive by remember { mutableStateOf(false) }
    var showDictationDialog by remember { mutableStateOf(false) }

    // Auto-refresco continuo mientras la pestaña de pantalla esté activa y conectada
    LaunchedEffect(isConnected, isFocusWindowActive) {
        if (isConnected) {
            pcBridge.requestSnapshot(cropToActiveWindow = isFocusWindowActive)
            while (isActive) {
                delay(1500L)
                pcBridge.requestSnapshot(cropToActiveWindow = isFocusWindowActive)
            }
        }
    }

    if (!isConnected) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🖥️", fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pantalla de PC Remota",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Visualiza el escritorio de tu computadora en tiempo real y contrólalo con toques, gestos táctiles y teclado.",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔗 Conectar a la PC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onWakeClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ Despertar PC (Wake-on-LAN)", fontSize = 13.sp, color = Color(0xFFFBBF24))
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSessionLocked) {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔒", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pantalla de Windows bloqueada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onUnlockClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Desbloquear", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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
                    activeWindowBounds = telemetry?.activeWindowBounds
                )
            }

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

    if (showDictationDialog) {
        var dictationInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDictationDialog = false },
            title = { Text("🎙️ Escribir en la PC", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = dictationInput,
                    onValueChange = { dictationInput = it },
                    label = { Text("Texto a escribir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val toSend = dictationInput
                    showDictationDialog = false
                    scope.launch {
                        pcBridge.typeTextDirectly(toSend)
                        onShowSnackbar("⌨️ Texto enviado a la PC")
                    }
                }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDictationDialog = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun DeckMacrosTabContent(
    isConnected: Boolean,
    isSessionLocked: Boolean,
    activeDeliverable: PcDropzoneFile?,
    pcBridge: PcWorkspaceBridge,
    telemetry: com.asistente.celular.nlu.pc.PcSystemTelemetry?,
    onOpenMacroDeck: () -> Unit,
    onOpenDesigner: () -> Unit,
    onManageModules: () -> Unit,
    onEnterDeskStandby: (() -> Unit)?,
    onShowSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notificación de archivo entregable si existe
        activeDeliverable?.let { file ->
            item {
                PcDropzoneDeliverableCard(
                    file = file,
                    modifier = Modifier.fillMaxWidth(),
                    onDismiss = {}
                )
            }
        }

        // 1. Acciones Rápidas del Sistema
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acciones del Sistema",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(
                            icon = "🔒",
                            label = "Bloquear",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("lock")
                                    onShowSnackbar("🔒 PC Bloqueada")
                                }
                            }
                        )
                        QuickActionButton(
                            icon = "🪟",
                            label = "Escritorio",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    pcBridge.executeWindowCommand("minimize_all")
                                    onShowSnackbar("🪟 Mostrando Escritorio")
                                }
                            }
                        )
                        QuickActionButton(
                            icon = "❌",
                            label = "Cerrar App",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    pcBridge.executeWindowCommand("close")
                                    onShowSnackbar("❌ Ventana cerrada")
                                }
                            }
                        )
                        if (onEnterDeskStandby != null) {
                            QuickActionButton(
                                icon = "📺",
                                label = "Standby",
                                modifier = Modifier.weight(1f),
                                onClick = onEnterDeskStandby
                            )
                        }
                    }
                }
            }
        }

        // 2. Control de Volumen & Multimedia
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔊", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Multimedia & Audio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${telemetry?.masterVolumePercent ?: 50}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("volume_down")
                                    onShowSnackbar("🔉 Volumen -")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🔉 Bajar", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("volume_mute")
                                    onShowSnackbar("🔇 Silenciar")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🔇 Mute", fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("volume_up")
                                    onShowSnackbar("🔊 Volumen +")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🔊 Subir", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controles de Reproducción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("media_prev")
                                    onShowSnackbar("⏮️ Anterior")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⏮️", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("media_play_pause")
                                    onShowSnackbar("⏯️ Play/Pausa")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("⏯️ Play/Pausa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("media_next")
                                    onShowSnackbar("⏭️ Siguiente")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⏭️", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 3. Macros & Automatización
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎛️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Macros y Herramientas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenMacroDeck,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("🎛️ Macro Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenDesigner,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("🎨 Diseñar Rutina", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onManageModules,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("🧩 Gestionar Módulos (Blender, DAW, Adobe)", fontSize = 12.sp, color = Color(0xFF8B5CF6))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.LightGray,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StudioTabContent(
    isModuleEnabled: (PcModuleId) -> Boolean,
    enabledModules: List<com.asistente.celular.nlu.pc.module.PcModuleDefinition>,
    pcBridge: PcWorkspaceBridge,
    onManageModules: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val studioModulesActive = isModuleEnabled(PcModuleId.PROJECT_BROWSER) ||
            isModuleEnabled(PcModuleId.AUDIO_MIXER) ||
            isModuleEnabled(PcModuleId.WIRELESS_AUDIO_MONITOR) ||
            isModuleEnabled(PcModuleId.ABLETON_LIVE) ||
            isModuleEnabled(PcModuleId.FL_STUDIO)

    if (!studioModulesActive) {
        EmptyTabPlaceholder(
            icon = "🎵",
            title = "No hay módulos de Estudio activos",
            onAction = onManageModules
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isModuleEnabled(PcModuleId.PROJECT_BROWSER)) {
            item {
                PcProjectBrowserCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.AUDIO_MIXER)) {
            item {
                PcAudioMixerCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.WIRELESS_AUDIO_MONITOR)) {
            item {
                PcWirelessAudioMonitorCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        val dawModules = enabledModules.filter { it.id in listOf(PcModuleId.ABLETON_LIVE, PcModuleId.FL_STUDIO) }
        dawModules.forEach { module ->
            item(key = module.id.name) {
                PcModuleCardMolecule(
                    module = module,
                    onExecuteMacro = { moduleId, actionId ->
                        scope.launch {
                            val res = pcBridge.executeModuleAction(
                                PcModuleActionRequest(moduleId = moduleId, actionId = actionId)
                            )
                            onShowSnackbar(if (res.success) "⚡ ${res.message}" else "❌ Error: ${res.message}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AirSyncTabContent(
    activeDeliverable: PcDropzoneFile?,
    isModuleEnabled: (PcModuleId) -> Boolean,
    pcBridge: PcWorkspaceBridge,
    onDismissDeliverable: () -> Unit,
    onManageModules: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val airSyncActive = isModuleEnabled(PcModuleId.AIRSYNC_P2P) ||
            isModuleEnabled(PcModuleId.CLIPBOARD_MANAGER) ||
            activeDeliverable != null

    if (!airSyncActive) {
        EmptyTabPlaceholder(
            icon = "📁",
            title = "No hay módulos de AirSync activos",
            onAction = onManageModules
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        activeDeliverable?.let { file ->
            item {
                PcDropzoneDeliverableCard(
                    file = file,
                    modifier = Modifier.fillMaxWidth(),
                    onDismiss = onDismissDeliverable
                )
            }
        }

        if (isModuleEnabled(PcModuleId.AIRSYNC_P2P)) {
            item {
                PcAirSyncCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.CLIPBOARD_MANAGER)) {
            item {
                PcClipboardSnippetCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }
    }
}

@Composable
private fun TelemetryTabContent(
    telemetry: com.asistente.celular.nlu.pc.PcSystemTelemetry?,
    isModuleEnabled: (PcModuleId) -> Boolean,
    enabledModules: List<com.asistente.celular.nlu.pc.module.PcModuleDefinition>,
    pcBridge: PcWorkspaceBridge,
    onManageModules: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val telemetryActive = isModuleEnabled(PcModuleId.HARDWARE_WATCHDOG) ||
            isModuleEnabled(PcModuleId.SCREEN_COPILOT) ||
            isModuleEnabled(PcModuleId.WORKSPACE_TERMINAL_MEMORY) ||
            isModuleEnabled(PcModuleId.CUSTOM_PLUGINS) ||
            isModuleEnabled(PcModuleId.WEB_BROWSERS)

    if (telemetry == null && !telemetryActive) {
        EmptyTabPlaceholder(
            icon = "📊",
            title = "No hay telemetría ni módulos activos",
            onAction = onManageModules
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tarjeta principal de Recursos y Estado en Tiempo Real
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💻", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = telemetry?.hostname ?: "PC Remota",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Text(
                                text = "${telemetry?.roundTripLatencyMs ?: 0} ms • ${telemetry?.activeTransportType?.name ?: "LAN"}",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CPU
                    val cpuVal = (telemetry?.cpuPercent ?: 0f).coerceIn(0f, 100f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CPU", fontSize = 12.sp, color = Color.LightGray)
                        Text(
                            "${cpuVal.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cpuVal > 80f) Color(0xFFEF4444) else Color(0xFF38BDF8)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { cpuVal / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (cpuVal > 80f) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        trackColor = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // RAM
                    val ramVal = (telemetry?.ramPercent ?: 0f).coerceIn(0f, 100f)
                    val ramUsed = telemetry?.ramUsedGb ?: 0f
                    val ramTotal = telemetry?.ramTotalGb ?: 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RAM", fontSize = 12.sp, color = Color.LightGray)
                        Text(
                            text = if (ramTotal > 0f) "${ramVal.toInt()}% (${String.format(java.util.Locale.US, "%.1f", ramUsed)} / ${String.format(java.util.Locale.US, "%.1f", ramTotal)} GB)" else "${ramVal.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ramVal > 85f) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ramVal / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (ramVal > 85f) Color(0xFFEF4444) else Color(0xFF10B981),
                        trackColor = Color(0xFF0F172A)
                    )

                    if (telemetry != null && (!telemetry.activeWindowTitle.isNullOrBlank() || telemetry.isBatteryPresent)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!telemetry.activeWindowTitle.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("VENTANA ACTIVA", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = telemetry.activeWindowTitle,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (telemetry.isBatteryPresent) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("BATERÍA", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${telemetry.batteryPercent ?: 0}% ${if (telemetry.isBatteryCharging) "⚡" else "🔋"}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isModuleEnabled(PcModuleId.HARDWARE_WATCHDOG)) {
            item {
                PcHardwareWatchdogCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.SCREEN_COPILOT)) {
            item {
                PcScreenCopilotCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.WORKSPACE_TERMINAL_MEMORY)) {
            item {
                PcWorkspaceContextCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.CUSTOM_PLUGINS)) {
            item {
                PcCustomPluginsCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        val browserModules = enabledModules.filter { it.id == PcModuleId.WEB_BROWSERS }
        browserModules.forEach { module ->
            item(key = module.id.name) {
                PcModuleCardMolecule(
                    module = module,
                    onExecuteMacro = { moduleId, actionId ->
                        scope.launch {
                            val res = pcBridge.executeModuleAction(
                                PcModuleActionRequest(moduleId = moduleId, actionId = actionId)
                            )
                            onShowSnackbar(if (res.success) "⚡ ${res.message}" else "❌ Error: ${res.message}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyTabPlaceholder(
    icon: String,
    title: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Gestionar Módulos")
            }
        }
    }
}

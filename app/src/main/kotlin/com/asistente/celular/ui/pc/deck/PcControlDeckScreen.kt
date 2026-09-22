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

/**
 * Pestañas principales para navegación segmentada de la workstation de PC.
 * Elimina el desplazamiento vertical infinito agrupando las herramientas por contexto de uso.
 */
enum class PcDeckTab(val title: String, val icon: String) {
    DECK("Deck & Macros", "🎛️"),
    STUDIO("Estudio", "🎵"),
    AIRSYNC("AirSync", "📁"),
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

    var selectedTab by remember { mutableStateOf(PcDeckTab.DECK) }
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
                PcDeckTab.DECK -> {
                    DeckMacrosTabContent(
                        isConnected = isConnected,
                        isSessionLocked = telemetry?.isSessionLocked == true,
                        activeDeliverable = activeDeliverable,
                        isModuleEnabled = isModuleEnabled,
                        enabledModules = enabledModules,
                        pcBridge = pcBridge,
                        routineRepository = effectiveRoutineRepo,
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
                        onDismissDeliverable = { activeDeliverable = null },
                        onOpenMacroDeck = { showMacroDeckScreen = true },
                        onOpenDesigner = { showRoutineDesignerScreen = true },
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                }
                PcDeckTab.STUDIO -> {
                    StudioTabContent(
                        isModuleEnabled = isModuleEnabled,
                        enabledModules = enabledModules,
                        pcBridge = pcBridge,
                        onManageModules = { showSelectorDialog = true },
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
private fun DeckMacrosTabContent(
    isConnected: Boolean,
    isSessionLocked: Boolean,
    activeDeliverable: PcDropzoneFile?,
    isModuleEnabled: (PcModuleId) -> Boolean,
    enabledModules: List<com.asistente.celular.nlu.pc.module.PcModuleDefinition>,
    pcBridge: PcWorkspaceBridge,
    routineRepository: com.asistente.celular.data.JsonAutomatedRoutineRepository,
    onConnectClick: () -> Unit,
    onWakeClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onDismissDeliverable: () -> Unit,
    onOpenMacroDeck: () -> Unit,
    onOpenDesigner: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner Wake-on-LAN si está desconectado
        if (!isConnected) {
            item {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔌", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PC Desconectado / Apagado",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                            Text(
                                text = "Enciende o reactiva tu equipo de trabajo remotamente.",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onConnectClick,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🔗 Conectar", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onWakeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("⚡ Despertar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Banner Desbloqueo si la sesión está bloqueada
        if (isConnected && isSessionLocked) {
            item {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔒", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sesión de Windows Bloqueada",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = "Tu PC está en la pantalla de bloqueo (Winlogon).",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onUnlockClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("🔓 Desbloquear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Tarjeta de Lanzamiento Rápido de Macro Deck & Rutinas
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎛️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Centro de Macros Táctiles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "0 MB Streaming",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Macro Deck", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenDesigner,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Diseñar Rutina", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Entregable reciente si existe
        activeDeliverable?.let { file ->
            item {
                PcDropzoneDeliverableCard(
                    file = file,
                    modifier = Modifier.fillMaxWidth(),
                    onDismiss = onDismissDeliverable
                )
            }
        }

        // Módulos específicos del Deck
        if (isModuleEnabled(PcModuleId.STUDIO_SCENES)) {
            item {
                PcStudioScenesCard(
                    pcBridge = pcBridge,
                    modifier = Modifier.fillMaxWidth(),
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        if (isModuleEnabled(PcModuleId.AUTOMATED_ROUTINES)) {
            item {
                PcAutomatedRoutinesCard(
                    pcBridge = pcBridge,
                    routineRepository = routineRepository,
                    modifier = Modifier.fillMaxWidth(),
                    onOpenDesigner = onOpenDesigner,
                    onShowSnackbar = onShowSnackbar
                )
            }
        }

        // Otros módulos genéricos del deck si aplican
        val genericDeckModules = enabledModules.filter {
            it.id in listOf(PcModuleId.ADOBE_CREATIVE, PcModuleId.BLENDER, PcModuleId.UNREAL_ENGINE)
        }
        genericDeckModules.forEach { module ->
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

    if (!telemetryActive) {
        EmptyTabPlaceholder(
            icon = "📊",
            title = "No hay módulos de Telemetría activos",
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

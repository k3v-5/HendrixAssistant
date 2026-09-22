package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
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
import com.asistente.celular.nlu.pc.module.PcModuleCategory
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.pc.module.PcModuleManager
import kotlinx.coroutines.launch

/**
 * Pantalla principal del Centro de Módulos y Plugins de PC (Quick Command Deck).
 * Diseñada para máxima ergonomía táctil y cero consumo de datos de streaming de video.
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

    var showSelectorDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<PcModuleCategory?>(null) }
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
            } catch (e: Exception) {
                // Silently ignore
            }
        }
    }

    val filteredModules = remember(enabledModules, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            enabledModules
        } else {
            enabledModules.filter { it.category == selectedCategoryFilter }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B1120),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Centro de Control PC",
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
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
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
                                snackbarHostState.showSnackbar("⚠️ No se pudo enviar el paquete Wake-on-LAN (Verifica si hay MAC registrada)")
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
                    IconButton(onClick = { showRoutineDesignerScreen = true }) {
                        Icon(
                            Icons.Default.AutoFixHigh,
                            contentDescription = "Diseñador de Rutinas",
                            tint = Color(0xFF10B981)
                        )
                    }
                    if (onEnterDeskStandby != null) {
                        IconButton(onClick = onEnterDeskStandby) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = "Modo Pantalla Escritorio (Desk Standby)",
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
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Banner Wake-on-LAN cuando el PC está desconectado o apagado
            if (!isConnected) {
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
                                onClick = { showConnectDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🔗 Conectar", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val sent = pcBridge.wakeOnLan()
                                        if (sent) {
                                            snackbarHostState.showSnackbar("⚡ Paquete Wake-on-LAN emitido a la red local")
                                        } else {
                                            snackbarHostState.showSnackbar("⚠️ No hay dirección MAC guardada para esta PC")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("⚡ Despertar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Banner Desbloqueo Remoto cuando el PC está conectado pero la sesión de Windows está bloqueada
            if (isConnected && telemetry?.isSessionLocked == true) {
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
                                text = "Tu PC está encendida en la pantalla de bloqueo (Winlogon).",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showUnlockDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("🔓 Desbloquear", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Banner de Ahorro de Datos Extremo
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🛡️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Modo Ahorro de Datos: 0 MB de video",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Envío directo de macros táctiles ultraligeros a ${telemetry?.hostname ?: "PC Remoto"}.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tarjeta destacada de entregables recientes (Renders / Audio en Drive)
            activeDeliverable?.let { file ->
                PcDropzoneDeliverableCard(
                    file = file,
                    modifier = Modifier.fillMaxWidth(),
                    onDismiss = { activeDeliverable = null }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Chips horizontales de categorías
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("Todos (${enabledModules.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF8B5CF6),
                        selectedLabelColor = Color.White
                    )
                )

                PcModuleCategory.entries.forEach { category ->
                    val count = enabledModules.count { it.category == category }
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                        },
                        label = {
                            Text("${category.iconEmoji} ${category.displayName} ($count)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista o Grid de tarjetas de módulos activos
            if (filteredModules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🧩", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay módulos activos en esta categoría",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showSelectorDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Administrar Módulos")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredModules, key = { it.id.name }) { module ->
                        when (module.id) {
                            PcModuleId.CLIPBOARD_MANAGER -> {
                                PcClipboardSnippetCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.AUDIO_MIXER -> {
                                PcAudioMixerCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.STUDIO_SCENES -> {
                                PcStudioScenesCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.PROJECT_BROWSER -> {
                                PcProjectBrowserCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.HARDWARE_WATCHDOG -> {
                                PcHardwareWatchdogCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.SCREEN_COPILOT -> {
                                PcScreenCopilotCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.CUSTOM_PLUGINS -> {
                                PcCustomPluginsCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.WIRELESS_AUDIO_MONITOR -> {
                                PcWirelessAudioMonitorCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.AUTOMATED_ROUTINES -> {
                                routineRepository?.let { repo ->
                                    PcAutomatedRoutinesCard(
                                        pcBridge = pcBridge,
                                        routineRepository = repo,
                                        modifier = Modifier.fillMaxWidth(),
                                        onOpenDesigner = { showRoutineDesignerScreen = true },
                                        onShowSnackbar = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                } ?: run {
                                    PcModuleCardMolecule(
                                        module = module,
                                        onExecuteMacro = { moduleId, actionId ->
                                            scope.launch {
                                                val res = pcBridge.executeModuleAction(
                                                    PcModuleActionRequest(moduleId = moduleId, actionId = actionId)
                                                )
                                                snackbarHostState.showSnackbar(
                                                    message = if (res.success) "⚡ ${res.message}" else "❌ Error: ${res.message}"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            PcModuleId.AIRSYNC_P2P -> {
                                PcAirSyncCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            PcModuleId.WORKSPACE_TERMINAL_MEMORY -> {
                                PcWorkspaceContextCard(
                                    pcBridge = pcBridge,
                                    modifier = Modifier.fillMaxWidth(),
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            else -> {
                                PcModuleCardMolecule(
                                    module = module,
                                    onExecuteMacro = { moduleId, actionId ->
                                        scope.launch {
                                            val res = pcBridge.executeModuleAction(
                                                PcModuleActionRequest(moduleId = moduleId, actionId = actionId)
                                            )
                                            snackbarHostState.showSnackbar(
                                                message = if (res.success) "⚡ ${res.message}" else "❌ Error: ${res.message}"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
                            coordinator?.updateEndpoint(ip, port)
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

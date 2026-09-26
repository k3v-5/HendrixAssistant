package com.asistente.celular.ui.pc.deck

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.asistente.celular.nlu.pc.module.PcModuleCategory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.asistente.celular.nlu.pc.PcWindowInfo
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
import androidx.compose.ui.draw.clip
import com.asistente.celular.ui.components.oled.OledCard
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated

/**
 * Pestañas principales para navegación segmentada de la workstation de PC.
 * Prioriza la visualización de la pantalla en vivo y atajos esenciales sin scroll infinito.
 */
enum class PcDeckTab(val title: String, val icon: ImageVector) {
    SCREEN("Pantalla", Icons.Default.DesktopWindows),
    DECK("Atajos", Icons.Default.Tune),
    AIRSYNC("Archivos", Icons.Default.FolderShared),
    TELEMETRY("Sistema", Icons.Default.BarChart)
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
    onImmersiveModeChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isConnected by pcBridge.isConnected.collectAsState()
    val telemetry by pcBridge.telemetry.collectAsState()

    val allModules by moduleManager.modules.collectAsState()
    val enabledModules by moduleManager.enabledModules.collectAsState()

    var selectedTab by remember { mutableStateOf(PcDeckTab.SCREEN) }
    val isScreenImmersive = selectedTab == PcDeckTab.SCREEN

    LaunchedEffect(isScreenImmersive) {
        onImmersiveModeChanged?.invoke(isScreenImmersive)
    }

    var showSelectorDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var unlockPinInput by remember { mutableStateOf("") }
    var showMacroDeckScreen by remember { mutableStateOf(false) }
    var showRoutineDesignerScreen by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    val coordinator = pcBridge as? com.asistente.celular.pc.PcRemoteCoordinator
    val savedConfig by coordinator?.endpointConfig?.collectAsState() ?: remember { mutableStateOf(null) }
    val openWindows by coordinator?.openWindows?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    var manualIpInput by remember(savedConfig) { mutableStateOf(savedConfig?.localIp?.takeIf { it.isNotBlank() } ?: "192.168.100.159") }
    var manualPortInput by remember(savedConfig) { mutableStateOf((savedConfig?.port ?: 8899).toString()) }
    var manualPinInput by remember(savedConfig) { mutableStateOf(savedConfig?.pin?.takeIf { it.isNotBlank() } ?: "123456") }

    val context = LocalContext.current
    val otaCoordinator = remember(pcBridge, context) {
        com.asistente.celular.pc.ota.PcOtaUpdateCoordinator(
            context = context.applicationContext,
            pcBridge = pcBridge,
            scope = scope
        )
    }
    val otaUpdateInfo by otaCoordinator.updateInfo.collectAsState()
    val otaDownloadState by otaCoordinator.downloadState.collectAsState()

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

    val anyDeckDialogVisible = showConnectDialog || showSelectorDialog || showUnlockDialog
    BackHandler(enabled = anyDeckDialogVisible) {
        showConnectDialog = false
        showSelectorDialog = false
        showUnlockDialog = false
    }
    BackHandler(enabled = !anyDeckDialogVisible && selectedTab != PcDeckTab.DECK) {
        selectedTab = PcDeckTab.DECK
    }
    BackHandler(enabled = !anyDeckDialogVisible && selectedTab == PcDeckTab.DECK) {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoidBlack,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (!isScreenImmersive) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "WORKSTATION PC",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Indicador de conexión
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isConnected) NeonCyan else NeonRed,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) (telemetry?.hostname ?: "ONLINE") else "OFFLINE",
                                fontSize = 10.sp,
                                color = if (isConnected) NeonCyan else NeonRed,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showConnectDialog = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Configurar Conexión PC",
                                tint = NeonCyan
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val ok = pcBridge.wakeOnLan()
                                if (ok) {
                                    snackbarHostState.showSnackbar("Paquete Wake-on-LAN emitido a la red local")
                                } else {
                                    snackbarHostState.showSnackbar("No se pudo enviar Wake-on-LAN (Verifica MAC registrada)")
                                }
                            }
                        }) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = "Despertar PC (Wake-on-LAN)",
                                tint = NeonAmber
                            )
                        }
                        IconButton(onClick = { showSelectorDialog = true }) {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = "Gestor de Módulos",
                                tint = TextSecondary
                            )
                        }
                        if (onEnterDeskStandby != null) {
                            IconButton(onClick = onEnterDeskStandby) {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = "Modo Desk Standby",
                                    tint = NeonCyan
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBlack)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isScreenImmersive) PaddingValues(0.dp) else paddingValues)
        ) {
            if (!isScreenImmersive) {
                // Barra de Pestañas Segmentadas en VoidBlack con línea indicadora NeonCyan
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = VoidBlack,
                    contentColor = NeonCyan,
                    divider = {
                        androidx.compose.material3.HorizontalDivider(
                            thickness = 1.dp,
                            color = VoidBorder
                        )
                    }
                ) {
                    PcDeckTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedTab == tab) NeonCyan else TextMuted
                                )
                            },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) NeonCyan else TextMuted,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                // Banner proactivo si hay una nueva compilación OTA disponible en la PC
                if (otaUpdateInfo?.available == true) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdateAlt,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nueva compilación disponible en PC",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val mbSize = (otaUpdateInfo?.apkSizeBytes ?: 0L).toFloat() / (1024f * 1024f)
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.1f", mbSize)} MB • Lista para instalar",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            when (val state = otaDownloadState) {
                                is com.asistente.celular.nlu.pc.ota.OtaDownloadState.Downloading -> {
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                                is com.asistente.celular.nlu.pc.ota.OtaDownloadState.ReadyToInstall -> {
                                    Button(
                                        onClick = { otaCoordinator.triggerInstall(state.apkFile) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Instalar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Descargando actualización desde la PC...")
                                                otaCoordinator.downloadAndInstall(otaUpdateInfo!!)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Actualizar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                    }
                                }
                            }
                        }
                    }
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
                        openWindows = openWindows,
                        onConnectClick = { showConnectDialog = true },
                        onWakeClick = {
                            scope.launch {
                                val sent = pcBridge.wakeOnLan()
                                snackbarHostState.showSnackbar(
                                    if (sent) "Paquete Wake-on-LAN emitido a la red local"
                                    else "No hay dirección MAC guardada para esta PC"
                                )
                            }
                        },
                        onUnlockClick = { showUnlockDialog = true },
                        onExitScreenTab = { selectedTab = PcDeckTab.DECK },
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
                        enabledModules = enabledModules,
                        isModuleEnabled = isModuleEnabled,
                        routineRepository = effectiveRoutineRepo,
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
                        otaCoordinator = otaCoordinator,
                        otaUpdateInfo = otaUpdateInfo,
                        otaDownloadState = otaDownloadState,
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
            onEnableAll = { moduleManager.enableAllModules() },
            onDisableAll = { moduleManager.disableAllModules() },
            onResetDefaults = { moduleManager.resetToDefaults() },
            onDismiss = { showSelectorDialog = false }
        )
    }

    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Desbloquear Sesión de Windows",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Ingresa tu PIN o contraseña de Windows para iniciar sesión remotamente:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = unlockPinInput,
                        onValueChange = { unlockPinInput = it },
                        label = { Text("PIN o Contraseña") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = VoidBorder,
                            focusedContainerColor = VoidBlack,
                            unfocusedContainerColor = VoidBlack,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
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
                            snackbarHostState.showSnackbar("Enviando comando de desbloqueo a Windows...")
                            val ok = pcBridge.unlockSession(pinToSend)
                            if (ok) {
                                snackbarHostState.showSnackbar("Sesión de Windows desbloqueada")
                            } else {
                                snackbarHostState.showSnackbar("La sesión aún continúa bloqueada")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                ) {
                    Text("Desbloquear", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = VoidSurfaceElevated
        )
    }

    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Conectar con Hendrix PC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configura la IP y PIN de tu computadora para conectar directamente:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = { manualIpInput = it },
                        label = { Text("IP de la PC (LAN)") },
                        placeholder = { Text("192.168.100.159") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = VoidBorder,
                            focusedContainerColor = VoidBlack,
                            unfocusedContainerColor = VoidBlack,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
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
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = VoidBorder,
                                focusedContainerColor = VoidBlack,
                                unfocusedContainerColor = VoidBlack,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = manualPinInput,
                            onValueChange = { manualPinInput = it },
                            label = { Text("PIN") },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = VoidBorder,
                                focusedContainerColor = VoidBlack,
                                unfocusedContainerColor = VoidBlack,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
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
                                snackbarHostState.showSnackbar("Conectado con éxito a la PC")
                            } else {
                                snackbarHostState.showSnackbar("No se pudo conectar. Verifica que el servidor de PC esté abierto y el firewall permitido.")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Conectar", fontWeight = FontWeight.Bold, color = VoidBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = VoidSurfaceElevated
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
    openWindows: List<PcWindowInfo> = emptyList(),
    onConnectClick: () -> Unit,
    onWakeClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onExitScreenTab: () -> Unit = {},
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
                color = VoidSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, VoidBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pantalla de PC Remota",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Visualiza el escritorio de tu computadora en tiempo real y contrólalo con toques, gestos táctiles y teclado.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectar a la PC", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VoidBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onWakeClick,
                        border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Despertar PC (Wake-on-LAN)", fontSize = 13.sp, color = NeonAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = onExitScreenTab) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ir al Deck de Atajos", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isSessionLocked) {
                Surface(
                    color = NeonAmber.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pantalla de Windows bloqueada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonAmber,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onUnlockClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Desbloquear", fontSize = 11.sp, color = VoidBlack, fontWeight = FontWeight.Bold)
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
                    activeWindowBounds = telemetry?.activeWindowBounds,
                    openWindows = openWindows,
                    onRequestWindows = { pcBridge.getOpenWindows() },
                    onFocusWindow = { hwnd -> scope.launch { pcBridge.focusWindow(hwnd) } }
                )

                // Botón flotante para regresar al Deck / Atajos en modo inmersivo
                Surface(
                    onClick = onExitScreenTab,
                    color = VoidSurfaceElevated.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, VoidBorder),
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deck",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Text("Escribir en la PC", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                OutlinedTextField(
                    value = dictationInput,
                    onValueChange = { dictationInput = it },
                    label = { Text("Texto a escribir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = VoidBorder,
                        focusedContainerColor = VoidBlack,
                        unfocusedContainerColor = VoidBlack,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSend = dictationInput
                        showDictationDialog = false
                        scope.launch {
                            pcBridge.typeTextDirectly(toSend)
                            onShowSnackbar("⌨️ Texto enviado a la PC")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Enviar", fontWeight = FontWeight.Bold, color = VoidBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDictationDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = VoidSurfaceElevated
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
    enabledModules: List<com.asistente.celular.nlu.pc.module.PcModuleDefinition>,
    isModuleEnabled: (PcModuleId) -> Boolean,
    routineRepository: com.asistente.celular.data.JsonAutomatedRoutineRepository?,
    onOpenMacroDeck: () -> Unit,
    onOpenDesigner: () -> Unit,
    onManageModules: () -> Unit,
    onEnterDeskStandby: (() -> Unit)?,
    onShowSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf<PcModuleCategory?>(null) }

    val filteredModules = remember(enabledModules, selectedCategory) {
        if (selectedCategory == null) enabledModules
        else enabledModules.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
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

        // 1. Barra de Control Principal del Quick Deck (Header con Launcher a MacroDeck, Módulos y Estado)
        item {
            Surface(
                color = VoidSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, VoidBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Quick Command Deck",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isConnected) "Conectado • ${telemetry?.hostname ?: "PC"} (${telemetry?.roundTripLatencyMs ?: 0}ms)" else "Desconectado de PC",
                                    fontSize = 11.sp,
                                    color = if (isConnected) NeonGreen else NeonRed
                                )
                            }
                        }

                        // Botón Gestionar Módulos con badge de activos
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onManageModules()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = NeonLilac.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonLilac.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Extension, contentDescription = null, tint = NeonLilac, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${enabledModules.size} suites",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonLilac
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lanzadores secundarios: Macro Deck con perfiles y Diseñador de Rutinas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenMacroDeck()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Macro Deck Full", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenDesigner()
                            },
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Brush, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diseñar Rutina", fontSize = 12.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Acciones Rápidas del Sistema & Control de Medios
        item {
            Surface(
                color = VoidSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, VoidBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Acciones de Sistema
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acciones del Sistema",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Windows OS",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Lock,
                            label = "Bloquear",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("lock")
                                    onShowSnackbar("PC Bloqueada")
                                }
                            }
                        )
                        QuickActionButton(
                            icon = Icons.Default.DesktopWindows,
                            label = "Escritorio",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    pcBridge.executeWindowCommand("minimize_all")
                                    onShowSnackbar("Mostrando Escritorio")
                                }
                            }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Close,
                            label = "Cerrar App",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    pcBridge.executeWindowCommand("close")
                                    onShowSnackbar("Ventana cerrada")
                                }
                            }
                        )
                        if (onEnterDeskStandby != null) {
                            QuickActionButton(
                                icon = Icons.Default.Tv,
                                label = "Standby",
                                modifier = Modifier.weight(1f),
                                onClick = onEnterDeskStandby
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Multimedia & Volumen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = NeonLilac, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Multimedia & Audio",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "${telemetry?.masterVolumePercent ?: 50}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Anterior
                        Surface(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("media_prev")
                                    onShowSnackbar("Anterior")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = VoidSurfaceElevated,
                            border = BorderStroke(1.dp, VoidBorder),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Play / Pause
                        Surface(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("media_play_pause")
                                    onShowSnackbar("Play / Pausa")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = NeonCyan,
                            modifier = Modifier.weight(1.4f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play/Pausa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = VoidBlack)
                                }
                            }
                        }

                        // Siguiente
                        Surface(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("media_next")
                                    onShowSnackbar("Siguiente")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = VoidSurfaceElevated,
                            border = BorderStroke(1.dp, VoidBorder),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Mute
                        Surface(
                            onClick = {
                                scope.launch {
                                    pcBridge.executeQuickCommand("volume_mute")
                                    onShowSnackbar("Mute conmutado")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = VoidSurfaceElevated,
                            border = BorderStroke(1.dp, VoidBorder),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Mute", tint = NeonAmber, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // 3. Selector de Categorías de Módulos (Filtro Horizontal)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todos (${enabledModules.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = VoidSurface,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selectedCategory == null) NeonCyan else VoidBorder),
                    shape = RoundedCornerShape(10.dp)
                )

                PcModuleCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val count = enabledModules.count { it.category == cat }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (isSelected) null else cat },
                        label = { Text("${cat.iconEmoji} ${cat.displayName} ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonLilac.copy(alpha = 0.25f),
                            selectedLabelColor = NeonLilac,
                            containerColor = VoidSurface,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) NeonLilac else VoidBorder),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 4. Módulos y Suites Activos en el Deck
        if (filteredModules.isEmpty()) {
            item {
                Surface(
                    color = VoidSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, VoidBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedCategory != null) "No tienes módulos activos de ${selectedCategory?.displayName}" else "No hay módulos activos en tu Deck",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Activa Unreal Engine, Blender, DAWs o tus suites favoritas para verlas aquí.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onManageModules()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gestionar Suites & Módulos", color = VoidBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            filteredModules.forEach { module ->
                when (module.id) {
                    PcModuleId.CUSTOM_PLUGINS -> {
                        item(key = module.id.name) {
                            PcCustomPluginsCard(
                                pcBridge = pcBridge,
                                modifier = Modifier.fillMaxWidth(),
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }
                    PcModuleId.AUDIO_MIXER -> {
                        item(key = module.id.name) {
                            PcAudioMixerCard(
                                pcBridge = pcBridge,
                                modifier = Modifier.fillMaxWidth(),
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }
                    PcModuleId.STUDIO_SCENES -> {
                        item(key = module.id.name) {
                            PcStudioScenesCard(
                                pcBridge = pcBridge,
                                modifier = Modifier.fillMaxWidth(),
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }
                    PcModuleId.AUTOMATED_ROUTINES -> {
                        if (routineRepository != null) {
                            item(key = module.id.name) {
                                PcAutomatedRoutinesCard(
                                    pcBridge = pcBridge,
                                    routineRepository = routineRepository,
                                    modifier = Modifier.fillMaxWidth(),
                                    onOpenDesigner = onOpenDesigner,
                                    onShowSnackbar = onShowSnackbar
                                )
                            }
                        }
                    }
                    PcModuleId.PROJECT_BROWSER -> {
                        item(key = module.id.name) {
                            PcProjectBrowserCard(
                                pcBridge = pcBridge,
                                modifier = Modifier.fillMaxWidth(),
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }
                    PcModuleId.WIRELESS_AUDIO_MONITOR -> {
                        item(key = module.id.name) {
                            PcWirelessAudioMonitorCard(
                                pcBridge = pcBridge,
                                modifier = Modifier.fillMaxWidth(),
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }
                    PcModuleId.CLIPBOARD_MANAGER -> {
                        item(key = module.id.name) {
                            PcClipboardSnippetCard(
                                pcBridge = pcBridge,
                                modifier = Modifier.fillMaxWidth(),
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }
                    // Módulos de software con comandos macro (Unreal Engine 5, Blender 3D, Adobe Suite, Ableton Live, FL Studio, Web Browsers, etc.)
                    else -> {
                        item(key = module.id.name) {
                            PcModuleCardMolecule(
                                module = module,
                                onExecuteMacro = { moduleId, actionId ->
                                    scope.launch {
                                        val res = pcBridge.executeModuleAction(
                                            PcModuleActionRequest(moduleId = moduleId, actionId = actionId)
                                        )
                                        onShowSnackbar(if (res.success) res.message else "Error: ${res.message}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = VoidSurfaceElevated,
        border = BorderStroke(1.dp, VoidBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonLilac,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
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
            icon = Icons.Default.FolderShared,
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
    otaCoordinator: com.asistente.celular.pc.ota.PcOtaUpdateCoordinator? = null,
    otaUpdateInfo: com.asistente.celular.nlu.pc.ota.PcAppUpdateInfo? = null,
    otaDownloadState: com.asistente.celular.nlu.pc.ota.OtaDownloadState = com.asistente.celular.nlu.pc.ota.OtaDownloadState.Idle,
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
            icon = Icons.Default.BarChart,
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
                color = VoidSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VoidBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = telemetry?.hostname ?: "PC Remota",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = VoidSurfaceElevated,
                            border = BorderStroke(1.dp, VoidBorder)
                        ) {
                            Text(
                                text = "${telemetry?.roundTripLatencyMs ?: 0} ms • ${telemetry?.activeTransportType?.name ?: "LAN"}",
                                fontSize = 11.sp,
                                color = NeonGreen,
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
                        Text("CPU", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            "${cpuVal.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cpuVal > 80f) NeonRed else NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { cpuVal / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (cpuVal > 80f) NeonRed else NeonCyan,
                        trackColor = VoidBlack
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
                        Text("RAM", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = if (ramTotal > 0f) "${ramVal.toInt()}% (${String.format(java.util.Locale.US, "%.1f", ramUsed)} / ${String.format(java.util.Locale.US, "%.1f", ramTotal)} GB)" else "${ramVal.toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ramVal > 85f) NeonRed else NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ramVal / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (ramVal > 85f) NeonRed else NeonCyan,
                        trackColor = VoidBlack
                    )

                    // Footer telemetry
                    if (telemetry != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (telemetry.activeWindowTitle.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VoidSurfaceElevated,
                                    border = BorderStroke(1.dp, VoidBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("VENTANA ACTIVA", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = telemetry.activeWindowTitle,
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (telemetry.isBatteryPresent) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VoidSurfaceElevated,
                                    border = BorderStroke(1.dp, VoidBorder)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("BATERÍA", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (telemetry.isBatteryCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                                contentDescription = null,
                                                tint = NeonAmber,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${telemetry.batteryPercent ?: 0}%",
                                                fontSize = 11.sp,
                                                color = NeonAmber,
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
        }

        // Tarjeta de Actualizaciones de App (OTA Local)
        item {
            val otaInfo = otaUpdateInfo
            val otaState = otaDownloadState

            Surface(
                color = VoidSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VoidBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Actualizaciones de App (OTA Local)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    onShowSnackbar("Buscando nueva versión en la PC...")
                                    val res = otaCoordinator?.checkForUpdates()
                                    if (res != null && res.available) {
                                        onShowSnackbar("¡Nueva compilación encontrada en la PC!")
                                    } else {
                                        onShowSnackbar("Tu app ya está al día con la PC.")
                                    }
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (otaInfo != null && otaInfo.available) {
                        val mbSize = otaInfo.apkSizeBytes.toFloat() / (1024f * 1024f)
                        Text(
                            text = "¡Hay una nueva compilación de desarrollo lista en tu PC!",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tamaño: ${String.format(java.util.Locale.US, "%.1f", mbSize)} MB • Archivo: ${otaInfo.fileName}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        when (val state = otaState) {
                            is com.asistente.celular.nlu.pc.ota.OtaDownloadState.Downloading -> {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Descargando desde la PC...", fontSize = 11.sp, color = TextSecondary)
                                        Text("${(state.progress * 100).toInt()}%", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = NeonCyan,
                                        trackColor = VoidBlack
                                    )
                                }
                            }
                            is com.asistente.celular.nlu.pc.ota.OtaDownloadState.ReadyToInstall -> {
                                Button(
                                    onClick = { otaCoordinator?.triggerInstall(state.apkFile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Instalar Actualización Ahora", fontWeight = FontWeight.Bold, color = VoidBlack)
                                    }
                                }
                            }
                            is com.asistente.celular.nlu.pc.ota.OtaDownloadState.Error -> {
                                Text(
                                    text = "Error: ${state.message}",
                                    fontSize = 11.sp,
                                    color = NeonRed
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            otaCoordinator?.downloadAndInstall(otaInfo)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Reintentar Descarga", color = VoidBlack)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            onShowSnackbar("Iniciando descarga OTA de ${otaInfo.fileName}...")
                                            otaCoordinator?.downloadAndInstall(otaInfo)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Descargar e Instalar de Inmediato", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VoidBlack)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Si Play Protect muestra una advertencia, pulsa en 'Más detalles' e 'Instalar de todas formas'.",
                                fontSize = 10.sp,
                                color = NeonAmber
                            )
                        }
                    } else {
                        Text(
                            text = "Tu aplicación móvil está sincronizada con la última versión de desarrollo de tu PC.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    onShowSnackbar("Comprobando compilaciones en PC...")
                                    val res = otaCoordinator?.checkForUpdates()
                                    if (res != null && res.available) {
                                        onShowSnackbar("¡Nueva compilación encontrada en la PC!")
                                    } else {
                                        onShowSnackbar("No hay nuevas compilaciones. Ya tienes la última versión.")
                                    }
                                }
                            },
                            border = BorderStroke(1.dp, VoidBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscar Actualizaciones en la PC", fontSize = 12.sp, color = TextPrimary)
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
                            onShowSnackbar(if (res.success) res.message else "Error: ${res.message}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyTabPlaceholder(
    icon: ImageVector,
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Gestionar Módulos", color = VoidBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}

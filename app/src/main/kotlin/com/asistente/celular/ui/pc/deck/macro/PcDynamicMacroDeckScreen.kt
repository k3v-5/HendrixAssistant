package com.asistente.celular.ui.pc.deck.macro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Tune
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckAction
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckControl
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfile
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRegistry
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcDynamicMacroDeckScreen(
    pcBridge: PcWorkspaceBridge,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileRepository: MacroDeckProfileRepository? = null,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveRepo = remember(profileRepository) {
        profileRepository ?: com.asistente.celular.data.JsonMacroDeckProfileRepository(context, scope)
    }
    val allProfiles by effectiveRepo.profiles.collectAsState()

    val foregroundApp by pcBridge.foregroundApp.collectAsState()
    val isConnected by pcBridge.isConnected.collectAsState()

    var autoFollowForeground by remember { mutableStateOf(true) }
    var manualProfileOverride by remember { mutableStateOf<MacroDeckProfile?>(null) }
    var showStudioScreen by remember { mutableStateOf(false) }

    // Determinar el perfil activo
    val activeProfile = remember(allProfiles, foregroundApp, autoFollowForeground, manualProfileOverride) {
        if (!autoFollowForeground && manualProfileOverride != null) {
            allProfiles.firstOrNull { it.id == manualProfileOverride!!.id } ?: manualProfileOverride!!
        } else if (foregroundApp.isNotBlank()) {
            effectiveRepo.findProfileForProcess(foregroundApp)
        } else {
            manualProfileOverride?.let { override -> allProfiles.firstOrNull { it.id == override.id } }
                ?: allProfiles.firstOrNull { it.id == MacroDeckProfileRegistry.DEFAULT_DESKTOP_PROFILE.id }
                ?: allProfiles.firstOrNull()
                ?: MacroDeckProfileRegistry.DEFAULT_DESKTOP_PROFILE
        }
    }

    if (showStudioScreen) {
        CustomMacroDeckStudioScreen(
            repository = effectiveRepo,
            pcBridge = pcBridge,
            initialProfileId = activeProfile.id,
            onNavigateBack = { showStudioScreen = false },
            onShowSnackbar = onShowSnackbar
        )
        return
    }

    val accentColor = Color(activeProfile.themeAccentColorHex)

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${activeProfile.iconEmoji} ${activeProfile.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showStudioScreen = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Personalizar Deck en Studio",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Auto",
                            color = if (autoFollowForeground) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = autoFollowForeground,
                            onCheckedChange = { autoFollowForeground = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentColor,
                                checkedTrackColor = accentColor.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF090D16),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Profile Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allProfiles.forEach { profile ->
                    val isSelected = profile.id == activeProfile.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            manualProfileOverride = profile
                            autoFollowForeground = false
                        },
                        label = {
                            Text(
                                text = "${profile.iconEmoji} ${profile.name}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(profile.themeAccentColorHex).copy(alpha = 0.3f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Black.copy(alpha = 0.3f),
                            labelColor = Color.LightGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color(profile.themeAccentColorHex) else Color.DarkGray,
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = { showStudioScreen = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Editar / Nuevo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF38BDF8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF38BDF8).copy(alpha = 0.6f),
                        borderWidth = 1.dp,
                        enabled = true,
                        selected = false
                    )
                )
            }

            // Profile Header Banner
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeProfile.headerSubtitle,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (foregroundApp.isNotBlank()) {
                        Text(
                            text = "PC: $foregroundApp",
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Tactile Controls Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = activeProfile.controls,
                    key = { it.id },
                    span = { control ->
                        when (control) {
                            is MacroDeckControl.MacroButton -> if (control.isFullWidth) GridItemSpan(3) else GridItemSpan(1)
                            is MacroDeckControl.MacroFader -> GridItemSpan(3)
                            is MacroDeckControl.MacroJogWheel -> GridItemSpan(3)
                        }
                    }
                ) { control ->
                    when (control) {
                        is MacroDeckControl.MacroButton -> {
                            MacroDeckButtonCell(
                                button = control,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        executeMacroAction(pcBridge, control.action, onShowSnackbar)
                                    }
                                }
                            )
                        }
                        is MacroDeckControl.MacroFader -> {
                            MacroDeckFaderCell(
                                fader = control,
                                onValueChange = { newValue ->
                                    scope.launch {
                                        val percent = (newValue * 100).toInt()
                                        pcBridge.setMasterVolume(percent)
                                    }
                                }
                            )
                        }
                        is MacroDeckControl.MacroJogWheel -> {
                            MacroDeckJogWheelCell(
                                jogWheel = control,
                                onStepForward = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        executeMacroAction(pcBridge, control.onStepForwardAction, onShowSnackbar)
                                    }
                                },
                                onStepBackward = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        executeMacroAction(pcBridge, control.onStepBackwardAction, onShowSnackbar)
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

private suspend fun executeMacroAction(
    bridge: PcWorkspaceBridge,
    action: MacroDeckAction,
    onShowSnackbar: (String) -> Unit
) {
    when (action) {
        is MacroDeckAction.ShortcutAction -> {
            bridge.sendInteraction(
                PcInteractionAction(
                    type = com.asistente.celular.nlu.pc.PcActionType.HOTKEY,
                    keyCodes = listOf(action.keySequence)
                )
            )
            onShowSnackbar("Atajo: ${action.keySequence}")
        }
        is MacroDeckAction.QuickCommandAction -> {
            bridge.executeQuickCommand(action.command)
            onShowSnackbar("Comando: ${action.command}")
        }
        is MacroDeckAction.StudioSceneAction -> {
            bridge.executeStudioScene(action.sceneId)
            onShowSnackbar("Escena: ${action.sceneId}")
        }
        is MacroDeckAction.PluginAction -> {
            bridge.executeCustomPluginAction(action.pluginId, action.actionId, action.params)
            onShowSnackbar("Plugin: ${action.pluginId}")
        }
        is MacroDeckAction.RoutineAction -> {
            onShowSnackbar("Rutina disparada: ${action.routineId}")
        }
    }
}

@Composable
private fun MacroDeckButtonCell(
    button: MacroDeckControl.MacroButton,
    onClick: () -> Unit
) {
    val buttonColor = Color(button.colorHex)

    Surface(
        color = Color(0xFF161F30),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, buttonColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (button.isFullWidth) 60.dp else 84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = button.iconEmoji,
                fontSize = if (button.isFullWidth) 20.sp else 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = button.label,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = button.subtitle
            if (sub != null) {
                Text(
                    text = sub,
                    color = Color.LightGray.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MacroDeckFaderCell(
    fader: MacroDeckControl.MacroFader,
    onValueChange: (Float) -> Unit
) {
    var currentValue by remember { mutableFloatStateOf(fader.initialValue) }
    val faderColor = Color(fader.colorHex)

    Surface(
        color = Color(0xFF161F30),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, faderColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = fader.iconEmoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fader.label,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "${(currentValue * 100).toInt()}%",
                    color = faderColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = currentValue,
                onValueChange = {
                    currentValue = it
                    onValueChange(it)
                },
                valueRange = fader.minValue..fader.maxValue,
                colors = SliderDefaults.colors(
                    thumbColor = faderColor,
                    activeTrackColor = faderColor,
                    inactiveTrackColor = Color.DarkGray
                )
            )
        }
    }
}

@Composable
private fun MacroDeckJogWheelCell(
    jogWheel: MacroDeckControl.MacroJogWheel,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit
) {
    val jogColor = Color(jogWheel.colorHex)

    Surface(
        color = Color(0xFF161F30),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, jogColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStepBackward,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = jogColor.copy(alpha = 0.2f)),
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Retroceder", tint = jogColor)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = jogWheel.iconEmoji, fontSize = 20.sp)
                Text(
                    text = jogWheel.label,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onStepForward,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = jogColor.copy(alpha = 0.2f)),
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Avanzar", tint = jogColor)
            }
        }
    }
}

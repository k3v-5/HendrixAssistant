package com.asistente.celular.ui.pc.deck.macro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckAction
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckControl
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfile
import com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRepository
import kotlinx.coroutines.launch
import java.util.UUID

private val ACCENT_PALETTE = listOf(
    0xFF6366F1 to "Índigo",
    0xFF06B6D4 to "Cian",
    0xFF10B981 to "Esmeralda",
    0xFFF97316 to "Naranja",
    0xFF8B5CF6 to "Púrpura",
    0xFFEF4444 to "Rojo",
    0xFFF59E0B to "Ámbar",
    0xFFEC4899 to "Rosa",
    0xFF0284C7 to "Azul Sky"
)

private val PROCESS_REGEX_PRESETS = listOf(
    "VS Code" to "(?i).*(code|idea64|studio64|windowsterminal).*",
    "Blender" to "(?i).*blender.*",
    "Edición Video" to "(?i).*(premiere|resolve|afterfx).*",
    "DAW Audio" to "(?i).*(ableton|fl64|reaper).*",
    "Photoshop" to "(?i).*photoshop.*",
    "Navegador" to "(?i).*(chrome|msedge|firefox).*",
    "General" to ".*"
)

/**
 * Estudio Visual completo para la creación, clonación, edición y personalización
 * de perfiles táctiles de Macro Deck estilo Stream Deck Pro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMacroDeckStudioScreen(
    repository: MacroDeckProfileRepository,
    pcBridge: PcWorkspaceBridge,
    initialProfileId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val profiles by repository.profiles.collectAsState()

    var selectedProfileId by remember {
        mutableStateOf(initialProfileId ?: profiles.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(profiles) {
        if (selectedProfileId.isBlank() || profiles.none { it.id == selectedProfileId }) {
            profiles.firstOrNull()?.let { selectedProfileId = it.id }
        }
    }

    val currentProfile = profiles.firstOrNull { it.id == selectedProfileId }

    var editName by remember { mutableStateOf("") }
    var editEmoji by remember { mutableStateOf("") }
    var editSubtitle by remember { mutableStateOf("") }
    var editRegex by remember { mutableStateOf("") }
    var editColorHex by remember { mutableLongStateOf(0xFF6366F1) }
    var editControls by remember { mutableStateOf<List<MacroDeckControl>>(emptyList()) }

    LaunchedEffect(currentProfile?.id) {
        if (currentProfile != null) {
            editName = currentProfile.name
            editEmoji = currentProfile.iconEmoji
            editSubtitle = currentProfile.headerSubtitle
            editRegex = currentProfile.targetProcessRegex
            editColorHex = currentProfile.themeAccentColorHex
            editControls = currentProfile.controls
        }
    }

    var showControlEditorDialog by remember { mutableStateOf(false) }
    var editingControlIndex by remember { mutableStateOf<Int?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val accentColor = Color(editColorHex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "🖲️ Custom Deck Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Diseñador Visual de Perfiles & Acciones",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Restaurar Fábrica", tint = Color(0xFFF59E0B))
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentProfile != null) {
                                val updated = currentProfile.copy(
                                    name = editName.trim().ifBlank { "Perfil Personalizado" },
                                    iconEmoji = editEmoji.trim().ifBlank { "🖲️" },
                                    headerSubtitle = editSubtitle.trim(),
                                    targetProcessRegex = editRegex.trim().ifBlank { ".*" },
                                    themeAccentColorHex = editColorHex,
                                    controls = editControls
                                )
                                scope.launch {
                                    repository.saveProfile(updated)
                                    onShowSnackbar("Perfil '${updated.name}' guardado con éxito")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar", tint = Color(0xFF10B981))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1120))
            )
        },
        containerColor = Color(0xFF070B14),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Selector Row
            item {
                Text(
                    text = "PERFILES DISPONIBLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profiles.forEach { profile ->
                        val isSelected = profile.id == selectedProfileId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedProfileId = profile.id
                            },
                            label = {
                                Text(
                                    text = "${profile.iconEmoji} ${profile.name}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(profile.themeAccentColorHex).copy(alpha = 0.35f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) Color(profile.themeAccentColorHex) else Color(0xFF334155),
                                borderWidth = 1.dp,
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }

                    // Botón Crear Perfil
                    OutlinedButton(
                        onClick = {
                            val newId = "profile_${UUID.randomUUID().toString().take(8)}"
                            val newProfile = MacroDeckProfile(
                                id = newId,
                                name = "Nuevo Perfil",
                                iconEmoji = "🚀",
                                targetProcessRegex = ".*",
                                headerSubtitle = "Superficie de Control Personalizada",
                                themeAccentColorHex = 0xFF6366F1,
                                controls = listOf(
                                    MacroDeckControl.MacroButton(
                                        id = "btn_${UUID.randomUUID().toString().take(6)}",
                                        label = "Acción 1",
                                        iconEmoji = "⚡",
                                        colorHex = 0xFF6366F1,
                                        action = MacroDeckAction.ShortcutAction("Ctrl+S", "Guardar")
                                    )
                                )
                            )
                            scope.launch {
                                repository.saveProfile(newProfile)
                                selectedProfileId = newId
                                onShowSnackbar("Nuevo perfil creado")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo", fontSize = 12.sp)
                    }
                }
            }

            // Profile Actions: Clone & Delete
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentProfile != null) {
                                val clonedId = "profile_${UUID.randomUUID().toString().take(8)}"
                                val cloned = currentProfile.copy(
                                    id = clonedId,
                                    name = "${editName} (Copia)",
                                    iconEmoji = editEmoji,
                                    headerSubtitle = editSubtitle,
                                    targetProcessRegex = editRegex,
                                    themeAccentColorHex = editColorHex,
                                    controls = editControls
                                )
                                scope.launch {
                                    repository.saveProfile(cloned)
                                    selectedProfileId = clonedId
                                    onShowSnackbar("Perfil clonado exitosamente")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clonar Perfil", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar", fontSize = 12.sp)
                    }
                }
            }

            // Profile Metadata Form Card
            item {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "METADATOS DEL PERFIL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editEmoji,
                                onValueChange = { editEmoji = it.take(4) },
                                label = { Text("Emoji", fontSize = 11.sp) },
                                modifier = Modifier.width(72.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nombre del Perfil", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        OutlinedTextField(
                            value = editSubtitle,
                            onValueChange = { editSubtitle = it },
                            label = { Text("Subtítulo descriptivo", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = editRegex,
                            onValueChange = { editRegex = it },
                            label = { Text("Regex de Proceso Objetivo en Windows", fontSize = 11.sp) },
                            placeholder = { Text("(?i).*blender.*", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Process Regex Presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PROCESS_REGEX_PRESETS.forEach { (label, regexVal) ->
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (editRegex == regexVal) accentColor else Color.LightGray,
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                        .clickable { editRegex = regexVal }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Accent Color Picker Row
                        Text("Color Temático:", fontSize = 11.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ACCENT_PALETTE.forEach { (colorHexVal, _) ->
                                val isChosen = editColorHex == colorHexVal
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHexVal))
                                        .clickable { editColorHex = colorHexVal }
                                        .then(
                                            if (isChosen) Modifier.background(
                                                Color.White.copy(alpha = 0.3f),
                                                CircleShape
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Controls Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTROLES TÁCTILES (${editControls.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )

                    Button(
                        onClick = {
                            editingControlIndex = null
                            showControlEditorDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Control", fontSize = 12.sp)
                    }
                }
            }

            // Controls List
            itemsIndexed(editControls) { index, control ->
                ControlItemCard(
                    control = control,
                    index = index,
                    totalCount = editControls.size,
                    accentColor = accentColor,
                    onMoveUp = {
                        if (index > 0) {
                            val list = editControls.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index - 1, item)
                            editControls = list
                        }
                    },
                    onMoveDown = {
                        if (index < editControls.size - 1) {
                            val list = editControls.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index + 1, item)
                            editControls = list
                        }
                    },
                    onEdit = {
                        editingControlIndex = index
                        showControlEditorDialog = true
                    },
                    onDelete = {
                        val list = editControls.toMutableList()
                        list.removeAt(index)
                        editControls = list
                    },
                    onTest = {
                        scope.launch {
                            when (control) {
                                is MacroDeckControl.MacroButton -> {
                                    when (val act = control.action) {
                                        is MacroDeckAction.ShortcutAction -> {
                                            pcBridge.sendInteraction(
                                                PcInteractionAction(
                                                    type = PcActionType.HOTKEY,
                                                    keyCodes = listOf(act.keySequence)
                                                )
                                            )
                                            onShowSnackbar("Atajo simulado en PC: ${act.keySequence}")
                                        }
                                        is MacroDeckAction.QuickCommandAction -> {
                                            pcBridge.executeQuickCommand(act.command)
                                            onShowSnackbar("Comando ejecutado en PC: ${act.command}")
                                        }
                                        is MacroDeckAction.StudioSceneAction -> {
                                            pcBridge.executeQuickCommand("scene:${act.sceneId}")
                                            onShowSnackbar("Escena activada: ${act.sceneId}")
                                        }
                                        is MacroDeckAction.RoutineAction -> {
                                            onShowSnackbar("Rutina disparada: ${act.routineId}")
                                        }
                                        else -> {}
                                    }
                                }
                                is MacroDeckControl.MacroFader -> {
                                    pcBridge.executeQuickCommand("${control.targetParameter}:80")
                                    onShowSnackbar("Fader probado: ${control.targetParameter}")
                                }
                                is MacroDeckControl.MacroJogWheel -> {
                                    val fwd = control.onStepForwardAction
                                    if (fwd is MacroDeckAction.ShortcutAction) {
                                        pcBridge.sendInteraction(
                                            PcInteractionAction(
                                                type = PcActionType.HOTKEY,
                                                keyCodes = listOf(fwd.keySequence)
                                            )
                                        )
                                        onShowSnackbar("Jog simulado: ${fwd.keySequence}")
                                    }
                                }
                            }
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal para Añadir / Editar Control
    if (showControlEditorDialog) {
        val currentCtrl = editingControlIndex?.let { editControls.getOrNull(it) }
        ControlEditorModal(
            initialControl = currentCtrl,
            onDismiss = { showControlEditorDialog = false },
            onSave = { updatedControl ->
                val list = editControls.toMutableList()
                if (editingControlIndex != null && editingControlIndex!! in list.indices) {
                    list[editingControlIndex!!] = updatedControl
                } else {
                    list.add(updatedControl)
                }
                editControls = list
                showControlEditorDialog = false
            }
        )
    }

    // Modal Confirmar Restauración a Fábrica
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Restaurar Valores de Fábrica") },
            text = { Text("¿Deseas restaurar todos los perfiles de Macro Deck predeterminados? Se perderán las modificaciones no respaldadas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.resetToDefaults()
                            onShowSnackbar("Perfiles restaurados a valores de fábrica")
                            showResetConfirmDialog = false
                        }
                    }
                ) {
                    Text("Restaurar", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal Confirmar Eliminación de Perfil
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminar Perfil") },
            text = { Text("¿Estás seguro de eliminar el perfil '${currentProfile?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentProfile != null) {
                            scope.launch {
                                repository.deleteProfile(currentProfile.id)
                                onShowSnackbar("Perfil eliminado")
                                showDeleteConfirmDialog = false
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ControlItemCard(
    control: MacroDeckControl,
    index: Int,
    totalCount: Int,
    accentColor: Color,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val controlTypeLabel = when (control) {
        is MacroDeckControl.MacroButton -> "Botón"
        is MacroDeckControl.MacroFader -> "Fader"
        is MacroDeckControl.MacroJogWheel -> "Jog Wheel"
    }

    val actionSummary = when (control) {
        is MacroDeckControl.MacroButton -> when (val act = control.action) {
            is MacroDeckAction.ShortcutAction -> "Atajo: ${act.keySequence}"
            is MacroDeckAction.QuickCommandAction -> "Comando: ${act.command}"
            is MacroDeckAction.StudioSceneAction -> "Escena: ${act.sceneId}"
            is MacroDeckAction.RoutineAction -> "Rutina: ${act.routineId}"
            is MacroDeckAction.PluginAction -> "Plugin: ${act.pluginId}.${act.actionId}"
        }
        is MacroDeckControl.MacroFader -> "Parámetro: ${control.targetParameter} (${control.minValue}..${control.maxValue})"
        is MacroDeckControl.MacroJogWheel -> "Jog: Avanzar/Retroceder"
    }

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(control.colorHex).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(control.colorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = control.iconEmoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Label & Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = control.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = controlTypeLabel,
                        fontSize = 10.sp,
                        color = Color(control.colorHex),
                        modifier = Modifier
                            .background(Color(control.colorHex).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = actionSummary,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Actions: Up, Down, Edit, Delete, Test
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Subir",
                        tint = if (index > 0) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = index < totalCount - 1, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Bajar",
                        tint = if (index < totalCount - 1) Color.LightGray else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onTest, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Probar", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ControlEditorModal(
    initialControl: MacroDeckControl?,
    onDismiss: () -> Unit,
    onSave: (MacroDeckControl) -> Unit
) {
    var controlType by remember {
        mutableStateOf(
            when (initialControl) {
                is MacroDeckControl.MacroFader -> "Fader"
                is MacroDeckControl.MacroJogWheel -> "Jog Wheel"
                else -> "Botón"
            }
        )
    }

    var label by remember { mutableStateOf(initialControl?.label ?: "Nueva Acción") }
    var iconEmoji by remember { mutableStateOf(initialControl?.iconEmoji ?: "⚡") }
    var colorHex by remember { mutableLongStateOf(initialControl?.colorHex ?: 0xFF6366F1) }

    // Campos de Botón
    var actionType by remember {
        mutableStateOf(
            when (val act = (initialControl as? MacroDeckControl.MacroButton)?.action) {
                is MacroDeckAction.QuickCommandAction -> "Comando Rápido"
                is MacroDeckAction.StudioSceneAction -> "Escena de Estudio"
                is MacroDeckAction.RoutineAction -> "Rutina"
                else -> "Atajo de Teclado"
            }
        )
    }
    var keySequence by remember {
        mutableStateOf(
            ((initialControl as? MacroDeckControl.MacroButton)?.action as? MacroDeckAction.ShortcutAction)?.keySequence ?: "Ctrl+S"
        )
    }
    var quickCommand by remember {
        mutableStateOf(
            ((initialControl as? MacroDeckControl.MacroButton)?.action as? MacroDeckAction.QuickCommandAction)?.command ?: "media_play_pause"
        )
    }
    var sceneId by remember {
        mutableStateOf(
            ((initialControl as? MacroDeckControl.MacroButton)?.action as? MacroDeckAction.StudioSceneAction)?.sceneId ?: "streaming"
        )
    }

    // Campos de Fader
    var targetParameter by remember {
        mutableStateOf((initialControl as? MacroDeckControl.MacroFader)?.targetParameter ?: "master_volume")
    }

    // Campos de Jog Wheel
    var jogForwardKey by remember {
        mutableStateOf(
            ((initialControl as? MacroDeckControl.MacroJogWheel)?.onStepForwardAction as? MacroDeckAction.ShortcutAction)?.keySequence ?: "Right"
        )
    }
    var jogBackwardKey by remember {
        mutableStateOf(
            ((initialControl as? MacroDeckControl.MacroJogWheel)?.onStepBackwardAction as? MacroDeckAction.ShortcutAction)?.keySequence ?: "Left"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialControl != null) "Editar Control" else "Añadir Control al Deck",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            val dialogScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dialogScroll),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de Tipo de Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Botón", "Fader", "Jog Wheel").forEach { t ->
                        FilterChip(
                            selected = controlType == t,
                            onClick = { controlType = t },
                            label = { Text(t, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(colorHex).copy(alpha = 0.3f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = iconEmoji,
                        onValueChange = { iconEmoji = it.take(4) },
                        label = { Text("Emoji", fontSize = 11.sp) },
                        modifier = Modifier.width(68.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Etiqueta", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Configuración según tipo
                when (controlType) {
                    "Botón" -> {
                        Text("Tipo de Acción:", fontSize = 11.sp, color = Color.Gray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Atajo de Teclado", "Comando Rápido", "Escena de Estudio").forEach { at ->
                                FilterChip(
                                    selected = actionType == at,
                                    onClick = { actionType = at },
                                    label = { Text(at, fontSize = 10.sp) }
                                )
                            }
                        }

                        when (actionType) {
                            "Atajo de Teclado" -> {
                                OutlinedTextField(
                                    value = keySequence,
                                    onValueChange = { keySequence = it },
                                    label = { Text("Secuencia de Teclas", fontSize = 11.sp) },
                                    placeholder = { Text("Ej: Ctrl+Shift+P, F12, Space", color = Color.Gray, fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            "Comando Rápido" -> {
                                OutlinedTextField(
                                    value = quickCommand,
                                    onValueChange = { quickCommand = it },
                                    label = { Text("Comando del Sistema", fontSize = 11.sp) },
                                    placeholder = { Text("volume_mute, media_play_pause, lock", color = Color.Gray, fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            "Escena de Estudio" -> {
                                OutlinedTextField(
                                    value = sceneId,
                                    onValueChange = { sceneId = it },
                                    label = { Text("ID de Escena", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                    "Fader" -> {
                        OutlinedTextField(
                            value = targetParameter,
                            onValueChange = { targetParameter = it },
                            label = { Text("Parámetro Objetivo", fontSize = 11.sp) },
                            placeholder = { Text("master_volume, mic_gain, brightness", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    "Jog Wheel" -> {
                        OutlinedTextField(
                            value = jogForwardKey,
                            onValueChange = { jogForwardKey = it },
                            label = { Text("Tecla Avanzar (Step Forward)", fontSize = 11.sp) },
                            placeholder = { Text("Right, Down, Ctrl+Right", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = jogBackwardKey,
                            onValueChange = { jogBackwardKey = it },
                            label = { Text("Tecla Retroceder (Step Backward)", fontSize = 11.sp) },
                            placeholder = { Text("Left, Up, Ctrl+Left", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Paleta de Colores para el Control
                Text("Color:", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ACCENT_PALETTE.forEach { (cHex, _) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(cHex))
                                .clickable { colorHex = cHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorHex == cHex) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ctrlId = initialControl?.id ?: "ctrl_${UUID.randomUUID().toString().take(6)}"
                    val newCtrl = when (controlType) {
                        "Fader" -> MacroDeckControl.MacroFader(
                            id = ctrlId,
                            label = label.trim().ifBlank { "Fader" },
                            iconEmoji = iconEmoji.trim().ifBlank { "🎚️" },
                            colorHex = colorHex,
                            targetParameter = targetParameter.trim().ifBlank { "volume" }
                        )
                        "Jog Wheel" -> MacroDeckControl.MacroJogWheel(
                            id = ctrlId,
                            label = label.trim().ifBlank { "Jog Wheel" },
                            iconEmoji = iconEmoji.trim().ifBlank { "🔄" },
                            colorHex = colorHex,
                            onStepForwardAction = MacroDeckAction.ShortcutAction(jogForwardKey.trim().ifBlank { "Right" }),
                            onStepBackwardAction = MacroDeckAction.ShortcutAction(jogBackwardKey.trim().ifBlank { "Left" })
                        )
                        else -> {
                            val act = when (actionType) {
                                "Comando Rápido" -> MacroDeckAction.QuickCommandAction(quickCommand.trim().ifBlank { "media_play_pause" })
                                "Escena de Estudio" -> MacroDeckAction.StudioSceneAction(sceneId.trim().ifBlank { "main" })
                                else -> MacroDeckAction.ShortcutAction(keySequence.trim().ifBlank { "Ctrl+S" })
                            }
                            MacroDeckControl.MacroButton(
                                id = ctrlId,
                                label = label.trim().ifBlank { "Acción" },
                                iconEmoji = iconEmoji.trim().ifBlank { "⚡" },
                                colorHex = colorHex,
                                action = act
                            )
                        }
                    }
                    onSave(newCtrl)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

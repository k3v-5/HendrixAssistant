package com.asistente.celular.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.ai.personality.AssistantPersonality
import com.asistente.celular.hardware.ShakeSensitivity
import com.asistente.celular.voice.stt.AsrModelDownloadState
import com.asistente.celular.voice.stt.OfflineAsrModelManager
import com.asistente.celular.voice.stt.SttEngineType

enum class SettingsSectionTab(val title: String, val icon: ImageVector) {
    VOICE_AUDIO("Voz & Audio", Icons.Default.Mic),
    AI_MODELS("IA & Modelos", Icons.Default.Psychology),
    SENSORS_GESTURES("Gestos & Físico", Icons.Default.Smartphone),
    VAULT_SMART("Bóveda & Red", Icons.Default.Shield)
}

/**
 * Pantalla de Configuración modular para Hendrix Assistant.
 * Rediseñada con navegación segmentada en 4 pestañas para eliminar el scroll vertical infinito
 * y agrupar las opciones en dominios cohesivos con estética OLED Void.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: LlmConfig,
    isWakeWordActive: Boolean,
    wakeWordSensitivity: com.asistente.celular.voice.kws.WakeWordSensitivity = com.asistente.celular.voice.kws.WakeWordSensitivity.MEDIUM,
    onChangeWakeWordSensitivity: (com.asistente.celular.voice.kws.WakeWordSensitivity) -> Unit = {},
    isShakeToWakeEnabled: Boolean = false,
    shakeSensitivity: ShakeSensitivity = ShakeSensitivity.NORMAL,
    onChangeShakeSensitivity: (ShakeSensitivity) -> Unit = {},
    isPocketSilenceEnabled: Boolean = true,
    isFlipToMuteEnabled: Boolean = true,
    onToggleShakeToWake: (Boolean) -> Unit = {},
    onTogglePocketSilence: (Boolean) -> Unit = {},
    onToggleFlipToMute: (Boolean) -> Unit = {},
    isOverlayEnabled: Boolean = true,
    onToggleOverlay: (Boolean) -> Unit = {},
    sttEngineType: SttEngineType = SttEngineType.ANDROID_SYSTEM,
    onChangeSttEngine: (SttEngineType) -> Unit = {},
    offlineAsrModelManager: OfflineAsrModelManager? = null,
    onStartAsrDownload: (String) -> Unit = {},
    onDeleteAsrModel: (String) -> Unit = {},
    onExportVault: () -> Unit = {},
    onRestoreVault: () -> Unit = {},
    onSyncVaultPc: () -> Unit = {},
    ttsPitch: Float = 1.0f,
    ttsSpeechRate: Float = 1.0f,
    onChangeTtsParameters: (Float, Float) -> Unit = { _, _ -> },
    onTestTtsVoice: () -> Unit = {},
    smartHomeCustomSubnet: String? = null,
    onChangeSmartHomeCustomSubnet: (String?) -> Unit = {},
    localModelManager: com.asistente.celular.ai.local.LocalModelManager? = null,
    smartDevices: List<com.asistente.celular.nlu.smarthome.SmartDevice> = emptyList(),
    isScanningSmartDevices: Boolean = false,
    onSaveConfig: (LlmConfig) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onDownloadModel: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onDeleteModel: (String) -> Unit = {},
    onSelectLocalModel: (String) -> Unit = {},
    onDiscoverSmartDevices: () -> Unit = {},
    onAddManualSmartDevice: (String, String) -> Unit = { _, _ -> },
    onDeleteSmartDevice: (String) -> Unit = {},
    onToggleSmartDevice: (com.asistente.celular.nlu.smarthome.SmartDevice) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var selectedPersonality by remember { mutableStateOf(currentConfig.personality) }
    var zeroCloudMode by remember { mutableStateOf(currentConfig.zeroCloudMode) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var modelName by remember { mutableStateOf(currentConfig.modelName) }
    var customEndpoint by remember { mutableStateOf(currentConfig.customEndpoint ?: "") }
    var temperature by remember(currentConfig) { mutableFloatStateOf(currentConfig.temperature) }
    var maxTokens by remember(currentConfig) { mutableIntStateOf(currentConfig.maxTokens) }
    var systemPrompt by remember(currentConfig) { mutableStateOf(currentConfig.systemPrompt) }
    var isModelHarnessEnabled by remember { mutableStateOf(currentConfig.isModelHarnessEnabled) }
    var showApiKey by remember { mutableStateOf(false) }

    var currentPitch by remember(ttsPitch) { mutableFloatStateOf(ttsPitch) }
    var currentRate by remember(ttsSpeechRate) { mutableFloatStateOf(ttsSpeechRate) }

    var subnetInput by remember(smartHomeCustomSubnet) { mutableStateOf(smartHomeCustomSubnet ?: "") }

    var manualName by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("") }
    var showManualAddDialog by remember { mutableStateOf(false) }

    var selectedSectionTab by remember { mutableStateOf(SettingsSectionTab.VOICE_AUDIO) }
    var expandedCategories by remember { mutableStateOf(setOf(0, 1, 2, 3, 4, 5)) }

    fun toggleCategory(index: Int) {
        expandedCategories = if (expandedCategories.contains(index)) {
            expandedCategories - index
        } else {
            expandedCategories + index
        }
    }

    Scaffold(
        containerColor = VoidBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AJUSTES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Selector segmentado horizontal compacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsSectionTab.entries.forEach { tab ->
                    val isSelected = selectedSectionTab == tab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) VoidSurfaceElevated else VoidSurface,
                        border = BorderStroke(1.dp, if (isSelected) NeonCyan else VoidBorder),
                        modifier = Modifier.clickable { selectedSectionTab = tab }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ---------------------------------------------------------------
            // 1. INTELIGENCIA ARTIFICIAL & MODELOS
            // ---------------------------------------------------------------
            if (selectedSectionTab == SettingsSectionTab.AI_MODELS) {
                ExpandableSettingsCategoryCard(
                    icon = Icons.Default.Psychology,
                    title = "Inteligencia Artificial & Modelos",
                    subtitle = "Enrutador LLM (${selectedProvider.displayName}), Harness y parámetros",
                    isExpanded = expandedCategories.contains(0),
                    onToggleExpand = { toggleCategory(0) }
                ) {
                Text(
                    "Cuando la orden no sea una acción local del móvil, se derivará a este proveedor:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                AiProvider.entries.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedProvider = provider
                                modelName = provider.defaultModel
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = {
                                selectedProvider = provider
                                modelName = provider.defaultModel
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(provider.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Model Routing Harness para Gemini
                if (selectedProvider == AiProvider.GEMINI) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Model Routing Harness", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "Enruta entre Flash, Pro y Thinking según dificultad de la orden.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = isModelHarnessEnabled,
                                    onCheckedChange = { isModelHarnessEnabled = it }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (selectedProvider == AiProvider.LOCAL_SLM) {
                    if (localModelManager != null) {
                        LocalModelManagerSection(
                            localModelManager = localModelManager,
                            selectedModelId = modelName,
                            onSelectModel = { selectedId ->
                                modelName = selectedId
                                onSelectLocalModel(selectedId)
                            },
                            onDownloadModel = onDownloadModel,
                            onCancelDownload = onCancelDownload,
                            onDeleteModel = onDeleteModel
                        )
                    } else {
                        Text(
                            "Gestor de modelos locales no disponible.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    if (selectedProvider != AiProvider.OLLAMA) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key de ${selectedProvider.displayName}") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mostrar clave"
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!isModelHarnessEnabled || selectedProvider != AiProvider.GEMINI) {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            label = { Text("Nombre del Modelo Fijo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedProvider == AiProvider.OLLAMA) {
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { customEndpoint = it },
                            label = { Text("Endpoint de Ollama (ej: http://192.168.1.50:11434)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Parámetros de Inferencia LLM", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Temp: ${"%.2f".format(temperature)}", fontSize = 12.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.5f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Límite de Tokens (Max Tokens):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(512, 1024, 2048, 4096).forEach { tokens ->
                        FilterChip(
                            selected = maxTokens == tokens,
                            onClick = { maxTokens = tokens },
                            label = { Text("$tokens", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("Prompt del Sistema") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
            }
        }

            // ---------------------------------------------------------------
            // 2. VOZ, ASR & SÍNTESIS (TTS)
            // ---------------------------------------------------------------
            if (selectedSectionTab == SettingsSectionTab.VOICE_AUDIO) {
                ExpandableSettingsCategoryCard(
                    icon = Icons.Default.Mic,
                    title = "Voz, Reconocimiento & Síntesis",
                    subtitle = "Palabra de activación ('Oye Hendrix'), TTS y motor STT",
                    isExpanded = expandedCategories.contains(1),
                    onToggleExpand = { toggleCategory(1) }
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Palabra de activación ('Oye Hendrix')", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Escucha en segundo plano persistente", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = isWakeWordActive,
                        onCheckedChange = onToggleWakeWord
                    )
                }

                if (isWakeWordActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sensibilidad acústica:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.asistente.celular.voice.kws.WakeWordSensitivity.entries.forEach { sens ->
                            FilterChip(
                                selected = wakeWordSensitivity == sens,
                                onClick = { onChangeWakeWordSensitivity(sens) },
                                label = {
                                    Text(
                                        when (sens) {
                                            com.asistente.celular.voice.kws.WakeWordSensitivity.LOW -> "Baja"
                                            com.asistente.celular.voice.kws.WakeWordSensitivity.MEDIUM -> "Media"
                                            com.asistente.celular.voice.kws.WakeWordSensitivity.HIGH -> "Alta"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }

                    val context = LocalContext.current
                    val canDrawOverlays = Settings.canDrawOverlays(context)
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

                    if (!canDrawOverlays) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Permiso de Ventana Flotante necesario",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Concede 'Mostrar sobre otras apps' para que Hendrix responda mientras usas otra aplicación.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Conceder permiso", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!isIgnoringBattery) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Batería sin restricciones", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(
                                    "Configura Hendrix como 'Sin restricciones' para evitar que el sistema duerma el micrófono.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                ) {
                                    Text("Ajustar batería", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text("Parámetros de Voz (TTS)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Velocidad: ${"%.2f".format(currentRate)}x", fontSize = 12.sp, modifier = Modifier.width(105.dp))
                    Slider(
                        value = currentRate,
                        onValueChange = {
                            currentRate = it
                            onChangeTtsParameters(currentPitch, it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tono: ${"%.2f".format(currentPitch)}x", fontSize = 12.sp, modifier = Modifier.width(105.dp))
                    Slider(
                        value = currentPitch,
                        onValueChange = {
                            currentPitch = it
                            onChangeTtsParameters(it, currentRate)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = onTestTtsVoice,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Probar Voz", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text("Motor de Reconocimiento de Voz (STT)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SttEngineType.entries.forEach { engType ->
                        FilterChip(
                            selected = sttEngineType == engType,
                            onClick = { onChangeSttEngine(engType) },
                            label = {
                                Text(
                                    if (engType == SttEngineType.ANDROID_SYSTEM) "Sistema Android" else "Offline Sherpa-ONNX",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }

                if (sttEngineType == SttEngineType.OFFLINE_SHERPA_ONNX && offlineAsrModelManager != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val downloadStates by offlineAsrModelManager.downloadStates.collectAsState()

                    offlineAsrModelManager.getAvailableModels().forEach { modelSpec ->
                        val state = downloadStates[modelSpec.id] ?: AsrModelDownloadState.NotDownloaded
                        val isDownloaded = state is AsrModelDownloadState.Downloaded

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(modelSpec.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${modelSpec.language} • ${modelSpec.sizeMb} MB", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                if (isDownloaded) {
                                    OutlinedButton(
                                        onClick = { onDeleteAsrModel(modelSpec.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Eliminar", fontSize = 10.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { onStartAsrDownload(modelSpec.id) },
                                        enabled = state !is AsrModelDownloadState.Downloading
                                    ) {
                                        Text(if (state is AsrModelDownloadState.Downloading) "Descargando..." else "Descargar", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // ---------------------------------------------------------------
            // 3. GESTOS FÍSICOS & SENSORES
            // ---------------------------------------------------------------
            if (selectedSectionTab == SettingsSectionTab.SENSORS_GESTURES) {
                ExpandableSettingsCategoryCard(
                    icon = Icons.Default.Smartphone,
                    title = "Gestos Físicos & Sensores",
                    subtitle = "Agitar para activar, silencio en bolsillo y mini HUD",
                    isExpanded = expandedCategories.contains(2),
                    onToggleExpand = { toggleCategory(2) }
                ) {
                // 1. Shake to wake
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Agitar para Activar (Shake to Wake)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Agita firmemente dos veces para escuchar sin tocar la pantalla",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isShakeToWakeEnabled,
                        onCheckedChange = onToggleShakeToWake
                    )
                }

                if (isShakeToWakeEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sensibilidad de agitación:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShakeSensitivity.entries.forEach { sens ->
                            FilterChip(
                                selected = shakeSensitivity == sens,
                                onClick = { onChangeShakeSensitivity(sens) },
                                label = {
                                    Text(
                                        when (sens) {
                                            ShakeSensitivity.GENTLE -> "Suave"
                                            ShakeSensitivity.NORMAL -> "Normal"
                                            ShakeSensitivity.VIGOROUS -> "Vigoroso"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // 2. Pocket silence
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Silencio en Bolsillo (Pocket Silence)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Pausa la escucha cuando el sensor de proximidad detecta bolsillo",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isPocketSilenceEnabled,
                        onCheckedChange = onTogglePocketSilence
                    )
                }

                // 3. Flip to mute
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Voltear para Silenciar (Flip to Mute)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Pon el móvil boca abajo sobre la mesa para silenciar la voz al instante",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isFlipToMuteEnabled,
                        onCheckedChange = onToggleFlipToMute
                    )
                }

                // 4. Ventana Flotante / Mini HUD
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ventana Flotante / Mini HUD (Overlay)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Despliega una burbuja flotante interactiva sobre cualquier app",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isOverlayEnabled,
                        onCheckedChange = onToggleOverlay
                    )
                }
            }
        }

            // ---------------------------------------------------------------
            // 4. DOMÓTICA & FOCOS INTELIGENTES
            // ---------------------------------------------------------------
            if (selectedSectionTab == SettingsSectionTab.VAULT_SMART) {
                ExpandableSettingsCategoryCard(
                    icon = Icons.Default.Lightbulb,
                    title = "Domótica & Focos Inteligentes",
                    subtitle = "Xiaomi / Yeelight WiFi local (${smartDevices.size} vinculados)",
                    isExpanded = expandedCategories.contains(3),
                    onToggleExpand = { toggleCategory(3) }
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Control local offline (puerto 55443)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    IconButton(onClick = onDiscoverSmartDevices) {
                        Icon(Icons.Default.Refresh, contentDescription = "Buscar focos")
                    }
                }

                if (smartDevices.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("No hay focos vinculados aún.", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                "Activa 'Control en LAN' en Xiaomi Home/Yeelight y presiona buscar.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    smartDevices.forEach { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (device.isPoweredOn)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = if (device.isPoweredOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${device.ipAddress} • ${if (device.isPoweredOn) "Encendido (${device.brightness}%)" else "Apagado"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = device.isPoweredOn,
                                    onCheckedChange = { onToggleSmartDevice(device) }
                                )
                                IconButton(onClick = { onDeleteSmartDevice(device.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDiscoverSmartDevices,
                        enabled = !isScanningSmartDevices,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isScanningSmartDevices) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buscando...", fontSize = 11.sp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buscar WiFi", fontSize = 11.sp)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { showManualAddDialog = !showManualAddDialog },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showManualAddDialog) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showManualAddDialog) "Ocultar IP" else "Añadir IP", fontSize = 11.sp)
                        }
                    }
                }

                if (showManualAddDialog) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Añadir foco manual", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("Nombre (ej: Foco Sala)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                label = { Text("IP (ej: 192.168.1.105)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    if (manualIp.isNotBlank()) {
                                        onAddManualSmartDevice(manualName, manualIp)
                                        manualIp = ""
                                        manualName = ""
                                        showManualAddDialog = false
                                    }
                                },
                                enabled = manualIp.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Guardar Foco", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = subnetInput,
                    onValueChange = {
                        subnetInput = it
                        onChangeSmartHomeCustomSubnet(it.ifBlank { null })
                    },
                    label = { Text("Subred personalizada / VLAN IoT (opcional)") },
                    placeholder = { Text("ej: 192.168.2.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

            // ---------------------------------------------------------------
            // 5. PERSONALIDAD & PRIVACIDAD (ZERO-CLOUD)
            // ---------------------------------------------------------------
            if (selectedSectionTab == SettingsSectionTab.AI_MODELS) {
                ExpandableSettingsCategoryCard(
                    icon = Icons.Default.Shield,
                    title = "Personalidad & Privacidad (Zero-Cloud)",
                    subtitle = "Tono del asistente y modo 100% offline",
                    isExpanded = expandedCategories.contains(4),
                    onToggleExpand = { toggleCategory(4) }
                ) {
                // Modo Zero Cloud
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (zeroCloudMode)
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Zero-Cloud Privacy Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    if (zeroCloudMode) "100% Offline con SLM local" else "Permite consultas cloud cuando aplique",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = zeroCloudMode,
                                onCheckedChange = { isEnabled ->
                                    zeroCloudMode = isEnabled
                                    if (isEnabled) {
                                        selectedProvider = AiProvider.LOCAL_SLM
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Personalidad del Asistente", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))

                AssistantPersonality.entries.forEach { pers ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPersonality = pers }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedPersonality == pers,
                            onClick = { selectedPersonality = pers }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(pers.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(pers.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

            // ---------------------------------------------------------------
            // 6. BÓVEDA & RESPALDO UNIFICADO (VAULT)
            // ---------------------------------------------------------------
            if (selectedSectionTab == SettingsSectionTab.VAULT_SMART) {
                ExpandableSettingsCategoryCard(
                    icon = Icons.Default.Archive,
                    title = "Bóveda & Respaldo (Hendrix Vault)",
                    subtitle = "Exportar, restaurar y sincronizar rutinas y notas con PC",
                    isExpanded = expandedCategories.contains(5),
                    onToggleExpand = { toggleCategory(5) }
                ) {
                    Text(
                        "Crea copias de seguridad de todas tus rutinas, botones, tareas, notas y configuraciones sin depender de servicios en la nube.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportVault,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar", fontSize = 11.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onRestoreVault,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restaurar", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onSyncVaultPc,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DesktopWindows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizar Bóveda con PC", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón Principal Guardar
            Button(
                onClick = {
                    val updated = currentConfig.copy(
                        provider = if (zeroCloudMode) AiProvider.LOCAL_SLM else selectedProvider,
                        apiKey = apiKey,
                        modelName = modelName,
                        customEndpoint = customEndpoint.takeIf { it.isNotBlank() },
                        isModelHarnessEnabled = isModelHarnessEnabled,
                        personality = selectedPersonality,
                        zeroCloudMode = zeroCloudMode,
                        temperature = temperature,
                        maxTokens = maxTokens,
                        systemPrompt = systemPrompt
                    )
                    onSaveConfig(updated)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GUARDAR CONFIGURACIÓN",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta atómica expandible tipo Acordeón para organizar categorías de ajustes.
 * Reduce drásticamente la altura de la pantalla y la fatiga visual.
 */
@Composable
private fun ExpandableSettingsCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                VoidSurfaceElevated
            else
                VoidSurface
        ),
        border = BorderStroke(1.dp, if (isExpanded) NeonCyan.copy(alpha = 0.4f) else VoidBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = NeonLilac.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeonLilac,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(
                        color = VoidBorder,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    content()
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

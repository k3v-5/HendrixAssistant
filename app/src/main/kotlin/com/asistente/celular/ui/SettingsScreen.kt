package com.asistente.celular.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ai.harness.ModelRegistry
import com.asistente.celular.ai.model.AiProvider
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.ai.personality.AssistantPersonality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: LlmConfig,
    isWakeWordActive: Boolean,
    isShakeToWakeEnabled: Boolean = false,
    isPocketSilenceEnabled: Boolean = true,
    isFlipToMuteEnabled: Boolean = true,
    onToggleShakeToWake: (Boolean) -> Unit = {},
    onTogglePocketSilence: (Boolean) -> Unit = {},
    onToggleFlipToMute: (Boolean) -> Unit = {},
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
    onAddManualSmartDevice: (name: String, ip: String) -> Unit = { _, _ -> },
    onDeleteSmartDevice: (String) -> Unit = {},
    onToggleSmartDevice: (com.asistente.celular.nlu.smarthome.SmartDevice) -> Unit = {},
    onBack: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var selectedPersonality by remember { mutableStateOf(currentConfig.personality) }
    var zeroCloudMode by remember { mutableStateOf(currentConfig.zeroCloudMode) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var modelName by remember { mutableStateOf(currentConfig.modelName) }
    var customEndpoint by remember { mutableStateOf(currentConfig.customEndpoint ?: "") }
    var isModelHarnessEnabled by remember { mutableStateOf(currentConfig.isModelHarnessEnabled) }
    var showApiKey by remember { mutableStateOf(false) }

    var isScanningDevices by remember { mutableStateOf(false) }
    var manualName by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("") }
    var showManualAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración del Asistente") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sección: Activación por Voz (Wake Word)
            Text("Voz y Activación Offline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Palabra de activación ('Oye Hendrix')", fontWeight = FontWeight.SemiBold)
                    Text("Escucha en segundo plano persistente", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(
                    checked = isWakeWordActive,
                    onCheckedChange = onToggleWakeWord
                )
            }

            if (isWakeWordActive) {
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
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "⚠️ Permiso de Ventana Flotante necesario",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Para que Hendrix pueda aparecer sobre la pantalla cuando digas 'Oye Hendrix' mientras usas otra app, debes conceder 'Mostrar sobre otras aplicaciones'.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                                Text("Conceder permiso de superposición", fontSize = 12.sp)
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
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🔋 Evitar que el sistema duerma el micrófono", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Android y MIUI/HyperOS pueden pausar el micrófono en segundo plano. Configura la batería de Hendrix como 'Sin restricciones'.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                                Text("Ajustar ahorro de batería", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Gestos y Sensores de Hardware (Punto 3)
            Text("Gestos y Sensores de Hardware", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Fusión de acelerómetro y proximidad para interacción física instantánea.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Shake to wake
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Agitar para Activar (Shake to Wake)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Agita firmemente el dispositivo dos veces para abrir la escucha sin tocar la pantalla",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = isShakeToWakeEnabled,
                    onCheckedChange = onToggleShakeToWake
                )
            }

            // 2. Pocket silence
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Silencio en Bolsillo (Pocket Silence)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pausa la escucha cuando el sensor de proximidad detecta que está en el bolsillo para ahorrar batería",
                        fontSize = 12.sp,
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
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voltear para Silenciar (Flip to Mute)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Coloca el teléfono boca abajo sobre la mesa para silenciar de inmediato la voz del asistente",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = isFlipToMuteEnabled,
                    onCheckedChange = onToggleFlipToMute
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Domótica y Focos Inteligentes Xiaomi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Focos Xiaomi / Yeelight (WiFi Local)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Control local offline sin internet (puerto LAN 55443)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = { onDiscoverSmartDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Buscar focos")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (smartDevices.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "No hay focos vinculados aún.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "Asegúrate de activar 'Control en LAN' en la app Xiaomi Home o Yeelight y presiona buscar, o ingresa la IP del foco.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                smartDevices.forEach { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
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
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (device.isPoweredOn) "💡" else "🌑", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${device.ipAddress}:${device.port} • ${if (device.isPoweredOn) "Encendido (${device.brightness}%)" else "Apagado"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
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
                    onClick = { onDiscoverSmartDevices() },
                    enabled = !isScanningSmartDevices,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isScanningSmartDevices) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscando...", fontSize = 12.sp)
                    } else {
                        Text("🔍 Buscar en WiFi")
                    }
                }
                OutlinedButton(
                    onClick = { showManualAddDialog = !showManualAddDialog },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showManualAddDialog) "Ocultar IP" else "➕ Añadir IP")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "💡 Tip: Asegúrate de tener activa la opción 'Control en LAN' (en app Yeelight o Xiaomi Home) para que el foco responda en tu red local.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )

            if (showManualAddDialog) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Añadir foco por dirección IP", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Nombre (ej: Foco Sala)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("Dirección IP (ej: 192.168.1.105)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                            Text("Guardar Foco")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Personalidad del Asistente (Punto 19)
            Text("Personalidad del Asistente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Modula el tono de respuesta, actitud y entonación de voz.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(pers.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(pers.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Modo Copiloto Offline Seguro / Zero-Cloud (Punto 30)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (zeroCloudMode)
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("🛡️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Copiloto Offline Seguro", fontWeight = FontWeight.Bold)
                                Text("Zero-Cloud Privacy Mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (zeroCloudMode)
                            "🔒 Modo Zero-Cloud ACTIVO: Las consultas se procesan 100% en el dispositivo con modelos SLM locales. Se bloquea cualquier llamada HTTP saliente para absoluta privacidad."
                        else
                            "Cuando está activo, fuerza el procesamiento 100% local y desconecta servicios en la nube para garantizar cero fuga de datos.",
                        fontSize = 12.sp,
                        color = if (zeroCloudMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Enrutador de Inteligencia Artificial
            Text("Motor de IA para Consultas Complejas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Cuando la orden no sea una acción local del móvil, se derivará a este proveedor.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))

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
                    Text(provider.displayName, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Configuración específica de Gemini y Harness
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
                                Text("Model Routing Harness", fontWeight = FontWeight.Bold)
                                Text(
                                    "Enruta automáticamente entre Gemini Flash, Pro o Thinking según la dificultad de la consulta.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = isModelHarnessEnabled,
                                onCheckedChange = { isModelHarnessEnabled = it }
                            )
                        }

                        if (isModelHarnessEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Modelos en el pool del Harness:\n• Gemini Flash (Rápido y ágil)\n• Gemini 2.5 Flash (Balanceado)\n• Gemini Pro (Razonamiento profundo)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val updated = currentConfig.copy(
                        provider = if (zeroCloudMode) AiProvider.LOCAL_SLM else selectedProvider,
                        apiKey = apiKey,
                        modelName = modelName,
                        customEndpoint = customEndpoint.takeIf { it.isNotBlank() },
                        isModelHarnessEnabled = isModelHarnessEnabled,
                        personality = selectedPersonality,
                        zeroCloudMode = zeroCloudMode
                    )
                    onSaveConfig(updated)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Configuración")
            }
        }
    }
}

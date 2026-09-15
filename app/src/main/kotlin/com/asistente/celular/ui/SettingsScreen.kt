package com.asistente.celular.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: LlmConfig,
    isWakeWordActive: Boolean,
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
                        provider = selectedProvider,
                        apiKey = apiKey,
                        modelName = modelName,
                        customEndpoint = customEndpoint.takeIf { it.isNotBlank() },
                        isModelHarnessEnabled = isModelHarnessEnabled
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

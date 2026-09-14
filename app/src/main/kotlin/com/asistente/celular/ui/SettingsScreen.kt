package com.asistente.celular.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    onSaveConfig: (LlmConfig) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onDownloadModel: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onDeleteModel: (String) -> Unit = {},
    onSelectLocalModel: (String) -> Unit = {},
    onBack: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentConfig.provider) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var modelName by remember { mutableStateOf(currentConfig.modelName) }
    var customEndpoint by remember { mutableStateOf(currentConfig.customEndpoint ?: "") }
    var isModelHarnessEnabled by remember { mutableStateOf(currentConfig.isModelHarnessEnabled) }
    var showApiKey by remember { mutableStateOf(false) }

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

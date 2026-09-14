package com.asistente.celular.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ai.local.LocalModelManager
import com.asistente.celular.ai.local.LocalModelSpec
import com.asistente.celular.ai.local.ModelDownloadState

@Composable
fun LocalModelManagerSection(
    localModelManager: LocalModelManager,
    selectedModelId: String,
    onSelectModel: (String) -> Unit,
    onDownloadModel: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadStates by localModelManager.downloadStates.collectAsState()
    val availableModels = remember { localModelManager.getAvailableModels() }
    val freeBytes = remember(downloadStates) { localModelManager.getAvailableStorageBytes() }
    val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = "Almacenamiento",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Gestor de Pesos Locales (GGUF)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Espacio libre en dispositivo: ${String.format(java.util.Locale.US, "%.1f", freeGb)} GB. Descarga modelos compactos para ejecutar razonamiento sin conexión a internet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        availableModels.forEach { spec ->
            ModelSpecCard(
                spec = spec,
                isActive = spec.id.equals(selectedModelId, ignoreCase = true),
                isDownloaded = localModelManager.isModelDownloaded(spec.id),
                downloadState = downloadStates[spec.id] ?: ModelDownloadState.Idle,
                onSelect = { onSelectModel(spec.id) },
                onDownload = { onDownloadModel(spec.id) },
                onCancel = { onCancelDownload(spec.id) },
                onDelete = { onDeleteModel(spec.id) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ModelSpecCard(
    spec: LocalModelSpec,
    isActive: Boolean,
    isDownloaded: Boolean,
    downloadState: ModelDownloadState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloading = downloadState is ModelDownloadState.Downloading

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = spec.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "Tamaño: ${spec.formattedSize} • Cuantización: ${spec.quantization}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                SuggestionChip(
                    onClick = {},
                    label = { Text("${spec.recommendedRamGb}GB RAM", fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = spec.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isDownloading -> {
                    val progress = downloadState as ModelDownloadState.Downloading
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = progress.formattedProgress,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar descarga", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (progress.progressPercent / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                isDownloaded -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Descargado",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Descargado y listo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Text(
                                    "Activo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = onSelect,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Usar", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar modelo",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                downloadState is ModelDownloadState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Error: ${downloadState.message}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar Descarga (${spec.formattedSize})", fontSize = 12.sp)
                        }
                    }
                }

                else -> {
                    // No descargado (Idle)
                    OutlinedButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Descargar pesos (${spec.formattedSize})", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

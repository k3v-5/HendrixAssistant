package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.plugin.PcPluginAction
import com.asistente.celular.nlu.pc.plugin.PcPluginActionResult
import com.asistente.celular.nlu.pc.plugin.PcPluginDefinition
import kotlinx.coroutines.launch

@Composable
fun PcCustomPluginsCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf<List<PcPluginDefinition>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var runningActionKey by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<PcPluginActionResult?>(null) }

    fun refreshPlugins() {
        isLoading = true
        scope.launch {
            try {
                plugins = pcBridge.queryCustomPlugins()
            } catch (e: Exception) {
                onShowSnackbar("Error al consultar plugins: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPlugins()
    }

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🧩", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Plugins & Scripts de Usuario",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${plugins.size} plugins activos en hendrix-desktop/plugins/",
                            fontSize = 11.sp,
                            color = Color(0xFFC4B5FD)
                        )
                    }
                }

                IconButton(
                    onClick = { refreshPlugins() },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF8B5CF6)
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recargar plugins",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (plugins.isEmpty() && !isLoading) {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "No hay plugins detectados en la PC.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Copia tus scripts en Python (ej. plugin_mi_tarea.py) dentro de la carpeta hendrix-desktop/plugins/ para ejecutarlos desde aquí o por voz.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    plugins.forEach { plugin ->
                        PluginItemView(
                            plugin = plugin,
                            runningActionKey = runningActionKey,
                            onExecute = { action ->
                                val key = "${plugin.id}:${action.id}"
                                runningActionKey = key
                                scope.launch {
                                    try {
                                        val res = pcBridge.executeCustomPluginAction(plugin.id, action.id)
                                        lastResult = res
                                        if (res.success) {
                                            onShowSnackbar("✅ ${action.label}: ${res.message} (${res.elapsedMs}ms)")
                                        } else {
                                            onShowSnackbar("⚠️ ${action.label}: ${res.message}")
                                        }
                                    } catch (e: Exception) {
                                        onShowSnackbar("Error al ejecutar plugin: ${e.localizedMessage}")
                                    } finally {
                                        runningActionKey = null
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Último resultado de ejecución
            lastResult?.let { res ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = if (res.success) Color(0xFF0F172A) else Color(0xFF7F1D1D).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (res.success) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (res.success) "✅ Ejecución exitosa (${res.elapsedMs}ms)" else "⚠️ Error en ejecución",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (res.success) Color(0xFF34D399) else Color(0xFFF87171)
                            )
                            IconButton(
                                onClick = { lastResult = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Text("×", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                        Text(
                            text = res.message,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        if (res.output.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = res.output,
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                maxLines = 4
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginItemView(
    plugin: PcPluginDefinition,
    runningActionKey: String?,
    onExecute: (PcPluginAction) -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plugin.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "v${plugin.version}",
                            fontSize = 9.sp,
                            color = Color(0xFFC4B5FD),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                if (plugin.author.isNotBlank()) {
                    Text(
                        text = plugin.author,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            if (plugin.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plugin.description,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acciones del plugin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plugin.actions.forEach { action ->
                    val isActionRunning = runningActionKey == "${plugin.id}:${action.id}"
                    val buttonColor = if (action.dangerous) Color(0xFFEF4444) else Color(0xFF6366F1)

                    Button(
                        onClick = { onExecute(action) },
                        enabled = runningActionKey == null,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isActionRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = action.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

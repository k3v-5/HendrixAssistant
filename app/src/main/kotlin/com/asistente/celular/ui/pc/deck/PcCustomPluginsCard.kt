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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.plugin.PcPluginAction
import com.asistente.celular.nlu.pc.plugin.PcPluginActionResult
import com.asistente.celular.nlu.pc.plugin.PcPluginDefinition
import com.asistente.celular.ui.theme.NeonGreen
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
        scope.launch {
            isLoading = true
            try {
                plugins = pcBridge.queryCustomPlugins()
            } catch (e: Exception) {
                onShowSnackbar("Error al cargar plugins: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPlugins()
    }

    Surface(
        color = VoidSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.35f)),
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
                            .size(34.dp)
                            .background(NeonPurple.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Plugins & Scripts de Usuario",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (plugins.isEmpty()) "Sin scripts activos en plugins/"
                            else "${plugins.size} script${if (plugins.size == 1) "" else "s"} activo${if (plugins.size == 1) "" else "s"} • Hot-Reload",
                            fontSize = 11.sp,
                            color = NeonLilac
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
                            color = NeonPurple
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recargar plugins",
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (plugins.isEmpty() && !isLoading) {
                Surface(
                    color = VoidSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VoidBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "No hay plugins detectados en la PC",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Agrega archivos en Python (ej. plugin_mi_tarea.py) dentro de la carpeta hendrix-desktop/plugins/ para ejecutarlos desde aquí o por comandos de voz.",
                            fontSize = 11.sp,
                            color = TextSecondary
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
                                            onShowSnackbar("${action.label.ifBlank { action.id }}: ${res.message} (${res.elapsedMs}ms)")
                                        } else {
                                            onShowSnackbar("${action.label.ifBlank { action.id }}: ${res.message}")
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

            // Consola / Último resultado de ejecución
            lastResult?.let { res ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = VoidBlack,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (res.success) NeonGreen.copy(alpha = 0.45f) else NeonRed.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (res.success) NeonGreen else NeonRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (res.success) "Ejecutado con éxito (${res.elapsedMs}ms)" else "Fallo en ejecución",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.success) NeonGreen else NeonRed
                                )
                            }
                            IconButton(
                                onClick = { lastResult = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = res.message,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                        if (res.output.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = VoidSurface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = res.output,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary,
                                        maxLines = 6,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun PluginItemView(
    plugin: PcPluginDefinition,
    runningActionKey: String?,
    onExecute: (PcPluginAction) -> Unit
) {
    Surface(
        color = VoidSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, VoidBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Cabecera del plugin: Icono/Emoji + Nombre + Versión + Autor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val iconText = plugin.icon.takeIf { it.isNotBlank() && it != "code" } ?: "⚡"
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = iconText,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = NeonPurple.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, NeonPurple.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "v${plugin.version}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonLilac,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (plugin.author.isNotBlank()) {
                            Text(
                                text = "Por ${plugin.author}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            if (plugin.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plugin.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            // Lista de acciones del plugin en tarjetas independientes horizontales
            if (plugin.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    plugin.actions.forEach { action ->
                        val isActionRunning = runningActionKey == "${plugin.id}:${action.id}"
                        val actionLabel = action.label.ifBlank { action.id }

                        Surface(
                            color = VoidSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, if (action.dangerous) NeonRed.copy(alpha = 0.35f) else VoidBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = actionLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    if (action.description.isNotBlank()) {
                                        Text(
                                            text = action.description,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { onExecute(action) },
                                    enabled = runningActionKey == null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (action.dangerous) NeonRed else NeonPurple,
                                        disabledContainerColor = (if (action.dangerous) NeonRed else NeonPurple).copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    if (isActionRunning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = VoidBlack
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Icon(
                                            imageVector = if (action.dangerous) Icons.Default.Warning else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = VoidBlack,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isActionRunning) "Corriendo..." else "Ejecutar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VoidBlack
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

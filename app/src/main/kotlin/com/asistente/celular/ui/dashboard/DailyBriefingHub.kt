package com.asistente.celular.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.tasks.TaskItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Command Center & Daily Briefing Hub para Hendrix Assistant.
 * Optimizado ergonómicamente para eliminar el desplazamiento vertical infinito
 * organizando la pantalla principal en cápsulas contextuales y carruseles horizontales.
 */
@Composable
fun DailyBriefingHub(
    llmConfig: LlmConfig,
    tasks: List<TaskItem>,
    notes: List<NoteItem>,
    smartDevices: List<SmartDevice>,
    onSendCommand: (String) -> Unit,
    onToggleSmartDevice: (SmartDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val pendingTasksCount = remember(tasks) { tasks.count { !it.isCompleted } }
    val pinnedNotesCount = remember(notes) { notes.count { it.isPinned } }

    val currentHour = remember { LocalTime.now().hour }
    val (greeting, greetingEmoji) = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "¡Buenos días!" to "☀️"
            in 12..18 -> "¡Buenas tardes!" to "🌤️"
            else -> "¡Buenas noches!" to "🌙"
        }
    }

    val formattedDate = remember {
        val now = LocalDate.now()
        val text = now.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES")))
        text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Cápsula Compacta de Saludo & Resumen de Voz Integrado
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = greetingEmoji, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Botón de Briefing Diario en Audio compacto
                    Button(
                        onClick = {
                            val briefPrompt = buildString {
                                append("Dame un resumen conciso de mi día. ")
                                if (pendingTasksCount > 0) {
                                    append("Tengo $pendingTasksCount tareas pendientes. ")
                                } else {
                                    append("No tengo tareas pendientes. ")
                                }
                                if (notes.isNotEmpty()) {
                                    append("Tengo ${notes.size} notas guardadas. ")
                                }
                                append("Dime qué me sugieres priorizar hoy.")
                            }
                            onSendCommand(briefPrompt)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resumen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badges de modo y personalidad
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // Badge Zero-Cloud / Privacidad
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (llmConfig.zeroCloudMode) Color(0xFF1B5E20).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(if (llmConfig.zeroCloudMode) "🛡️" else "☁️", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (llmConfig.zeroCloudMode) "Zero-Cloud Local" else "IA: ${llmConfig.provider.displayName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (llmConfig.zeroCloudMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Badge Personalidad
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🎭", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = llmConfig.personality.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 2. Barra de Métricas Clave (KPIs Rápidos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadge(
                modifier = Modifier.weight(1f),
                icon = "📋",
                count = pendingTasksCount.toString(),
                label = "Pendientes",
                highlight = pendingTasksCount > 0,
                onClick = { onSendCommand("Muestra mis tareas pendientes") }
            )
            MetricBadge(
                modifier = Modifier.weight(1f),
                icon = "📝",
                count = notes.size.toString(),
                label = if (pinnedNotesCount > 0) "$pinnedNotesCount fijas" else "Notas",
                highlight = false,
                onClick = { onSendCommand("Muestra mis notas") }
            )
            MetricBadge(
                modifier = Modifier.weight(1f),
                icon = "💡",
                count = smartDevices.size.toString(),
                label = "Domótica",
                highlight = smartDevices.any { it.isPoweredOn },
                onClick = { onSendCommand("Estado de los focos") }
            )
        }

        // 3. Carrusel Horizontal de Domótica (si hay focos vinculados)
        if (smartDevices.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "💡 Control Domótico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${smartDevices.count { it.isPoweredOn }} encendidos",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Carrusel horizontal de focos
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(smartDevices, key = { it.id }) { device ->
                        CompactSmartDeviceCard(
                            device = device,
                            onToggle = { onToggleSmartDevice(device) }
                        )
                    }
                }

                // Chips de escenas rápidas
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickActionChip("☀️ Brillo 100%") { onSendCommand("Pon el foco al 100 por ciento") }
                    QuickActionChip("🌙 Luz Noche 10%") { onSendCommand("Pon el foco al 10 por ciento") }
                    QuickActionChip("💡 Luz Cálida") { onSendCommand("Pon el foco en color cálido") }
                    QuickActionChip("🍿 Modo Cine") { onSendCommand("Activa el modo cine") }
                }
            }
        }

        // 4. Ecosistema de Aplicaciones
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "🚀 Accesos y Ecosistema de Apps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppActionCard(
                    icon = "🗺️",
                    title = "Navegar Casa",
                    app = "Google Maps",
                    onClick = { onSendCommand("Ir a casa con Google Maps") }
                )
                AppActionCard(
                    icon = "🚕",
                    title = "Pedir Uber",
                    app = "Uber",
                    onClick = { onSendCommand("Pedir un Uber") }
                )
                AppActionCard(
                    icon = "🚗",
                    title = "Tráfico",
                    app = "Waze",
                    onClick = { onSendCommand("Abrir Waze") }
                )
                AppActionCard(
                    icon = "📦",
                    title = "Compras",
                    app = "Mercado Libre",
                    onClick = { onSendCommand("Buscar compras en Mercado Libre") }
                )
                AppActionCard(
                    icon = "🛍️",
                    title = "Ofertas",
                    app = "Amazon",
                    onClick = { onSendCommand("Buscar en Amazon") }
                )
                AppActionCard(
                    icon = "🎵",
                    title = "Música",
                    app = "Spotify",
                    onClick = { onSendCommand("Pon música en Spotify") }
                )
                AppActionCard(
                    icon = "✉️",
                    title = "Correo",
                    app = "Gmail",
                    onClick = { onSendCommand("Abrir Gmail") }
                )
                AppActionCard(
                    icon = "▶️",
                    title = "Videos",
                    app = "YouTube",
                    onClick = { onSendCommand("Abrir YouTube") }
                )
            }
        }

        // 5. Productividad Instantánea
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "⚡ Productividad Instantánea",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProductivityChip("⏱️ Pomodoro 25m") { onSendCommand("Pon un temporizador de 25 minutos") }
                ProductivityChip("⏰ Alarma 7:00 AM") { onSendCommand("Pon una alarma a las 7 de la mañana") }
                ProductivityChip("🔦 Linterna") { onSendCommand("Enciende la linterna") }
                ProductivityChip("💱 50 USD a MXN") { onSendCommand("Convierte 50 dolares a pesos") }
                ProductivityChip("📝 Anotar Idea") { onSendCommand("Anota comprar café para la oficina") }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CompactSmartDeviceCard(
    device: SmartDevice,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (device.isPoweredOn)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .width(135.dp)
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (device.isPoweredOn) "💡" else "🌑",
                    fontSize = 20.sp
                )
                Switch(
                    checked = device.isPoweredOn,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(width = 36.dp, height = 24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = device.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (device.isPoweredOn) "Encendido (${device.brightness}%)" else "Apagado",
                fontSize = 10.sp,
                color = if (device.isPoweredOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricBadge(
    modifier: Modifier = Modifier,
    icon: String,
    count: String,
    label: String,
    highlight: Boolean,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (highlight)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AppActionCard(
    icon: String,
    title: String,
    app: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProductivityChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

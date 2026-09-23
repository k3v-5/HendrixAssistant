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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ai.model.LlmConfig
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.ui.theme.NeonLilac
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
    val (greeting, greetingIcon) = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "¡Buenos días!" to Icons.Default.WbSunny
            in 12..18 -> "¡Buenas tardes!" to Icons.Default.WbTwilight
            else -> "¡Buenas noches!" to Icons.Default.NightsStay
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonLilac.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = greetingIcon,
                            contentDescription = null,
                            tint = NeonLilac,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
                            Icon(
                                imageVector = if (llmConfig.zeroCloudMode) Icons.Default.Shield else Icons.Default.Cloud,
                                contentDescription = null,
                                tint = if (llmConfig.zeroCloudMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
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
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
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
                icon = Icons.Default.CheckCircle,
                count = pendingTasksCount.toString(),
                label = "Pendientes",
                highlight = pendingTasksCount > 0,
                onClick = { onSendCommand("Muestra mis tareas pendientes") }
            )
            MetricBadge(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EditNote,
                count = notes.size.toString(),
                label = if (pinnedNotesCount > 0) "$pinnedNotesCount fijas" else "Notas",
                highlight = false,
                onClick = { onSendCommand("Muestra mis notas") }
            )
            MetricBadge(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Lightbulb,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Control Domótico",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                    QuickActionChip("Brillo 100%", Icons.Default.WbSunny) { onSendCommand("Pon el foco al 100 por ciento") }
                    QuickActionChip("Luz Noche 10%", Icons.Default.NightsStay) { onSendCommand("Pon el foco al 10 por ciento") }
                    QuickActionChip("Luz Cálida", Icons.Default.Lightbulb) { onSendCommand("Pon el foco en color cálido") }
                    QuickActionChip("Modo Cine", Icons.Default.Movie) { onSendCommand("Activa el modo cine") }
                }
            }
        }

        // 4. Ecosistema de Aplicaciones
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Accesos y Ecosistema de Apps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppActionCard(
                    icon = Icons.Default.Map,
                    title = "Navegar Casa",
                    app = "Google Maps",
                    onClick = { onSendCommand("Ir a casa con Google Maps") }
                )
                AppActionCard(
                    icon = Icons.Default.DirectionsCar,
                    title = "Pedir Uber",
                    app = "Uber",
                    onClick = { onSendCommand("Pedir un Uber") }
                )
                AppActionCard(
                    icon = Icons.Default.Traffic,
                    title = "Tráfico",
                    app = "Waze",
                    onClick = { onSendCommand("Abrir Waze") }
                )
                AppActionCard(
                    icon = Icons.Default.ShoppingBag,
                    title = "Compras",
                    app = "Mercado Libre",
                    onClick = { onSendCommand("Buscar compras en Mercado Libre") }
                )
                AppActionCard(
                    icon = Icons.Default.ShoppingCart,
                    title = "Ofertas",
                    app = "Amazon",
                    onClick = { onSendCommand("Buscar en Amazon") }
                )
                AppActionCard(
                    icon = Icons.Default.MusicNote,
                    title = "Música",
                    app = "Spotify",
                    onClick = { onSendCommand("Pon música en Spotify") }
                )
                AppActionCard(
                    icon = Icons.Default.Email,
                    title = "Correo",
                    app = "Gmail",
                    onClick = { onSendCommand("Abrir Gmail") }
                )
                AppActionCard(
                    icon = Icons.Default.PlayCircle,
                    title = "Videos",
                    app = "YouTube",
                    onClick = { onSendCommand("Abrir YouTube") }
                )
            }
        }

        // 5. Productividad Instantánea
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Productividad Instantánea",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProductivityChip("Pomodoro 25m", Icons.Default.Timer) { onSendCommand("Pon un temporizador de 25 minutos") }
                ProductivityChip("Alarma 7:00 AM", Icons.Default.Alarm) { onSendCommand("Pon una alarma a las 7 de la mañana") }
                ProductivityChip("Linterna", Icons.Default.FlashlightOn) { onSendCommand("Enciende la linterna") }
                ProductivityChip("50 USD a MXN", Icons.Default.CurrencyExchange) { onSendCommand("Convierte 50 dolares a pesos") }
                ProductivityChip("Anotar Idea", Icons.Default.EditNote) { onSendCommand("Anota comprar café para la oficina") }
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
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (device.isPoweredOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
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
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
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
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppActionCard(
    icon: ImageVector,
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
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
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

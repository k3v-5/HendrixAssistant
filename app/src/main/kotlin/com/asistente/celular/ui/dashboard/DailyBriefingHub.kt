package com.asistente.celular.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
 * Reemplaza la pantalla vacía con un dashboard contextual interactivo que ofrece:
 * - Saludo contextual según hora del día y fecha.
 * - Resumen diario en audio ("▶️ Escuchar resumen del día").
 * - Widget de control domótico en vivo (focos Xiaomi/Yeelight).
 * - Chips de acceso directo a ecosistema profundo (Maps, Uber, Waze, MercadoLibre, Amazon, Spotify).
 * - Estado de privacidad y personalidad activa.
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Saludo Contextual & Insignias
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = greetingEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Badges de modo y personalidad
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // Badge Zero-Cloud / Privacidad
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (llmConfig.zeroCloudMode) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(if (llmConfig.zeroCloudMode) "🛡️" else "☁️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (llmConfig.zeroCloudMode) "Zero-Cloud 100% Offline" else "Enrutador IA: ${llmConfig.provider.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (llmConfig.zeroCloudMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Badge Personalidad
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("🎭", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = llmConfig.personality.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 2. Tarjeta Resumen del Día & Audio Briefing
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Centro de Mando Diario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Métricas clave
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadge(
                        modifier = Modifier.weight(1f),
                        icon = "✅",
                        count = pendingTasksCount.toString(),
                        label = "Pendientes",
                        highlight = pendingTasksCount > 0
                    )
                    MetricBadge(
                        modifier = Modifier.weight(1f),
                        icon = "📝",
                        count = notes.size.toString(),
                        label = if (pinnedNotesCount > 0) "$pinnedNotesCount fijas" else "Notas",
                        highlight = false
                    )
                    MetricBadge(
                        modifier = Modifier.weight(1f),
                        icon = "💡",
                        count = smartDevices.size.toString(),
                        label = "Domótica",
                        highlight = smartDevices.any { it.isPoweredOn }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón principal de Audio Briefing
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("▶️ Escuchar resumen del día", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Widget de Control Domótico Rápido (si hay focos vinculados)
        if (smartDevices.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Focos Xiaomi / Yeelight",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    smartDevices.forEach { device ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    if (device.isPoweredOn) "Encendido (${device.brightness}%)" else "Apagado",
                                    fontSize = 12.sp,
                                    color = if (device.isPoweredOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = device.isPoweredOn,
                                onCheckedChange = { onToggleSmartDevice(device) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
        }

        // 4. Ecosistema de Apps Profundo (Item 2)
        Text(
            text = "Acciones y Ecosistema de Apps",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppActionCard(
                icon = "🗺️",
                title = "Navegar a Casa",
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
                title = "Tráfico en Vivo",
                app = "Waze",
                onClick = { onSendCommand("Abrir Waze") }
            )
            AppActionCard(
                icon = "📦",
                title = "Mis Compras",
                app = "Mercado Libre",
                onClick = { onSendCommand("Buscar compras en Mercado Libre") }
            )
            AppActionCard(
                icon = "🛍️",
                title = "Ofertas Hoy",
                app = "Amazon",
                onClick = { onSendCommand("Buscar en Amazon") }
            )
            AppActionCard(
                icon = "🎵",
                title = "Música Focus",
                app = "Spotify",
                onClick = { onSendCommand("Pon música en Spotify") }
            )
            AppActionCard(
                icon = "✉️",
                title = "Bandeja Entrada",
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

        // 5. Productividad y Control Rápido
        Text(
            text = "Productividad Instantánea",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductivityChip("⏱️ Pomodoro 25m") { onSendCommand("Pon un temporizador de 25 minutos") }
            ProductivityChip("⏰ Alarma 7:00 AM") { onSendCommand("Pon una alarma a las 7 de la mañana") }
            ProductivityChip("🔦 Linterna") { onSendCommand("Enciende la linterna") }
            ProductivityChip("💱 50 USD a MXN") { onSendCommand("Convierte 50 dolares a pesos") }
            ProductivityChip("📝 Anotar Idea") { onSendCommand("Anota comprar café para la oficina") }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricBadge(
    modifier: Modifier = Modifier,
    icon: String,
    count: String,
    label: String,
    highlight: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (highlight)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 11.sp,
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = app,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
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
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

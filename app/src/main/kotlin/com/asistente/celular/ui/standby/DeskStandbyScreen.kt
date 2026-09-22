package com.asistente.celular.ui.standby

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de Modo "Desk Standby" (Smart Display de Escritorio).
 * 
 * Presenta un HUD estético OLED de alto contraste con:
 * 1. Reloj digital gigante y calendario.
 * 2. Telemetría de hardware de la PC en tiempo real (CPU, RAM, GPU).
 * 3. Botones rápidos de control multimedia y energía.
 * 4. Orbe de voz centralizado para escucha en campo lejano.
 */
@Composable
fun DeskStandbyScreen(
    pcBridge: PcWorkspaceBridge?,
    macroDeckRepository: com.asistente.celular.nlu.pc.deck.dynamic.MacroDeckProfileRepository? = null,
    isListening: Boolean = false,
    onStartListening: () -> Unit = {},
    onWakeWordClick: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null,
    onBack: () -> Unit = { onExit?.invoke() }
) {
    val scope = rememberCoroutineScope()
    val effectiveExit = onExit ?: onBack
    val effectiveMicClick = onWakeWordClick ?: onStartListening

    // Reloj dinámico
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val secondsFormatter = remember { SimpleDateFormat("ss", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")) }

    val isConnected = pcBridge?.isConnected?.collectAsState()?.value ?: false
    val telemetry = pcBridge?.telemetry?.collectAsState()?.value
    val hardware = pcBridge?.hardwareTelemetry?.collectAsState()?.value

    // Pulsación suave del orbe
    val infiniteTransition = rememberInfiniteTransition(label = "standby_orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF060913)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Barra Superior: Volver + Estado PC + Wake on LAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = effectiveExit) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Salir de Standby",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "PC CONECTADA" else "PC DESCONECTADA",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (!isConnected) {
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                scope.launch { pcBridge?.wakeOnLan() }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Wake on LAN",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Sección Central: Reloj Digital y Fecha Futurista
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = timeFormatter.format(currentTime),
                        fontSize = 76.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF),
                        letterSpacing = (-2).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = secondsFormatter.format(currentTime),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
                Text(
                    text = dateFormatter.format(currentTime).replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }

            // 3. Telemetría de PC en Vivo
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1426)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TELEMETRÍA EN TIEMPO REAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isConnected) "ACTIVO" else "SIN SEÑAL",
                            fontSize = 11.sp,
                            color = if (isConnected) Color(0xFF10B981) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val cpuPercent = (hardware?.cpuUsagePercent ?: telemetry?.cpuPercent?.toFloat() ?: 0f) / 100f
                    val ramUsed = hardware?.ramUsedMb ?: 0
                    val ramTotal = hardware?.ramTotalMb ?: 16384
                    val ramPercent = if (ramTotal > 0) ramUsed.toFloat() / ramTotal.toFloat() else 0f
                    val gpuTemp = hardware?.gpuTempCelsius ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // CPU
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CPU", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${(cpuPercent * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { cpuPercent.coerceIn(0f, 1f) },
                                color = Color(0xFF00E5FF),
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }

                        // RAM
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RAM", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("${"%.1f".format(ramUsed / 1024f)} GB", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ramPercent.coerceIn(0f, 1f) },
                                color = Color(0xFF818CF8),
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }

                        // GPU
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GPU", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                text = if (gpuTemp > 0) "${gpuTemp}°C" else "--",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gpuTemp > 75) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ((hardware?.gpuUsagePercent ?: 0f) / 100f).coerceIn(0f, 1f) },
                                color = Color(0xFFF59E0B),
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }

            // 4. Orbe de Voz y Botones de Control Rápido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Controles de volumen
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { scope.launch { pcBridge?.executeQuickCommand("volume_down") } },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF0D1426))
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Bajar Volumen", tint = Color.White)
                    }
                    IconButton(
                        onClick = { scope.launch { pcBridge?.executeQuickCommand("volume_up") } },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF0D1426))
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Subir Volumen", tint = Color.White)
                    }
                }

                // Orbe Central Interactivo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulseScale)
                        .shadow(24.dp, CircleShape, spotColor = if (isListening) Color(0xFF00E5FF) else Color(0xFF6366F1))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isListening)
                                    listOf(Color(0xFF00E5FF), Color(0xFF0284C7))
                                else
                                    listOf(Color(0xFF6366F1), Color(0xFF312E81))
                            )
                        )
                        .clickable(onClick = effectiveMicClick)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "Escuchar por voz",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Controles de reproducción
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { scope.launch { pcBridge?.executeQuickCommand("media_play_pause") } },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF0D1426))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pausa", tint = Color.White)
                    }
                    IconButton(
                        onClick = { scope.launch { pcBridge?.executeQuickCommand("lock") } },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF0D1426))
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Bloquear PC", tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import kotlinx.coroutines.launch

@Composable
fun PcHardwareWatchdogCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val telemetry by pcBridge.hardwareTelemetry.collectAsState()
    val latestWatchdogEvent by pcBridge.watchdogEvents.collectAsState()

    var isWatchdogActive by remember { mutableStateOf(false) }
    var autoSuspendEnabled by remember { mutableStateOf(true) }
    var selectedProcess by remember { mutableStateOf("blender") }
    var isStartingWatchdog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        pcBridge.queryHardwareTelemetry()
    }

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
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
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hardware & Render Watchdog",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = telemetry?.gpuName ?: "Monitoreo de GPU / CPU / Térmico",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            pcBridge.queryHardwareTelemetry()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar Telemetría",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Métricas de Hardware
            val gpuUsage = telemetry?.gpuUsagePercent ?: 0f
            val gpuTemp = telemetry?.gpuTempCelsius ?: 0
            val vramUsed = telemetry?.vramUsedMb ?: 0L
            val vramTotal = (telemetry?.vramTotalMb ?: 8192L).coerceAtLeast(1L)
            val vramProgress = (vramUsed.toFloat() / vramTotal.toFloat()).coerceIn(0f, 1f)

            val cpuUsage = telemetry?.cpuUsagePercent ?: 0f
            val ramUsed = telemetry?.ramUsedMb ?: 0L
            val ramTotal = (telemetry?.ramTotalMb ?: 16384L).coerceAtLeast(1L)
            val ramProgress = (ramUsed.toFloat() / ramTotal.toFloat()).coerceIn(0f, 1f)

            val tempColor = when {
                gpuTemp > 75 -> Color(0xFFEF4444)
                gpuTemp > 65 -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }

            // Grid de 2 columnas para GPU y CPU
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tarjeta GPU
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GPU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "${gpuTemp}°C",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tempColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${gpuUsage.toInt()}% uso", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("VRAM: ${vramUsed / 1024}GB / ${vramTotal / 1024}GB", fontSize = 9.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { vramProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }

                // Tarjeta CPU
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CPU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "${cpuUsage.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${ramUsed / 1024}GB", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("RAM: ${(ramProgress * 100).toInt()}% en uso", fontSize = 9.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ramProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }

            telemetry?.activeHeavyProcess?.let { heavyProc ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Proceso intensivo detectado: $heavyProc",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sección de Configuración del Vigilante de Render
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto-suspender al terminar render",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Apaga o suspende la PC automáticamente",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = autoSuspendEnabled,
                            onCheckedChange = { autoSuspendEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isStartingWatchdog = true
                            scope.launch {
                                try {
                                    val ok = pcBridge.startRenderWatchdog(
                                        processName = selectedProcess,
                                        autoSuspend = autoSuspendEnabled
                                    )
                                    isWatchdogActive = ok
                                    if (ok) {
                                        onShowSnackbar("Centinela activado para $selectedProcess")
                                    } else {
                                        onShowSnackbar("Error al activar centinela")
                                    }
                                } catch (e: Exception) {
                                    onShowSnackbar("Error: ${e.message}")
                                } finally {
                                    isStartingWatchdog = false
                                }
                            }
                        },
                        enabled = !isStartingWatchdog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWatchdogActive) Color(0xFF10B981) else Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (isStartingWatchdog) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isWatchdogActive) Icons.Default.Shield else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isWatchdogActive) "Vigilante de Render Activo" else "Activar Centinela de Render",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Notificación de evento de render completado
            latestWatchdogEvent?.let { event ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "¡Render Finalizado con Éxito!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = event.message,
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        if (event.durationSeconds > 0) {
                            val mins = event.durationSeconds / 60
                            val secs = event.durationSeconds % 60
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tiempo de render: ${mins}m ${secs}s | Pico GPU: ${event.peakGpuTemp}°C",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

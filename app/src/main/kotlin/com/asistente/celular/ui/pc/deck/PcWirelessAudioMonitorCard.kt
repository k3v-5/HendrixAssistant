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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.asistente.celular.pc.PcRemoteCoordinator
import kotlinx.coroutines.launch

@Composable
fun PcWirelessAudioMonitorCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val audioState by pcBridge.audioStreamState.collectAsState()

    val coordinator = pcBridge as? PcRemoteCoordinator
    val rmsLevel by coordinator?.audioStreamPlayer?.currentRms?.collectAsState() ?: remember { mutableFloatStateOf(0.0f) }
    val peakLevel by coordinator?.audioStreamPlayer?.currentPeak?.collectAsState() ?: remember { mutableFloatStateOf(0.0f) }

    var isToggling by remember { mutableStateOf(false) }
    var monitorVolume by remember { mutableFloatStateOf(1.0f) }

    val isStreaming = audioState.isStreaming

    fun toggleAudioMonitoring() {
        isToggling = true
        scope.launch {
            try {
                if (isStreaming) {
                    pcBridge.stopAudioMonitoring()
                    onShowSnackbar("⏹️ Monitor de audio inalámbrico detenido")
                } else {
                    val ok = pcBridge.startAudioMonitoring(sampleRate = 24000)
                    if (ok) {
                        onShowSnackbar("🎙️ Monitor de audio en vivo activado (< 25ms)")
                    } else {
                        onShowSnackbar("⚠️ Error al iniciar monitor de audio en la PC")
                    }
                }
            } catch (e: Exception) {
                onShowSnackbar("Error: ${e.localizedMessage}")
            } finally {
                isToggling = false
            }
        }
    }

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
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
                            .background(Color(0xFF06B6D4).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎧", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Monitor de Audio Inalámbrico",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isStreaming) "En vivo (WASAPI Loopback • 24kHz PCM)" else "Transmisión en tiempo real de la PC",
                            fontSize = 11.sp,
                            color = if (isStreaming) Color(0xFF67E8F9) else Color.Gray
                        )
                    }
                }

                // Badge En Vivo / Pausa
                Surface(
                    color = if (isStreaming) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isStreaming) Color(0xFFEF4444) else Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isStreaming) Color(0xFFEF4444) else Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isStreaming) "EN VIVO" else "INACTIVO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStreaming) Color(0xFFFCA5A5) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vúmetro / Indicador de niveles de audio en tiempo real
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Nivel de Audio (VU-Meter)", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = if (isStreaming) "${(rmsLevel * 100).toInt()}% RMS" else "0%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rmsLevel > 0.7f) Color(0xFFEF4444) else Color(0xFF06B6D4)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (isStreaming) (rmsLevel * 1.5f).coerceIn(0.0f, 1.0f) else 0.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (rmsLevel > 0.75f) Color(0xFFEF4444) else Color(0xFF06B6D4),
                    trackColor = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Control de volumen del monitor móvil
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Slider(
                    value = monitorVolume,
                    onValueChange = {
                        monitorVolume = it
                        coordinator?.audioStreamPlayer?.setVolume(it)
                    },
                    valueRange = 0.0f..1.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF06B6D4),
                        activeTrackColor = Color(0xFF06B6D4),
                        inactiveTrackColor = Color(0xFF1E293B)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${(monitorVolume * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Iniciar / Detener Monitoreo
            Button(
                onClick = { toggleAudioMonitoring() },
                enabled = !isToggling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStreaming) Color(0xFFEF4444) else Color(0xFF06B6D4)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                if (isToggling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conectando audio...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text(
                        text = if (isStreaming) "⏹️ Detener Monitoreo de Audio" else "▶️ Iniciar Monitoreo Inalámbrico",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

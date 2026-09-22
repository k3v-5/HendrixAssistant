package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.asistente.celular.nlu.pc.audio.PcAudioAppSession
import kotlinx.coroutines.launch

@Composable
fun PcAudioMixerCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val mixerState by pcBridge.audioMixerState.collectAsState()
    val telemetry by pcBridge.telemetry.collectAsState()

    LaunchedEffect(Unit) {
        pcBridge.queryAudioMixer()
    }

    val masterVol = mixerState?.masterVolumePercent ?: telemetry?.masterVolumePercent ?: 50
    val isMasterMuted = mixerState?.isMasterMuted ?: telemetry?.isVolumeMuted ?: false
    val sessions = mixerState?.sessions ?: emptyList()

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
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
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎚️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mezclador de Audio de Windows",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Control de volumen independiente por aplicación",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            val res = pcBridge.queryAudioMixer()
                            if (res != null) {
                                onShowSnackbar("🎚️ Sesiones de audio actualizadas (${res.sessions.size} apps)")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar Mezclador", tint = Color(0xFFF59E0B))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fader Maestro
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔊", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Volumen Maestro Windows",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMasterMuted) "Silenciado" else "$masterVol%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMasterMuted) Color(0xFFEF4444) else Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        pcBridge.setMasterMute(!isMasterMuted)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMasterMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute Maestro",
                                    tint = if (isMasterMuted) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    var sliderVal by remember(masterVol) { mutableFloatStateOf(masterVol.toFloat()) }
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        onValueChangeFinished = {
                            scope.launch {
                                pcBridge.setMasterVolume(sliderVal.toInt())
                            }
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista de faders de aplicaciones activas
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se detectaron aplicaciones reproduciendo audio en este momento.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Text(
                    text = "Aplicaciones Activas (${sessions.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                sessions.forEach { appSession ->
                    AudioAppFaderRow(
                        session = appSession,
                        onVolumeChange = { vol ->
                            scope.launch {
                                pcBridge.setAppVolume(appSession.processName, vol)
                            }
                        },
                        onMuteToggle = { muted ->
                            scope.launch {
                                pcBridge.setAppMute(appSession.processName, muted)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun AudioAppFaderRow(
    session: PcAudioAppSession,
    onVolumeChange: (Int) -> Unit,
    onMuteToggle: (Boolean) -> Unit
) {
    var currentVol by remember(session.volumePercent) { mutableFloatStateOf(session.volumePercent.toFloat()) }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = session.iconEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (session.isMuted) "Mute" else "${currentVol.toInt()}%",
                        fontSize = 11.sp,
                        color = if (session.isMuted) Color(0xFFEF4444) else Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onMuteToggle(!session.isMuted) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute App",
                            tint = if (session.isMuted) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Slider(
                value = currentVol,
                onValueChange = { currentVol = it },
                onValueChangeFinished = { onVolumeChange(currentVol.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF10B981),
                    activeTrackColor = Color(0xFF10B981),
                    inactiveTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}

package com.asistente.celular.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.daw.DawAction
import com.asistente.celular.nlu.ui.DawControlUiPayload
import com.asistente.celular.ui.pc.daw.DawProjectSelectorOrganism
import com.asistente.celular.ui.pc.daw.DawTransportBarMolecule

/**
 * Tarjeta interactiva para la estación de audio digital (DAW / Ableton Live).
 * Integra la barra de transporte de baja latencia, controles de grabación,
 * guardado de sesión, exportación de audio y selector de proyectos .als.
 */
@Composable
fun DawControlCard(
    payload: DawControlUiPayload,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Ícono de sintetizador/piano, Título y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape)
                            .border(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎹", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ableton Live",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = payload.hostname,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = payload.statusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Proyecto activo
            if (payload.activeProjectName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎵", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Proyecto activo: ${payload.activeProjectName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Diálogo o banner interactivo de confirmación de guardado de proyecto
            if (payload.isWaitingSaveConfirmation) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚠️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = payload.confirmationDialogTitle ?: "¿Guardar cambios en el proyecto actual?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ableton Live detectó modificaciones sin guardar antes de continuar.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onExecuteCommand("si guardar en ableton") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💾 Guardar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onExecuteCommand("descartar en ableton") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🗑️ Descartar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onExecuteCommand("cancelar en ableton") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("✖️ Cancelar", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de transporte (Play/Pausa, Rec, Loop, Export)
            DawTransportBarMolecule(
                isPlaying = payload.isPlaying,
                isRecording = payload.isRecording,
                onSendAction = { action ->
                    when (action) {
                        DawAction.PLAY_PAUSE -> onExecuteCommand("reproduce ableton")
                        DawAction.RECORD -> onExecuteCommand("graba en ableton")
                        DawAction.TOGGLE_LOOP -> onExecuteCommand("loop en ableton")
                        DawAction.TOGGLE_METRONOME -> onExecuteCommand("metronomo en ableton")
                        DawAction.EXPORT_AUDIO -> onExecuteCommand("exporta el audio en ableton")
                        DawAction.SAVE_PROJECT -> onExecuteCommand("guarda el proyecto en ableton")
                        DawAction.NEW_PROJECT -> onExecuteCommand("nuevo proyecto en ableton")
                        else -> onExecuteCommand("abre ableton")
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector y explorador de proyectos .als
            DawProjectSelectorOrganism(
                projects = payload.recentProjects,
                activeProjectName = payload.activeProjectName,
                onNewProject = { onExecuteCommand("nuevo proyecto en ableton") },
                onSaveProject = { onExecuteCommand("guarda el proyecto en ableton") },
                onOpenProject = { proj -> onExecuteCommand("carga el proyecto ${proj.name} en ableton") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para enfocar o lanzar Ableton
            Button(
                onClick = { onExecuteCommand("abre ableton") },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Enfocar Ableton Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

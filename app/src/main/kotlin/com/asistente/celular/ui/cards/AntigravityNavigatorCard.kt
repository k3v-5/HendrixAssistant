package com.asistente.celular.ui.cards

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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.ui.AntigravityNavigatorUiPayload
import com.asistente.celular.ui.pc.antigravity.AntigravityChatListOrganism
import com.asistente.celular.ui.pc.antigravity.AntigravityProjectSelectorMolecule

/**
 * Tarjeta interactiva para la navegación, gestión de proyectos y redacción de conversaciones
 * en Google Antigravity directamente desde el asistente Hendrix.
 */
@Composable
fun AntigravityNavigatorCard(
    payload: AntigravityNavigatorUiPayload,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProject by remember(payload.selectedProject, payload.projects) {
        mutableStateOf(payload.selectedProject ?: payload.projects.firstOrNull())
    }

    // Filtrar chats según el proyecto seleccionado si aplica
    val currentSelected = selectedProject
    val displayedChats = remember(currentSelected, payload.recentChats) {
        if (currentSelected != null && payload.recentChats.isNotEmpty()) {
            val filtered = payload.recentChats.filter {
                it.projectId == currentSelected.id ||
                it.workspaceUri.contains(currentSelected.name, ignoreCase = true) ||
                it.workspaceUri == currentSelected.workspaceUri
            }
            if (filtered.isNotEmpty()) filtered else payload.recentChats
        } else {
            payload.recentChats
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Ícono Antigravity, Título y Host
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF6366F1).copy(alpha = 0.18f), CircleShape)
                            .border(1.2.dp, Color(0xFF6366F1).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Antigravity",
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

            Spacer(modifier = Modifier.height(14.dp))

            // Selector horizontal de proyectos
            AntigravityProjectSelectorMolecule(
                projects = payload.projects,
                selectedProjectId = selectedProject?.id,
                onSelectProject = { proj ->
                    selectedProject = proj
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Lista interactiva de conversaciones
            AntigravityChatListOrganism(
                chats = displayedChats,
                onContinueChat = { chat, prompt ->
                    if (prompt.isNullOrBlank()) {
                        onExecuteCommand("en antigravity continúa el chat ${chat.title}")
                    } else {
                        onExecuteCommand("en antigravity continúa el chat ${chat.title} y escribe $prompt")
                    }
                },
                onNewChat = { prompt ->
                    val projName = selectedProject?.name
                    if (prompt.isNullOrBlank()) {
                        if (projName != null) {
                            onExecuteCommand("en antigravity en el proyecto $projName nuevo chat")
                        } else {
                            onExecuteCommand("en antigravity nuevo chat")
                        }
                    } else {
                        if (projName != null) {
                            onExecuteCommand("en antigravity en el proyecto $projName nuevo chat y escribe $prompt")
                        } else {
                            onExecuteCommand("en antigravity nuevo chat y escribe $prompt")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Acciones globales rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onExecuteCommand("proyectos en antigravity") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Refrescar", fontSize = 12.sp)
                }

                Button(
                    onClick = { onExecuteCommand("abre antigravity") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Enfocar App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

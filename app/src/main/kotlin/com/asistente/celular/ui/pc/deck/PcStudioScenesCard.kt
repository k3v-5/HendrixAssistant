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
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.scene.PcStudioScene
import com.asistente.celular.nlu.pc.scene.PcStudioSceneRegistry
import kotlinx.coroutines.launch

@Composable
fun PcStudioScenesCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var runningSceneId by remember { mutableStateOf<String?>(null) }
    var lastResultMessage by remember { mutableStateOf<String?>(null) }

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
                            .size(32.dp)
                            .background(NeonPurple.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Escenas de Estudio & Macros",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Automatizaciones encadenadas multi-paso",
                            fontSize = 11.sp,
                            color = NeonLilac
                        )
                    }
                }
            }

            // Mensaje de último resultado si existe
            if (!lastResultMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastResultMessage ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFC4B5FD),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de escenas preconfiguradas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PcStudioSceneRegistry.ALL_SCENES.forEach { scene ->
                    val isRunning = runningSceneId == scene.id
                    StudioSceneItem(
                        scene = scene,
                        isRunning = isRunning,
                        onExecute = {
                            if (runningSceneId == null) {
                                runningSceneId = scene.id
                                scope.launch {
                                    try {
                                        val result = pcBridge.executeStudioScene(scene.id)
                                        lastResultMessage = if (result.success) {
                                            "${result.message} (${result.stepsExecuted}/${result.totalSteps} pasos)"
                                        } else {
                                            result.message
                                        }
                                        onShowSnackbar(lastResultMessage ?: "")
                                    } catch (e: Exception) {
                                        lastResultMessage = "Error: ${e.message}"
                                        onShowSnackbar("Error ejecutando escena: ${e.message}")
                                    } finally {
                                        runningSceneId = null
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioSceneItem(
    scene: PcStudioScene,
    isRunning: Boolean,
    onExecute: () -> Unit
) {
    Surface(
        color = VoidSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(scene.accentColorHex).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val sceneIcon = when (scene.id) {
                    PcStudioSceneRegistry.SCENE_MUSIC_PRODUCTION -> Icons.Default.MusicNote
                    PcStudioSceneRegistry.SCENE_RENDER_NIGHT -> Icons.Default.NightsStay
                    PcStudioSceneRegistry.SCENE_CLOSE -> Icons.Default.PowerSettingsNew
                    PcStudioSceneRegistry.SCENE_STREAMING -> Icons.Default.Videocam
                    else -> Icons.Default.PlayCircle
                }
                Icon(
                    imageVector = sceneIcon,
                    contentDescription = null,
                    tint = Color(scene.accentColorHex),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scene.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = scene.description,
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LinearScale,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${scene.steps.size} pasos encadenados",
                        fontSize = 9.sp,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onExecute,
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Ejecutar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.asistente.celular.data.JsonAutomatedRoutineRepository
import com.asistente.celular.nlu.automation.AutomatedRoutine
import com.asistente.celular.nlu.automation.AutomatedRoutineAction
import com.asistente.celular.nlu.automation.AutomatedRoutineTrigger
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import kotlinx.coroutines.launch

@Composable
fun PcAutomatedRoutinesCard(
    pcBridge: PcWorkspaceBridge,
    routineRepository: JsonAutomatedRoutineRepository,
    modifier: Modifier = Modifier,
    onOpenDesigner: (() -> Unit)? = null,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val routines by routineRepository.routines.collectAsState()
    var executingRoutineId by remember { mutableStateOf<String?>(null) }

    fun executeRoutineNow(routine: AutomatedRoutine) {
        executingRoutineId = routine.id
        scope.launch {
            try {
                val count = routineRepository.engine.executeRoutine(routine) { action ->
                    when (action) {
                        is AutomatedRoutineAction.PcQuickCommandAction -> {
                            if (action.command == "wake_on_lan") {
                                pcBridge.wakeOnLan()
                            } else if (action.command == "unlock") {
                                pcBridge.unlockSession("")
                            } else {
                                pcBridge.executeQuickCommand(action.command)
                            }
                        }
                        is AutomatedRoutineAction.PcStudioSceneAction -> {
                            pcBridge.executeStudioScene(action.sceneId).success
                        }
                        is AutomatedRoutineAction.PcPluginAction -> {
                            pcBridge.executeCustomPluginAction(action.pluginId, action.actionId, action.params).success
                        }
                        is AutomatedRoutineAction.AssistantCommandAction -> {
                            pcBridge.executeQuickCommand(action.commandText)
                        }
                        is AutomatedRoutineAction.SpeakTtsAction -> {
                            onShowSnackbar("🗣️ ${action.text}")
                            true
                        }
                        else -> true
                    }
                }
                onShowSnackbar("⚡ Rutina '${routine.name}' ejecutada ($count acciones)")
            } catch (e: Exception) {
                onShowSnackbar("Error ejecutando rutina: ${e.localizedMessage}")
            } finally {
                executingRoutineId = null
            }
        }
    }

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
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
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⚡", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Automatizaciones 'Zero-Touch'",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${routines.count { it.isEnabled }} activas • Disparadas por presencia y PC",
                            fontSize = 11.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenDesigner != null) {
                        Button(
                            onClick = onOpenDesigner,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text("🎨 Diseñar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    // Botón para probar alerta proactiva
                    Button(
                        onClick = {
                            scope.launch {
                                pcBridge.triggerTestAlert("GPU_OVERHEAT")
                                onShowSnackbar("🔔 Alerta proactiva simulada emitida")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("⚠️ Alerta", fontSize = 10.sp, color = Color(0xFFFBBF24))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                routines.forEach { routine ->
                    RoutineItemRow(
                        routine = routine,
                        isExecuting = executingRoutineId == routine.id,
                        onToggle = {
                            scope.launch {
                                routineRepository.toggleRoutine(routine.id)
                            }
                        },
                        onExecuteNow = { executeRoutineNow(routine) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineItemRow(
    routine: AutomatedRoutine,
    isExecuting: Boolean,
    onToggle: () -> Unit,
    onExecuteNow: () -> Unit
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (routine.isEnabled) Color(0xFF10B981).copy(alpha = 0.3f) else Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = routine.iconEmoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = routine.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = routine.description,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Switch(
                    checked = routine.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF0F172A)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pills de disparadores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                routine.triggers.take(2).forEach { trigger ->
                    val (label, color) = when (trigger) {
                        is AutomatedRoutineTrigger.WifiSsidTrigger -> "📶 Wi-Fi: ${trigger.ssid}" to Color(0xFF3B82F6)
                        is AutomatedRoutineTrigger.GeofenceTrigger -> "📍 ${trigger.zoneName}" to Color(0xFF8B5CF6)
                        is AutomatedRoutineTrigger.PcEventTrigger -> "🌙 PC: ${trigger.eventType}" to Color(0xFFF59E0B)
                        is AutomatedRoutineTrigger.ChargingTrigger -> "⚡ Cargador" to Color(0xFF10B981)
                        is AutomatedRoutineTrigger.ScheduleTrigger -> "⏰ ${trigger.timeString}" to Color(0xFFEC4899)
                        is AutomatedRoutineTrigger.VoicePhraseTrigger -> "🗣️ '${trigger.phrases.firstOrNull() ?: ""}'" to Color(0xFF06B6D4)
                    }
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onExecuteNow() },
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("⚡ Probar", fontSize = 10.sp, color = Color(0xFF34D399))
                }
            }
        }
    }
}

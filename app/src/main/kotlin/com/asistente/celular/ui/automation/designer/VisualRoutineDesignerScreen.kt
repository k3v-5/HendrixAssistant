package com.asistente.celular.ui.automation.designer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.automation.AutomatedRoutine
import com.asistente.celular.nlu.automation.AutomatedRoutineAction
import com.asistente.celular.nlu.automation.AutomatedRoutineRepository
import com.asistente.celular.nlu.automation.AutomatedRoutineTrigger
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualRoutineDesignerScreen(
    routineRepository: AutomatedRoutineRepository,
    pcBridge: PcWorkspaceBridge? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val routines by routineRepository.routines.collectAsState()
    var selectedRoutineId by remember { mutableStateOf<String?>(null) }

    // Estado del editor actual
    var routineName by remember { mutableStateOf("Mi Rutina Creativa") }
    var routineIcon by remember { mutableStateOf("⚡") }
    var routineTriggers by remember { mutableStateOf<List<AutomatedRoutineTrigger>>(emptyList()) }
    var routineActions by remember { mutableStateOf<List<AutomatedRoutineAction>>(emptyList()) }

    // Estado del simulador en vivo
    var isSimulating by remember { mutableStateOf(false) }
    var activeSimulatingStep by remember { mutableIntStateOf(-1) }
    var simulationLogs by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(routines) {
        if (selectedRoutineId == null && routines.isNotEmpty()) {
            val first = routines.first()
            selectedRoutineId = first.id
            routineName = first.name
            routineIcon = first.iconEmoji
            routineTriggers = first.triggers
            routineActions = first.actions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diseñador Visual de Rutinas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val routineToSave = AutomatedRoutine(
                                    id = selectedRoutineId ?: UUID.randomUUID().toString(),
                                    name = routineName,
                                    iconEmoji = routineIcon,
                                    isEnabled = true,
                                    triggers = routineTriggers,
                                    actions = routineActions
                                )
                                routineRepository.saveRoutine(routineToSave)
                                onShowSnackbar("Rutina guardada con éxito")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar", tint = Color(0xFF10B981))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF090D16),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Selector de rutinas + botón de nueva rutina
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        selectedRoutineId = UUID.randomUUID().toString()
                        routineName = "Nueva Rutina"
                        routineIcon = "⚡"
                        routineTriggers = listOf(AutomatedRoutineTrigger.PcEventTrigger("RENDER_COMPLETED"))
                        routineActions = listOf(
                            AutomatedRoutineAction.SpeakTtsAction("Render finalizado en la PC."),
                            AutomatedRoutineAction.DelayAction(1000L)
                        )
                        simulationLogs = emptyList()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                routines.forEach { r ->
                    val isSelected = r.id == selectedRoutineId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedRoutineId = r.id
                            routineName = r.name
                            routineIcon = r.iconEmoji
                            routineTriggers = r.triggers
                            routineActions = r.actions
                            simulationLogs = emptyList()
                        },
                        label = {
                            Text("${r.iconEmoji} ${r.name}", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.35f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Black.copy(alpha = 0.3f),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info básica de la rutina
                item {
                    Surface(
                        color = Color(0xFF131D31),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = routineIcon,
                                    onValueChange = { routineIcon = it.take(2) },
                                    label = { Text("Icono", fontSize = 11.sp) },
                                    modifier = Modifier.width(70.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color.DarkGray
                                    )
                                )
                                OutlinedTextField(
                                    value = routineName,
                                    onValueChange = { routineName = it },
                                    label = { Text("Nombre de la Rutina", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }
                }

                // Disparadores (Triggers)
                item {
                    Text(
                        text = "1. DISPARADOR (TRIGGER)",
                        color = Color(0xFF818CF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Surface(
                        color = Color(0xFF131D31),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (routineTriggers.isEmpty()) {
                                Text("Sin disparador asignado", color = Color.Gray, fontSize = 12.sp)
                            } else {
                                routineTriggers.forEach { trigger ->
                                    val triggerText = when (trigger) {
                                        is AutomatedRoutineTrigger.PcEventTrigger -> "🖥️ Evento PC: ${trigger.eventType}"
                                        is AutomatedRoutineTrigger.WifiSsidTrigger -> "📶 Wi-Fi: ${trigger.ssid}"
                                        is AutomatedRoutineTrigger.GeofenceTrigger -> "📍 Geocerca: ${trigger.zoneName}"
                                        is AutomatedRoutineTrigger.ChargingTrigger -> "⚡ Al conectar cargador"
                                        is AutomatedRoutineTrigger.ScheduleTrigger -> "⏰ Horario: ${trigger.timeString}"
                                        is AutomatedRoutineTrigger.VoicePhraseTrigger -> "🗣️ Frase de voz: ${trigger.phrases.firstOrNull() ?: ""}"
                                    }
                                    Text(triggerText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Pipeline de Acciones Encadenadas
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. ACCIONES ENCADENADAS (${routineActions.size})",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        IconButton(
                            onClick = {
                                routineActions = routineActions + AutomatedRoutineAction.SpeakTtsAction("Nueva acción")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir paso", tint = Color(0xFF34D399))
                        }
                    }
                }

                itemsIndexed(routineActions) { index, action ->
                    val isExecutingThis = isSimulating && activeSimulatingStep == index
                    val stepBorderColor by animateColorAsState(
                        if (isExecutingThis) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.1f)
                    )

                    Surface(
                        color = if (isExecutingThis) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF131D31),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (isExecutingThis) 2.dp else 1.dp, stepBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                when (action) {
                                    is AutomatedRoutineAction.SpeakTtsAction -> {
                                        Text("🗣️ Hablar TTS", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("\"${action.text}\"", color = Color.White, fontSize = 12.sp)
                                    }
                                    is AutomatedRoutineAction.PcQuickCommandAction -> {
                                        Text("💻 Comando PC", color = Color(0xFFA78BFA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Acción: ${action.command}", color = Color.White, fontSize = 12.sp)
                                    }
                                    is AutomatedRoutineAction.PcStudioSceneAction -> {
                                        Text("🎬 Escena de Estudio", color = Color(0xFFF472B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Escena: ${action.sceneId}", color = Color.White, fontSize = 12.sp)
                                    }
                                    is AutomatedRoutineAction.DelayAction -> {
                                        Text("⏳ Pausa / Delay", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Esperar ${action.delayMillis} ms", color = Color.White, fontSize = 12.sp)
                                    }
                                    is AutomatedRoutineAction.PcPluginAction -> {
                                        Text("🔌 Plugin de Usuario", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("${action.pluginId} -> ${action.actionId}", color = Color.White, fontSize = 12.sp)
                                    }
                                    is AutomatedRoutineAction.AssistantCommandAction -> {
                                        Text("🤖 Comando Asistente", color = Color(0xFFE879F9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(action.commandText, color = Color.White, fontSize = 12.sp)
                                    }
                                    is AutomatedRoutineAction.EnterDeskStandbyAction -> {
                                        Text("🖥️ Modo Desk Standby", color = Color(0xFF67E8F9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Activar pantalla inteligente HUD", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    val list = routineActions.toMutableList()
                                    list.removeAt(index)
                                    routineActions = list
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Sección del Simulador
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (isSimulating) return@Button
                            scope.launch {
                                isSimulating = true
                                simulationLogs = listOf("▶️ Iniciando simulación paso a paso de '${routineName}'...")
                                for (i in routineActions.indices) {
                                    activeSimulatingStep = i
                                    val action = routineActions[i]
                                    val desc = when (action) {
                                        is AutomatedRoutineAction.SpeakTtsAction -> "Hablando: '${action.text}'"
                                        is AutomatedRoutineAction.PcQuickCommandAction -> "Ejecutando comando PC: '${action.command}'"
                                        is AutomatedRoutineAction.PcStudioSceneAction -> "Disparando escena: '${action.sceneId}'"
                                        is AutomatedRoutineAction.DelayAction -> "Esperando ${action.delayMillis} ms..."
                                        is AutomatedRoutineAction.PcPluginAction -> "Invocando plugin: '${action.pluginId}'"
                                        is AutomatedRoutineAction.AssistantCommandAction -> "Ejecutando comando: '${action.commandText}'"
                                        is AutomatedRoutineAction.EnterDeskStandbyAction -> "Activando modo pantalla de escritorio (Desk Standby)"
                                    }
                                    simulationLogs = simulationLogs + "Paso ${i + 1}: $desc"
                                    delay(if (action is AutomatedRoutineAction.DelayAction) action.delayMillis else 800L)
                                }
                                activeSimulatingStep = -1
                                simulationLogs = simulationLogs + "✅ Simulación completada con 100% de éxito."
                                isSimulating = false
                                onShowSnackbar("Simulación de rutina completada con éxito.")
                            }
                        },
                        enabled = !isSimulating && routineActions.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isSimulating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulando Paso ${activeSimulatingStep + 1}...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simular Rutina Paso a Paso", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Consola de Logs de Simulación
                if (simulationLogs.isNotEmpty()) {
                    item {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "CONSOLA DE SIMULACIÓN",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                simulationLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        color = if (log.startsWith("✅")) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

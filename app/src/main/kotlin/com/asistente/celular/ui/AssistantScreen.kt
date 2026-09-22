package com.asistente.celular.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.evaluator.InteractionEntry
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.ui.AssistantUiPayload
import com.asistente.celular.ui.cards.AssistantInteractiveCard
import com.asistente.celular.ui.dashboard.DailyBriefingHub
import com.asistente.celular.viewmodel.AssistantUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    tasks: List<TaskItem> = emptyList(),
    notes: List<NoteItem> = emptyList(),
    smartDevices: List<SmartDevice> = emptyList(),
    onToggleSmartDevice: (SmartDevice) -> Unit = {},
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendCommand: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearError: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.interactions.size) {
        if (state.interactions.isNotEmpty()) {
            listState.animateScrollToItem(state.interactions.size - 1)
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            kotlinx.coroutines.delay(4000)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Asistente Offline-First", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (state.isConnected) "Enrutador IA Activo (${state.llmConfig.provider.displayName})" else "100% Offline (Motor Local)",
                            fontSize = 12.sp,
                            color = if (state.isConnected) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Mensaje de error visible si algo falla con botón de cierre
                    AnimatedVisibility(visible = !state.isListening && state.error != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onClearError,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Indicador activo de escucha
                    AnimatedVisibility(visible = state.isListening) {
                        Text(
                            text = if (state.partialText.isNotBlank()) "Escuchando: \"${state.partialText}\"" else "🎙️ Escuchando… Habla ahora",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Escribe una orden o pregunta…") },
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            trailingIcon = {
                                if (inputText.isNotBlank()) {
                                    IconButton(onClick = {
                                        onSendCommand(inputText)
                                        inputText = ""
                                    }) {
                                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón de Micrófono / Hablar
                        VoiceActionButton(
                            isListening = state.isListening,
                            isSpeaking = state.isSpeaking,
                            isProcessing = state.isProcessing,
                            onStartListening = onStartListening,
                            onStopListening = onStopListening,
                            onStopSpeech = onStopSpeech
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.interactions.isEmpty()) {
                DailyBriefingHub(
                    llmConfig = state.llmConfig,
                    tasks = tasks,
                    notes = notes,
                    smartDevices = smartDevices,
                    onSendCommand = onSendCommand,
                    onToggleSmartDevice = onToggleSmartDevice,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.interactions, key = { it.id }) { interaction ->
                        InteractionCard(
                            interaction = interaction,
                            onSendCommand = onSendCommand
                        )
                    }
                }
            }

            // Indicador de estado animado (Orb / ondas)
            AnimatedVisibility(visible = state.isListening || state.isProcessing || state.isSpeaking) {
                AssistantStatusOrb(
                    isListening = state.isListening,
                    isProcessing = state.isProcessing,
                    isSpeaking = state.isSpeaking
                )
            }
        }
    }
}

@Composable
fun VoiceActionButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeech: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    FloatingActionButton(
        onClick = {
            when {
                isSpeaking -> onStopSpeech()
                isListening -> onStopListening()
                else -> onStartListening()
            }
        },
        containerColor = when {
            isSpeaking -> MaterialTheme.colorScheme.tertiary
            isListening -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        modifier = Modifier.scale(if (isListening) scale else 1.0f)
    ) {
        Icon(
            imageVector = when {
                isSpeaking -> Icons.Default.Stop
                isListening -> Icons.Default.MicOff
                else -> Icons.Default.Mic
            },
            contentDescription = "Micrófono"
        )
    }
}

@Composable
fun InteractionCard(
    interaction: InteractionEntry,
    onSendCommand: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Burbuja de usuario
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = interaction.userInput,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tarjeta de respuesta del Asistente
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Badge del motor utilizado
                    if (interaction.output.handledByAi) {
                        val harnessData = interaction.output.payload as? com.asistente.celular.ai.harness.HarnessResult
                        val badgeLabel = harnessData?.let { "✨ ${it.usedModel.displayName}" } ?: "✨ Inteligencia Artificial"

                        Column {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF7C4DFF).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = Color(0xFF7C4DFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C4DFF)
                                    )
                                }
                            }
                            if (harnessData != null) {
                                Text(
                                    text = harnessData.decision.reason,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Motor Local: ${interaction.skillName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = interaction.output.displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Renderizado condicional y expandible de controles interactivos (evita scroll masivo)
                val uiPayload = interaction.output.payload as? AssistantUiPayload
                if (uiPayload != null) {
                    var isExpanded by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "🔼 Ocultar controles interactivos" else "🔽 Ver controles interactivos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            AssistantInteractiveCard(
                                payload = uiPayload,
                                onExecuteCommand = onSendCommand,
                                onRequestBrightnessPermission = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyAssistantView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Asistente Hendrix",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Di \"Oye Hendrix\" o pulsa el micrófono para hablar.\n\nPrueba comandos locales como:\n• \"Enciende la linterna\"\n• \"Temporizador de 5 minutos\"\n• \"Alarma a las 7 de la mañana\"\n• \"Abre WhatsApp\"\n• \"Qué hora es\"\n\nO preguntas abiertas a la IA:\n• \"Explícame cómo funciona un satélite\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AssistantStatusOrb(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean
) {
    val statusText = when {
        isSpeaking -> "Sintetizando voz…"
        isProcessing -> "Analizando intención…"
        isListening -> "Escuchando…"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

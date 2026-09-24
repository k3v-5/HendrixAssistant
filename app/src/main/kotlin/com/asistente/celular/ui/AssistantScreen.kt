package com.asistente.celular.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.evaluator.InteractionEntry
import com.asistente.celular.nlu.notes.NoteItem
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.tasks.TaskItem
import com.asistente.celular.nlu.ui.AssistantUiPayload
import com.asistente.celular.ui.cards.AssistantInteractiveCard
import com.asistente.celular.ui.components.oled.OledCard
import com.asistente.celular.ui.components.oled.OledVoiceOrb
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import com.asistente.celular.viewmodel.AssistantUiState

/**
 * Pantalla principal de Hendrix: Chat conversacional puro, limpio y minimalista.
 *
 * Características UX de alto impacto:
 * - Toda la vista está dedicada exclusivamente al hilo de conversación (estilo AI Chat premium).
 * - Cero saturación visual: se removieron elementos sobrecargados (orbe gigante de 230dp, barras de telemetría y botones duplicados).
 * - Estado inicial acogedor y minimalista con micro-orbe de 72dp y chips de consulta rápida.
 * - Burbujas de mensaje modernas con avatar sutil e integración fluida de tarjetas interactivas.
 * - Barra de entrada ergonómica con botón de dictado por voz y envío rápido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    isPcConnected: Boolean = false,
    pcHostname: String? = null,
    pcTelemetry: PcSystemTelemetry? = null,
    onOpenPcModules: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenStandby: () -> Unit = {},
    tasks: List<TaskItem> = emptyList(),
    notes: List<NoteItem> = emptyList(),
    smartDevices: List<SmartDevice> = emptyList(),
    onToggleSmartDevice: (SmartDevice) -> Unit = {},
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSendCommand: (String) -> Unit,
    onStopSpeech: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearChat: (() -> Unit)? = null,
    onClearError: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

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
        containerColor = VoidBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (state.isConnected) NeonPurple else NeonGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "HENDRIX",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (state.isConnected) "ONLINE // ${state.llmConfig.provider.displayName.uppercase()}" else "LOCAL // OFFLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (state.isConnected) NeonPurple else NeonGreen
                            )
                        }
                    }
                },
                actions = {
                    // Botón para reiniciar/limpiar chat y regresar al orbe gigante
                    if (state.interactions.isNotEmpty() && onClearChat != null) {
                        IconButton(onClick = onClearChat) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reiniciar conversación",
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Píldora discreta de estado de PC
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VoidSurfaceElevated,
                        border = BorderStroke(1.dp, if (isPcConnected) NeonCyan.copy(alpha = 0.4f) else VoidBorder),
                        modifier = Modifier.clickable { onOpenPcModules() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isPcConnected) NeonCyan else NeonRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPcConnected) (pcHostname?.take(10) ?: "PC ON") else "PC OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isPcConnected) NeonCyan else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VoidBlack)
            )
        },
        bottomBar = {
            Surface(
                color = VoidBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, VoidBorder))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    // Mensaje de error discreto si ocurre un fallo
                    AnimatedVisibility(visible = !state.isListening && state.error != null) {
                        Surface(
                            color = VoidSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.error ?: "",
                                    color = NeonRed,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onClearError,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar",
                                        tint = NeonRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Indicador activo de escucha por voz
                    AnimatedVisibility(visible = state.isListening) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeonCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.partialText.isNotBlank()) "\"${state.partialText}\"" else "ESCUCHANDO...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    // Fila de Entrada de Chat & Botón de Voz
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Escribe una orden o pregunta...",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = VoidSurfaceElevated,
                                unfocusedContainerColor = VoidSurface,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = VoidBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(18.dp),
                            trailingIcon = {
                                if (inputText.isNotBlank()) {
                                    IconButton(onClick = {
                                        onSendCommand(inputText)
                                        inputText = ""
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Enviar",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Botón de Voz Ergonómico
                        OledVoiceButton(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack)
                .padding(padding)
        ) {
            if (state.interactions.isEmpty()) {
                // Estado Inicial Minimalista (Puro Chat Elegante)
                OledChatEmptyState(
                    isListening = state.isListening,
                    isProcessing = state.isProcessing,
                    isSpeaking = state.isSpeaking,
                    onOrbClick = {
                        when {
                            state.isSpeaking -> onStopSpeech()
                            state.isListening -> onStopListening()
                            else -> onStartListening()
                        }
                    },
                    onPromptClick = onSendCommand,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Hilo de Chat Conversacional Fluido
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.interactions, key = { it.id }) { interaction ->
                        OledInteractionCard(
                            interaction = interaction,
                            onSendCommand = onSendCommand
                        )
                    }

                    // Estado activo de procesamiento / escucha al pie del chat
                    if (state.isListening || state.isProcessing || state.isSpeaking) {
                        item {
                            OledListStatusIndicator(
                                isListening = state.isListening,
                                isProcessing = state.isProcessing,
                                isSpeaking = state.isSpeaking,
                                onOrbClick = {
                                    when {
                                        state.isSpeaking -> onStopSpeech()
                                        state.isListening -> onStopListening()
                                        else -> onStartListening()
                                    }
                                }
                            )
                        }
                    }
                }

                // Botón flotante ergonómico para desplazar al final (Scroll to Bottom)
                val showScrollToBottom by remember {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        state.interactions.size > 2 && lastVisible < state.interactions.size - 1
                    }
                }

                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                ) {
                    Surface(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            coroutineScope.launch {
                                listState.animateScrollToItem(state.interactions.size - 1)
                            }
                        },
                        shape = CircleShape,
                        color = VoidSurfaceElevated.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.7f)),
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Desplazar al final",
                                tint = NeonPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Estado inicial minimalista y limpio cuando aún no hay mensajes en el chat.
 */
@Composable
fun OledChatEmptyState(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    onOrbClick: () -> Unit,
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Orbe líquido bioluminiscente de 220dp
        OledVoiceOrb(
            isListening = isListening,
            isProcessing = isProcessing,
            isSpeaking = isSpeaking,
            onClick = onOrbClick,
            size = 220.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HENDRIX",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "¿En qué te puedo ayudar hoy?",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Chips de sugerencia rápida en carrusel horizontal ergonómico
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OledSuggestionChip(
                text = "Tareas de hoy",
                icon = Icons.Default.Assignment,
                onClick = { onPromptClick("¿Qué tareas tengo hoy?") }
            )
            OledSuggestionChip(
                text = "Captura de PC",
                icon = Icons.Default.DesktopWindows,
                onClick = { onPromptClick("Captura de pantalla de la computadora") }
            )
            OledSuggestionChip(
                text = "Explicar concepto",
                icon = Icons.Default.Lightbulb,
                onClick = { onPromptClick("Explica en un párrafo cómo funciona la inteligencia artificial") }
            )
        }
    }
}

@Composable
fun OledSuggestionChip(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = VoidSurface,
        border = BorderStroke(1.dp, VoidBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonLilac,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

/**
 * Botón de voz con micro-animación háptica y aro en cian neón.
 */
@Composable
fun OledVoiceButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeech: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseVoice")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = Modifier
            .size(50.dp)
            .scale(if (isListening) scale else 1.0f)
            .clip(CircleShape)
            .border(
                BorderStroke(
                    1.5.dp,
                    when {
                        isSpeaking -> NeonAmber
                        isListening -> NeonRed
                        else -> NeonCyan
                    }
                ),
                CircleShape
            )
            .clickable {
                when {
                    isSpeaking -> onStopSpeech()
                    isListening -> onStopListening()
                    else -> onStartListening()
                }
            },
        color = VoidSurfaceElevated,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when {
                    isSpeaking -> Icons.Default.Stop
                    isListening -> Icons.Default.MicOff
                    else -> Icons.Default.Mic
                },
                contentDescription = "Micrófono",
                tint = when {
                    isSpeaking -> NeonAmber
                    isListening -> NeonRed
                    else -> NeonCyan
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Tarjeta de interacción en el historial con estética conversacional OLED Void moderna.
 */
@Composable
fun OledInteractionCard(
    interaction: InteractionEntry,
    onSendCommand: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Mensaje del Usuario (Burbuja alineada a la derecha)
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .clip(RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp))
                .background(VoidSurfaceElevated)
                .border(BorderStroke(1.dp, VoidBorder), RoundedCornerShape(16.dp, 16.dp, 3.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = interaction.userInput,
                fontSize = 14.sp,
                color = TextPrimary
            )
        }

        // 2. Respuesta de Hendrix (Alineada a la izquierda con avatar sutil y formato limpio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Mini avatar Hendrix
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
            }

            // Cuerpo del mensaje
            OledCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(3.dp, 16.dp, 16.dp, 16.dp),
                backgroundColor = VoidSurface,
                borderColor = VoidBorder
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Badge del motor utilizado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (interaction.output.handledByAi) {
                            val harnessData = interaction.output.payload as? com.asistente.celular.ai.harness.HarnessResult
                            val badgeLabel = harnessData?.let { "AI // ${it.usedModel.displayName.uppercase()}" } ?: "AI ENGINE"

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonCyan
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonGreen.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LOCAL // ${interaction.skillName.uppercase()}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = interaction.output.displayText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    )

                    // Renderizado de controles interactivos expandibles
                    val uiPayload = interaction.output.payload as? AssistantUiPayload
                    if (uiPayload != null) {
                        var isExpanded by remember { mutableStateOf(false) }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoidSurfaceElevated,
                            border = BorderStroke(1.dp, VoidBorder),
                            modifier = Modifier.clickable { isExpanded = !isExpanded }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "[-] OCULTAR CONTROLES" else "[+] VER CONTROLES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
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
}

/**
 * Indicador animado compacto cuando Hendrix escucha o sintetiza dentro del historial de chat.
 */
@Composable
fun OledListStatusIndicator(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    onOrbClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OledVoiceOrb(
            isListening = isListening,
            isProcessing = isProcessing,
            isSpeaking = isSpeaking,
            onClick = onOrbClick,
            size = 48.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = when {
                isListening -> "ESCUCHANDO..."
                isProcessing -> "PROCESANDO..."
                isSpeaking -> "HABLANDO..."
                else -> ""
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = NeonCyan
        )
    }
}

/**
 * Composable de compatibilidad utilizado por dialogs y overlays.
 */
@Composable
fun AssistantStatusOrb(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean
) {
    OledListStatusIndicator(
        isListening = isListening,
        isProcessing = isProcessing,
        isSpeaking = isSpeaking,
        onOrbClick = {}
    )
}

/**
 * Composable de compatibilidad backward para llamadas anteriores a OledHeroAssistantView.
 */
@Composable
fun OledHeroAssistantView(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    isPcConnected: Boolean,
    pcTelemetry: PcSystemTelemetry?,
    onOrbClick: () -> Unit,
    onOpenPcModules: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenStandby: () -> Unit,
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OledChatEmptyState(
        isListening = isListening,
        isProcessing = isProcessing,
        isSpeaking = isSpeaking,
        onOrbClick = onOrbClick,
        onPromptClick = onPromptClick,
        modifier = modifier
    )
}

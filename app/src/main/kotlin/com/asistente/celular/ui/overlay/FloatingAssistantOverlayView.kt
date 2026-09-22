package com.asistente.celular.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Estado visual del Mini HUD flotante sobre la pantalla.
 */
enum class OverlayState {
    LISTENING,
    PROCESSING,
    SPEAKING,
    RESULT
}

@Composable
fun FloatingAssistantOverlayView(
    state: OverlayState,
    transcriptionText: String,
    responseText: String,
    onDismiss: () -> Unit,
    onExpandToApp: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    onSendToPc: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier
            .width(320.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra superior con manija de arrastre y botones de control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    OverlayState.LISTENING -> Color(0xFF00E676)
                                    OverlayState.PROCESSING -> Color(0xFFFFB300)
                                    OverlayState.SPEAKING -> Color(0xFF00B0FF)
                                    OverlayState.RESULT -> Color(0xFF7C4DFF)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (state) {
                            OverlayState.LISTENING -> "Escuchando..."
                            OverlayState.PROCESSING -> "Pensando..."
                            OverlayState.SPEAKING -> "Hendrix Responde"
                            OverlayState.RESULT -> "Completado"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onExpandToApp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInFull,
                            contentDescription = "Pantalla completa",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini Orb Reactivo
            PulsingMiniOrb(state = state)

            Spacer(modifier = Modifier.height(10.dp))

            // Texto de lo que el usuario está diciendo en tiempo real
            if (transcriptionText.isNotBlank()) {
                Text(
                    text = "\"$transcriptionText\"",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tarjeta de Respuesta o Resultado
            AnimatedVisibility(
                visible = responseText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = responseText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de chips de acción rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onSendToPc,
                    label = { Text("Enviar a PC", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
                AssistChip(
                    onClick = onDismiss,
                    label = { Text("Descartar", fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
fun PulsingMiniOrb(state: OverlayState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val gradient = when (state) {
        OverlayState.LISTENING -> Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF1DE9B6), Color(0x0000B0FF)))
        OverlayState.PROCESSING -> Brush.radialGradient(listOf(Color(0xFFFFD700), Color(0xFFFF9100), Color(0x00FF6D00)))
        OverlayState.SPEAKING -> Brush.radialGradient(listOf(Color(0xFF7C4DFF), Color(0xFF651FFF), Color(0x006200EA)))
        OverlayState.RESULT -> Brush.radialGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF), Color(0x0000E5FF)))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(54.dp)
            .scale(if (state == OverlayState.LISTENING || state == OverlayState.SPEAKING) scale else 1.0f)
            .clip(CircleShape)
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
        )
    }
}

package com.asistente.celular.ui.pc

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Tira táctil lateral de scroll virtual (Virtual Scroll Wheel Strip) para el visor de PC.
 * Permite desplazar contenido verticalmente en la computadora usando el pulgar con una sola mano.
 * Proporciona:
 * - Botones de paso rápido (Scroll ▲ / Scroll ▼).
 * - Pista táctil continua sensible al deslizamiento con retroalimentación háptica sutil.
 * - Diseño compacto OLED Void / Morado Neón con capacidad de colapso rápido.
 */
@Composable
fun PcVirtualScrollStrip(
    onScroll: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isExpanded by remember { mutableStateOf(true) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var isDraggingTrack by remember { mutableStateOf(false) }

    val trackColor = Color(0xFF140D24).copy(alpha = 0.92f)
    val accentColor = Color(0xFFA855F7)
    val cyanColor = Color(0xFF00F5FF)

    if (!isExpanded) {
        // Píldora colapsada discreta al borde
        Surface(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                isExpanded = true
            },
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            color = trackColor,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Expandir rueda de scroll",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = trackColor,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
            shadowElevation = 8.dp,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Botón Scroll Arriba (▲)
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onScroll(5.0f)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll Arriba",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pista táctil continua (Trackpad Scroll Strip)
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDraggingTrack) accentColor.copy(alpha = 0.25f) else Color(0xFF1E1433))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDraggingTrack = true
                                    accumulatedDrag = 0f
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                },
                                onDragEnd = {
                                    isDraggingTrack = false
                                    accumulatedDrag = 0f
                                },
                                onDragCancel = {
                                    isDraggingTrack = false
                                    accumulatedDrag = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount
                                    val threshold = 14f // Cada 14 píxeles de arrastre emite un pulso de scroll
                                    if (abs(accumulatedDrag) >= threshold) {
                                        val steps = (accumulatedDrag / threshold).toInt()
                                        accumulatedDrag -= steps * threshold
                                        val scrollDelta = -steps.toFloat() * 1.5f
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        onScroll(scrollDelta)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Ranuras táctiles visuales
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .width(if (index == 2) 18.dp else 12.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        if (isDraggingTrack) cyanColor.copy(alpha = 0.85f)
                                        else accentColor.copy(alpha = 0.55f)
                                    )
                            )
                        }
                    }
                }

                // Botón Scroll Abajo (▼)
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onScroll(-5.0f)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll Abajo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Píldora de ocultar / colapsar
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            isExpanded = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

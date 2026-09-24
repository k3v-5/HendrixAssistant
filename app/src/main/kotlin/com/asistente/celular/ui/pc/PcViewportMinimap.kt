package com.asistente.celular.ui.pc

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import kotlin.math.roundToInt

/**
 * Posicionador y Selector de Zonas Ultrawide con Recuadro de Viewport (PcViewportMinimap).
 * 
 * Diseñado para resolver la navegación en monitores ultra-anchos (21:9 y 32:9):
 * 1. Muestra en miniatura la pantalla completa del PC de forma panorámica.
 * 2. Muestra un "Recuadro" bioluminiscente de alta visibilidad:
 *    - Si scale > 1.05x: el recuadro representa el viewport real visible y se actualiza al desplazarse.
 *    - Si scale <= 1.05x: muestra un recuadro selector de 1/3 de monitor que el usuario puede arrastrar o pulsar
 *      para elegir exactamente dónde posicionarse y ampliar con zoom.
 * 3. Permite arrastrar el recuadro o pulsar cualquier punto para saltar de inmediato a esa posición.
 * 4. Micro-botones ergonómicos de acceso directo por tercios [ ◄ Izq | ■ Centro | Der ► ].
 * 5. La tarjeta es libremente arrastrable por la pantalla (arrastrando su cabecera) y colapsable a una píldora discreta.
 */
@Composable
fun PcViewportMinimap(
    viewportBounds: ViewportRatioBounds,
    aspectRatio: Float,
    scale: Float,
    onPanToRatio: (targetRatioX: Float, targetRatioY: Float) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true
) {
    val view = LocalView.current
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var cardDragOffset by remember { mutableStateOf(Offset.Zero) }

    // Ratios seleccionados en modo preview (cuando scale <= 1.05x)
    var targetSelectionRatioX by remember { mutableFloatStateOf(0.5f) }
    var targetSelectionRatioY by remember { mutableFloatStateOf(0.5f) }

    val safeAspect = aspectRatio.coerceIn(1.2f, 3.8f)
    val minimapWidthDp = 136.dp
    val minimapHeightDp = (136f / safeAspect).coerceIn(42f, 72f).dp

    val baseVoidColor = VoidBlack.copy(alpha = 0.92f)
    val isZoomed = scale > 1.05f

    Box(
        modifier = modifier
            .offset { IntOffset(cardDragOffset.x.roundToInt(), cardDragOffset.y.roundToInt()) }
            .animateContentSize()
    ) {
        if (!isExpanded) {
            // Píldora compacta cuando el usuario decide minimizarla temporalmente
            Surface(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    isExpanded = true
                },
                shape = RoundedCornerShape(12.dp),
                color = baseVoidColor,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Abrir Selector de Pantalla",
                        tint = NeonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (safeAspect >= 2.0f) "Selector 21:9" else "Posicionador",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            // Tarjeta completa expandida con recuadro interactivo y selector de zonas
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = baseVoidColor,
                border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.70f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cabecera: Título + Manija de arrastre de la tarjeta + Botón de minimizar
                    Row(
                        modifier = Modifier
                            .width(minimapWidthDp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    cardDragOffset += dragAmount
                                }
                            }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (safeAspect >= 2.0f) "RECUADRO 21:9" else "POSICIONADOR",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Icono discreto de arrastrar la tarjeta
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Mover tarjeta",
                            tint = TextMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )

                        // Botón de minimizar
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    isExpanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Minimizar",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Lienzo interactivo del minimapa con EL RECUADRO de posición
                    Box(
                        modifier = Modifier
                            .width(minimapWidthDp)
                            .height(minimapHeightDp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070312))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    val ratioX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val ratioY = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    targetSelectionRatioX = ratioX
                                    targetSelectionRatioY = ratioY
                                    onPanToRatio(ratioX, ratioY)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    val ratioX = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val ratioY = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    targetSelectionRatioX = ratioX
                                    targetSelectionRatioY = ratioY
                                    onPanToRatio(ratioX, ratioY)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 1. Líneas divisorias de las 3 zonas (1/3 y 2/3) con efecto punteado
                            val zone1X = w / 3f
                            val zone2X = (2f * w) / 3f
                            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)

                            drawLine(
                                color = Color.White.copy(alpha = 0.20f),
                                start = Offset(zone1X, 0f),
                                end = Offset(zone1X, h),
                                strokeWidth = 1f,
                                pathEffect = dashedEffect
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.20f),
                                start = Offset(zone2X, 0f),
                                end = Offset(zone2X, h),
                                strokeWidth = 1f,
                                pathEffect = dashedEffect
                            )

                            // 2. Determinar posición y tamaño del RECUADRO
                            val boxLeft: Float
                            val boxTop: Float
                            val boxW: Float
                            val boxH: Float

                            if (isZoomed) {
                                // Modo Zoom activo: el recuadro refleja la porción de pantalla visible en el celular
                                boxLeft = (viewportBounds.minX * w).coerceIn(0f, w - 8f)
                                boxTop = (viewportBounds.minY * h).coerceIn(0f, h - 8f)
                                boxW = (viewportBounds.width * w).coerceIn(16f, w - boxLeft)
                                boxH = (viewportBounds.height * h).coerceIn(12f, h - boxTop)
                            } else {
                                // Modo 1.0x (Sin zoom aún): Muestra un recuadro selector de 1/3 centrado en targetSelectionRatio
                                val previewBoxW = (w / 3f).coerceAtLeast(24f)
                                val previewBoxH = h * 0.88f
                                boxLeft = (targetSelectionRatioX * w - previewBoxW / 2f).coerceIn(0f, w - previewBoxW)
                                boxTop = (targetSelectionRatioY * h - previewBoxH / 2f).coerceIn(0f, h - previewBoxH)
                                boxW = previewBoxW
                                boxH = previewBoxH
                            }

                            // 3. Relleno bioluminiscente del recuadro
                            drawRoundRect(
                                color = if (isZoomed) NeonCyan.copy(alpha = 0.25f) else NeonAmber.copy(alpha = 0.28f),
                                topLeft = Offset(boxLeft, boxTop),
                                size = Size(boxW, boxH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // 4. Borde neón brillante del recuadro (el cuadro visible para seleccionar)
                            val strokeBorderColor = if (isZoomed) NeonCyan else NeonAmber
                            drawRoundRect(
                                color = strokeBorderColor,
                                topLeft = Offset(boxLeft, boxTop),
                                size = Size(boxW, boxH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // 5. Cruz central / retícula dentro del recuadro
                            val centerX = boxLeft + boxW / 2f
                            val centerY = boxTop + boxH / 2f
                            val crosshairSize = 4.dp.toPx()

                            drawLine(
                                color = strokeBorderColor,
                                start = Offset(centerX - crosshairSize, centerY),
                                end = Offset(centerX + crosshairSize, centerY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawLine(
                                color = strokeBorderColor,
                                start = Offset(centerX, centerY - crosshairSize),
                                end = Offset(centerX, centerY + crosshairSize),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // 6. Esquinas reforzadas (Camera bracket style)
                            val cornerLen = 5.dp.toPx()
                            // Superior Izquierda
                            drawLine(strokeBorderColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), 3.dp.toPx())
                            drawLine(strokeBorderColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), 3.dp.toPx())
                            // Superior Derecha
                            drawLine(strokeBorderColor, Offset(boxLeft + boxW, boxTop), Offset(boxLeft + boxW - cornerLen, boxTop), 3.dp.toPx())
                            drawLine(strokeBorderColor, Offset(boxLeft + boxW, boxTop), Offset(boxLeft + boxW, boxTop + cornerLen), 3.dp.toPx())
                            // Inferior Izquierda
                            drawLine(strokeBorderColor, Offset(boxLeft, boxTop + boxH), Offset(boxLeft + cornerLen, boxTop + boxH), 3.dp.toPx())
                            drawLine(strokeBorderColor, Offset(boxLeft, boxTop + boxH), Offset(boxLeft, boxTop + boxH - cornerLen), 3.dp.toPx())
                            // Inferior Derecha
                            drawLine(strokeBorderColor, Offset(boxLeft + boxW, boxTop + boxH), Offset(boxLeft + boxW - cornerLen, boxTop + boxH), 3.dp.toPx())
                            drawLine(strokeBorderColor, Offset(boxLeft + boxW, boxTop + boxH), Offset(boxLeft + boxW, boxTop + boxH - cornerLen), 3.dp.toPx())
                        }
                    }

                    // Fila inferior: Micro-botones de Salto Rápido a las 3 Zonas [ ◄ Izq | ■ Centro | Der ► ]
                    Row(
                        modifier = Modifier.width(minimapWidthDp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZoneQuickSnapButton("◄ Izq") {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            targetSelectionRatioX = 0.167f
                            targetSelectionRatioY = 0.5f
                            onPanToRatio(0.167f, 0.5f)
                        }
                        ZoneQuickSnapButton("■ Centro") {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            targetSelectionRatioX = 0.5f
                            targetSelectionRatioY = 0.5f
                            onPanToRatio(0.5f, 0.5f)
                        }
                        ZoneQuickSnapButton("Der ►") {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            targetSelectionRatioX = 0.833f
                            targetSelectionRatioY = 0.5f
                            onPanToRatio(0.833f, 0.5f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneQuickSnapButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF160B29),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.90f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

package com.asistente.celular.ui.pc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.WindowBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Organismo visual para interactuar con la pantalla de la PC mediante fotogramas Snapshot WebP.
 * Soporta zoom y paneo multitáctil, modo trackpad de precisión y Lupa de Aumento HD (3.5x Zoom Loupe).
 */
@Composable
fun PcSnapshotCanvasOrganism(
    snapshotBytes: ByteArray?,
    onSendAction: (PcInteractionAction) -> Unit,
    onRequestRefresh: () -> Unit,
    isLoupeEnabled: Boolean = false,
    isWindowFocusActive: Boolean = false,
    activeWindowBounds: WindowBounds? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Estados de transformación (Zoom y Paneo)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Coordenadas relativas del cursor virtual (0.0f a 1.0f)
    var cursorRatioX by remember { mutableFloatStateOf(0.5f) }
    var cursorRatioY by remember { mutableFloatStateOf(0.5f) }

    // Estado de la Lupa de Precisión (Zoom Loupe)
    var isLoupeActive by remember { mutableStateOf(false) }
    var loupeTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Decodificación en segundo plano del snapshot WebP
    LaunchedEffect(snapshotBytes) {
        if (snapshotBytes != null && snapshotBytes.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                try {
                    val bmp = BitmapFactory.decodeByteArray(snapshotBytes, 0, snapshotBytes.size)
                    if (bmp != null) {
                        imageBitmap = bmp.asImageBitmap()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF0F172A))
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxOffsetX = (containerSize.width * (scale - 1f)) / 2f
                    val maxOffsetY = (containerSize.height * (scale - 1f)) / 2f
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                        y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapPos ->
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        // Calcular coordenada normalizada considerando escala y paneo
                        val normX = ((tapPos.x - offset.x) / (containerSize.width * scale)).coerceIn(0f, 1f)
                        val normY = ((tapPos.y - offset.y) / (containerSize.height * scale)).coerceIn(0f, 1f)
                        cursorRatioX = normX
                        cursorRatioY = normY
                        onSendAction(
                            PcInteractionAction(
                                type = PcActionType.CLICK,
                                xRatio = normX,
                                yRatio = normY
                            )
                        )
                    },
                    onDoubleTap = { tapPos ->
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        val normX = ((tapPos.x - offset.x) / (containerSize.width * scale)).coerceIn(0f, 1f)
                        val normY = ((tapPos.y - offset.y) / (containerSize.height * scale)).coerceIn(0f, 1f)
                        onSendAction(
                            PcInteractionAction(
                                type = PcActionType.DOUBLE_CLICK,
                                xRatio = normX,
                                yRatio = normY
                            )
                        )
                    },
                    onLongPress = { tapPos ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        val normX = ((tapPos.x - offset.x) / (containerSize.width * scale)).coerceIn(0f, 1f)
                        val normY = ((tapPos.y - offset.y) / (containerSize.height * scale)).coerceIn(0f, 1f)
                        onSendAction(
                            PcInteractionAction(
                                type = PcActionType.RIGHT_CLICK,
                                xRatio = normX,
                                yRatio = normY
                            )
                        )
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startPos ->
                        isLoupeActive = true
                        loupeTouchOffset = startPos
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        loupeTouchOffset += dragAmount
                        val normX = ((loupeTouchOffset.x - offset.x) / (containerSize.width * scale)).coerceIn(0f, 1f)
                        val normY = ((loupeTouchOffset.y - offset.y) / (containerSize.height * scale)).coerceIn(0f, 1f)
                        cursorRatioX = normX
                        cursorRatioY = normY
                        onSendAction(
                            PcInteractionAction(
                                type = PcActionType.MOUSE_MOVE,
                                xRatio = normX,
                                yRatio = normY
                            )
                        )
                    },
                    onDragEnd = {
                        isLoupeActive = false
                    },
                    onDragCancel = {
                        isLoupeActive = false
                    }
                )
            }
    ) {
        val bmp = imageBitmap
        if (bmp != null) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dibujar el fotograma escalado y centrado
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                    dstSize = IntSize((size.width * scale).toInt(), (size.height * scale).toInt())
                )

                // Dibujar cursor virtual
                val cursorScreenX = (cursorRatioX * size.width * scale) + offset.x
                val cursorScreenY = (cursorRatioY * size.height * scale) + offset.y

                // Resaltado de Ventana Activa si el enfoque está habilitado
                if (isWindowFocusActive && activeWindowBounds != null) {
                    val normLeft = (activeWindowBounds.left.toFloat() / 1920f).coerceIn(0f, 1f)
                    val normTop = (activeWindowBounds.top.toFloat() / 1080f).coerceIn(0f, 1f)
                    val normW = (activeWindowBounds.width.toFloat() / 1920f).coerceIn(0.05f, 1f)
                    val normH = (activeWindowBounds.height.toFloat() / 1080f).coerceIn(0.05f, 1f)

                    val boxX = (normLeft * size.width * scale) + offset.x
                    val boxY = (normTop * size.height * scale) + offset.y
                    val boxW = normW * size.width * scale
                    val boxH = normH * size.height * scale

                    drawRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 8.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
            }
        } else {
            // Estado de espera o sin fotograma
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Conectando con la pantalla del PC...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Lupa de Precisión HD (3.5x Zoom Loupe)
        val showLoupe = (isLoupeActive || isLoupeEnabled) && bmp != null
        if (showLoupe) {
            val loupeSizeDp = 130.dp
            val loupePx = with(density) { loupeSizeDp.toPx() }
            val touchX = if (isLoupeActive) loupeTouchOffset.x else ((cursorRatioX * containerSize.width * scale) + offset.x)
            val touchY = if (isLoupeActive) loupeTouchOffset.y else ((cursorRatioY * containerSize.height * scale) + offset.y)
            val loupeOffset = IntOffset(
                x = (touchX - (loupePx / 2f)).coerceIn(0f, (containerSize.width - loupePx)).toInt(),
                y = (touchY - loupePx - 24.dp.value).coerceIn(0f, (containerSize.height - loupePx)).toInt()
            )

            Box(
                modifier = Modifier
                    .offset { loupeOffset }
                    .size(loupeSizeDp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(3.5.dp, Color(0xFF38BDF8), CircleShape)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mag = 3.5f
                    val sampleX = cursorRatioX * bmp.width
                    val sampleY = cursorRatioY * bmp.height
                    val cropW = (bmp.width / (scale * mag)).toInt().coerceAtLeast(10)
                    val cropH = (bmp.height / (scale * mag)).toInt().coerceAtLeast(10)

                    drawImage(
                        image = bmp,
                        srcOffset = IntOffset(
                            (sampleX - cropW / 2).toInt().coerceIn(0, bmp.width - cropW),
                            (sampleY - cropH / 2).toInt().coerceIn(0, bmp.height - cropH)
                        ),
                        srcSize = IntSize(cropW, cropH),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )

                    // Retícula de precisión HD
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                        radius = 16.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(size.width / 2f - 14f, size.height / 2f),
                        end = Offset(size.width / 2f + 14f, size.height / 2f),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(size.width / 2f, size.height / 2f - 14f),
                        end = Offset(size.width / 2f, size.height / 2f + 14f),
                        strokeWidth = 2.5f
                    )
                    drawCircle(
                        color = Color.Red,
                        radius = 2.5.dp.toPx()
                    )
                }

                // Coordenadas en porcentaje de pantalla
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${(cursorRatioX * 100).toInt()}% , ${(cursorRatioY * 100).toInt()}%",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Botón flotante para refrescar captura manualmente
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            IconButton(
                onClick = onRequestRefresh,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refrescar pantalla",
                    tint = Color.White
                )
            }
        }
    }
}

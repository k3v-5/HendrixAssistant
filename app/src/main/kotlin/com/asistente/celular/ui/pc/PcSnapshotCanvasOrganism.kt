package com.asistente.celular.ui.pc

import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
 * Modos de interacción táctil con la pantalla de la PC.
 */
enum class PcTouchMode(val title: String, val icon: String, val badge: String) {
    DIRECT_TOUCH("Táctil", "🖐️", "Toque Directo"),
    TRACKPAD("Trackpad", "🖱️", "Cursor Relativo"),
    NAVIGATE("Navegar", "🧭", "Zoom y Paneo Seguro")
}

/**
 * Organismo visual de alto rendimiento para interactuar con la pantalla de la PC mediante fotogramas Snapshot WebP.
 *
 * Características avanzadas de interacción:
 * - Preservación estricta de la relación de aspecto de la PC (ContentScale.Fit con centrado automático).
 * - Zoom por pinza multitáctil basado en centroide (1.0x a 6.0x) sin saltos de coordenadas.
 * - Límites de paneo estrictos para evitar perder la imagen fuera de la pantalla.
 * - Soporte para 3 modos de interacción:
 *     1) Direct Touch: toque directo estilo pantalla táctil de Windows.
 *     2) Trackpad: control relativo de precisión con puntero virtual.
 *     3) Navigate: paneo y zoom libres sin enviar clics accidentales a la PC.
 * - HUD táctil de zoom flotante (+, -, presets de zoom 100% / 200% / 350%, reset fit).
 * - Lupa de Precisión HD 3.8x inteligente con auto-inversión vertical para no ser tapada por el pulgar.
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

    // Modo de interacción táctil activo
    var touchMode by remember { mutableStateOf(PcTouchMode.DIRECT_TOUCH) }

    // Estados de transformación (Zoom y Paneo)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Coordenadas relativas del cursor virtual sobre la pantalla de la PC (0.0f a 1.0f)
    var cursorRatioX by remember { mutableFloatStateOf(0.5f) }
    var cursorRatioY by remember { mutableFloatStateOf(0.5f) }

    // Estado de la Lupa de Precisión (Zoom Loupe)
    var isManualLoupeActive by remember { mutableStateOf(false) }
    var loupeTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Decodificación asíncrona del snapshot WebP
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

    // Cálculos de ajuste de relación de aspecto (Fit Center)
    val bmp = imageBitmap
    val bmpW = bmp?.width?.toFloat() ?: 1920f
    val bmpH = bmp?.height?.toFloat() ?: 1080f
    val bmpAspect = bmpW / bmpH

    val cW = containerSize.width.toFloat().coerceAtLeast(1f)
    val cH = containerSize.height.toFloat().coerceAtLeast(1f)
    val containerAspect = cW / cH

    val (fitW, fitH) = if (containerAspect > bmpAspect) {
        Pair(cH * bmpAspect, cH)
    } else {
        Pair(cW, cW / bmpAspect)
    }

    // Dimensiones escaladas
    val scaledW = fitW * scale
    val scaledH = fitH * scale

    // Límites de paneo (clamping para que la imagen nunca se escape del lienzo)
    val maxPanX = if (scaledW > cW) (scaledW - cW) / 2f else 0f
    val maxPanY = if (scaledH > cH) (scaledH - cH) / 2f else 0f

    // Función auxiliar para limitar offset
    fun clampOffset(raw: Offset, s: Float): Offset {
        val sW = fitW * s
        val sH = fitH * s
        val mX = if (sW > cW) (sW - cW) / 2f else 0f
        val mY = if (sH > cH) (sH - cH) / 2f else 0f
        return Offset(
            x = raw.x.coerceIn(-mX, mX),
            y = raw.y.coerceIn(-mY, mY)
        )
    }

    // Posición superior izquierda del fotograma de la PC en pantalla
    val imageScreenLeft = (cW / 2f + offset.x) - (scaledW / 2f)
    val imageScreenTop = (cH / 2f + offset.y) - (scaledH / 2f)

    // Convierte un punto táctil de la pantalla a ratios de la PC [0f..1f]
    fun screenToPcRatio(touchPos: Offset): Pair<Float, Float> {
        val normX = ((touchPos.x - imageScreenLeft) / scaledW).coerceIn(0f, 1f)
        val normY = ((touchPos.y - imageScreenTop) / scaledH).coerceIn(0f, 1f)
        return Pair(normX, normY)
    }

    // Restablece el zoom a 100% centrado
    fun resetZoom() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        scale = 1f
        offset = Offset.Zero
    }

    // Ajusta el zoom de forma incremental
    fun adjustZoom(delta: Float) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val newScale = (scale + delta).coerceIn(1f, 6f)
        scale = newScale
        offset = clampOffset(offset, newScale)
    }

    // Cicla entre presets de zoom (100% -> 200% -> 350% -> 100%)
    fun cycleZoomPreset() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val newScale = when {
            scale < 1.7f -> 2.0f
            scale < 3.0f -> 3.5f
            else -> 1.0f
        }
        scale = newScale
        offset = clampOffset(offset, newScale)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF0B1120))
            .onSizeChanged { containerSize = it }
            // Manejador de PINZA MULTITÁCTIL (Pinch-to-Zoom y Paneo por centroide)
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(1f, 6f)

                    if (newScale != oldScale || pan != Offset.Zero) {
                        val factor = newScale / oldScale
                        val curCenterX = (cW / 2f) + offset.x
                        val curCenterY = (cH / 2f) + offset.y

                        val newCenterX = centroid.x + (curCenterX - centroid.x) * factor + pan.x
                        val newCenterY = centroid.y + (curCenterY - centroid.y) * factor + pan.y

                        val rawOffset = Offset(
                            x = newCenterX - (cW / 2f),
                            y = newCenterY - (cH / 2f)
                        )
                        scale = newScale
                        offset = clampOffset(rawOffset, newScale)
                    }
                }
            }
            // Manejador de TOQUES SIMPLES SEGÚN EL MODO ACTIVO
            .pointerInput(touchMode) {
                when (touchMode) {
                    PcTouchMode.DIRECT_TOUCH -> {
                        detectTapGestures(
                            onTap = { tapPos ->
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                val (normX, normY) = screenToPcRatio(tapPos)
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
                                val (normX, normY) = screenToPcRatio(tapPos)
                                cursorRatioX = normX
                                cursorRatioY = normY
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
                                val (normX, normY) = screenToPcRatio(tapPos)
                                cursorRatioX = normX
                                cursorRatioY = normY
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
                    PcTouchMode.TRACKPAD -> {
                        detectTapGestures(
                            onTap = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onSendAction(
                                    PcInteractionAction(
                                        type = PcActionType.CLICK,
                                        xRatio = cursorRatioX,
                                        yRatio = cursorRatioY
                                    )
                                )
                            },
                            onDoubleTap = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onSendAction(
                                    PcInteractionAction(
                                        type = PcActionType.DOUBLE_CLICK,
                                        xRatio = cursorRatioX,
                                        yRatio = cursorRatioY
                                    )
                                )
                            },
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onSendAction(
                                    PcInteractionAction(
                                        type = PcActionType.RIGHT_CLICK,
                                        xRatio = cursorRatioX,
                                        yRatio = cursorRatioY
                                    )
                                )
                            }
                        )
                    }
                    PcTouchMode.NAVIGATE -> {
                        detectTapGestures(
                            onDoubleTap = { tapPos ->
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    val targetScale = 2.5f
                                    val (normX, normY) = screenToPcRatio(tapPos)
                                    val targetCenterX = cW * 0.5f - (normX - 0.5f) * fitW * targetScale
                                    val targetCenterY = cH * 0.5f - (normY - 0.5f) * fitH * targetScale
                                    scale = targetScale
                                    offset = clampOffset(Offset(targetCenterX - cW * 0.5f, targetCenterY - cH * 0.5f), targetScale)
                                }
                            }
                        )
                    }
                }
            }
            // Manejador de ARRASTRES CONTINUOS (Drag en Trackpad o Paneo en Navegación)
            .pointerInput(touchMode) {
                if (touchMode == PcTouchMode.TRACKPAD) {
                    detectDragGestures(
                        onDragStart = { startPos ->
                            if (isLoupeEnabled) {
                                isManualLoupeActive = true
                                loupeTouchOffset = startPos
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            loupeTouchOffset += dragAmount

                            // Aceleración de cursor balístico suave
                            val sensitivity = 1.35f
                            val deltaX = (dragAmount.x * sensitivity) / scaledW
                            val deltaY = (dragAmount.y * sensitivity) / scaledH

                            cursorRatioX = (cursorRatioX + deltaX).coerceIn(0f, 1f)
                            cursorRatioY = (cursorRatioY + deltaY).coerceIn(0f, 1f)

                            onSendAction(
                                PcInteractionAction(
                                    type = PcActionType.MOUSE_MOVE,
                                    xRatio = cursorRatioX,
                                    yRatio = cursorRatioY
                                )
                            )
                        },
                        onDragEnd = { isManualLoupeActive = false },
                        onDragCancel = { isManualLoupeActive = false }
                    )
                } else if (touchMode == PcTouchMode.NAVIGATE) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset = clampOffset(offset + dragAmount, scale)
                    }
                }
            }
    ) {
        if (bmp != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dibujar el fotograma escalado y centrado respetando la relación de aspecto real
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset(imageScreenLeft.toInt(), imageScreenTop.toInt()),
                    dstSize = IntSize(scaledW.toInt(), scaledH.toInt())
                )

                // Resaltado de Ventana Activa si el enfoque está habilitado
                if (isWindowFocusActive && activeWindowBounds != null) {
                    val normLeft = (activeWindowBounds.left.toFloat() / 1920f).coerceIn(0f, 1f)
                    val normTop = (activeWindowBounds.top.toFloat() / 1080f).coerceIn(0f, 1f)
                    val normW = (activeWindowBounds.width.toFloat() / 1920f).coerceIn(0.05f, 1f)
                    val normH = (activeWindowBounds.height.toFloat() / 1080f).coerceIn(0.05f, 1f)

                    val boxX = imageScreenLeft + (normLeft * scaledW)
                    val boxY = imageScreenTop + (normTop * scaledH)
                    val boxW = normW * scaledW
                    val boxH = normH * scaledH

                    drawRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Dibujar cursor virtual sobre la posición normalizada
                val cursorScreenX = imageScreenLeft + (cursorRatioX * scaledW)
                val cursorScreenY = imageScreenTop + (cursorRatioY * scaledH)

                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    radius = 12.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 6.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
            }
        } else {
            // Estado de espera o reconexión
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Conectando con la pantalla del PC...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ==========================================
        // BARRA SUPERIOR: SELECTOR DE MODO Y ACCIONES
        // ==========================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Selector de modo táctil
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PcTouchMode.entries.forEach { mode ->
                        val isSelected = touchMode == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
                            modifier = Modifier.clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                touchMode = mode
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Botón de refresco manual
                IconButton(
                    onClick = onRequestRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar pantalla",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ==========================================
        // HUD FLOTANTE DE ZOOM (CONTROLES ERGONÓMICOS)
        // ==========================================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Zoom Out (-)
                IconButton(
                    onClick = { adjustZoom(-0.5f) },
                    enabled = scale > 1.0f,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Reducir zoom",
                        tint = if (scale > 1.0f) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Porcentaje actual / Ciclo rápido
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .clickable { cycleZoomPreset() }
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Zoom In (+)
                IconButton(
                    onClick = { adjustZoom(+0.5f) },
                    enabled = scale < 6.0f,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar zoom",
                        tint = if (scale < 6.0f) Color.White else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Restablecer a 1x (Ajustar a pantalla)
                IconButton(
                    onClick = { resetZoom() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Ajustar a pantalla completa",
                        tint = if (scale > 1.05f) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ==========================================
        // LUPA DE PRECISIÓN HD (ZOOM LOUPE 3.8X)
        // ==========================================
        val showLoupe = (isManualLoupeActive || isLoupeEnabled) && bmp != null
        if (showLoupe) {
            val loupeSizeDp = 138.dp
            val loupePx = with(density) { loupeSizeDp.toPx() }

            val cursorScreenX = imageScreenLeft + (cursorRatioX * scaledW)
            val cursorScreenY = imageScreenTop + (cursorRatioY * scaledH)

            val anchorX = if (isManualLoupeActive) loupeTouchOffset.x else cursorScreenX
            val anchorY = if (isManualLoupeActive) loupeTouchOffset.y else cursorScreenY

            // Auto-inversión: si el toque está en la parte superior, colocar la lupa abajo para no salirse
            val targetY = if (anchorY < cH * 0.45f) {
                anchorY + 24.dp.value
            } else {
                anchorY - loupePx - 24.dp.value
            }

            val loupeOffset = IntOffset(
                x = (anchorX - (loupePx / 2f)).coerceIn(8f, (cW - loupePx - 8f)).toInt(),
                y = targetY.coerceIn(8f, (cH - loupePx - 8f)).toInt()
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
                    val mag = 3.8f
                    val sampleX = cursorRatioX * bmp.width
                    val sampleY = cursorRatioY * bmp.height
                    val cropW = (bmp.width / (scale * mag)).toInt().coerceAtLeast(12)
                    val cropH = (bmp.height / (scale * mag)).toInt().coerceAtLeast(12)

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

                    // Retícula de mira telescópica HD
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                        radius = 18.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(size.width / 2f - 16f, size.height / 2f),
                        end = Offset(size.width / 2f + 16f, size.height / 2f),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(size.width / 2f, size.height / 2f - 16f),
                        end = Offset(size.width / 2f, size.height / 2f + 16f),
                        strokeWidth = 2.5f
                    )
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 3.dp.toPx()
                    )
                }

                // Coordenadas en porcentaje de pantalla
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
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
    }
}

package com.asistente.celular.ui.pc

import android.graphics.BitmapFactory
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector
import com.asistente.celular.ui.theme.NeonCyan
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.asistente.celular.nlu.pc.PcWindowInfo
import com.asistente.celular.nlu.pc.WindowBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Modos de interacción táctil con la pantalla de la PC (mantenido para compatibilidad).
 */
enum class PcTouchMode(val title: String, val icon: ImageVector, val badge: String) {
    DIRECT_TOUCH("Táctil", Icons.Default.TouchApp, "Toque Directo"),
    TRACKPAD("Trackpad", Icons.Default.Mouse, "Cursor Relativo"),
    NAVIGATE("Navegar", Icons.Default.PanTool, "Zoom y Paneo Seguro")
}

/**
 * Organismo visual de ultra alto rendimiento para interactuar con la pantalla de la PC.
 */
@Composable
fun PcSnapshotCanvasOrganism(
    snapshotBytes: ByteArray?,
    onSendAction: (PcInteractionAction) -> Unit,
    onRequestRefresh: () -> Unit,
    isLoupeEnabled: Boolean = false,
    isWindowFocusActive: Boolean = false,
    activeWindowBounds: WindowBounds? = null,
    openWindows: List<PcWindowInfo> = emptyList(),
    onRequestWindows: (suspend () -> List<PcWindowInfo>)? = null,
    onFocusWindow: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Modo de interacción táctil activo (Toque Directo por defecto, alternable a Trackpad)
    var touchMode by remember { mutableStateOf(PcTouchMode.DIRECT_TOUCH) }

    // Estados de transformación (Zoom y Paneo)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Coordenadas relativas del cursor virtual sobre la pantalla de la PC (0.0f a 1.0f)
    var cursorRatioX by remember { mutableFloatStateOf(0.5f) }
    var cursorRatioY by remember { mutableFloatStateOf(0.5f) }

    // Estado del gesto de pulsación sostenida (Hold Timer 2s / 4s)
    var holdAnchor by remember { mutableStateOf(Offset.Zero) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdPhase by remember { mutableIntStateOf(1) } // 1 = 0..2s (Clic Izq), 2 = 2..4s (Clic Der)
    var isHoldingVisual by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    // Píldora de Zoom Efímera
    var isZoomPillVisible by remember { mutableStateOf(false) }
    var lastZoomTimestamp by remember { mutableLongStateOf(0L) }

    // Retroalimentación visual de acciones (Toast flotante)
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastPhase by remember { mutableIntStateOf(1) }
    var toastTrigger by remember { mutableLongStateOf(0L) }

    // Lupa de Precisión (Zoom Loupe)
    var isManualLoupeActive by remember { mutableStateOf(false) }
    var loupeTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Posicionador y Selector de Zonas Ultrawide
    var isMinimapVisible by remember { mutableStateOf(true) }

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

    // Auto-desvanecimiento de la píldora de zoom
    LaunchedEffect(lastZoomTimestamp) {
        if (lastZoomTimestamp > 0L) {
            isZoomPillVisible = true
            delay(1400)
            isZoomPillVisible = false
        }
    }

    // Auto-desvanecimiento del toast de acción
    LaunchedEffect(toastTrigger) {
        if (toastMessage != null) {
            delay(1800)
            toastMessage = null
        }
    }

    // Cálculos dinámicos de ajuste de relación de aspecto desacoplados (Fit Center vía PcTouchGeometryHelper)
    fun getCurrentFitResult(): FitResult {
        val curBmp = imageBitmap
        val bmpW = curBmp?.width?.toFloat() ?: 1920f
        val bmpH = curBmp?.height?.toFloat() ?: 1080f
        val curCW = containerSize.width.toFloat().coerceAtLeast(1f)
        val curCH = containerSize.height.toFloat().coerceAtLeast(1f)

        return PcTouchGeometryHelper.calculateFitDimensions(
            containerW = curCW,
            containerH = curCH,
            bmpW = bmpW,
            bmpH = bmpH,
            scale = scale,
            offset = offset
        )
    }

    // Función auxiliar para limitar offset con soporte completo en X e Y
    fun clampOffset(raw: Offset, s: Float): Offset {
        val curCW = containerSize.width.toFloat().coerceAtLeast(1f)
        val curCH = containerSize.height.toFloat().coerceAtLeast(1f)
        val curBmp = imageBitmap
        val bmpW = curBmp?.width?.toFloat() ?: 1920f
        val bmpH = curBmp?.height?.toFloat() ?: 1080f

        val baseFit = PcTouchGeometryHelper.calculateFitDimensions(
            containerW = curCW,
            containerH = curCH,
            bmpW = bmpW,
            bmpH = bmpH,
            scale = 1f,
            offset = Offset.Zero
        )

        val (clampedX, clampedY) = PcTouchGeometryHelper.clampOffset(
            rawX = raw.x,
            rawY = raw.y,
            fitW = baseFit.fitW,
            fitH = baseFit.fitH,
            cW = curCW,
            cH = curCH,
            scale = s
        )
        return Offset(clampedX, clampedY)
    }

    // Convierte un punto táctil de la pantalla a ratios de la PC [0f..1f] sin desfasarse a cualquier nivel de zoom
    fun screenToPcRatio(touchPos: Offset): Pair<Float, Float> {
        val fit = getCurrentFitResult()
        return PcTouchGeometryHelper.screenToPcRatio(
            touchX = touchPos.x,
            touchY = touchPos.y,
            imageScreenLeft = fit.imageScreenLeft,
            imageScreenTop = fit.imageScreenTop,
            scaledW = fit.scaledW,
            scaledH = fit.scaledH
        )
    }

    // Restablece el zoom a 100% centrado
    fun resetZoom() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        scale = 1f
        offset = Offset.Zero
    }

    fun showToast(msg: String, phase: Int = 1) {
        toastMessage = msg
        toastPhase = phase
        toastTrigger = System.currentTimeMillis()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF07040C)) // OLED Void Dark Obsidian Purple
            .onSizeChanged { containerSize = it }
            // MOTOR GESTUAL UNIFICADO DE ALTO RENDIMIENTO
            .pointerInput(Unit) {
                var previousCentroid: Offset? = null
                var previousDistance: Float? = null
                var singleTouchDownTime = 0L
                var singleTouchStartPos = Offset.Zero
                var isDragging = false
                var isPinchGestureActive = false
                var hasFiredLeftClick = false
                var hasFiredRightClick = false
                var lastTapTimestamp = 0L
                var lastTapPosition = Offset.Zero
                var lastMouseMoveSentTime = 0L
                var lastScrollSentTime = 0L

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.filter { it.pressed }

                        when (activePointers.size) {
                            0 -> {
                                // Todos los dedos levantados
                                holdJob?.cancel()
                                isHoldingVisual = false
                                holdProgress = 0f

                                val now = System.currentTimeMillis()
                                val duration = now - singleTouchDownTime

                                if (!isPinchGestureActive) {
                                    // Si fue un toque rápido sin arrastre y no se disparó clic temporizado previo
                                    if (singleTouchDownTime > 0L && !isDragging && !hasFiredLeftClick && !hasFiredRightClick && duration < 350L) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        val (normX, normY) = screenToPcRatio(singleTouchStartPos)
                                        cursorRatioX = normX
                                        cursorRatioY = normY

                                        val tapDelta = singleTouchStartPos - lastTapPosition
                                        val tapDist = sqrt(tapDelta.x * tapDelta.x + tapDelta.y * tapDelta.y)

                                        if (now - lastTapTimestamp < 320L && tapDist < 50f) {
                                            // Doble Clic Rápido
                                            onSendAction(
                                                PcInteractionAction(
                                                    type = PcActionType.DOUBLE_CLICK,
                                                    xRatio = normX,
                                                    yRatio = normY
                                                )
                                            )
                                            showToast("Doble Clic", 1)
                                            lastTapTimestamp = 0L
                                        } else {
                                            // Clic Izquierdo Rápido
                                            onSendAction(
                                                PcInteractionAction(
                                                    type = PcActionType.CLICK,
                                                    xRatio = normX,
                                                    yRatio = normY
                                                )
                                            )
                                            showToast("Clic Primario", 1)
                                            lastTapTimestamp = now
                                            lastTapPosition = singleTouchStartPos
                                        }
                                    } else if (isDragging) {
                                        // Al levantar el dedo tras arrastrar, asegurar envío de posición final precisa
                                        onSendAction(
                                            PcInteractionAction(
                                                type = PcActionType.MOUSE_MOVE,
                                                xRatio = cursorRatioX,
                                                yRatio = cursorRatioY
                                            )
                                        )
                                    }
                                }

                                // Reset de estados
                                previousCentroid = null
                                previousDistance = null
                                singleTouchDownTime = 0L
                                isDragging = false
                                isPinchGestureActive = false
                                hasFiredLeftClick = false
                                hasFiredRightClick = false
                            }

                            1 -> {
                                if (isPinchGestureActive) {
                                    // El usuario estaba pellizcando/paneando con dos dedos y acaba de levantar uno.
                                    // Ignorar para evitar saltos o clics accidentales hasta que levante todos los dedos.
                                    previousCentroid = null
                                    previousDistance = null
                                    activePointers[0].consume()
                                } else {
                                    val pointer = activePointers[0]
                                    previousCentroid = null
                                    previousDistance = null

                                    // Primer contacto del dedo con la pantalla
                                    if (!pointer.previousPressed && pointer.pressed) {
                                        singleTouchDownTime = System.currentTimeMillis()
                                        singleTouchStartPos = pointer.position
                                        isDragging = false
                                        hasFiredLeftClick = false
                                        hasFiredRightClick = false

                                        if (touchMode == PcTouchMode.DIRECT_TOUCH) {
                                            val (normX, normY) = screenToPcRatio(pointer.position)
                                            cursorRatioX = normX
                                            cursorRatioY = normY
                                        }

                                        // Iniciar temporizador de clic sostenido (2s Clic Izq / 4s Clic Der)
                                        holdJob?.cancel()
                                        holdJob = coroutineScope.launch {
                                            holdAnchor = pointer.position
                                            holdProgress = 0f
                                            holdPhase = 1
                                            isHoldingVisual = true

                                            val startTime = System.currentTimeMillis()

                                            // Fase 1: 0 a 2000 ms (Progreso Cian)
                                            while (true) {
                                                val elapsed = System.currentTimeMillis() - startTime
                                                if (elapsed >= 2000L) break
                                                holdProgress = (elapsed / 2000f).coerceIn(0f, 1f)
                                                delay(16)
                                            }

                                            // Hito 2.0s alcanzado: Clic Izquierdo
                                            holdProgress = 1f
                                            hasFiredLeftClick = true
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            val (normX, normY) = screenToPcRatio(holdAnchor)
                                            cursorRatioX = normX
                                            cursorRatioY = normY
                                            onSendAction(
                                                PcInteractionAction(
                                                    type = PcActionType.CLICK,
                                                    xRatio = normX,
                                                    yRatio = normY
                                                )
                                            )
                                            showToast("Clic Primario (2s)", 1)

                                            // Fase 2: 2000 a 4000 ms (Progreso Ámbar)
                                            holdPhase = 2
                                            val phase2StartTime = System.currentTimeMillis()
                                            while (true) {
                                                val elapsed = System.currentTimeMillis() - phase2StartTime
                                                if (elapsed >= 2000L) break
                                                holdProgress = (elapsed / 2000f).coerceIn(0f, 1f)
                                                delay(16)
                                            }

                                            // Hito 4.0s alcanzado: Clic Derecho
                                            holdProgress = 1f
                                            hasFiredRightClick = true
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            onSendAction(
                                                PcInteractionAction(
                                                    type = PcActionType.RIGHT_CLICK,
                                                    xRatio = normX,
                                                    yRatio = normY
                                                )
                                            )
                                            showToast("Clic Secundario (4s)", 2)
                                            delay(350)
                                            isHoldingVisual = false
                                        }
                                    } else {
                                        // El dedo se encuentra en movimiento o sostenido
                                        val moveDelta = pointer.position - singleTouchStartPos
                                        val moveDist = sqrt(moveDelta.x * moveDelta.x + moveDelta.y * moveDelta.y)

                                        if (!isDragging && moveDist > 22f) {
                                            // Movimiento mayor al umbral de arrastre: cancelar temporizador
                                            isDragging = true
                                            holdJob?.cancel()
                                            isHoldingVisual = false
                                            holdProgress = 0f
                                        }

                                        if (isDragging) {
                                            val dragDelta = pointer.position - pointer.previousPosition

                                            if (touchMode == PcTouchMode.DIRECT_TOUCH) {
                                                // Modo Táctil Directo: cursor sigue exactamente al dedo en cualquier nivel de zoom
                                                val (normX, normY) = screenToPcRatio(pointer.position)
                                                cursorRatioX = normX
                                                cursorRatioY = normY
                                                pointer.consume()

                                                val now = System.currentTimeMillis()
                                                if (now - lastMouseMoveSentTime >= 28L) {
                                                    lastMouseMoveSentTime = now
                                                    onSendAction(
                                                        PcInteractionAction(
                                                            type = PcActionType.MOUSE_MOVE,
                                                            xRatio = cursorRatioX,
                                                            yRatio = cursorRatioY
                                                        )
                                                    )
                                                }
                                            } else {
                                                // Modo Trackpad: desplazamiento relativo de cursor calibrado con escala en vivo
                                                val fit = getCurrentFitResult()
                                                val deltaX = dragDelta.x / fit.scaledW
                                                val deltaY = dragDelta.y / fit.scaledH
                                                cursorRatioX = (cursorRatioX + deltaX).coerceIn(0f, 1f)
                                                cursorRatioY = (cursorRatioY + deltaY).coerceIn(0f, 1f)
                                                pointer.consume()

                                                val now = System.currentTimeMillis()
                                                if (now - lastMouseMoveSentTime >= 28L) {
                                                    lastMouseMoveSentTime = now
                                                    onSendAction(
                                                        PcInteractionAction(
                                                            type = PcActionType.MOUSE_MOVE,
                                                            xRatio = cursorRatioX,
                                                            yRatio = cursorRatioY
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                // 2 O MÁS DEDOS: PINCH-TO-ZOOM Y PANEO POR CENTROIDE CON DIMENSIONES EN VIVO
                                isPinchGestureActive = true
                                holdJob?.cancel()
                                isHoldingVisual = false
                                holdProgress = 0f
                                singleTouchDownTime = 0L
                                isDragging = false

                                val p0 = activePointers[0]
                                val p1 = activePointers[1]
                                val currentCentroid = (p0.position + p1.position) / 2f
                                val diff = p0.position - p1.position
                                val currentDistance = sqrt(diff.x * diff.x + diff.y * diff.y).coerceAtLeast(1f)

                                if (previousDistance != null && previousCentroid != null) {
                                    val distanceDelta = currentDistance - previousDistance!!
                                    val panDelta = currentCentroid - previousCentroid!!

                                    val gestureClassification = PcTouchGeometryHelper.classifyTwoFingerGesture(
                                        distanceDelta = distanceDelta,
                                        currentDistance = currentDistance,
                                        centroidDelta = panDelta
                                    )

                                    val isScrollIntent = when (touchMode) {
                                        PcTouchMode.TRACKPAD -> gestureClassification == TwoFingerGestureType.SCROLL
                                        PcTouchMode.DIRECT_TOUCH -> (scale <= 1.05f && gestureClassification == TwoFingerGestureType.SCROLL)
                                        PcTouchMode.NAVIGATE -> false
                                    }

                                    if (isScrollIntent) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastScrollSentTime >= 32L) {
                                            lastScrollSentTime = now
                                            val scrollDelta = PcTouchGeometryHelper.calculateScrollDelta(panDelta.y, sensitivity = 0.08f)
                                            if (kotlin.math.abs(scrollDelta) >= 0.15f) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                onSendAction(
                                                    PcInteractionAction(
                                                        type = PcActionType.SCROLL,
                                                        xRatio = cursorRatioX,
                                                        yRatio = cursorRatioY,
                                                        scrollDeltaY = scrollDelta
                                                    )
                                                )
                                                showToast(if (scrollDelta > 0) "Scroll ▲" else "Scroll ▼", 1)
                                            }
                                        }
                                    } else {
                                        val zoomFactor = currentDistance / previousDistance!!
                                        val oldScale = scale
                                        val newScale = (scale * zoomFactor).coerceIn(1f, 6f)

                                        if (newScale != oldScale || panDelta != Offset.Zero) {
                                            val liveCW = containerSize.width.toFloat().coerceAtLeast(1f)
                                            val liveCH = containerSize.height.toFloat().coerceAtLeast(1f)

                                            val factor = newScale / oldScale
                                            val curCenterX = (liveCW / 2f) + offset.x
                                            val curCenterY = (liveCH / 2f) + offset.y

                                            val newCenterX = currentCentroid.x + (curCenterX - currentCentroid.x) * factor + panDelta.x
                                            val newCenterY = currentCentroid.y + (curCenterY - currentCentroid.y) * factor + panDelta.y

                                            val rawOffset = Offset(
                                                x = newCenterX - (liveCW / 2f),
                                                y = newCenterY - (liveCH / 2f)
                                            )
                                            scale = newScale
                                            offset = clampOffset(rawOffset, newScale)

                                            lastZoomTimestamp = System.currentTimeMillis()
                                        }
                                    }
                                }

                                previousDistance = currentDistance
                                previousCentroid = currentCentroid
                                p0.consume()
                                p1.consume()
                            }
                        }
                    }
                }
            }
    ) {
        val bmp = imageBitmap
        if (bmp != null) {
            val fit = getCurrentFitResult()
            val imageScreenLeft = fit.imageScreenLeft
            val imageScreenTop = fit.imageScreenTop
            val scaledW = fit.scaledW
            val scaledH = fit.scaledH

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
                        color = Color(0xFFA855F7),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Dibujar cursor virtual sobre la posición normalizada (Orbe Morado Neón)
                val cursorScreenX = imageScreenLeft + (cursorRatioX * scaledW)
                val cursorScreenY = imageScreenTop + (cursorRatioY * scaledH)

                drawCircle(
                    color = Color(0xFFA855F7).copy(alpha = 0.45f),
                    radius = 12.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
                drawCircle(
                    color = Color(0xFFA855F7),
                    radius = 5.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(cursorScreenX, cursorScreenY)
                )

                // ==========================================
                // RADAR HOLOGRÁFICO DE CLIC SOSTENIDO (2s / 4s)
                // ==========================================
                if (isHoldingVisual) {
                    val phaseColor = if (holdPhase == 1) Color(0xFFA855F7) else Color(0xFFFFB800)
                    val ringRadius = 34.dp.toPx()

                    // Fondo con brillo translúcido
                    drawCircle(
                        color = phaseColor.copy(alpha = 0.22f),
                        radius = ringRadius + 4.dp.toPx(),
                        center = holdAnchor
                    )

                    // Pista circular sutil
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f),
                        radius = ringRadius,
                        center = holdAnchor,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Arco de progreso que se va llenando
                    drawArc(
                        color = phaseColor,
                        startAngle = -90f,
                        sweepAngle = holdProgress * 360f,
                        useCenter = false,
                        topLeft = Offset(holdAnchor.x - ringRadius, holdAnchor.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Punto focal central
                    drawCircle(
                        color = phaseColor,
                        radius = 4.dp.toPx(),
                        center = holdAnchor
                    )
                }
            }
        } else {
            // Estado de espera o carga inicial
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFA855F7))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sincronizando pantalla del PC...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ==========================================
        // PÍLDORA DE ZOOM EFÍMERA (AL PELLIZCAR)
        // ==========================================
        AnimatedVisibility(
            visible = isZoomPillVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF140D24).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.65f)),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        color = Color(0xFFC084FC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // BANNER DE NOTIFICACIÓN TOAST FLOTANTE
        // ==========================================
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF140D24).copy(alpha = 0.95f),
                border = BorderStroke(
                    1.dp,
                    if (toastPhase == 2) Color(0xFFFFB800) else Color(0xFFA855F7)
                ),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (toastPhase == 2) Color(0xFFFFB800) else Color(0xFFA855F7))
                    )
                    Text(
                        text = toastMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ==========================================
        // MICRO-CONTROLES DISCRETOS DE ESQUINA (1X / REFRESCAR)
        // ==========================================
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF140D24).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0xFF26163D))
        ) {
            Row(
                modifier = Modifier.padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Selector de Modo Táctil (Directo / Trackpad)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (touchMode == PcTouchMode.DIRECT_TOUCH) Color(0xFFA855F7).copy(alpha = 0.22f) else Color(0xFF26163D).copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable {
                            touchMode = if (touchMode == PcTouchMode.DIRECT_TOUCH) PcTouchMode.TRACKPAD else PcTouchMode.DIRECT_TOUCH
                            showToast(if (touchMode == PcTouchMode.DIRECT_TOUCH) "Modo Toque Directo" else "Modo Trackpad", 1)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = touchMode.icon,
                            contentDescription = touchMode.title,
                            tint = if (touchMode == PcTouchMode.DIRECT_TOUCH) Color(0xFFC084FC) else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = touchMode.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (touchMode == PcTouchMode.DIRECT_TOUCH) Color(0xFFC084FC) else Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                if (scale > 1.05f) {
                    IconButton(
                        onClick = { resetZoom() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Restablecer a 1x",
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        isMinimapVisible = !isMinimapVisible
                        showToast(if (isMinimapVisible) "Posicionador visible" else "Posicionador oculto", 1)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Alternar Selector / Zonas",
                        tint = if (isMinimapVisible) NeonCyan else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onRequestRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar pantalla",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ==========================================
        // MINIMAPA RADAR Y SELECTOR DE ZONAS ULTRAWIDE
        // ==========================================
        if (bmp != null && isMinimapVisible) {
            val curCW = containerSize.width.toFloat().coerceAtLeast(1f)
            val curCH = containerSize.height.toFloat().coerceAtLeast(1f)
            val fit = getCurrentFitResult()
            val viewportBounds = PcTouchGeometryHelper.calculateVisibleViewportRatio(
                containerW = curCW,
                containerH = curCH,
                imageScreenLeft = fit.imageScreenLeft,
                imageScreenTop = fit.imageScreenTop,
                scaledW = fit.scaledW,
                scaledH = fit.scaledH
            )
            val monitorAspect = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)

            PcViewportMinimap(
                viewportBounds = viewportBounds,
                aspectRatio = monitorAspect,
                scale = scale,
                onPanToRatio = { targetRatioX, targetRatioY ->
                    val newScale = if (scale <= 1.05f) 2.5f else scale
                    scale = newScale

                    val targetFit = PcTouchGeometryHelper.calculateFitDimensions(
                        containerW = curCW,
                        containerH = curCH,
                        bmpW = bmp.width.toFloat(),
                        bmpH = bmp.height.toFloat(),
                        scale = newScale,
                        offset = Offset.Zero
                    )
                    val rawOffset = PcTouchGeometryHelper.ratioToOffset(
                        targetRatioX = targetRatioX,
                        targetRatioY = targetRatioY,
                        scaledW = targetFit.scaledW,
                        scaledH = targetFit.scaledH
                    )
                    offset = clampOffset(rawOffset, newScale)
                    lastZoomTimestamp = System.currentTimeMillis()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 64.dp, start = 12.dp)
            )
        }

        // ==========================================
        // LUPA DE PRECISIÓN HD (ZOOM LOUPE 3.8X)
        // ==========================================
        val showLoupe = (isManualLoupeActive || isLoupeEnabled) && bmp != null
        if (showLoupe) {
            val loupeSizeDp = 138.dp
            val loupePx = with(density) { loupeSizeDp.toPx() }
            val fit = getCurrentFitResult()
            val curCW = containerSize.width.toFloat().coerceAtLeast(1f)
            val curCH = containerSize.height.toFloat().coerceAtLeast(1f)

            val cursorScreenX = fit.imageScreenLeft + (cursorRatioX * fit.scaledW)
            val cursorScreenY = fit.imageScreenTop + (cursorRatioY * fit.scaledH)

            val anchorX = if (isManualLoupeActive) loupeTouchOffset.x else cursorScreenX
            val anchorY = if (isManualLoupeActive) loupeTouchOffset.y else cursorScreenY

            // Auto-inversión: si el toque está en la parte superior, colocar la lupa abajo para no salirse
            val targetY = if (anchorY < curCH * 0.45f) {
                anchorY + 24.dp.value
            } else {
                anchorY - loupePx - 24.dp.value
            }

            val loupeOffset = IntOffset(
                x = (anchorX - (loupePx / 2f)).coerceIn(8f, (curCW - loupePx - 8f)).toInt(),
                y = targetY.coerceIn(8f, (curCH - loupePx - 8f)).toInt()
            )

            Box(
                modifier = Modifier
                    .offset { loupeOffset }
                    .size(loupeSizeDp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(3.5.dp, Color(0xFFA855F7), CircleShape)
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
                        color = Color(0xFFA855F7).copy(alpha = 0.4f),
                        radius = 18.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFFA855F7),
                        start = Offset(size.width / 2f - 16f, size.height / 2f),
                        end = Offset(size.width / 2f + 16f, size.height / 2f),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color(0xFFA855F7),
                        start = Offset(size.width / 2f, size.height / 2f - 16f),
                        end = Offset(size.width / 2f, size.height / 2f + 16f),
                        strokeWidth = 2.5f
                    )
                    drawCircle(
                        color = Color(0xFFFFB800),
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
                        color = Color(0xFFC084FC),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ==========================================
        // TIRA TÁCTIL LATERAL DE SCROLL VIRTUAL (RUEDA DE RATÓN)
        // ==========================================
        if (bmp != null) {
            PcVirtualScrollStrip(
                onScroll = { deltaY ->
                    onSendAction(
                        PcInteractionAction(
                            type = PcActionType.SCROLL,
                            xRatio = cursorRatioX,
                            yRatio = cursorRatioY,
                            scrollDeltaY = deltaY
                        )
                    )
                    showToast(if (deltaY > 0) "Scroll ▲" else "Scroll ▼", 1)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )
        }

        // ==========================================
        // BOLA FLOTANTE (GESTOR DE VENTANAS DE WINDOWS)
        // ==========================================
        if (onRequestWindows != null || onFocusWindow != null) {
            PcWindowSwitcherBubbleOrganism(
                openWindows = openWindows,
                onRequestWindows = onRequestWindows ?: { emptyList() },
                onFocusWindow = { hwnd ->
                    if (onFocusWindow != null) {
                        onFocusWindow(hwnd)
                    } else {
                        onSendAction(
                            PcInteractionAction(
                                type = PcActionType.FOCUS_WINDOW,
                                hwnd = hwnd
                            )
                        )
                    }
                    showToast("Conmutando aplicación...", 1)
                }
            )
        }
    }
}

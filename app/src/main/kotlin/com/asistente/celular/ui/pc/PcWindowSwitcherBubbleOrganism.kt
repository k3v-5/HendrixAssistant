package com.asistente.celular.ui.pc

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWindowInfo
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Organismo visual de "Bola Flotante" (Floating App Switcher Bubble).
 *
 * Características:
 * 1. Ocupa un espacio mínimo (~46dp) y se puede arrastrar libremente a cualquier posición.
 * 2. Estética Morado Neón OLED Obsidian de alto contraste.
 * 3. Al tocarse, expande una tarjeta flotante con las aplicaciones de la barra de tareas de Windows.
 * 4. Al seleccionar una app, Windows la enfoca de inmediato en primer plano sin necesidad de hacer zoom.
 */
@Composable
fun PcWindowSwitcherBubbleOrganism(
    openWindows: List<PcWindowInfo>,
    onRequestWindows: suspend () -> List<PcWindowInfo>,
    onFocusWindow: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var isExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var windowsList by remember { mutableStateOf(openWindows) }

    // Tamaño del contenedor padre para mantener la bola dentro de la pantalla
    var parentSize by remember { mutableStateOf(IntSize.Zero) }

    // Posición flotante de la bola (por defecto arriba a la derecha)
    var bubbleOffsetX by remember { mutableFloatStateOf(0f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(160f) }
    var isPositionInitialized by remember { mutableStateOf(false) }

    val bubbleSizeDp = 48.dp
    val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }

    // Inicializar posición pegada al borde derecho cuando se conoce el tamaño de pantalla
    LaunchedEffect(parentSize) {
        if (parentSize.width > 0 && !isPositionInitialized) {
            bubbleOffsetX = (parentSize.width - bubbleSizePx - with(density) { 16.dp.toPx() })
            isPositionInitialized = true
        }
    }

    // Sincronizar lista si cambia desde el exterior
    LaunchedEffect(openWindows) {
        if (openWindows.isNotEmpty()) {
            windowsList = openWindows
        }
    }

    // Función para refrescar la lista de ventanas
    fun refreshList() {
        scope.launch {
            isLoading = true
            val updated = onRequestWindows()
            if (updated.isNotEmpty()) {
                windowsList = updated
            }
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { parentSize = it }
    ) {
        // Fondo semi-transparente para cerrar el menú al hacer tap afuera cuando está expandido
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { isExpanded = false }
            )
        }

        // ==========================================
        // 1. LA BOLA FLOTANTE (COMPACTA Y ARRASTRABLE)
        // ==========================================
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        bubbleOffsetX.roundToInt(),
                        bubbleOffsetY.roundToInt()
                    )
                }
                .size(bubbleSizeDp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2E1065),
                            Color(0xFF130D22),
                            Color(0xFF07040C)
                        )
                    )
                )
                .border(
                    border = BorderStroke(
                        1.5.dp,
                        if (isExpanded) NeonLilac else NeonPurple
                    ),
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Imán a los bordes laterales (snap to left or right)
                            if (parentSize.width > 0) {
                                val margin = 16f
                                val midX = parentSize.width / 2f
                                bubbleOffsetX = if (bubbleOffsetX < midX) {
                                    margin
                                } else {
                                    (parentSize.width - bubbleSizePx - margin)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val maxX = (parentSize.width - bubbleSizePx).coerceAtLeast(0f)
                        val maxY = (parentSize.height - bubbleSizePx).coerceAtLeast(0f)
                        bubbleOffsetX = (bubbleOffsetX + dragAmount.x).coerceIn(0f, maxX)
                        bubbleOffsetY = (bubbleOffsetY + dragAmount.y).coerceIn(40f, maxY - 40f)
                    }
                }
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    isExpanded = !isExpanded
                    if (isExpanded) {
                        refreshList()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Ventanas Abiertas",
                tint = if (isExpanded) Color.White else NeonLilac,
                modifier = Modifier.size(24.dp)
            )

            // Indicador de número de apps abiertas
            if (windowsList.isNotEmpty() && !isExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(NeonPurple)
                        .border(1.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${windowsList.size.coerceAtMost(9)}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // ==========================================
        // 2. TARJETA FLOTANTE EXPANDIDA (SELECTOR DE APPS)
        // ==========================================
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF130D22).copy(alpha = 0.96f),
                border = BorderStroke(1.2.dp, NeonPurple.copy(alpha = 0.8f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Cabecera del Gestor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = NeonLilac,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Ventanas de Windows",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${windowsList.size} aplicaciones abiertas",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = NeonPurple,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                IconButton(
                                    onClick = { refreshList() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refrescar",
                                        tint = NeonLilac,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lista de aplicaciones
                    if (windowsList.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se detectaron ventanas abiertas en la PC.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(windowsList, key = { it.hwnd }) { win ->
                                WindowItemRow(
                                    window = win,
                                    onSelect = {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        onFocusWindow(win.hwnd)
                                        isExpanded = false
                                    }
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
 * Fila interactiva para cada ventana de la lista.
 */
@Composable
private fun WindowItemRow(
    window: PcWindowInfo,
    onSelect: () -> Unit
) {
    val processClean = window.process.removeSuffix(".exe")
    val badgeLetters = processClean.take(2).uppercase()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (window.isForeground) Color(0xFF2E1065).copy(alpha = 0.5f) else Color(0xFF1B132B).copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (window.isForeground) NeonPurple else Color(0xFF2D1B4E)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badge con iniciales de la aplicación
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (window.isForeground) NeonPurple.copy(alpha = 0.35f) else Color(0xFF2A1647)
                    )
                    .border(
                        1.dp,
                        if (window.isForeground) NeonLilac else NeonPurple.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeLetters,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (window.isForeground) Color.White else NeonLilac
                )
            }

            // Título y proceso
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = window.title,
                    fontSize = 13.sp,
                    fontWeight = if (window.isForeground) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = window.process,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Indicador de primer plano
            if (window.isForeground) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Text(
                        text = "Al frente",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonGreen
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonPurple.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Traer",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeonLilac,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

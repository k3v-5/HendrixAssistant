package com.asistente.celular.ui.pc.deck.antigravity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.AntigravityChat
import com.asistente.celular.nlu.pc.AntigravityProfile
import com.asistente.celular.nlu.pc.NightTaskStatus
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.NeonViolet
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidBorderHighlight
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tarjeta de control y monitoreo en tiempo real para el Pool Multicuenta de Gemini Pro
 * y la automatización de Antigravity en la computadora de escritorio.
 *
 * Capacidades:
 * - Supervisión del estado de cuota, enfriamiento y sesión activa de las 5 cuentas.
 * - Conmutación inmediata de cuenta ("Switch") con reanudación secuencial de chats activos.
 * - Monitoreo de conversaciones activas con prompts en vuelo.
 * - Control y telemetría del supervisor de tareas nocturnas (Night Watchdog).
 */
@Composable
fun PcAntigravityPoolCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val profiles by pcBridge.antigravityProfiles.collectAsState()
    val activeChats by pcBridge.activeAntigravityChats.collectAsState()
    val nightStatus by pcBridge.nightTaskStatus.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var switchingAccountName by remember { mutableStateOf<String?>(null) }
    var autoResumeChats by remember { mutableStateOf(true) }
    var showNightTaskControls by remember { mutableStateOf(false) }
    var nightGoalInput by remember { mutableStateOf("") }
    var isStartingNightTask by remember { mutableStateOf(false) }

    val activeProfile = profiles.firstOrNull { it.isActive }

    LaunchedEffect(Unit) {
        pcBridge.queryAntigravityProfiles()
        pcBridge.queryNightTaskStatus()
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VoidSurface),
        border = BorderStroke(1.dp, VoidBorderHighlight.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Cabecera principal
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🪐", fontSize = 18.sp)
                    }

                    Column {
                        Text(
                            text = "Pool Multicuenta Antigravity",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (activeProfile != null) NeonGreen else NeonAmber)
                            )
                            Text(
                                text = if (activeProfile != null) "Activa: ${activeProfile.name}" else "Sin cuenta seleccionada",
                                color = if (activeProfile != null) NeonLilac else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isRefreshing = true
                            scope.launch {
                                pcBridge.queryAntigravityProfiles()
                                pcBridge.queryNightTaskStatus()
                                delay(400)
                                isRefreshing = false
                                onShowSnackbar("Estado de perfiles Antigravity actualizado")
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeonLilac,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refrescar",
                                tint = NeonLilac,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Toggle de Auto-reanudación de chats
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = VoidSurfaceElevated,
                border = BorderStroke(1.dp, VoidBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reanudar chats al conmutar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Retoma de inmediato las tareas de IA activas con la nueva cuenta",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = autoResumeChats,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            autoResumeChats = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonPurple.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = VoidBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Lista de Cuentas del Pool
            Text(
                text = "CUENTAS DISPONIBLES (${profiles.size})",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (profiles.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VoidSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se han detectado perfiles. Pulsa el botón de actualizar.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profiles.forEach { profile ->
                        PoolAccountRow(
                            profile = profile,
                            isSwitching = switchingAccountName == profile.name,
                            onSwitch = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                switchingAccountName = profile.name
                                scope.launch {
                                    onShowSnackbar("Conmutando a ${profile.name} en la PC...")
                                    val success = pcBridge.activateAntigravityProfile(
                                        name = profile.name,
                                        resumeChats = autoResumeChats
                                    )
                                    switchingAccountName = null
                                    if (success) {
                                        onShowSnackbar("✅ Sesión conmutada exitosamente a ${profile.name}")
                                    } else {
                                        onShowSnackbar("❌ Error al conmutar perfil a ${profile.name}")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 4. Sección de Chats Activos (si los hay)
            if (activeChats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = VoidBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = NeonLilac,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "CHATS EN EJECUCIÓN (${activeChats.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonLilac,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "ACTIVOS",
                            color = NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeChats.forEach { chat ->
                        ActiveChatRow(chat = chat)
                    }
                }
            }

            // 5. Sección del Supervisor Nocturno (Night Watchdog)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = VoidBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            NightWatchdogSection(
                status = nightStatus,
                isExpanded = showNightTaskControls,
                onToggleExpand = { showNightTaskControls = !showNightTaskControls },
                goalInput = nightGoalInput,
                onGoalChange = { nightGoalInput = it },
                isStarting = isStartingNightTask,
                onStartTask = { goal ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isStartingNightTask = true
                    scope.launch {
                        onShowSnackbar("Iniciando tarea nocturna supervisada...")
                        val success = pcBridge.startAntigravityNightTask(
                            goal = goal,
                            workspace = "f:/Dev/AsistenteCelular"
                        )
                        isStartingNightTask = false
                        if (success) {
                            nightGoalInput = ""
                            showNightTaskControls = false
                            onShowSnackbar("🌙 Tarea nocturna iniciada con rotación continua")
                        } else {
                            onShowSnackbar("❌ Error al iniciar tarea nocturna")
                        }
                    }
                },
                onStopTask = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val success = pcBridge.stopAntigravityNightTask("USER_REQUESTED")
                        if (success) {
                            onShowSnackbar("🛑 Tarea nocturna detenida")
                        } else {
                            onShowSnackbar("❌ Error al detener tarea nocturna")
                        }
                    }
                }
            )
        }
    }
}

/**
 * Fila atómica de presentación para una cuenta del Pool de Gemini Pro.
 */
@Composable
private fun PoolAccountRow(
    profile: AntigravityProfile,
    isSwitching: Boolean,
    onSwitch: () -> Unit
) {
    val isCurrent = profile.isActive
    val inCooldown = profile.inCooldown
    val hasCred = profile.hasCredential

    val borderColor = when {
        isCurrent -> NeonGreen.copy(alpha = 0.8f)
        inCooldown -> NeonAmber.copy(alpha = 0.5f)
        !hasCred -> NeonRed.copy(alpha = 0.4f)
        else -> VoidBorder
    }

    val backgroundColor = when {
        isCurrent -> NeonGreen.copy(alpha = 0.05f)
        else -> VoidSurfaceElevated
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    // Badge de estado
                    when {
                        isCurrent -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonGreen.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonGreen)
                            ) {
                                Text(
                                    text = "EN SESIÓN",
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        inCooldown -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonAmber.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonAmber)
                            ) {
                                val remainingMin = (profile.cooldownRemainingSeconds / 60).coerceAtLeast(1)
                                Text(
                                    text = "COOLDOWN ~${remainingMin}m",
                                    color = NeonAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        !hasCred -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonRed.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, NeonRed)
                            ) {
                                Text(
                                    text = "SIN LOGIN",
                                    color = NeonRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonPurple.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "LISTA",
                                    color = NeonLilac,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profile.email.isNotBlank()) {
                        Text(
                            text = profile.email,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (profile.usageCount > 0) {
                        Text(
                            text = "• ${profile.usageCount} turnos",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de acción Switch
            if (isCurrent) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Activa",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onSwitch,
                    enabled = !isSwitching && hasCred,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        disabledContainerColor = VoidBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isSwitching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Conmutar",
                            tint = if (hasCred) VoidBlack else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Conmutar",
                            color = if (hasCred) VoidBlack else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila que muestra una conversación activa en Antigravity.
 */
@Composable
private fun ActiveChatRow(chat: AntigravityChat) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = VoidSurfaceElevated,
        border = BorderStroke(1.dp, VoidBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chat.preview.isNotBlank()) {
                    Text(
                        text = chat.preview,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
            )
        }
    }
}

/**
 * Sección de telemetría y control del supervisor nocturno (Night Watchdog).
 */
@Composable
private fun NightWatchdogSection(
    status: NightTaskStatus?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    goalInput: String,
    onGoalChange: (String) -> Unit,
    isStarting: Boolean,
    onStartTask: (String) -> Unit,
    onStopTask: () -> Unit
) {
    val isRunning = status != null && status.status != "IDLE" && status.status != "STOPPED" && status.status != "COMPLETED"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = if (isRunning) NeonLilac else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SUPERVISOR NOCTURNO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) NeonLilac else TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isRunning) NeonGreen.copy(alpha = 0.2f) else VoidSurfaceElevated,
                    border = BorderStroke(1.dp, if (isRunning) NeonGreen else VoidBorder)
                ) {
                    Text(
                        text = status?.status ?: "IDLE",
                        color = if (isRunning) NeonGreen else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Si la tarea está corriendo, mostrar métricas clave
        if (isRunning && status != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = VoidSurfaceElevated,
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Meta en curso:",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = status.goal,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(label = "Turnos cuenta", value = "${status.currentTurns}/40")
                        MetricItem(label = "Turnos totales", value = "${status.totalTurns}")
                        MetricItem(label = "Rotaciones", value = "${status.rotationsCount}")
                        MetricItem(label = "Recuperaciones", value = "${status.loopRecoveriesCount}")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onStopTask,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp), tint = VoidBlack)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Detener Tarea Nocturna", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Formulario expandible para programar o iniciar una nueva tarea nocturna
        AnimatedVisibility(
            visible = isExpanded && !isRunning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = onGoalChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Objetivo de programación nocturna...", fontSize = 11.sp, color = TextMuted) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = VoidBorder,
                        focusedContainerColor = VoidSurfaceElevated,
                        unfocusedContainerColor = VoidSurfaceElevated
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onStartTask(goalInput) },
                    enabled = goalInput.isNotBlank() && !isStarting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        disabledContainerColor = VoidBorder
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (isStarting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VoidBlack, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = VoidBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Iniciar Tarea Nocturna Multicuenta", color = VoidBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonLilac)
    }
}

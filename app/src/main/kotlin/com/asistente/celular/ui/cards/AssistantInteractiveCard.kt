package com.asistente.celular.ui.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.ui.AssistantUiPayload
import com.asistente.celular.nlu.ui.BatteryUiPayload
import com.asistente.celular.nlu.ui.BrightnessUiPayload
import com.asistente.celular.nlu.ui.CurrencyUiPayload
import com.asistente.celular.nlu.ui.DrivingModeUiPayload
import com.asistente.celular.nlu.ui.EmergencySosUiPayload
import com.asistente.celular.nlu.ui.ExpenseReportUiPayload
import com.asistente.celular.nlu.ui.FlashlightUiPayload
import com.asistente.celular.nlu.ui.LocalFileResultsUiPayload
import com.asistente.celular.nlu.ui.NotesUiPayload
import com.asistente.celular.nlu.ui.OcrGlanceUiPayload
import com.asistente.celular.nlu.ui.Otp2FaUiPayload
import com.asistente.celular.nlu.ui.PomodoroWidgetPayload
import com.asistente.celular.nlu.ui.RoutineUiPayload
import com.asistente.celular.nlu.ui.ScreenVisionUiPayload
import com.asistente.celular.nlu.ui.SmartBulbUiPayload
import com.asistente.celular.nlu.ui.TallyCounterWidgetPayload
import com.asistente.celular.nlu.ui.VolumeUiPayload
import com.asistente.celular.nlu.ui.WhatsAppQuickReplyPayload
import com.asistente.celular.nlu.ui.VoiceprintUiPayload
import com.asistente.celular.nlu.ui.KnowledgeRagUiPayload
import com.asistente.celular.nlu.ui.VoiceJournalUiPayload
import com.asistente.celular.nlu.ui.MeshSyncUiPayload
import com.asistente.celular.nlu.ui.CallScreeningUiPayload
import com.asistente.celular.nlu.ui.InterpreterUiPayload
import com.asistente.celular.nlu.ui.ContextTriggerUiPayload
import com.asistente.celular.nlu.ui.AmbientDockUiPayload
import com.asistente.celular.nlu.ui.BatteryHealthUiPayload
import com.asistente.celular.nlu.ui.PrivacyFirewallUiPayload
import com.asistente.celular.nlu.ui.SecurityAuditUiPayload
import com.asistente.celular.nlu.ui.HardwareGestureUiPayload
import com.asistente.celular.nlu.ui.CameraDirectorUiPayload
import com.asistente.celular.nlu.ui.HealthTelemetryUiPayload
import com.asistente.celular.nlu.ui.TaskPlanUiPayload
import com.asistente.celular.nlu.ui.AutomotiveUiPayload
import com.asistente.celular.nlu.ui.WearCompanionUiPayload
import com.asistente.celular.nlu.ui.MeetingRecorderUiPayload
import com.asistente.celular.nlu.ui.MultiModelOrchestratorUiPayload
import com.asistente.celular.nlu.ui.DocumentChatUiPayload
import com.asistente.celular.nlu.ui.SoundscapeUiPayload
import com.asistente.celular.nlu.ui.VoiceCraftUiPayload
import com.asistente.celular.nlu.ui.PcWorkspaceUiPayload
import com.asistente.celular.nlu.ui.PcTaskApprovalUiPayload
import com.asistente.celular.nlu.ui.AntigravityNavigatorUiPayload
import com.asistente.celular.nlu.ui.DawControlUiPayload
import com.asistente.celular.nlu.ui.AiTrafficAuditUiPayload
import com.asistente.celular.nlu.ui.TimerStatusUiPayload
import com.asistente.celular.nlu.ui.ScreenCopilotGuideUiPayload
import com.asistente.celular.nlu.ui.WebSearchUiPayload
import com.asistente.celular.nlu.ui.AutomatedRoutineCreatedUiPayload
import com.asistente.celular.nlu.ui.EpisodicProjectUiPayload
import kotlin.math.roundToInt


/**
 * Dispatcher principal para renderizar tarjetas visuales interactivas en la ventana flotante
 * según el tipo de [AssistantUiPayload] emitido por la habilidad ejecutada.
 */
@Composable
fun AssistantInteractiveCard(
    payload: AssistantUiPayload?,
    onExecuteCommand: (String) -> Unit,
    onRequestBrightnessPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (payload == null) return

    Box(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        when (payload) {
            is SmartBulbUiPayload -> SmartBulbCard(payload, onExecuteCommand)
            is BatteryUiPayload -> BatteryCard(payload)
            is FlashlightUiPayload -> FlashlightCard(payload, onExecuteCommand)
            is VolumeUiPayload -> VolumeCard(payload, onExecuteCommand)
            is BrightnessUiPayload -> BrightnessCard(payload, onExecuteCommand, onRequestBrightnessPermission)
            is CurrencyUiPayload -> CurrencyCard(payload)
            is NotesUiPayload -> NotesCard(payload, onExecuteCommand)
            is RoutineUiPayload -> RoutineCard(payload, onExecuteCommand)
            is WhatsAppQuickReplyPayload -> WhatsAppReplyCard(payload, onExecuteCommand)
            is PomodoroWidgetPayload -> PomodoroCard(payload, onExecuteCommand)
            is TallyCounterWidgetPayload -> TallyCounterCard(payload, onExecuteCommand)
            is ScreenVisionUiPayload -> ScreenVisionCard(payload, onExecuteCommand)
            is OcrGlanceUiPayload -> OcrGlanceCard(payload, onExecuteCommand)
            is DrivingModeUiPayload -> DrivingModeCard(payload, onExecuteCommand)
            is ExpenseReportUiPayload -> ExpenseReportCard(payload, onExecuteCommand)
            is LocalFileResultsUiPayload -> LocalFilesCard(payload, onExecuteCommand)
            is EmergencySosUiPayload -> EmergencySosCard(payload, onExecuteCommand)
            is Otp2FaUiPayload -> Otp2FaCard(payload)
            is VoiceprintUiPayload -> VoiceprintCard(payload)
            is KnowledgeRagUiPayload -> KnowledgeRagCard(payload, onExecuteCommand)
            is VoiceJournalUiPayload -> VoiceJournalCard(payload, onExecuteCommand)
            is MeshSyncUiPayload -> MeshSyncCard(payload, onExecuteCommand)
            is CallScreeningUiPayload -> CallScreeningCard(payload, onExecuteCommand)
            is InterpreterUiPayload -> InterpreterCard(payload)
            is ContextTriggerUiPayload -> ContextTriggerCard(payload, onExecuteCommand)
            is AmbientDockUiPayload -> AmbientDockCard(payload, onExecuteCommand)
            is BatteryHealthUiPayload -> BatteryHealthCard(payload, onExecuteCommand)
            is PrivacyFirewallUiPayload -> PrivacyFirewallCard(payload)
            is SecurityAuditUiPayload -> SecurityAuditCard(payload, onExecuteCommand)
            is HardwareGestureUiPayload -> HardwareGestureCard(payload)
            is CameraDirectorUiPayload -> CameraDirectorCard(payload, onExecuteCommand)
            is HealthTelemetryUiPayload -> HealthTelemetryCard(payload)
            is TaskPlanUiPayload -> TaskPlanCard(payload, onExecuteCommand)
            is AutomotiveUiPayload -> AutomotiveCard(payload, onExecuteCommand)
            is WearCompanionUiPayload -> WearCompanionCard(payload)
            is MeetingRecorderUiPayload -> MeetingRecorderCard(payload, onExecuteCommand)
            is MultiModelOrchestratorUiPayload -> MultiModelOrchestratorCard(payload)
            is DocumentChatUiPayload -> DocumentChatCard(payload, onExecuteCommand)
            is SoundscapeUiPayload -> SoundscapeCard(payload, onExecuteCommand)
            is VoiceCraftUiPayload -> VoiceCraftCard(payload, onExecuteCommand)
            is PcWorkspaceUiPayload -> PcWorkspaceCard(payload, onExecuteCommand)
            is PcTaskApprovalUiPayload -> PcTaskApprovalCard(payload, onExecuteCommand)
            is AntigravityNavigatorUiPayload -> AntigravityNavigatorCard(payload, onExecuteCommand)
            is DawControlUiPayload -> DawControlCard(payload, onExecuteCommand)
            is AiTrafficAuditUiPayload -> AiTrafficAuditCard(payload, onExecuteCommand)
            is TimerStatusUiPayload -> TimerStatusCard(payload, onExecuteCommand)
            is ScreenCopilotGuideUiPayload -> ScreenCopilotGuideCard(payload, onExecuteCommand)
            is WebSearchUiPayload -> WebSearchCard(payload, onExecuteCommand)
            is AutomatedRoutineCreatedUiPayload -> AutomatedRoutineCreatedCard(payload, onExecuteCommand)
            is EpisodicProjectUiPayload -> EpisodicProjectCard(payload, onExecuteCommand)
        }

    }
}

/**
 * Tarjeta interactiva para focos inteligentes Xiaomi Yeelight.
 * Permite encendido/apagado, slider de brillo en tiempo real y chips para escenas y modos dinámicos.
 */
@Composable
fun SmartBulbCard(
    payload: SmartBulbUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    var sliderBrightness by remember(payload.brightness) {
        mutableFloatStateOf(payload.brightness.coerceIn(1, 100).toFloat())
    }
    var powerState by remember(payload.isPowerOn) {
        mutableStateOf(payload.isPowerOn)
    }

    val rgb = payload.colorRgb
    val bulbColor = when {
        !powerState -> Color(0xFF757575)
        rgb != null -> Color(rgb or -0x1000000)
        payload.activeMode == "candle" -> Color(0xFFFF9800)
        payload.activeMode == "party" -> Color(0xFFE91E63)
        payload.activeMode == "night" -> Color(0xFF5C6BC0)
        else -> Color(0xFFFFD54F)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila de cabecera: Ícono iluminado, nombre y Switch de energía
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(bulbColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.5.dp, bulbColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Foco",
                            tint = bulbColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = payload.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (powerState) "Encendido • ${sliderBrightness.roundToInt()}%" else "Apagado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = powerState,
                    onCheckedChange = { newState ->
                        powerState = newState
                        onExecuteCommand(if (newState) "enciende el foco" else "apaga el foco")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slider de brillo
            Text(
                text = "Brillo: ${sliderBrightness.roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sliderBrightness,
                onValueChange = { sliderBrightness = it },
                onValueChangeFinished = {
                    onExecuteCommand("pon el foco al ${sliderBrightness.roundToInt()}%")
                },
                valueRange = 1f..100f,
                enabled = powerState,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips de efectos y escenas rápidas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPresetChip(label = "Vela", icon = Icons.Default.Whatshot, onClick = { onExecuteCommand("modo vela") })
                QuickPresetChip(label = "Fiesta", icon = Icons.Default.Celebration, onClick = { onExecuteCommand("modo fiesta") })
                QuickPresetChip(label = "Noche", icon = Icons.Default.NightsStay, onClick = { onExecuteCommand("luz de noche") })
                QuickPresetChip(label = "Cálido", icon = Icons.Default.WbSunny, onClick = { onExecuteCommand("luz calida") })
                QuickPresetChip(label = "Frío", icon = Icons.Default.AcUnit, onClick = { onExecuteCommand("luz fria") })
                QuickPresetChip(label = "Detener", icon = Icons.Default.Stop, onClick = { onExecuteCommand("detener efecto") })
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        },
        label = { Text(text = label, fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    )
}

/**
 * Tarjeta interactiva de Batería con indicador visual animado y estado de carga.
 */
@Composable
fun BatteryCard(payload: BatteryUiPayload) {
    val levelColor = when {
        payload.percent > 50 -> Color(0xFF4CAF50)
        payload.percent > 20 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(levelColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                payload.isCharging -> Icons.Default.BatteryChargingFull
                                payload.percent <= 20 -> Icons.Default.BatteryAlert
                                else -> Icons.Default.BatteryFull
                            },
                            contentDescription = "Batería",
                            tint = levelColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nivel de Batería",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val chargeDesc = if (payload.isCharging) {
                            payload.chargeSource ?: "Cargando"
                        } else {
                            "Descargando"
                        }
                        Text(
                            text = chargeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${payload.percent}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = levelColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { (payload.percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = levelColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (payload.isPowerSaveMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Modo ahorro de energía activado",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva para la Linterna del dispositivo.
 */
@Composable
fun FlashlightCard(
    payload: FlashlightUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    var isOn by remember(payload.isOn) { mutableStateOf(payload.isOn) }
    val glowColor = if (isOn) Color(0xFFFFD54F) else Color(0xFF757575)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(glowColor.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, glowColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Linterna",
                        tint = glowColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Linterna",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOn) "Encendida" else "Apagada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    val next = !isOn
                    isOn = next
                    onExecuteCommand(if (next) "enciende la linterna" else "apaga la linterna")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = if (isOn) "Apagar" else "Encender", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tarjeta interactiva de Control de Volumen con deslizador y presets rápidos.
 */
@Composable
fun VolumeCard(
    payload: VolumeUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    var sliderVolume by remember(payload.percent) {
        mutableFloatStateOf(payload.percent.coerceIn(0, 100).toFloat())
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            sliderVolume == 0f -> Icons.Default.VolumeMute
                            sliderVolume > 60f -> Icons.Default.VolumeUp
                            else -> Icons.Default.VolumeDown
                        },
                        contentDescription = "Volumen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Volumen de ${payload.streamName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${sliderVolume.roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Slider(
                value = sliderVolume,
                onValueChange = { sliderVolume = it },
                onValueChangeFinished = {
                    onExecuteCommand("volumen de ${payload.streamName} al ${sliderVolume.roundToInt()}%")
                },
                valueRange = 0f..100f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickPresetChip(label = "Silencio", onClick = {
                    sliderVolume = 0f
                    onExecuteCommand("silencia el volumen")
                })
                QuickPresetChip(label = "50%", onClick = {
                    sliderVolume = 50f
                    onExecuteCommand("volumen al 50%")
                })
                QuickPresetChip(label = "100%", onClick = {
                    sliderVolume = 100f
                    onExecuteCommand("volumen al maximo")
                })
            }
        }
    }
}

/**
 * Tarjeta interactiva de Brillo de Pantalla con aviso de permiso WRITE_SETTINGS si aplica.
 */
@Composable
fun BrightnessCard(
    payload: BrightnessUiPayload,
    onExecuteCommand: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    var sliderBrightness by remember(payload.percent) {
        mutableFloatStateOf(payload.percent.coerceIn(1, 100).toFloat())
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            sliderBrightness > 70f -> Icons.Default.BrightnessHigh
                            sliderBrightness > 30f -> Icons.Default.BrightnessMedium
                            else -> Icons.Default.BrightnessLow
                        },
                        contentDescription = "Brillo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Brillo de Pantalla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${sliderBrightness.roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!payload.hasPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permiso requerido para modificar el brillo",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Habilitar", fontSize = 11.sp)
                    }
                }
            } else {
                Slider(
                    value = sliderBrightness,
                    onValueChange = { sliderBrightness = it },
                    onValueChangeFinished = {
                        onExecuteCommand("brillo al ${sliderBrightness.roundToInt()}%")
                    },
                    valueRange = 1f..100f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPresetChip(label = "25%", onClick = {
                        sliderBrightness = 25f
                        onExecuteCommand("brillo al 25%")
                    })
                    QuickPresetChip(label = "50%", onClick = {
                        sliderBrightness = 50f
                        onExecuteCommand("brillo al 50%")
                    })
                    QuickPresetChip(label = "75%", onClick = {
                        sliderBrightness = 75f
                        onExecuteCommand("brillo al 75%")
                    })
                    QuickPresetChip(label = "100%", onClick = {
                        sliderBrightness = 100f
                        onExecuteCommand("brillo al 100%")
                    })
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de Conversión de Monedas.
 */
@Composable
fun CurrencyCard(payload: CurrencyUiPayload) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyExchange,
                    contentDescription = "Divisas",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Conversión de Moneda",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${payload.amount} ${payload.fromCurrency}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = payload.fromName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Hacia",
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = payload.resultAmount,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = payload.toName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tasa de referencia: 1 ${payload.fromCurrency} ≈ ${payload.rate} ${payload.toCurrency}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Tarjeta interactiva de Notas Rápidas estilo Google Keep con acción de fijado y borrado.
 */
@Composable
fun NotesCard(
    payload: NotesUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    var isPinned by remember(payload.isPinned) { mutableStateOf(payload.isPinned) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = payload.title.ifBlank { "Nota Rápida" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = {
                        val targetTitle = payload.title.ifBlank { payload.content.take(20) }
                        isPinned = !isPinned
                        onExecuteCommand("fija la nota $targetTitle")
                    }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Desfijar" else "Fijar",
                            tint = if (isPinned) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        val targetTitle = payload.title.ifBlank { payload.content.take(20) }
                        onExecuteCommand("borra la nota $targetTitle")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = payload.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Tarjeta interactiva para Rutinas y Macros creadas en Lenguaje Natural (Punto 8).
 * Muestra el disparador por voz, lista de acciones secuenciales y botón de prueba inmediata.
 */
@Composable
fun RoutineCard(
    payload: RoutineUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Rutina",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = payload.name.ifBlank { "Rutina Personalizada" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Disparador: \"${payload.triggerPhrase}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Pasos de la rutina (${payload.actionsCount}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                payload.actionSummaries.forEachIndexed { index, summary ->
                    Text(
                        text = "${index + 1}. $summary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onExecuteCommand(payload.triggerPhrase) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Probar Rutina Ahora", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tarjeta interactiva para Ghostwriter de Respuestas Rápidas de WhatsApp (Punto 12).
 * Muestra el remitente, mensaje recibido y 3 opciones inteligentes generadas por IA.
 */
@Composable
fun WhatsAppReplyCard(
    payload: WhatsAppQuickReplyPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF25D366).copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WhatsApp • ${payload.senderName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${payload.messageSnippet}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Respuestas inteligentes sugeridas:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                payload.suggestedReplies.forEach { reply ->
                    OutlinedButton(
                        onClick = { onExecuteCommand("responde a ${payload.senderName}: $reply") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = reply, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/**
 * Widget interactivo de Pomodoro en la ventana flotante (Punto 25).
 */
@Composable
fun PomodoroCard(
    payload: PomodoroWidgetPayload,
    onExecuteCommand: (String) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE).copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Temporizador Pomodoro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                        Text(
                            text = if (payload.currentPhase == "work") "Sesión de Enfoque (${payload.workMinutes}m)" else "Descanso (${payload.breakMinutes}m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                    }
                }

                Text(
                    text = "${payload.workMinutes}:00",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB71C1C)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isRunning = !isRunning
                        onExecuteCommand(if (isRunning) "inicia pomodoro de ${payload.workMinutes} minutos" else "pausa el temporizador")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isRunning) "Pausar" else "Iniciar ${payload.workMinutes}m", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onExecuteCommand("inicia descanso de ${payload.breakMinutes} minutos")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Descanso ${payload.breakMinutes}m")
                }
            }
        }
    }
}

/**
 * Widget interactivo de Contador Rápido (Tally Counter) en la ventana flotante (Punto 25).
 */
@Composable
fun TallyCounterCard(
    payload: TallyCounterWidgetPayload,
    onExecuteCommand: (String) -> Unit
) {
    var count by remember(payload.currentCount) { mutableStateOf(payload.currentCount) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = payload.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$count",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val next = (count - payload.step).coerceAtLeast(0)
                        count = next
                        onExecuteCommand("cuenta ${payload.title} es $next")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Restar")
                }

                Button(
                    onClick = {
                        val next = count + payload.step
                        count = next
                        onExecuteCommand("cuenta ${payload.title} es $next")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar")
                }

                IconButton(
                    onClick = {
                        count = 0
                        onExecuteCommand("reinicia el contador de ${payload.title}")
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva para Visión de Pantalla en Vivo (Screen Understanding).
 */
@Composable
fun ScreenVisionCard(
    payload: ScreenVisionUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visión de Pantalla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                payload.packageName?.let {
                    Text(
                        text = it.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = payload.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onExecuteCommand("resume la pantalla") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resumir")
                }
                OutlinedButton(
                    onClick = { onExecuteCommand("traduce la pantalla") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Traducir")
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva para Camera Glance & OCR Offline.
 */
@Composable
fun OcrGlanceCard(
    payload: OcrGlanceUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Texto Detectado (OCR)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = payload.fullText.ifBlank { "Sin texto legible." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Tarjeta interactiva para Modo Conducción.
 */
@Composable
fun DrivingModeCard(
    payload: DrivingModeUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Modo Conducción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = if (payload.isActive) "● ACTIVO" else "DESACTIVADO",
                    color = if (payload.isActive) Color(0xFF81C784) else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            payload.connectedDeviceName?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Conectado a: $it",
                    color = Color(0xFF90CAF9),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (payload.isActive) {
                        onExecuteCommand("desactiva modo auto")
                    } else {
                        onExecuteCommand("activa modo auto")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (payload.isActive) Color(0xFFD32F2F) else Color(0xFF1976D2)
                )
            ) {
                Text(
                    text = if (payload.isActive) "Terminar Conducción" else "Activar Modo Conducción",
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Tarjeta interactiva de Finanzas y Reporte de Gastos Rápidos.
 */
@Composable
fun ExpenseReportCard(
    payload: ExpenseReportUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Finanzas Personales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$${"%.2f".format(payload.totalAmount)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = payload.currency,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (payload.recentExpenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Últimos movimientos:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                payload.recentExpenses.take(3).forEach { expense ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${expense.category.displayName}: ${expense.note}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "$${"%.2f".format(expense.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de Resultados de Búsqueda de Archivos Locales.
 */
@Composable
fun LocalFilesCard(
    payload: LocalFileResultsUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Archivos Encontrados (${payload.files.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            payload.files.take(4).forEach { file ->
                val sizeKb = (file.sizeBytes / 1024).coerceAtLeast(1)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${file.category.displayName} • ${sizeKb} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de Emergencia SOS Manos Libres.
 */
@Composable
fun EmergencySosCard(
    payload: EmergencySosUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0E0E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALERTA SOS ACTIVA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF8A80)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Linterna en código Morse transmitiendo\n• Ubicación GPS calculada y compartida",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onExecuteCommand("cancela emergencia") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Desactivar Alerta SOS", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tarjeta interactiva para Códigos 2FA OTP.
 */
@Composable
fun Otp2FaCard(
    payload: Otp2FaUiPayload
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Código de Seguridad 2FA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "De: ${payload.sender}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = payload.code,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Copiado automáticamente al portapapeles",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun VoiceprintCard(payload: VoiceprintUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Huella Vocal de Propietario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (payload.isMatch) "Identidad: ${payload.speakerName}" else "Voz no reconocida",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (payload.isMatch) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Coincidencia acústica", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${payload.confidencePercent}%",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { payload.confidencePercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = if (payload.isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun KnowledgeRagCard(
    payload: KnowledgeRagUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Grafo de Conocimiento & RAG",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Consulta: \"${payload.query}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            payload.results.take(3).forEach { res ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = res.entity?.name ?: "Dato semántico",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${(res.similarityScore * 100).toInt()}% sim",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = res.textSnippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceJournalCard(
    payload: VoiceJournalUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Diario de Voz & Mind Dump",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tono: ${payload.sentiment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = payload.summary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (payload.actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Acciones extraídas (TODOs):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                payload.actionItems.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MeshSyncCard(
    payload: MeshSyncUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Hendrix Mesh Local (P2P)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${payload.peersCount} nodo(s) en red LAN",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            payload.lastClipboardSnippet?.let { snippet ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Último portapapeles compartido: \"$snippet\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onExecuteCommand("enviar portapapeles a pc") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reenviar Portapapeles a PC")
            }
        }
    }
}

@Composable
fun CallScreeningCard(
    payload: CallScreeningUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Filtro de Llamadas IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${payload.callerName ?: payload.callerNumber} (${payload.statusText})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (payload.spamPercent >= 80) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Riesgo de Spam: ${payload.spamPercent}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = payload.liveSnippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InterpreterCard(payload: InterpreterUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Modo Intérprete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(text = "${payload.langA} ⇄ ${payload.langB}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "${payload.lastSpeaker}: \"${payload.lastOriginal}\"", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Traducción: \"${payload.lastTranslated}\"", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ContextTriggerCard(
    payload: ContextTriggerUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Contexto Proactivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Zona: ${payload.activeZone} (${payload.connectedWifi ?: "Sin Wi-Fi"})", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Reglas activas: ${payload.activeRulesCount}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AmbientDockCard(
    payload: AmbientDockUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Modo Ambient Dock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = payload.dockType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            payload.ambientMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun BatteryHealthCard(
    payload: BatteryHealthUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Salud Térmica de Batería", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Nivel: ${payload.level}% | Temp: ${payload.temperature}°C (${payload.thermalStatus})", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tasa de carga: ${payload.currentMa} mA | Límite corte inteligente: ${payload.smartCutoffTarget}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PrivacyFirewallCard(payload: PrivacyFirewallUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Firewall de Privacidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${payload.blockedTrackersCount} rastreadores bloqueados", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            payload.recentTrackers.take(2).forEach {
                Text(text = "• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SecurityAuditCard(
    payload: SecurityAuditUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Auditoría de Seguridad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Puntaje de salud: ${payload.securityScore}/100", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Apps con permisos críticos: ${payload.riskyAppsCount}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HardwareGestureCard(payload: HardwareGestureUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Gestos Físicos de Hardware", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Último: ${payload.lastDetectedGesture}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CameraDirectorCard(
    payload: CameraDirectorUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Asistente de Cámara", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Lente: ${payload.activeLens} | Cuenta: ${payload.countdownSeconds}s", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = payload.statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HealthTelemetryCard(payload: HealthTelemetryUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Hub de Salud Local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "${payload.steps} / ${payload.goalSteps} pasos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Pulso: ${payload.heartRate} lpm | Sueño: ${payload.sleepHours} hrs (${payload.recoveryText})", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TaskPlanCard(
    payload: TaskPlanUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Planificador Autónomo Multi-Paso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = payload.statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            payload.steps.forEach { step ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${step.stepNumber}. ${step.description}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "[${step.status.name}]", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AutomotiveCard(
    payload: AutomotiveUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Android Auto Hendrix Car", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (payload.isCarConnected) "Conectado a ${payload.headUnitName ?: "Vehículo"}" else "Consola desconectada",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (payload.isCarConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Plantilla: ${payload.currentTemplate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (payload.shortcuts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    payload.shortcuts.forEach { shortcut ->
                        OutlinedButton(
                            onClick = { onExecuteCommand("auto $shortcut") },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = shortcut, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WearCompanionCard(payload: WearCompanionUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Wear OS Companion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${payload.connectedWearCount} smartwatch(es) enlazado(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = payload.devicesSummary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Última sincronización: ${payload.lastSyncText}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MeetingRecorderCard(
    payload: MeetingRecorderUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Grabadora & Diarización", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (payload.isRecording) "Grabando: ${payload.meetingTitle}" else "Minuta: ${payload.meetingTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (payload.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Intervenciones: ${payload.turnsCount} | Último orador: ${payload.latestTurnSpeaker ?: "Ninguno"}",
                style = MaterialTheme.typography.bodySmall
            )
            if (payload.agreements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Acuerdos detectados:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                payload.agreements.take(3).forEach { agreement ->
                    Text(
                        text = "• $agreement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (payload.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onExecuteCommand("detener grabacion reunion") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "Finalizar y Generar Minuta", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun MultiModelOrchestratorCard(payload: MultiModelOrchestratorUiPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Orquestador Multi-Modelo IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (payload.fastPathActive) "Ruta Ultra-Rápida Activa (<300ms)" else "Ruta Profunda Especializada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Modelo: ${payload.selectedModel} | Latencia: ${payload.expectedLatencyMs}ms",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Complejidad detectada: ${payload.complexity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DocumentChatCard(
    payload: DocumentChatUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Chat con Documentos & PDFs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = payload.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Q: ${payload.question}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = payload.answer,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ref: ${payload.sectionReference} • Confianza: ${payload.confidencePercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SoundscapeCard(
    payload: SoundscapeUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Paisaje Sonoro & Ondas Binaurales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (payload.isPlaying) "Reproduciendo ${payload.soundscapeName}" else "Paisaje sonoro en pausa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Volumen: ${payload.volumePercent}%" + (payload.remainingMinutes?.let { " • $it min restantes" } ?: ""),
                style = MaterialTheme.typography.bodySmall
            )
            if (payload.isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onExecuteCommand("detener sonido") },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Detener Sonido", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun VoiceCraftCard(
    payload: VoiceCraftUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "VoiceCraft Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Perfil acústico: ${payload.styleName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tono: ${payload.pitchShift}x | Velocidad: ${payload.speechRate}x | Formante: ${payload.formantFactor}x",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onExecuteCommand("restablecer voz") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Restablecer Voz Original", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Tarjeta interactiva para la gestión y control rápido de la PC desde el flujo conversacional.
 */
@Composable
fun PcWorkspaceCard(
    payload: PcWorkspaceUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila 1: Cabecera con estado de conexión y host
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (payload.isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = payload.hostname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (payload.isConnected) "En Línea" else "Desconectado",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (payload.isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Ventana activa y telemetría
            val telemetry = payload.telemetry
            if (telemetry != null) {
                Spacer(modifier = Modifier.height(6.dp))
                if (telemetry.activeWindowTitle.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = telemetry.activeWindowTitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CPU: ${telemetry.cpuPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RAM: ${telemetry.ramPercent.toInt()}% (${telemetry.ramUsedGb.toInt()}G)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Volumen: ${telemetry.masterVolumePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onExecuteCommand("silencia la pc") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Mute", fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = { onExecuteCommand("pausa la musica en la pc") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Media", fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = { onExecuteCommand("bloquea la pc") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Bloquear", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onExecuteCommand("muestrame la pantalla de la pc") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Abrir Espacio de Trabajo (Workspace)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva para la aprobación y confirmación de planes autónomos (RPA).
 */
@Composable
fun PcTaskApprovalCard(
    payload: PcTaskApprovalUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    com.asistente.celular.ui.pc.PcTaskPlanApprovalCard(
        plan = payload.plan,
        onApproveAndExecute = { onExecuteCommand("aprobar plan de pc") },
        onCancel = { onExecuteCommand("cancelar plan de pc") }
    )
}

/**
 * Tarjeta interactiva para la Auditoría Local de Privacidad y Monitor de Tráfico de IA.
 * Renderiza métricas de soberanía local, ratio offline vs nube, entidades PII protegidas y transacciones.
 */
@Composable
fun AiTrafficAuditCard(
    payload: AiTrafficAuditUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Privacidad IA",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Soberanía de Datos & IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Monitor local de privacidad en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de progreso de soberanía
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ratio de Ejecución Local",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "${payload.localRatioPercentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (payload.localRatioPercentage >= 70.0) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (payload.localRatioPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (payload.localRatioPercentage >= 70.0) Color(0xFF10B981) else Color(0xFFF59E0B),
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Fila de estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditMetricChip(
                    title = "Locales",
                    value = "${payload.localRequests}",
                    subtext = "NPU / Device",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF10B981)
                )
                AuditMetricChip(
                    title = "Nube",
                    value = "${payload.cloudRequests}",
                    subtext = "Gemini / API",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF06B6D4)
                )
                AuditMetricChip(
                    title = "PII Sanitizados",
                    value = "${payload.piiProtectedCount}",
                    subtext = "Protegidos",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFFA855F7)
                )
            }

            if (payload.recentRecordsSummary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Últimas Transacciones:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                payload.recentRecordsSummary.forEach { summaryText ->
                    Text(
                        text = "• $summaryText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para limpiar
            OutlinedButton(
                onClick = { onExecuteCommand("limpiar auditoría de IA") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF94A3B8)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Limpiar Registro de Auditoría", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AuditMetricChip(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
            Text(text = subtext, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun TimerStatusCard(
    payload: TimerStatusUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    val total = if (payload.totalDurationSeconds > 0) payload.totalDurationSeconds else 1L
    val progress = (payload.remainingSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val mins = payload.remainingSeconds / 60
    val secs = payload.remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    val stateColor = when {
        payload.isRinging -> Color(0xFFEF4444)
        payload.state == com.asistente.celular.nlu.timer.TimerState.PAUSED -> Color(0xFFF59E0B)
        else -> Color(0xFF06B6D4)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, stateColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = payload.label.ifBlank { "Temporizador" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .background(stateColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            payload.isRinging -> "¡ALERTA!"
                            payload.state == com.asistente.celular.nlu.timer.TimerState.PAUSED -> "PAUSADO"
                            else -> "CORRIENDO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display grande de tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = stateColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = stateColor,
                trackColor = Color(0xFF334155),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (payload.isRinging) {
                    Button(
                        onClick = { onExecuteCommand("apaga la alarma") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Detener Alarma", fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (payload.state == com.asistente.celular.nlu.timer.TimerState.RUNNING) {
                        OutlinedButton(
                            onClick = { onExecuteCommand("pausa el temporizador") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pausar")
                        }
                    } else {
                        Button(
                            onClick = { onExecuteCommand("reanuda el temporizador") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reanudar")
                        }
                    }

                    OutlinedButton(
                        onClick = { onExecuteCommand("cancela el temporizador") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenCopilotGuideCard(
    payload: ScreenCopilotGuideUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👁️", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Screen Copilot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                val activeWin = payload.activeWindow
                if (!activeWin.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = activeWin.take(24),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA78BFA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = payload.guidanceTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0)
            )

            if (!payload.targetAreaDescription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1B4B), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "📍 Ubicación: ${payload.targetAreaDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC7D2FE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pasos guiados
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                payload.guidanceSteps.forEachIndexed { idx, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color(0xFF8B5CF6), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            if (!payload.recommendedShortcut.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onExecuteCommand("presiona ${payload.recommendedShortcut} en la pc") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Ejecutar Atajo: ${payload.recommendedShortcut}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WebSearchCard(
    payload: WebSearchUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌐", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Investigación en Internet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = payload.spokenAnswer,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0)
            )

            if (payload.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Fuentes consultadas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    payload.sources.take(3).forEach { src ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = src.title.take(35),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = src.sourceName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutomatedRoutineCreatedCard(
    payload: AutomatedRoutineCreatedUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = payload.routineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("ACTIVA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Disparador: ${payload.triggerSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA7F3D0)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                payload.actionsSummary.forEach { act ->
                    Text(text = "• $act", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onExecuteCommand("ejecutar rutina ${payload.routineName}") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Probar Rutina Ahora", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EpisodicProjectCard(
    payload: EpisodicProjectUiPayload,
    onExecuteCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEC4899).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎨", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = payload.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = payload.lastActiveFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF472B6)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Proyecto: ${payload.projectName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFCE7F3)
            )

            val path = payload.filePath
            if (!path.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = path,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onExecuteCommand("abrir proyecto ${payload.projectName} en la pc") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
            ) {
                Text("Reanudar Proyecto en PC", fontWeight = FontWeight.Bold)
            }
        }
    }
}




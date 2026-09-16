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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
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
import com.asistente.celular.nlu.ui.FlashlightUiPayload
import com.asistente.celular.nlu.ui.NotesUiPayload
import com.asistente.celular.nlu.ui.SmartBulbUiPayload
import com.asistente.celular.nlu.ui.VolumeUiPayload
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
                QuickPresetChip(label = "🕯️ Vela", onClick = { onExecuteCommand("modo vela") })
                QuickPresetChip(label = "🎉 Fiesta", onClick = { onExecuteCommand("modo fiesta") })
                QuickPresetChip(label = "🌙 Noche", onClick = { onExecuteCommand("luz de noche") })
                QuickPresetChip(label = "☀️ Cálido", onClick = { onExecuteCommand("luz calida") })
                QuickPresetChip(label = "❄️ Frío", onClick = { onExecuteCommand("luz fria") })
                QuickPresetChip(label = "⏹️ Detener", onClick = { onExecuteCommand("detener efecto") })
            }
        }
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
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
                Text(
                    text = "⚡ Modo ahorro de energía activado",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.SemiBold
                )
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

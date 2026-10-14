package com.asistente.celular.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.schedule.ScheduleActivityItem
import com.asistente.celular.nlu.schedule.ScheduleDay
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Función pura desacoplada para evaluar y encontrar la rutina/actividad de horario en curso.
 * Garantiza pruebas unitarias exhaustivas con 100% de cobertura y cero dependencias de UI.
 */
fun findOngoingRoutine(
    activities: List<ScheduleActivityItem>,
    currentDay: ScheduleDay,
    currentTimeStr: String
): ScheduleActivityItem? {
    return activities.firstOrNull { it.isOngoingAt(currentDay, currentTimeStr) }
}

/**
 * Tirita discreta y elegante para la pantalla principal de Hendrix Assistant.
 * Si hay una rutina/actividad en la hora actual, se muestra con el nombre y la hora de finalización.
 */
@Composable
fun OngoingRoutineStrip(
    activities: List<ScheduleActivityItem>,
    onClick: () -> Unit = {},
    onRoutineClick: ((ScheduleActivityItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    var currentDay by remember {
        mutableStateOf(ScheduleDay.currentDay())
    }

    // Actualización periódica reactiva cada 30 segundos
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDay = ScheduleDay.currentDay()
        }
    }

    val ongoing = remember(activities, currentTimeStr, currentDay) {
        findOngoingRoutine(activities, currentDay, currentTimeStr)
    }

    AnimatedVisibility(
        visible = ongoing != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (ongoing != null) {
            val accentColor = remember(ongoing.colorHex) {
                runCatching {
                    Color(android.graphics.Color.parseColor(ongoing.colorHex))
                }.getOrDefault(NeonCyan)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (onRoutineClick != null) {
                            onRoutineClick(ongoing)
                        } else {
                            onClick()
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = VoidSurfaceElevated.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Indicador de estado en vivo
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Nombre de la rutina
                        Text(
                            text = ongoing.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Separador discreto
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Hora de finalización
                        Text(
                            text = "Finaliza ${ongoing.endTime}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver horario",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

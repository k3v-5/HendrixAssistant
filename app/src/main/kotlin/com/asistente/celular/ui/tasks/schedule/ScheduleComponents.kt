package com.asistente.celular.ui.tasks.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.asistente.celular.ui.theme.NeonAmber
import com.asistente.celular.ui.theme.NeonCyan
import com.asistente.celular.ui.theme.NeonGreen
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.NeonRed
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fila selectora de días para filtrar la visualización del horario.
 * Destaca con badge visual el día actual de la semana.
 */
@Composable
fun ScheduleDaySelectorRow(
    selectedDay: ScheduleDay?,
    onSelectDay: (ScheduleDay?) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDay = remember { ScheduleDay.currentDay() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Opción "TODOS"
        val isAllSelected = selectedDay == null
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isAllSelected) VoidSurfaceElevated else VoidSurface,
            border = BorderStroke(1.dp, if (isAllSelected) NeonCyan else VoidBorder),
            modifier = Modifier.clickable { onSelectDay(null) }
        ) {
            Text(
                text = "TODOS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isAllSelected) NeonCyan else TextMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        ScheduleDay.entries.forEach { day ->
            val isSelected = selectedDay == day
            val isToday = day == currentDay

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) VoidSurfaceElevated else VoidSurface,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) NeonCyan else if (isToday) NeonPurple.copy(alpha = 0.5f) else VoidBorder
                ),
                modifier = Modifier.clickable { onSelectDay(day) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.shortLabel,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else TextPrimary
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta en vivo que detecta la actividad en curso en el momento exacto
 * o la siguiente actividad programada para hoy.
 */
@Composable
fun ScheduleLiveActivityCard(
    activities: List<ScheduleActivityItem>,
    modifier: Modifier = Modifier
) {
    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    val currentDay = remember { ScheduleDay.currentDay() }

    // Actualizar reloj cada 30 segundos
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    val ongoingActivity = remember(activities, currentTimeStr, currentDay) {
        activities.firstOrNull { it.isOngoingAt(currentDay, currentTimeStr) }
    }

    val nextActivity = remember(activities, currentTimeStr, currentDay, ongoingActivity) {
        if (ongoingActivity != null) null
        else {
            activities
                .filter { it.isEnabled && it.occursOn(currentDay) && it.startTime > currentTimeStr }
                .minByOrNull { it.startTime }
        }
    }

    if (ongoingActivity != null) {
        val accentColor = runCatching {
            Color(android.graphics.Color.parseColor(ongoingActivity.colorHex))
        }.getOrDefault(NeonCyan)

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = VoidSurfaceElevated,
            border = BorderStroke(1.5.dp, accentColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EN CURSO AHORA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = NeonGreen
                        )
                    }
                    Text(
                        text = "${ongoingActivity.startTime} - ${ongoingActivity.endTime}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = ongoingActivity.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (ongoingActivity.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ongoingActivity.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = ongoingActivity.category,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (ongoingActivity.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = ongoingActivity.location, fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    } else if (nextActivity != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = VoidSurface,
            border = BorderStroke(1.dp, VoidBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PRÓXIMA ACTIVIDAD HOY",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = nextActivity.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
                Text(
                    text = nextActivity.startTime,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber
                )
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar una actividad de horario.
 */
@Composable
fun ScheduleActivityItemCard(
    activity: ScheduleActivityItem,
    onToggleEnabled: (String) -> Unit,
    onEditActivity: (ScheduleActivityItem) -> Unit,
    onDeleteActivity: (String) -> Unit,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(activity.colorHex) {
        runCatching {
            Color(android.graphics.Color.parseColor(activity.colorHex))
        }.getOrDefault(NeonCyan)
    }

    val isDimmed = !activity.isEnabled

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDimmed) VoidSurface.copy(alpha = 0.5f) else VoidSurface,
        border = BorderStroke(
            if (isHighlighted) 2.dp else 1.dp,
            if (isHighlighted) accentColor else if (isDimmed) VoidBorder.copy(alpha = 0.4f) else VoidBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Franja lateral con el color temático
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDimmed) TextMuted else accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información central
            Column(modifier = Modifier.weight(1f)) {
                // Fila de Horario y Categoría
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${activity.startTime} - ${activity.endTime}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isDimmed) TextMuted else accentColor
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = (if (isDimmed) TextMuted else accentColor).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = activity.category,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDimmed) TextMuted else accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = activity.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDimmed) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (activity.description.isNotBlank()) {
                    Text(
                        text = activity.description,
                        fontSize = 11.sp,
                        color = if (isDimmed) TextMuted.copy(alpha = 0.6f) else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Días de repetición y ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysSummary = if (activity.daysOfWeek.size == 7) {
                        "Todos los días"
                    } else if (activity.daysOfWeek == ScheduleDay.WEEKDAYS) {
                        "Lun - Vie"
                    } else if (activity.daysOfWeek == ScheduleDay.WEEKEND) {
                        "Fin de semana"
                    } else {
                        activity.daysOfWeek.sortedBy { it.dayIndex }.joinToString(", ") { it.shortLabel }
                    }

                    Text(
                        text = daysSummary,
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )

                    if (activity.location.isNotBlank()) {
                        Text(
                            text = "• ${activity.location}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Acciones: Toggle enabled, editar y borrar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onEditActivity(activity) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { onDeleteActivity(activity.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = NeonRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Switch(
                    checked = activity.isEnabled,
                    onCheckedChange = { onToggleEnabled(activity.id) },
                    modifier = Modifier.size(36.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = VoidBlack,
                        checkedTrackColor = accentColor,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = VoidSurfaceElevated
                    )
                )
            }
        }
    }
}

/**
 * Contenido principal de la pestaña de Horarios dentro de TasksScreen.
 */
@Composable
fun ScheduleTabContent(
    activities: List<ScheduleActivityItem>,
    onAddActivity: (ScheduleActivityItem) -> Unit,
    onUpdateActivity: (ScheduleActivityItem) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onToggleEnabled: (String) -> Unit,
    onOpenAddDialog: () -> Unit,
    initialActivityId: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilterDay by remember { mutableStateOf<ScheduleDay?>(ScheduleDay.currentDay()) }
    var activityToEdit by remember { mutableStateOf<ScheduleActivityItem?>(null) }

    LaunchedEffect(initialActivityId, activities) {
        if (initialActivityId != null) {
            val target = activities.firstOrNull { it.id == initialActivityId }
            if (target != null) {
                val currentDay = ScheduleDay.currentDay()
                if (target.occursOn(currentDay)) {
                    selectedFilterDay = currentDay
                } else {
                    selectedFilterDay = target.daysOfWeek.firstOrNull() ?: currentDay
                }
                activityToEdit = target
            }
        }
    }

    val filteredActivities = remember(activities, selectedFilterDay) {
        val list = if (selectedFilterDay == null) {
            activities
        } else {
            activities.filter { it.occursOn(selectedFilterDay!!) }
        }
        list.sortedBy { it.startTime }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Selector horizontal de días
        ScheduleDaySelectorRow(
            selectedDay = selectedFilterDay,
            onSelectDay = { selectedFilterDay = it }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tarjeta en vivo para actividad en curso hoy
        ScheduleLiveActivityCard(activities = activities)

        Spacer(modifier = Modifier.height(12.dp))

        // Lista de actividades o estado vacío
        if (filteredActivities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VoidSurface,
                    border = BorderStroke(1.dp, VoidBorder),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedFilterDay != null) {
                                "Sin actividades el ${selectedFilterDay?.fullLabel}"
                            } else {
                                "No tienes actividades programadas"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registra tus bloques de trabajo, estudio, descanso o entrenamiento.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenAddDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AGREGAR ACTIVIDAD",
                                color = VoidBlack,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedFilterDay != null) {
                                "ACTIVIDADES (${selectedFilterDay?.fullLabel?.uppercase()})"
                            } else {
                                "TODAS LAS ACTIVIDADES"
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "${filteredActivities.size} BLOQUES",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        )
                    }
                }

                items(filteredActivities, key = { it.id }) { activity ->
                    ScheduleActivityItemCard(
                        activity = activity,
                        onToggleEnabled = onToggleEnabled,
                        onEditActivity = { activityToEdit = it },
                        onDeleteActivity = onDeleteActivity,
                        isHighlighted = activity.id == initialActivityId
                    )
                }
            }
        }
    }

    // Modal de edición si se seleccionó una actividad
    if (activityToEdit != null) {
        ScheduleActivityDialog(
            initialActivity = activityToEdit,
            onDismissRequest = { activityToEdit = null },
            onSaveActivity = { updated ->
                onUpdateActivity(updated)
                activityToEdit = null
            }
        )
    }
}

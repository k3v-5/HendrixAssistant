package com.asistente.celular.ui.tasks.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.asistente.celular.ui.theme.TextMuted
import com.asistente.celular.ui.theme.TextPrimary
import com.asistente.celular.ui.theme.TextSecondary
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import kotlinx.coroutines.launch

/**
 * Vista de Calendario Semanal completo para visualizar, filtrar y gestionar todas las rutinas
 * y actividades a lo largo de los 7 días de la semana (Lunes a Domingo).
 * 
 * Sigue los principios de diseño OLED Void & Neon Monolith y arquitectura reactiva.
 */
@Composable
fun ScheduleWeekCalendarView(
    activities: List<ScheduleActivityItem>,
    onEditActivity: (ScheduleActivityItem) -> Unit,
    onToggleEnabled: (String) -> Unit,
    onAddNewActivityForDay: (ScheduleDay) -> Unit,
    highlightedActivityId: String? = null,
    modifier: Modifier = Modifier
) {
    val currentDay = remember { ScheduleDay.currentDay() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Mapeo ordenado de actividades por día de la semana
    val activitiesByDay = remember(activities) {
        ScheduleDay.entries.associateWith { day ->
            activities
                .filter { it.occursOn(day) }
                .sortedBy { it.startTime }
        }
    }

    val totalRoutinesCount = activities.size
    val activeRoutinesCount = remember(activities) { activities.count { it.isEnabled } }

    // Calcular índices de cabecera de cada día para salto directo al pulsar en el strip
    val dayItemIndices = remember(activitiesByDay) {
        val map = mutableMapOf<ScheduleDay, Int>()
        var currentIndex = 1 // 0 es el resumen superior
        ScheduleDay.entries.forEach { day ->
            map[day] = currentIndex
            val dayActivities = activitiesByDay[day] ?: emptyList()
            currentIndex += 1 // Cabecera del día
            currentIndex += if (dayActivities.isEmpty()) 1 else dayActivities.size // Filas
        }
        map
    }

    // Auto-scroll hacia la actividad resaltada si existe
    LaunchedEffect(highlightedActivityId, activities) {
        if (highlightedActivityId != null) {
            val target = activities.firstOrNull { it.id == highlightedActivityId }
            if (target != null) {
                val day = if (target.occursOn(currentDay)) currentDay else target.daysOfWeek.firstOrNull() ?: currentDay
                val targetIndex = dayItemIndices[day]
                if (targetIndex != null) {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tira superior interactiva de días de la semana con mini-indicadores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScheduleDay.entries.forEach { day ->
                val isToday = day == currentDay
                val dayActivities = activitiesByDay[day] ?: emptyList()
                val hasActivities = dayActivities.isNotEmpty()

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isToday) VoidSurfaceElevated else VoidSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isToday) NeonGreen.copy(alpha = 0.8f) else if (hasActivities) NeonPurple.copy(alpha = 0.4f) else VoidBorder
                    ),
                    modifier = Modifier
                        .clickable {
                            val targetIndex = dayItemIndices[day]
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(targetIndex)
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = day.shortLabel,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                                color = if (isToday) NeonGreen else TextPrimary
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${dayActivities.size}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (hasActivities) NeonCyan else TextMuted
                        )

                        // Puntos de color por categoría
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val dotColors = dayActivities.take(3).map { item ->
                                runCatching {
                                    Color(android.graphics.Color.parseColor(item.colorHex))
                                }.getOrDefault(NeonCyan)
                            }
                            if (dotColors.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(TextMuted.copy(alpha = 0.3f))
                                )
                            } else {
                                dotColors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Lista vertical con la agenda completa de la semana dividida por días
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tarjeta de resumen global de la semana
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VoidSurface,
                    border = BorderStroke(1.dp, VoidBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "CALENDARIO SEMANAL",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$totalRoutinesCount bloques configurados ($activeRoutinesCount activos)",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoidSurfaceElevated,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "7 DÍAS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Secciones por cada día de la semana
            ScheduleDay.entries.forEach { day ->
                val isToday = day == currentDay
                val dayActivities = activitiesByDay[day] ?: emptyList()

                // Encabezado de la sección del día
                item(key = "header_${day.name}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = day.fullLabel.uppercase(),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (isToday) NeonGreen else TextPrimary
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = NeonGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "HOY",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (dayActivities.isEmpty()) "0 rutinas" else "${dayActivities.size} rutinas",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = VoidSurfaceElevated,
                                border = BorderStroke(1.dp, VoidBorder),
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { onAddNewActivityForDay(day) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Añadir a ${day.fullLabel}",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (dayActivities.isEmpty()) {
                    item(key = "empty_${day.name}") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddNewActivityForDay(day) },
                            shape = RoundedCornerShape(10.dp),
                            color = VoidSurface.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, VoidBorder.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sin rutinas para el ${day.fullLabel.lowercase()}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "+ Añadir",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                } else {
                    items(dayActivities, key = { "${day.name}_${it.id}" }) { activity ->
                        val isHighlighted = activity.id == highlightedActivityId
                        WeekCalendarRoutineCard(
                            activity = activity,
                            onEditActivity = onEditActivity,
                            onToggleEnabled = onToggleEnabled,
                            isHighlighted = isHighlighted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar un bloque de rutina en el Calendario Semanal.
 * - Sin botones de borrar ni editar redundantes en la tarjeta.
 * - Un solo toque sobre la tarjeta abre el diálogo de edición (donde también se puede eliminar).
 * - Switch directo para activar/desactivar.
 */
@Composable
fun WeekCalendarRoutineCard(
    activity: ScheduleActivityItem,
    onEditActivity: (ScheduleActivityItem) -> Unit,
    onToggleEnabled: (String) -> Unit,
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
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditActivity(activity) },
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlighted) accentColor.copy(alpha = 0.12f) else if (isDimmed) VoidSurface.copy(alpha = 0.4f) else VoidSurface,
        border = BorderStroke(
            if (isHighlighted) 2.dp else 1.dp,
            if (isHighlighted) accentColor else if (isDimmed) VoidBorder.copy(alpha = 0.4f) else VoidBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Franja lateral con el color temático
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isDimmed) TextMuted else accentColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${activity.startTime} - ${activity.endTime}",
                        fontSize = 11.sp,
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

                    if (activity.isReminderEnabled) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Recordatorio activo",
                            tint = NeonAmber,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    if (isHighlighted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonGreen.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "EN CURSO",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = activity.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDimmed) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (activity.location.isNotBlank() || activity.description.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        if (activity.location.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = activity.location,
                                fontSize = 10.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (activity.location.isNotBlank() && activity.description.isNotBlank()) {
                            Text(text = " • ", fontSize = 10.sp, color = TextMuted)
                        }
                        if (activity.description.isNotBlank()) {
                            Text(
                                text = activity.description,
                                fontSize = 10.sp,
                                color = if (isDimmed) TextMuted.copy(alpha = 0.6f) else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Switch directo de activación
            Switch(
                checked = activity.isEnabled,
                onCheckedChange = { onToggleEnabled(activity.id) },
                modifier = Modifier.size(34.dp),
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

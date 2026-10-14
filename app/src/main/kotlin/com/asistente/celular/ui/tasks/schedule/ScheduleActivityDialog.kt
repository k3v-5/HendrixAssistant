package com.asistente.celular.ui.tasks.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Diálogo interactivo para crear o editar una actividad en el horario.
 * Soporta selección de horas con TimePickerDialog, días recurrentes, categorías y colores.
 */
@Composable
fun ScheduleActivityDialog(
    initialActivity: ScheduleActivityItem? = null,
    defaultDay: ScheduleDay = ScheduleDay.currentDay(),
    onDismissRequest: () -> Unit,
    onSaveActivity: (ScheduleActivityItem) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialActivity?.title ?: "") }
    var description by remember { mutableStateOf(initialActivity?.description ?: "") }
    var location by remember { mutableStateOf(initialActivity?.location ?: "") }
    var startTime by remember { mutableStateOf(initialActivity?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(initialActivity?.endTime ?: "10:30") }
    var selectedDays by remember {
        mutableStateOf(initialActivity?.daysOfWeek ?: setOf(defaultDay))
    }
    var category by remember { mutableStateOf(initialActivity?.category ?: ScheduleActivityItem.DEFAULT_CATEGORY) }
    var colorHex by remember { mutableStateOf(initialActivity?.colorHex ?: ScheduleActivityItem.DEFAULT_COLOR) }
    var isReminderEnabled by remember { mutableStateOf(initialActivity?.isReminderEnabled ?: false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = initialActivity != null

    fun showTimePicker(initialTime: String, onTimeSelected: (String) -> Unit) {
        val parts = initialTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format("%02d:%02d", hourOfDay, minute)
                onTimeSelected(formatted)
            },
            h,
            m,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = VoidSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isEditing) "EDITAR ACTIVIDAD" else "NUEVA ACTIVIDAD",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = "Horario semanal de actividades",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campo Título
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Título de la actividad *", color = TextSecondary) },
                    placeholder = { Text("Ej. Sesión de Enfoque, Gimnasio, Clase...") },
                    singleLine = true,
                    isError = errorMessage != null && title.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = VoidBorder,
                        focusedContainerColor = VoidSurface,
                        unfocusedContainerColor = VoidSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Selectores de Hora de Inicio y Fin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hora Inicio
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showTimePicker(startTime) { startTime = it }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = VoidSurface,
                        border = BorderStroke(1.dp, VoidBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("INICIO", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = startTime,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Hora Fin
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showTimePicker(endTime) { endTime = it }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = VoidSurface,
                        border = BorderStroke(1.dp, VoidBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("FIN", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = endTime,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Días de repetición
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DÍAS DE REPETICIÓN",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "L-V",
                                fontSize = 10.sp,
                                color = NeonCyan,
                                modifier = Modifier
                                    .clickable { selectedDays = ScheduleDay.WEEKDAYS }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "Finde",
                                fontSize = 10.sp,
                                color = NeonPurple,
                                modifier = Modifier
                                    .clickable { selectedDays = ScheduleDay.WEEKEND }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "Todos",
                                fontSize = 10.sp,
                                color = NeonGreen,
                                modifier = Modifier
                                    .clickable { selectedDays = ScheduleDay.ALL_DAYS }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScheduleDay.entries.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else VoidSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else VoidBorder
                                ),
                                modifier = Modifier
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                                        } else {
                                            selectedDays + day
                                        }
                                    }
                            ) {
                                Text(
                                    text = day.shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Selector de Categoría
                Column {
                    Text(
                        text = "CATEGORÍA",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScheduleActivityItem.PRESET_CATEGORIES.forEach { cat ->
                            val isCatSelected = category == cat
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCatSelected) VoidSurfaceElevated else VoidSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCatSelected) NeonPurple else VoidBorder
                                ),
                                modifier = Modifier.clickable { category = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = if (isCatSelected) NeonPurple else TextMuted,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Paleta de Colores
                Column {
                    Text(
                        text = "COLOR DISTINTIVO",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScheduleActivityItem.PRESET_COLORS.forEach { hex ->
                            val parsedColor = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(NeonCyan)
                            val isColSelected = colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (isColSelected) 2.dp else 0.dp,
                                        color = if (isColSelected) TextPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }

                // Descripción / Notas
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o notas (opcional)", color = TextSecondary) },
                    placeholder = { Text("Ej. Metas de la sesión, temas a repasar...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = VoidBorder,
                        focusedContainerColor = VoidSurface,
                        unfocusedContainerColor = VoidSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Ubicación
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lugar / Ubicación (opcional)", color = TextSecondary) },
                    placeholder = { Text("Ej. Escritorio PC, Gimnasio, Sala...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = VoidBorder,
                        focusedContainerColor = VoidSurface,
                        unfocusedContainerColor = VoidSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Switch de Recordatorio anticipado
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VoidSurface,
                    border = BorderStroke(1.dp, VoidBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = if (isReminderEnabled) NeonAmber else TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Aviso anticipado", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Notificar 10 min antes de iniciar", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isReminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = NeonAmber,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = VoidSurfaceElevated
                            )
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF3366),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Por favor ingresa un título para la actividad."
                        return@Button
                    }
                    if (selectedDays.isEmpty()) {
                        errorMessage = "Debes seleccionar al menos un día de la semana."
                        return@Button
                    }
                    val activity = initialActivity?.copy(
                        title = title.trim(),
                        description = description.trim(),
                        location = location.trim(),
                        startTime = startTime,
                        endTime = endTime,
                        daysOfWeek = selectedDays,
                        category = category.trim(),
                        colorHex = colorHex,
                        isReminderEnabled = isReminderEnabled,
                        updatedAt = System.currentTimeMillis()
                    ) ?: ScheduleActivityItem(
                        title = title.trim(),
                        description = description.trim(),
                        location = location.trim(),
                        startTime = startTime,
                        endTime = endTime,
                        daysOfWeek = selectedDays,
                        category = category.trim(),
                        colorHex = colorHex,
                        isReminderEnabled = isReminderEnabled
                    )
                    onSaveActivity(activity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isEditing) "GUARDAR CAMBIOS" else "CREAR ACTIVIDAD",
                    color = VoidBlack,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar", color = TextSecondary, fontSize = 12.sp)
            }
        }
    )
}

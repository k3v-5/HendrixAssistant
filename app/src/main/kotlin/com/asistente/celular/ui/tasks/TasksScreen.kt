package com.asistente.celular.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asistente.celular.nlu.tasks.TaskItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    onToggleTask: (String) -> Unit,
    onAddTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit
) {
    var selectedList by remember { mutableStateOf(TaskItem.DEFAULT_LIST) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCompletedSection by remember { mutableStateOf(true) }

    val allLists = remember(tasks) {
        val lists = tasks.map { it.listName }.distinct().toMutableList()
        if (!lists.contains(TaskItem.DEFAULT_LIST)) {
            lists.add(0, TaskItem.DEFAULT_LIST)
        }
        lists
    }

    val filteredTasks = tasks.filter { it.listName == selectedList }
    val pendingTasks = filteredTasks.filter { !it.isCompleted }
    val completedTasks = filteredTasks.filter { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tareas",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (completedTasks.isNotEmpty()) {
                        IconButton(onClick = onClearCompleted) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Limpiar completadas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir tarea")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selector de listas horizontal (Google Tasks style)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allLists) { listName ->
                    FilterChip(
                        selected = selectedList == listName,
                        onClick = { selectedList = listName },
                        label = { Text(listName) }
                    )
                }
            }

            if (pendingTasks.isEmpty() && completedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay tareas en '$selectedList'",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca el botón + o pídele a Hendrix por voz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                ) {
                    // Tareas pendientes
                    items(pendingTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }

                    // Tareas completadas
                    if (completedTasks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCompletedSection = !showCompletedSection }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completadas (${completedTasks.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (showCompletedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (showCompletedSection) {
                            items(completedTasks, key = { it.id }) { task ->
                                TaskRow(
                                    task = task,
                                    onToggle = { onToggleTask(task.id) },
                                    onDelete = { onDeleteTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            currentList = selectedList,
            onDismiss = { showAddDialog = false },
            onConfirm = { newTask ->
                onAddTask(newTask)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TaskRow(
    task: TaskItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val checkColor by animateColorAsState(
        targetValue = if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "taskCheckColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox circular estilo Google Tasks
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(checkColor)
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completada",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.dueDateMillis != null || task.reminderMillis != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TaskDueBadge(
                        dueDateMillis = task.dueDateMillis,
                        hasReminder = task.reminderMillis != null,
                        isCompleted = task.isCompleted
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Eliminar tarea",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TaskDueBadge(
    dueDateMillis: Long?,
    hasReminder: Boolean,
    isCompleted: Boolean
) {
    if (dueDateMillis == null) return

    val now = System.currentTimeMillis()
    val isOverdue = dueDateMillis < now && !isCompleted
    val isToday = isSameDay(dueDateMillis, now)

    val badgeColor = when {
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        isToday -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when {
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        isOverdue -> MaterialTheme.colorScheme.onErrorContainer
        isToday -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val formatter = SimpleDateFormat("d MMM, h:mm a", Locale("es", "ES"))
    val dateText = formatter.format(Date(dueDateMillis))

    Surface(
        color = badgeColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasReminder) Icons.Default.Alarm else Icons.Default.Event,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOverdue) "Vencida: $dateText" else dateText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun AddTaskDialog(
    currentList: String,
    onDismiss: () -> Unit,
    onConfirm: (TaskItem) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCalendar by remember { mutableStateOf<Calendar?>(null) }
    var hasReminder by remember { mutableStateOf(false) }

    fun showDatePicker() {
        val current = selectedCalendar ?: Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = selectedCalendar ?: Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedCalendar = cal
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker() {
        val current = selectedCalendar ?: Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = selectedCalendar ?: Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                selectedCalendar = cal
                hasReminder = true
            },
            current.get(Calendar.HOUR_OF_DAY),
            current.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Tarea", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Detalles o notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selectores de Fecha y Hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCalendar != null,
                        onClick = { showDatePicker() },
                        label = {
                            val cal = selectedCalendar
                            Text(
                                if (cal != null) {
                                    val f = SimpleDateFormat("d MMM", Locale("es", "ES"))
                                    f.format(cal.time)
                                } else "Fecha"
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )

                    FilterChip(
                        selected = hasReminder,
                        onClick = { showTimePicker() },
                        label = {
                            val cal = selectedCalendar
                            Text(
                                if (hasReminder && cal != null) {
                                    val f = SimpleDateFormat("h:mm a", Locale("es", "ES"))
                                    f.format(cal.time)
                                } else "Hora"
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val dueMillis = selectedCalendar?.timeInMillis
                        val reminderMillis = if (hasReminder) dueMillis else null
                        onConfirm(
                            TaskItem(
                                title = title.trim(),
                                description = description.trim(),
                                dueDateMillis = dueMillis,
                                reminderMillis = reminderMillis,
                                listName = currentList
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

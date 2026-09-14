package com.asistente.celular.ui.notes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asistente.celular.nlu.notes.NoteItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: List<NoteItem>,
    onAddNote: (NoteItem) -> Unit,
    onUpdateNote: (NoteItem) -> Unit,
    onDeleteNote: (String) -> Unit,
    onTogglePin: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedNoteForEdit by remember { mutableStateOf<NoteItem?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else {
            val q = searchQuery.lowercase()
            notes.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
        }
    }

    val pinnedNotes = filteredNotes.filter { it.isPinned }
    val regularNotes = filteredNotes.filter { !it.isPinned }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notas",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreatingNewNote = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir nota")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de búsqueda rápida
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar en tus notas...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron notas" else "No tienes notas guardadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca el botón + o dile a Hendrix: 'Toma una nota...'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sección de Notas Fijadas
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "FIJADAS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { selectedNoteForEdit = note },
                                onTogglePin = { onTogglePin(note.id) },
                                onDelete = { onDeleteNote(note.id) }
                            )
                        }
                    }

                    // Sección de Otras Notas
                    if (regularNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "OTRAS",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(regularNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { selectedNoteForEdit = note },
                                onTogglePin = { onTogglePin(note.id) },
                                onDelete = { onDeleteNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para crear o editar nota
    if (isCreatingNewNote) {
        NoteEditDialog(
            note = null,
            onDismiss = { isCreatingNewNote = false },
            onSave = { newNote ->
                onAddNote(newNote)
                isCreatingNewNote = false
            }
        )
    }

    selectedNoteForEdit?.let { noteToEdit ->
        NoteEditDialog(
            note = noteToEdit,
            onDismiss = { selectedNoteForEdit = null },
            onSave = { updated ->
                onUpdateNote(updated)
                selectedNoteForEdit = null
            }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = SimpleDateFormat("d MMM, h:mm a", Locale("es", "ES"))
    val updatedDate = formatter.format(Date(note.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Fijar nota",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar nota",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = updatedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun NoteEditDialog(
    note: NoteItem?,
    onDismiss: () -> Unit,
    onSave: (NoteItem) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (note == null) "Nueva Nota" else "Editar Nota",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Fijar nota",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Nota...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank() || title.isNotBlank()) {
                        val derivedTitle = if (title.isBlank()) {
                            val words = content.trim().split("\\s+".toRegex())
                            if (words.size > 4) words.take(4).joinToString(" ") + "..." else content.trim()
                        } else title.trim()

                        val updated = note?.copy(
                            title = derivedTitle,
                            content = content.trim(),
                            isPinned = isPinned,
                            updatedAt = System.currentTimeMillis()
                        ) ?: NoteItem(
                            title = derivedTitle,
                            content = content.trim(),
                            isPinned = isPinned
                        )
                        onSave(updated)
                    }
                },
                enabled = content.isNotBlank() || title.isNotBlank()
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

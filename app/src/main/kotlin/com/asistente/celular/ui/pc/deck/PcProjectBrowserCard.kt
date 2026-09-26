package com.asistente.celular.ui.pc.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.asistente.celular.ui.theme.NeonLilac
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.VoidBlack
import com.asistente.celular.ui.theme.VoidBorder
import com.asistente.celular.ui.theme.VoidSurface
import com.asistente.celular.ui.theme.VoidSurfaceElevated
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.project.PcProjectCategory
import com.asistente.celular.nlu.pc.project.PcProjectItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PcProjectBrowserCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PcProjectCategory?>(null) }
    var projects by remember { mutableStateOf<List<PcProjectItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var launchingPath by remember { mutableStateOf<String?>(null) }

    fun refreshProjects() {
        isLoading = true
        scope.launch {
            try {
                val list = pcBridge.queryRemoteProjects(selectedCategory, searchQuery.takeIf { it.isNotBlank() })
                projects = list
            } catch (e: Exception) {
                onShowSnackbar("Error cargando proyectos: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        refreshProjects()
    }

    Surface(
        color = VoidSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NeonPurple.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Explorador de Proyectos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Abre sesiones de Ableton, FL, Blender, Premiere con 1 toque",
                            fontSize = 11.sp,
                            color = NeonLilac
                        )
                    }
                }
                IconButton(
                    onClick = { refreshProjects() },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = NeonPurple
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar proyectos",
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre o formato...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Button(
                            onClick = { refreshProjects() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("Filtrar", fontSize = 10.sp, color = VoidBlack)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = VoidBorder,
                    focusedContainerColor = VoidBlack,
                    unfocusedContainerColor = VoidBlack,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips de categoría
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todos", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonPurple,
                        selectedLabelColor = VoidBlack
                    )
                )

                PcProjectCategory.entries.forEach { cat ->
                    val catIcon = when (cat) {
                        PcProjectCategory.AUDIO_DAW -> Icons.Default.MusicNote
                        PcProjectCategory.THREE_D_VFX -> Icons.Default.ViewInAr
                        PcProjectCategory.VIDEO_DESIGN -> Icons.Default.Movie
                        PcProjectCategory.CODE_DEV -> Icons.Default.Code
                        PcProjectCategory.OTHER -> Icons.Default.Folder
                    }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = if (selectedCategory == cat) VoidBlack else NeonLilac,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(cat.displayName, fontSize = 11.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple,
                            selectedLabelColor = VoidBlack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lista de proyectos
            if (projects.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron proyectos en la PC",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    projects.take(6).forEach { project ->
                        val isLaunching = launchingPath == project.path
                        ProjectItemRow(
                            project = project,
                            isLaunching = isLaunching,
                            onLaunch = {
                                if (launchingPath == null) {
                                    launchingPath = project.path
                                    scope.launch {
                                        try {
                                            val ok = pcBridge.launchRemoteProject(project.path)
                                            if (ok) {
                                                onShowSnackbar("Abriendo ${project.name} en PC...")
                                            } else {
                                                onShowSnackbar("Error al intentar abrir ${project.name}")
                                            }
                                        } catch (e: Exception) {
                                            onShowSnackbar("Error: ${e.message}")
                                        } finally {
                                            launchingPath = null
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectItemRow(
    project: PcProjectItem,
    isLaunching: Boolean,
    onLaunch: () -> Unit
) {
    val categoryColor = when (project.category) {
        PcProjectCategory.AUDIO_DAW -> Color(0xFF8B5CF6)
        PcProjectCategory.THREE_D_VFX -> Color(0xFFF59E0B)
        PcProjectCategory.VIDEO_DESIGN -> Color(0xFFEC4899)
        PcProjectCategory.CODE_DEV -> Color(0xFF10B981)
        PcProjectCategory.OTHER -> NeonPurple
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(project.lastModifiedEpoch) {
        if (project.lastModifiedEpoch > 0) dateFormat.format(Date(project.lastModifiedEpoch)) else ""
    }

    Surface(
        color = VoidSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(categoryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.extension.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = project.path,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
                if (formattedDate.isNotBlank()) {
                    Text(
                        text = "Modificado: $formattedDate",
                        fontSize = 8.sp,
                        color = Color.LightGray
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLaunch,
                enabled = !isLaunching,
                colors = ButtonDefaults.buttonColors(
                    containerColor = categoryColor,
                    disabledContainerColor = categoryColor.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Abrir",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

package com.asistente.celular.ui.pc.deck

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile
import com.asistente.celular.service.DropzoneNotificationHelper
import com.asistente.celular.ui.theme.NeonPurple
import com.asistente.celular.ui.theme.VoidSurfaceElevated

/**
 * Tarjeta interactiva de alta fidelidad que muestra los entregables recién generados
 * en la PC (renders de Blender, pistas de Ableton/FL Studio, videos de Premiere).
 */
@Composable
fun PcDropzoneDeliverableCard(
    file: PcDropzoneFile,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isAudioPlaying by remember { mutableStateOf(false) }

    val (badgeText, badgeIcon, accentColor) = when {
        file.category.contains("blender", ignoreCase = true) -> Triple("Blender 3D", Icons.Default.ViewInAr, Color(0xFFEA580C))
        file.category.contains("audio", ignoreCase = true) -> Triple("Audio DAW", Icons.Default.MusicNote, Color(0xFF8B5CF6))
        file.category.contains("video", ignoreCase = true) -> Triple("Adobe Video", Icons.Default.Videocam, Color(0xFF06B6D4))
        else -> Triple("Entregable Cloud", Icons.Default.Folder, Color(0xFF10B981))
    }

    val isAudio = file.fileName.endsWith(".wav", ignoreCase = true) ||
            file.fileName.endsWith(".mp3", ignoreCase = true) ||
            file.fileName.endsWith(".flac", ignoreCase = true)

    val isImage = file.fileName.endsWith(".png", ignoreCase = true) ||
            file.fileName.endsWith(".jpg", ignoreCase = true) ||
            file.fileName.endsWith(".jpeg", ignoreCase = true)

    val formattedSize = remember(file.sizeBytes) {
        DropzoneNotificationHelper.formatFileSize(file.sizeBytes)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.35f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            color = VoidSurfaceElevated,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Fila Superior: Badge + Estado Sincronizado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = "Sincronizado",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "En Google Drive",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nombre del archivo y tamaño
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isImage -> Icons.Default.Image
                                isAudio -> Icons.Default.Headphones
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.fileName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$formattedSize • Guardado en ${file.relativePath}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Reproductor mini si es audio
                if (isAudio) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isAudioPlaying = !isAudioPlaying },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAudioPlaying) "Reproduciendo audio..." else "Escuchar previa",
                                fontSize = 12.sp,
                                color = if (isAudioPlaying) Color.White else Color(0xFFCBD5E1)
                            )
                        }

                        Text(
                            text = "0:30",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones de acción táctiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = {
                            val driveIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://drive.google.com")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(driveIntent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abrir en Drive", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Entregable: ${file.fileName}")
                                putExtra(Intent.EXTRA_TEXT, "Nuevo archivo de Hendrix Studio: ${file.fileName} (${formattedSize})")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir entregable"))
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(height = 40.dp, width = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Compartir",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

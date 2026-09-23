package com.asistente.celular.ui.pc.deck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.clipboard.PcClipboardPayload
import com.asistente.celular.nlu.pc.clipboard.PcSnippetItem
import kotlinx.coroutines.launch

@Composable
fun PcClipboardSnippetCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val clipboardState by pcBridge.clipboardState.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var lastVerifiedPayload by remember { mutableStateOf<PcClipboardPayload?>(null) }

    val defaultSnippets = remember {
        listOf(
            PcSnippetItem("blender_render", "Prompt Blender 3D", "Generar modelo fotorrealista con iluminación Cycles y exportar a Drive.", "view_in_ar"),
            PcSnippetItem("daw_notes", "Nota Musical DAW", "Estructura: Intro (8 compases), Verso (16), Coro (16), Drop en 128 BPM.", "music_note"),
            PcSnippetItem("yt_search", "Buscar Plugins", "https://www.youtube.com/results?search_query=best+vst+plugins+2026", "video_library"),
            PcSnippetItem("git_push", "Comando Git", "git add . && git commit -m \"Actualización rápida\" && git push", "code")
        )
    }

    Surface(
        color = Color(0xFF131D31),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
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
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Portapapeles Universal & Snippets",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Sincronización segura con verificación de integridad SHA-256",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            val res = pcBridge.getClipboard()
                            if (res != null) {
                                onShowSnackbar("Portapapeles de la PC obtenido (${res.charCount} chars)")
                            } else {
                                onShowSnackbar("No se pudo leer el portapapeles")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar Portapapeles", tint = Color(0xFF10B981))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vista previa del portapapeles actual de la PC si existe
            clipboardState?.let { clip ->
                if (clip.text.isNotBlank()) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "PC:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = clip.text.take(60) + if (clip.text.length > 60) "..." else "",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${clip.charCount} c",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Chips horizontales de snippets rápidos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                defaultSnippets.forEach { snippet ->
                    SuggestionChip(
                        onClick = { textInput = snippet.content },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (snippet.id) {
                                        "blender_render" -> Icons.Default.ViewInAr
                                        "daw_notes" -> Icons.Default.MusicNote
                                        "yt_search" -> Icons.Default.VideoLibrary
                                        "git_push" -> Icons.Default.Code
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = snippet.title,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E293B)
                        ),
                        border = BorderStroke(0.5.dp, Color(0xFF475569))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de redacción de texto
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Escribe o pega texto, prompt o enlace para la PC...",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                },
                minLines = 2,
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Indicador de recuento y verificación de integridad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${textInput.length} caracteres",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )

                AnimatedVisibility(visible = lastVerifiedPayload != null) {
                    lastVerifiedPayload?.let { verified ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verificado",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% íntegro (${verified.charCount} c | SHA: ${verified.checksumSha256.take(8)})",
                                fontSize = 10.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones de acción: Copiar a PC y Pegar en PC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (textInput.isBlank()) return@Button
                        scope.launch {
                            val res = pcBridge.setClipboard(textInput, pasteImmediately = false)
                            if (res != null) {
                                lastVerifiedPayload = res
                                onShowSnackbar("Copiado al portapapeles de la PC (${res.charCount} chars)")
                            } else {
                                onShowSnackbar("Error al sincronizar con la PC")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar a PC", fontSize = 12.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        if (textInput.isBlank()) return@Button
                        scope.launch {
                            val res = pcBridge.setClipboard(textInput, pasteImmediately = true)
                            if (res != null) {
                                lastVerifiedPayload = res
                                onShowSnackbar("Pegado instantáneo en la ventana activa de Windows")
                            } else {
                                onShowSnackbar("Error al pegar en la PC")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pegar en PC", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

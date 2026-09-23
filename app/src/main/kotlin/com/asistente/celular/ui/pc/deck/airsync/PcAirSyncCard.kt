package com.asistente.celular.ui.pc.deck.airsync

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.airsync.AirSyncSharedFile
import com.asistente.celular.nlu.pc.airsync.AirSyncTransferState
import com.asistente.celular.nlu.pc.airsync.AirSyncUploadOptions
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PcAirSyncCard(
    pcBridge: PcWorkspaceBridge,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sharedFiles by pcBridge.airSyncSharedFiles.collectAsState()
    val transfers by pcBridge.airSyncTransfers.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var autoPasteEnabled by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }

    val lensImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                try {
                    val tempFile = File(context.cacheDir, "lens_snap_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    onShowSnackbar("Transmitiendo foto a la PC con inyección en portapapeles...")
                    val success = pcBridge.uploadAirSyncFile(
                        file = tempFile,
                        options = AirSyncUploadOptions(
                            copyToClipboard = true,
                            autoPaste = autoPasteEnabled,
                            targetDestination = "dropzone"
                        )
                    )
                    if (success) {
                        onShowSnackbar(
                            if (autoPasteEnabled) "Imagen pegada automáticamente en la PC"
                            else "Imagen copiada al portapapeles de la PC (Ctrl+V listo)"
                        )
                    } else {
                        onShowSnackbar("Fallo al enviar imagen a la PC vía AirSync")
                    }
                } catch (e: Exception) {
                    onShowSnackbar("Error al procesar imagen: ${e.localizedMessage}")
                } finally {
                    isUploading = false
                }
            }
        }
    }

    val generalFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                try {
                    val tempFile = File(context.cacheDir, "airsync_upload_${System.currentTimeMillis()}.dat")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    onShowSnackbar("Subiendo archivo pesado a la PC vía TCP...")
                    val success = pcBridge.uploadAirSyncFile(
                        file = tempFile,
                        options = AirSyncUploadOptions(
                            copyToClipboard = false,
                            autoPaste = false,
                            targetDestination = "dropzone"
                        )
                    )
                    if (success) {
                        onShowSnackbar("Archivo enviado con éxito a la Dropzone de PC")
                    } else {
                        onShowSnackbar("Error al subir archivo a la PC")
                    }
                } catch (e: Exception) {
                    onShowSnackbar("Error: ${e.localizedMessage}")
                } finally {
                    isUploading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        pcBridge.queryAirSyncSharedFiles()
    }

    Surface(
        color = Color(0xFF131E2A),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
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
                            .background(Color(0xFF00E5FF).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "AirSync",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hendrix AirSync P2P",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Transferencia LAN bidireccional sin internet (Puerto 8900)",
                            color = Color(0xFF80D8FF),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            pcBridge.queryAirSyncSharedFiles()
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00E5FF), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lens-to-Workspace Action Section
            Surface(
                color = Color(0xFF00E5FF).copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LENS-TO-WORKSPACE (MÓVIL -> PC)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    Text(
                        text = "Envía fotos o bocetos a la PC con inyección instantánea en portapapeles (Ctrl+V)",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { lensImageLauncher.launch("image/*") },
                            enabled = !isUploading,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Foto / Boceto", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { generalFileLauncher.launch("*/*") },
                            enabled = !isUploading,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Subir Archivo", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-pegar (Ctrl+V) en ventana activa:",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Switch(
                            checked = autoPasteEnabled,
                            onCheckedChange = { autoPasteEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00E5FF),
                                checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(36.dp, 20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Transfer Section
            val activeTransfer = transfers.firstOrNull { it.state == AirSyncTransferState.IN_PROGRESS || it.state == AirSyncTransferState.PENDING }
            if (activeTransfer != null) {
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeTransfer.fileName,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val speedMb = activeTransfer.speedBytesPerSec / (1024.0 * 1024.0)
                            Text(
                                text = "${"%.1f".format(speedMb)} MB/s",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = activeTransfer.progressPercent,
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF00E5FF),
                            trackColor = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val percent = (activeTransfer.progressPercent * 100).toInt()
                            Text(
                                text = "$percent% transferido",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (activeTransfer.etaSeconds > 0) "ETA: ${activeTransfer.etaSeconds}s" else "Calculando...",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Shared Files List
            Text(
                text = "ARCHIVOS DISPONIBLES EN PC (${sharedFiles.size})",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (sharedFiles.isEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay archivos pesados compartidos en este momento.\nPuedes compartir stems de audio, renders o zips desde la PC.",
                        color = Color.LightGray.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                sharedFiles.take(4).forEach { file ->
                    AirSyncFileRow(
                        file = file,
                        onDownload = {
                            scope.launch {
                                val dest = File(
                                    System.getProperty("java.io.tmpdir") ?: "/sdcard/Download",
                                    file.fileName
                                ).absolutePath
                                onShowSnackbar("Iniciando descarga LAN de ${file.fileName}...")
                                val success = pcBridge.downloadAirSyncFile(file.fileId, dest)
                                if (success) {
                                    onShowSnackbar("Descarga completada: ${file.fileName}")
                                } else {
                                    onShowSnackbar("Error al transferir ${file.fileName}")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun AirSyncFileRow(
    file: AirSyncSharedFile,
    onDownload: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeMb = file.fileSizeBytes / (1024.0 * 1024.0)
                Text(
                    text = "${"%.2f".format(sizeMb)} MB  •  ${file.totalChunks} bloques  •  SHA-256",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onDownload,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Descargar",
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bajar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

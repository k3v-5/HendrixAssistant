package com.asistente.celular.pc.airsync

import android.util.Log
import com.asistente.celular.nlu.pc.airsync.AirSyncDirection
import com.asistente.celular.nlu.pc.airsync.AirSyncTransfer
import com.asistente.celular.nlu.pc.airsync.AirSyncTransferState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * Cliente TCP nativo de alta velocidad para transferencia P2P LAN en Hendrix AirSync.
 * Diseñado con protocolo binario directo sin sobrecarga HTTP/WebSocket para alcanzar velocidades
 * de transferencia máximas en red local (Wi-Fi 6 / Ethernet) con verificación SHA-256 por bloque y reanudación.
 */
class AirSyncClient {

    companion object {
        private const val TAG = "AirSyncClient"
        private const val MAGIC = "HASY"
        private const val DEFAULT_CHUNK_SIZE = 262144 // 256 KB
        private const val SOCKET_TIMEOUT_MS = 15000
    }

    /**
     * Descarga un archivo por lotes/chunks directamente desde la PC a través del socket TCP de AirSync.
     */
    suspend fun downloadFile(
        host: String,
        port: Int,
        fileId: String,
        destinationFile: File,
        transferId: String = UUID.randomUUID().toString(),
        onProgress: (AirSyncTransfer) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val socket = Socket()
        try {
            destinationFile.parentFile?.mkdirs()
            val initialBytes = if (destinationFile.exists()) destinationFile.length() else 0L
            val startChunk = (initialBytes / DEFAULT_CHUNK_SIZE).toInt()

            var transfer = AirSyncTransfer(
                transferId = transferId,
                fileName = destinationFile.name,
                fileSizeBytes = initialBytes,
                direction = AirSyncDirection.RECEIVE,
                state = AirSyncTransferState.PENDING,
                bytesTransferred = initialBytes,
                startedAt = System.currentTimeMillis()
            )
            onProgress(transfer)

            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
            socket.soTimeout = SOCKET_TIMEOUT_MS

            val outStream = socket.getOutputStream()
            val inStream = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(inStream, Charsets.UTF_8))

            // 1. Enviar solicitud de comando PULL_FILE
            val reqJson = JSONObject().apply {
                put("magic", MAGIC)
                put("command", "PULL_FILE")
                put("fileId", fileId)
                put("startChunk", startChunk)
                put("chunkSize", DEFAULT_CHUNK_SIZE)
            }
            outStream.write((reqJson.toString() + "\n").toByteArray(Charsets.UTF_8))
            outStream.flush()

            // 2. Leer respuesta inicial de streaming (JSON en una línea)
            val ackLine = reader.readLine() ?: return@withContext false
            val ackJson = JSONObject(ackLine)
            val status = ackJson.optString("status", "")
            if (status != "STREAMING") {
                Log.e(TAG, "Respuesta inesperada de servidor AirSync: $ackLine")
                transfer = transfer.copy(
                    state = AirSyncTransferState.FAILED,
                    errorMessage = "Error servidor: $status"
                )
                onProgress(transfer)
                return@withContext false
            }

            val totalSize = ackJson.optLong("totalSize", 0L)
            val totalChunks = ackJson.optInt("totalChunks", 0)
            val chunkSize = ackJson.optInt("chunkSize", DEFAULT_CHUNK_SIZE)

            transfer = transfer.copy(
                fileSizeBytes = totalSize,
                state = AirSyncTransferState.IN_PROGRESS
            )
            onProgress(transfer)

            // Abrir archivo con RandomAccessFile para soportar reanudación
            RandomAccessFile(destinationFile, "rw").use { raf ->
                val startOffset = startChunk.toLong() * chunkSize
                raf.seek(startOffset)

                var bytesDownloaded = startOffset
                val startTime = System.currentTimeMillis()
                var lastProgressUpdate = startTime

                val headerBuffer = ByteArray(40) // 4B index + 4B length + 32B SHA-256
                val md = MessageDigest.getInstance("SHA-256")

                for (chunkIdx in startChunk until totalChunks) {
                    // Leer encabezado de 40 bytes
                    readFully(inStream, headerBuffer, 40)
                    val byteBuf = ByteBuffer.wrap(headerBuffer)
                    val recvChunkIndex = byteBuf.int
                    val recvChunkLen = byteBuf.int
                    val recvChunkHash = ByteArray(32)
                    byteBuf.get(recvChunkHash)

                    if (recvChunkIndex != chunkIdx) {
                        Log.w(TAG, "Desfase en índice de chunk recibido: esperado $chunkIdx, recibido $recvChunkIndex")
                    }

                    // Leer datos del chunk
                    val chunkData = ByteArray(recvChunkLen)
                    readFully(inStream, chunkData, recvChunkLen)

                    // Validar integridad SHA-256 del bloque
                    md.reset()
                    val computedHash = md.digest(chunkData)
                    if (!computedHash.contentEquals(recvChunkHash)) {
                        Log.e(TAG, "Fallo de validación de checksum SHA-256 en chunk $chunkIdx")
                        transfer = transfer.copy(
                            state = AirSyncTransferState.FAILED,
                            errorMessage = "Fallo de integridad SHA-256 en bloque $chunkIdx"
                        )
                        onProgress(transfer)
                        return@withContext false
                    }

                    // Escribir bloque en el archivo
                    raf.write(chunkData, 0, recvChunkLen)
                    bytesDownloaded += recvChunkLen

                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate >= 250 || bytesDownloaded >= totalSize) {
                        val elapsedSecs = (now - startTime).coerceAtLeast(1) / 1000.0
                        val netBytes = (bytesDownloaded - startOffset).coerceAtLeast(0)
                        val speed = netBytes / elapsedSecs
                        val remainingBytes = (totalSize - bytesDownloaded).coerceAtLeast(0)
                        val eta = if (speed > 0) (remainingBytes / speed).toInt() else 0
                        val progress = if (totalSize > 0) (bytesDownloaded.toFloat() / totalSize.toFloat()) else 1.0f

                        transfer = transfer.copy(
                            bytesTransferred = bytesDownloaded,
                            progressPercent = progress,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                        onProgress(transfer)
                        lastProgressUpdate = now
                    }
                }
            }

            transfer = transfer.copy(
                state = AirSyncTransferState.COMPLETED,
                bytesTransferred = destinationFile.length(),
                progressPercent = 1.0f,
                speedBytesPerSec = 0.0,
                etaSeconds = 0,
                completedAt = System.currentTimeMillis()
            )
            onProgress(transfer)
            Log.i(TAG, "Transferencia AirSync completada: ${destinationFile.name} (${destinationFile.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error durante transferencia AirSync: ${e.message}", e)
            val errorTransfer = AirSyncTransfer(
                transferId = transferId,
                fileName = destinationFile.name,
                fileSizeBytes = destinationFile.length(),
                direction = AirSyncDirection.RECEIVE,
                state = AirSyncTransferState.FAILED,
                errorMessage = e.localizedMessage ?: "Error de socket de transferencia"
            )
            onProgress(errorTransfer)
            false
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Sube un archivo local desde el celular hacia la PC vía TCP de alta velocidad (Lens-to-Workspace o Push).
     */
    suspend fun uploadFile(
        host: String,
        port: Int,
        sourceFile: File,
        options: com.asistente.celular.nlu.pc.airsync.AirSyncUploadOptions = com.asistente.celular.nlu.pc.airsync.AirSyncUploadOptions(),
        transferId: String = UUID.randomUUID().toString(),
        onProgress: (AirSyncTransfer) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val socket = Socket()
        try {
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                Log.e(TAG, "El archivo origen no existe o no se puede leer: ${sourceFile.absolutePath}")
                return@withContext false
            }

            val fileSize = sourceFile.length()
            val totalChunks = if (fileSize == 0L) 1 else ((fileSize + DEFAULT_CHUNK_SIZE - 1) / DEFAULT_CHUNK_SIZE).toInt()

            var transfer = AirSyncTransfer(
                transferId = transferId,
                fileName = sourceFile.name,
                fileSizeBytes = fileSize,
                direction = AirSyncDirection.SEND,
                state = AirSyncTransferState.PENDING,
                bytesTransferred = 0L,
                startedAt = System.currentTimeMillis()
            )
            onProgress(transfer)

            socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
            socket.soTimeout = SOCKET_TIMEOUT_MS

            val outStream = socket.getOutputStream()
            val inStream = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(inStream, Charsets.UTF_8))

            // 1. Enviar encabezado de comando PUSH_FILE
            val reqJson = JSONObject().apply {
                put("magic", MAGIC)
                put("command", "PUSH_FILE")
                put("fileName", sourceFile.name)
                put("fileSize", fileSize)
                put("chunkSize", DEFAULT_CHUNK_SIZE)
                put("totalChunks", totalChunks)
                put("copyToClipboard", options.copyToClipboard)
                put("autoPaste", options.autoPaste)
                put("targetDestination", options.targetDestination)
            }
            outStream.write((reqJson.toString() + "\n").toByteArray(Charsets.UTF_8))
            outStream.flush()

            // 2. Leer ACK de preparación
            val ackLine = reader.readLine() ?: return@withContext false
            val ackJson = JSONObject(ackLine)
            if (ackJson.optString("status") != "READY_TO_RECEIVE") {
                val errMsg = ackJson.optString("message", "Servidor no listo para recibir")
                Log.e(TAG, "Servidor AirSync rechazó PUSH_FILE: $errMsg")
                transfer = transfer.copy(
                    state = AirSyncTransferState.FAILED,
                    errorMessage = errMsg
                )
                onProgress(transfer)
                return@withContext false
            }

            transfer = transfer.copy(state = AirSyncTransferState.IN_PROGRESS)
            onProgress(transfer)

            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_CHUNK_SIZE)
            var bytesSent = 0L
            val startTime = System.currentTimeMillis()
            var lastProgressUpdate = startTime

            java.io.FileInputStream(sourceFile).use { fis ->
                for (chunkIdx in 0 until totalChunks) {
                    val bytesRead = fis.read(buffer)
                    val currentLen = if (bytesRead > 0) bytesRead else 0

                    md.reset()
                    if (currentLen > 0) {
                        md.update(buffer, 0, currentLen)
                    }
                    val chunkHash = md.digest()

                    // Armar encabezado binario de 40B: [Index: 4B][Length: 4B][SHA-256: 32B]
                    val headerBuf = ByteBuffer.allocate(40)
                    headerBuf.putInt(chunkIdx)
                    headerBuf.putInt(currentLen)
                    headerBuf.put(chunkHash)

                    outStream.write(headerBuf.array())
                    if (currentLen > 0) {
                        outStream.write(buffer, 0, currentLen)
                    }
                    outStream.flush()

                    bytesSent += currentLen
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate >= 250 || bytesSent >= fileSize) {
                        val elapsedSecs = (now - startTime).coerceAtLeast(1) / 1000.0
                        val speed = bytesSent / elapsedSecs
                        val remainingBytes = (fileSize - bytesSent).coerceAtLeast(0)
                        val eta = if (speed > 0) (remainingBytes / speed).toInt() else 0
                        val progress = if (fileSize > 0) (bytesSent.toFloat() / fileSize.toFloat()) else 1.0f

                        transfer = transfer.copy(
                            bytesTransferred = bytesSent,
                            progressPercent = progress,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                        onProgress(transfer)
                        lastProgressUpdate = now
                    }
                }
            }

            // 3. Esperar confirmación de completado del servidor
            val compLine = reader.readLine()
            if (compLine != null) {
                val compJson = JSONObject(compLine)
                if (compJson.optString("status") != "COMPLETED") {
                    val errMsg = compJson.optString("message", "Error al procesar subida en servidor")
                    Log.e(TAG, "Error finalizando PUSH_FILE: $errMsg")
                    transfer = transfer.copy(
                        state = AirSyncTransferState.FAILED,
                        errorMessage = errMsg
                    )
                    onProgress(transfer)
                    return@withContext false
                }
            }

            transfer = transfer.copy(
                state = AirSyncTransferState.COMPLETED,
                bytesTransferred = fileSize,
                progressPercent = 1.0f,
                speedBytesPerSec = 0.0,
                etaSeconds = 0,
                completedAt = System.currentTimeMillis()
            )
            onProgress(transfer)
            Log.i(TAG, "Subida AirSync completada con éxito: ${sourceFile.name} ($fileSize bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error durante subida AirSync: ${e.message}", e)
            val errorTransfer = AirSyncTransfer(
                transferId = transferId,
                fileName = sourceFile.name,
                fileSizeBytes = sourceFile.length(),
                direction = AirSyncDirection.SEND,
                state = AirSyncTransferState.FAILED,
                errorMessage = e.localizedMessage ?: "Error de socket al subir archivo"
            )
            onProgress(errorTransfer)
            false
        } finally {
            try {
                socket.close()
            } catch (ignored: Exception) {}
        }
    }

    private fun readFully(inStream: java.io.InputStream, buffer: ByteArray, length: Int) {
        var bytesRead = 0
        while (bytesRead < length) {
            val count = inStream.read(buffer, bytesRead, length - bytesRead)
            if (count < 0) {
                throw java.io.EOFException("Fin prematuro del flujo TCP AirSync ($bytesRead/$length bytes leídos)")
            }
            bytesRead += count
        }
    }
}

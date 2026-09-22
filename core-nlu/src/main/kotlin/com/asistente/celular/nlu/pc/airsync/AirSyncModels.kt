package com.asistente.celular.nlu.pc.airsync

/**
 * Dirección del flujo de datos en Hendrix AirSync.
 */
enum class AirSyncDirection {
    SEND,
    RECEIVE
}

/**
 * Estados del ciclo de vida de una transferencia en Hendrix AirSync.
 */
enum class AirSyncTransferState {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Archivo indexado y listo para compartir o descargar a través de Hendrix AirSync.
 */
data class AirSyncSharedFile(
    val fileId: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val sha256: String = "",
    val totalChunks: Int = 0,
    val chunkSizeBytes: Int = 262144, // 256 KB por defecto
    val readyForDownload: Boolean = true
)

/**
 * Telemetría y estado en tiempo real de una transferencia activa de Hendrix AirSync.
 */
data class AirSyncTransfer(
    val transferId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val direction: AirSyncDirection,
    val state: AirSyncTransferState = AirSyncTransferState.PENDING,
    val bytesTransferred: Long = 0L,
    val progressPercent: Float = 0.0f,
    val speedBytesPerSec: Double = 0.0,
    val etaSeconds: Int = 0,
    val errorMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Opciones avanzadas para la subida de archivos vía Hendrix AirSync (Mobile -> PC),
 * incluyendo "Lens-to-Workspace" (inyección en portapapeles y auto-pegado).
 */
data class AirSyncUploadOptions(
    val copyToClipboard: Boolean = false,
    val autoPaste: Boolean = false,
    val targetDestination: String = "dropzone"
)

package com.asistente.celular.nlu.pc.dropzone

/**
 * Metadatos de un archivo generado en la computadora y depositado en la carpeta Dropzone.
 */
data class PcDropzoneFile(
    val fileName: String,
    val category: String,
    val sizeBytes: Long,
    val relativePath: String,
    val timestamp: Long
)

/**
 * Información de configuración y estado del buzón de entregables (Dropzone) en la PC.
 */
data class PcDropzoneInfo(
    val rootPath: String,
    val cloudProvider: String,
    val isCloudSynced: Boolean,
    val categories: Map<String, String> = emptyMap(),
    val recentFiles: List<PcDropzoneFile> = emptyList()
)

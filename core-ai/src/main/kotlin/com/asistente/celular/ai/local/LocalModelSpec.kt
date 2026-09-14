package com.asistente.celular.ai.local

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Formato de serialización del modelo para inferencia on-device.
 */
@Serializable
enum class LocalModelFormat {
    GGUF,
    TFLITE,
    ONNX
}

/**
 * Especificación de un modelo de lenguaje descargable para ejecución local.
 */
@Serializable
data class LocalModelSpec(
    val id: String,
    val name: String,
    val description: String,
    val parameterCount: String,
    val quantization: String,
    val sizeBytes: Long,
    val fileName: String,
    val downloadUrl: String,
    val recommendedRamGb: Int,
    val format: LocalModelFormat = LocalModelFormat.GGUF
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f MB", mb)
            }
        }
}

/**
 * Estado reactivo del proceso de descarga de un modelo.
 */
sealed class ModelDownloadState {
    object Idle : ModelDownloadState()

    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L
    ) : ModelDownloadState() {
        val formattedProgress: String
            get() {
                val currentMb = downloadedBytes / (1024.0 * 1024.0)
                val totalMb = totalBytes / (1024.0 * 1024.0)
                return String.format(java.util.Locale.US, "%.1f / %.1f MB (%d%%)", currentMb, totalMb, progressPercent)
            }
    }

    data class Success(
        val file: File,
        val totalBytes: Long
    ) : ModelDownloadState()

    data class Error(
        val message: String
    ) : ModelDownloadState()
}

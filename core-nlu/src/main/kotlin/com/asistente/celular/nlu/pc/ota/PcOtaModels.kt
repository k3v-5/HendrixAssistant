package com.asistente.celular.nlu.pc.ota

import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Metadatos de la actualización del APK compilado en la PC.
 */
data class PcAppUpdateInfo(
    val available: Boolean = false,
    val fileName: String = "app-debug.apk",
    val apkSizeBytes: Long = 0L,
    val lastModifiedEpoch: Long = 0L,
    val sha256: String = "",
    val airsyncFileId: String = "hendrix_latest_apk",
    val downloadUrl: String = "",
    val message: String? = null
)

/**
 * Estados del ciclo de vida de descarga e instalación OTA.
 */
sealed interface OtaDownloadState {
    object Idle : OtaDownloadState
    object Checking : OtaDownloadState
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : OtaDownloadState
    data class ReadyToInstall(val apkFile: File) : OtaDownloadState
    data class Error(val message: String) : OtaDownloadState
}

/**
 * Contrato para el gestor de actualizaciones OTA directas desde la PC (SOLID OCP & DIP).
 */
interface PcOtaUpdateManager {
    val updateInfo: StateFlow<PcAppUpdateInfo?>
    val downloadState: StateFlow<OtaDownloadState>

    /**
     * Consulta si existe una nueva compilación en la PC.
     */
    suspend fun checkForUpdates(): PcAppUpdateInfo?

    /**
     * Descarga el APK desde la PC e inicia el instalador del sistema de Android.
     */
    suspend fun downloadAndInstall(info: PcAppUpdateInfo): Boolean

    /**
     * Dispara el instalador nativo de Android usando FileProvider.
     */
    fun triggerInstall(apkFile: File)
}

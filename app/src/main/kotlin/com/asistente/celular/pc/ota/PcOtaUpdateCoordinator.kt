package com.asistente.celular.pc.ota

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.ota.OtaDownloadState
import com.asistente.celular.nlu.pc.ota.PcAppUpdateInfo
import com.asistente.celular.nlu.pc.ota.PcOtaUpdateManager
import com.asistente.celular.pc.PcRemoteCoordinator
import com.asistente.celular.pc.airsync.AirSyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Coordinador responsable de la detección, descarga en red local e instalación
 * autónoma de actualizaciones de la app móvil (OTA) generadas en la PC de desarrollo.
 */
class PcOtaUpdateCoordinator(
    private val context: Context,
    private val pcBridge: PcWorkspaceBridge,
    private val scope: CoroutineScope,
    private val airSyncClient: AirSyncClient = AirSyncClient(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
) : PcOtaUpdateManager {

    companion object {
        private const val TAG = "PcOtaUpdateCoordinator"
        private const val DEFAULT_PC_IP = "192.168.100.159"
        private const val DEFAULT_OTA_PORT = 8901
    }

    private val _updateInfo = MutableStateFlow<PcAppUpdateInfo?>(null)
    override val updateInfo: StateFlow<PcAppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadState = MutableStateFlow<OtaDownloadState>(OtaDownloadState.Idle)
    override val downloadState: StateFlow<OtaDownloadState> = _downloadState.asStateFlow()

    init {
        // Al conectarse a la PC, verificar de forma proactiva si hay un nuevo APK compilado
        scope.launch {
            pcBridge.isConnected.collect { isConnected ->
                if (isConnected) {
                    try {
                        checkForUpdates()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error en comprobación inicial de actualización OTA: ${e.message}")
                    }
                } else {
                    _updateInfo.value = null
                    _downloadState.value = OtaDownloadState.Idle
                }
            }
        }
    }

    /**
     * Consulta el endpoint HTTP del servidor de PC para verificar si hay una compilación más reciente.
     */
    override suspend fun checkForUpdates(): PcAppUpdateInfo? = withContext(Dispatchers.IO) {
        _downloadState.value = OtaDownloadState.Checking
        try {
            val coordinator = pcBridge as? PcRemoteCoordinator
            val config = coordinator?.endpointConfig?.value
            val host = config?.localIp?.takeIf { it.isNotBlank() } ?: DEFAULT_PC_IP
            val port = config?.otaPort ?: DEFAULT_OTA_PORT

            val url = "http://$host:$port/api/update/latest"
            val request = Request.Builder().url(url).get().build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Respuesta no exitosa al verificar actualización OTA: ${response.code}")
                _downloadState.value = OtaDownloadState.Idle
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val available = json.optBoolean("available", false)
            if (!available) {
                _updateInfo.value = null
                _downloadState.value = OtaDownloadState.Idle
                return@withContext null
            }

            val lastModifiedEpoch = json.optLong("lastModifiedEpoch", 0L)
            val apkSizeBytes = json.optLong("apkSizeBytes", 0L)
            val sha256 = json.optString("sha256", "")
            val airsyncFileId = json.optString("airsyncFileId", "hendrix_latest_apk")
            val downloadUrl = json.optString("downloadUrl", "http://$host:$port/api/update/download")

            // Obtener fecha de la última instalación de la app en el dispositivo
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val installedUpdateTime = packageInfo.lastUpdateTime

            // Si el APK en la PC es más nuevo que la app instalada en el dispositivo
            val isNewer = lastModifiedEpoch > (installedUpdateTime + 5000L) // Margen de 5s para evitar falsos positivos

            val info = PcAppUpdateInfo(
                available = isNewer,
                fileName = json.optString("fileName", "app-debug.apk"),
                apkSizeBytes = apkSizeBytes,
                lastModifiedEpoch = lastModifiedEpoch,
                sha256 = sha256,
                airsyncFileId = airsyncFileId,
                downloadUrl = downloadUrl,
                message = if (isNewer) "Nueva compilación lista en tu PC" else "Tu versión está actualizada"
            )

            _updateInfo.value = if (isNewer) info else null
            _downloadState.value = OtaDownloadState.Idle
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando actualizaciones OTA", e)
            _downloadState.value = OtaDownloadState.Idle
            null
        }
    }

    /**
     * Descarga el APK desde la PC con progreso en tiempo real y dispara el instalador de Android.
     */
    override suspend fun downloadAndInstall(info: PcAppUpdateInfo): Boolean = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "hendrix-app-update.apk")

        _downloadState.value = OtaDownloadState.Downloading(0f, 0L, info.apkSizeBytes)

        var downloadSuccess = false

        // 1. Intento primario: Descarga HTTP streaming de alta velocidad
        try {
            val request = Request.Builder().url(info.downloadUrl).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful && response.body != null) {
                val body = response.body!!
                val contentLength = if (body.contentLength() > 0) body.contentLength() else info.apkSizeBytes
                var bytesCopied = 0L

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(65536)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            val progress = if (contentLength > 0) (bytesCopied.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else 0.5f
                            _downloadState.value = OtaDownloadState.Downloading(progress, bytesCopied, contentLength)
                        }
                        output.flush()
                    }
                }
                downloadSuccess = targetFile.exists() && targetFile.length() > 1024L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Descarga HTTP falló, recurriendo a AirSync P2P: ${e.message}")
        }

        // 2. Intento secundario: Hendrix AirSync P2P sobre TCP 8900
        if (!downloadSuccess) {
            try {
                val coordinator = pcBridge as? PcRemoteCoordinator
                val config = coordinator?.endpointConfig?.value
                val host = config?.localIp?.takeIf { it.isNotBlank() } ?: DEFAULT_PC_IP
                val airSyncPort = config?.airSyncPort ?: 8900

                val ok = airSyncClient.downloadFile(
                    host = host,
                    port = airSyncPort,
                    fileId = info.airsyncFileId,
                    destinationFile = targetFile
                ) { transfer ->
                    val progress = if (transfer.fileSizeBytes > 0) {
                        (transfer.bytesTransferred.toFloat() / transfer.fileSizeBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    _downloadState.value = OtaDownloadState.Downloading(
                        progress = progress,
                        bytesDownloaded = transfer.bytesTransferred,
                        totalBytes = transfer.fileSizeBytes
                    )
                }
                downloadSuccess = ok && targetFile.exists()
            } catch (e: Exception) {
                Log.e(TAG, "Descarga AirSync también falló: ${e.message}")
            }
        }

        if (!downloadSuccess) {
            _downloadState.value = OtaDownloadState.Error("No se pudo descargar el archivo APK desde la PC.")
            return@withContext false
        }

        // Validación de integridad SHA-256 opcional
        if (info.sha256.isNotBlank()) {
            val localSha = calculateSha256(targetFile)
            if (!localSha.equals(info.sha256, ignoreCase = true)) {
                Log.w(TAG, "Aviso: Hash SHA-256 no coincidió (esperado: ${info.sha256}, local: $localSha)")
            }
        }

        _downloadState.value = OtaDownloadState.ReadyToInstall(targetFile)

        withContext(Dispatchers.Main) {
            triggerInstall(targetFile)
        }
        true
    }

    /**
     * Lanza el instalador oficial de paquetes de Android.
     */
    override fun triggerInstall(apkFile: File) {
        if (!apkFile.exists()) {
            Log.e(TAG, "No se puede instalar: el archivo ${apkFile.absolutePath} no existe")
            return
        }

        // En Android 8.0+ (API 26+), verificar si la app tiene permiso para instalar paquetes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Log.w(TAG, "Permiso REQUEST_INSTALL_PACKAGES no concedido. Solicitando acceso en Ajustes...")
                val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(manageIntent)
                return
            }
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
            Log.i(TAG, "🚀 Instalador nativo de Android invocado correctamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error invocando el instalador de Android", e)
            _downloadState.value = OtaDownloadState.Error("Error al iniciar instalación: ${e.message}")
        }
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}

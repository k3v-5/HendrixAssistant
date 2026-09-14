package com.asistente.celular.ai.local

import android.content.Context
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Contrato para la administración y descarga de modelos de lenguaje en almacenamiento local.
 */
interface LocalModelManager {
    val downloadStates: StateFlow<Map<String, ModelDownloadState>>

    fun getAvailableModels(): List<LocalModelSpec>
    fun getModelSpec(modelId: String): LocalModelSpec?
    fun isModelDownloaded(modelId: String): Boolean
    fun getModelFile(modelId: String): File?
    fun startDownload(modelId: String)
    fun cancelDownload(modelId: String)
    fun deleteModel(modelId: String): Boolean
    fun getAvailableStorageBytes(): Long
    fun getModelDirectory(): File
}

/**
 * Implementación robusta de [LocalModelManager] usando OkHttp streaming y Corrutinas.
 */
class DefaultLocalModelManager(
    private val context: Context,
    private val httpClient: OkHttpClient = createDefaultHttpClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : LocalModelManager {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    override val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    private val modelsDir: File by lazy {
        val dir = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    override fun getModelDirectory(): File = modelsDir

    override fun getAvailableModels(): List<LocalModelSpec> {
        return LocalModelCatalog.ALL_MODELS
    }

    override fun getModelSpec(modelId: String): LocalModelSpec? {
        return LocalModelCatalog.findById(modelId)
    }

    override fun isModelDownloaded(modelId: String): Boolean {
        val file = getModelFile(modelId) ?: return false
        val spec = getModelSpec(modelId) ?: return file.exists() && file.length() > 0
        // Consideramos descargado si existe y su tamaño es al menos el 95% del tamaño esperado
        return file.exists() && file.length() >= (spec.sizeBytes * 0.95)
    }

    override fun getModelFile(modelId: String): File? {
        val spec = getModelSpec(modelId) ?: return null
        return File(modelsDir, spec.fileName)
    }

    override fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(modelsDir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            Log.w(TAG, "Error consultando StatFs: ${e.message}")
            Long.MAX_VALUE
        }
    }

    override fun startDownload(modelId: String) {
        val spec = getModelSpec(modelId) ?: run {
            Log.e(TAG, "Modelo no encontrado en catálogo: $modelId")
            return
        }

        if (isModelDownloaded(modelId)) {
            val file = getModelFile(modelId) ?: return
            updateState(modelId, ModelDownloadState.Success(file, file.length()))
            return
        }

        if (activeJobs[modelId]?.isActive == true) {
            Log.w(TAG, "Descarga ya en progreso para: $modelId")
            return
        }

        val availableBytes = getAvailableStorageBytes()
        val requiredBytes = spec.sizeBytes + (50 * 1024 * 1024) // 50 MB de margen de seguridad
        if (availableBytes < requiredBytes) {
            val freeMb = availableBytes / (1024 * 1024)
            val neededMb = spec.sizeBytes / (1024 * 1024)
            updateState(
                modelId,
                ModelDownloadState.Error("Espacio insuficiente en disco ($freeMb MB libres). Se requieren ~$neededMb MB.")
            )
            return
        }

        val job = scope.launch {
            val targetFile = File(modelsDir, spec.fileName)
            val partFile = File(modelsDir, "${spec.fileName}.part")

            try {
                updateState(modelId, ModelDownloadState.Downloading(0, 0L, spec.sizeBytes))

                val request = Request.Builder()
                    .url(spec.downloadUrl)
                    .header("User-Agent", "Hendrix-Android-Assistant/1.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Servidor respondió con código HTTP ${response.code}")
                    }

                    val body = response.body ?: throw IllegalStateException("Cuerpo de respuesta vacío")
                    val totalLength = if (body.contentLength() > 0) body.contentLength() else spec.sizeBytes

                    var downloadedBytes = 0L
                    var lastUpdateTime = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L
                    var currentSpeed = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(partFile).use { output ->
                            val buffer = ByteArray(64 * 1024) // 64 KB chunks
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (!isActive) {
                                    // Cancelación solicitada
                                    output.flush()
                                    partFile.delete()
                                    updateState(modelId, ModelDownloadState.Idle)
                                    return@launch
                                }

                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead
                                bytesSinceLastUpdate += bytesRead

                                val now = System.currentTimeMillis()
                                val elapsed = now - lastUpdateTime
                                if (elapsed >= 300) { // Actualizar UI cada 300 ms para máxima fluidez
                                    currentSpeed = (bytesSinceLastUpdate * 1000L) / elapsed
                                    lastUpdateTime = now
                                    bytesSinceLastUpdate = 0L

                                    val percent = ((downloadedBytes * 100) / totalLength).toInt().coerceIn(0, 99)
                                    updateState(
                                        modelId,
                                        ModelDownloadState.Downloading(
                                            progressPercent = percent,
                                            downloadedBytes = downloadedBytes,
                                            totalBytes = totalLength,
                                            speedBytesPerSec = currentSpeed
                                        )
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    // Renombrado atómico del archivo .part al destino final
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    val renameOk = partFile.renameTo(targetFile)
                    if (!renameOk) {
                        throw IllegalStateException("No se pudo renombrar el archivo temporal de descarga")
                    }

                    updateState(modelId, ModelDownloadState.Success(targetFile, targetFile.length()))
                    Log.i(TAG, "Descarga completada con éxito: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                }
            } catch (e: Exception) {
                if (partFile.exists()) {
                    partFile.delete()
                }
                if (isActive) {
                    val msg = e.localizedMessage ?: "Error de red desconocido"
                    Log.e(TAG, "Error descargando modelo $modelId: $msg", e)
                    updateState(modelId, ModelDownloadState.Error(msg))
                } else {
                    updateState(modelId, ModelDownloadState.Idle)
                }
            } finally {
                activeJobs.remove(modelId)
            }
        }

        activeJobs[modelId] = job
    }

    override fun cancelDownload(modelId: String) {
        val job = activeJobs.remove(modelId)
        job?.cancel()

        val spec = getModelSpec(modelId)
        if (spec != null) {
            val partFile = File(modelsDir, "${spec.fileName}.part")
            if (partFile.exists()) {
                partFile.delete()
            }
        }
        updateState(modelId, ModelDownloadState.Idle)
    }

    override fun deleteModel(modelId: String): Boolean {
        cancelDownload(modelId)
        val file = getModelFile(modelId) ?: return false
        val deleted = if (file.exists()) file.delete() else true
        updateState(modelId, ModelDownloadState.Idle)
        return deleted
    }

    private fun updateState(modelId: String, state: ModelDownloadState) {
        val current = _downloadStates.value.toMutableMap()
        current[modelId] = state
        _downloadStates.value = current
    }

    companion object {
        private const val TAG = "LocalModelManager"

        private fun createDefaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}

package com.asistente.celular.ai.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalModelManagerTest {

    private class FakeLocalModelManager(
        private val tempDir: File
    ) : LocalModelManager {
        private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
        override val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

        override fun getAvailableModels(): List<LocalModelSpec> = LocalModelCatalog.ALL_MODELS

        override fun getModelSpec(modelId: String): LocalModelSpec? = LocalModelCatalog.findById(modelId)

        override fun isModelDownloaded(modelId: String): Boolean {
            val file = getModelFile(modelId) ?: return false
            return file.exists() && file.length() > 0
        }

        override fun getModelFile(modelId: String): File? {
            val spec = getModelSpec(modelId) ?: return null
            return File(tempDir, spec.fileName)
        }

        override fun startDownload(modelId: String) {
            val spec = getModelSpec(modelId) ?: return
            val current = _downloadStates.value.toMutableMap()
            current[modelId] = ModelDownloadState.Downloading(50, spec.sizeBytes / 2, spec.sizeBytes, 1_000_000L)
            _downloadStates.value = current
        }

        override fun cancelDownload(modelId: String) {
            val current = _downloadStates.value.toMutableMap()
            current[modelId] = ModelDownloadState.Idle
            _downloadStates.value = current
        }

        override fun deleteModel(modelId: String): Boolean {
            val file = getModelFile(modelId)
            cancelDownload(modelId)
            return file?.delete() ?: true
        }

        override fun getAvailableStorageBytes(): Long = 10_000_000_000L // 10 GB

        override fun getModelDirectory(): File = tempDir
    }

    @Test
    fun testDownloadStateTransitions() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "models_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val manager = FakeLocalModelManager(tempDir)
            val modelId = "smollm2-135m"

            assertFalse(manager.isModelDownloaded(modelId))

            manager.startDownload(modelId)
            val state = manager.downloadStates.value[modelId]
            assertTrue(state is ModelDownloadState.Downloading)
            assertEquals(50, (state as ModelDownloadState.Downloading).progressPercent)

            manager.cancelDownload(modelId)
            val cancelledState = manager.downloadStates.value[modelId]
            assertTrue(cancelledState is ModelDownloadState.Idle)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testFileDetectionAndDeletion() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "models_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            val manager = FakeLocalModelManager(tempDir)
            val modelId = "smollm2-135m"
            val file = manager.getModelFile(modelId)!!

            assertFalse(manager.isModelDownloaded(modelId))

            // Simular archivo descargado
            file.writeText("fake gguf weights content")
            assertTrue(manager.isModelDownloaded(modelId))

            // Eliminar
            val deleted = manager.deleteModel(modelId)
            assertTrue(deleted)
            assertFalse(manager.isModelDownloaded(modelId))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

package com.asistente.celular.voice.stt

import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineAsrModelManagerTest {

    private lateinit var tempDir: File
    private lateinit var mockContext: Context
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("asr_models_test").toFile()
        mockContext = object : ContextWrapper(null) {
            override fun getFilesDir(): File = tempDir
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test available models catalogue`() {
        val models = DefaultOfflineAsrModelManager.AVAILABLE_MODELS
        assertTrue(models.isNotEmpty())
        val zipformer = models.firstOrNull { it.id == "sherpa_onnx_zipformer_es" }
        assertNotNull(zipformer)
        assertTrue(zipformer!!.isRecommended)
        assertTrue(zipformer.sizeMb > 0)
    }

    @Test
    fun `test SttEngineType fromName parsing`() {
        assertEquals(SttEngineType.ANDROID_SYSTEM, SttEngineType.fromName("ANDROID_SYSTEM"))
        assertEquals(SttEngineType.ANDROID_SYSTEM, SttEngineType.fromName("android_system"))
        assertEquals(SttEngineType.OFFLINE_SHERPA_ONNX, SttEngineType.fromName("OFFLINE_SHERPA_ONNX"))
        assertEquals(SttEngineType.ANDROID_SYSTEM, SttEngineType.fromName("unknown_invalid_engine"))
        assertEquals(SttEngineType.ANDROID_SYSTEM, SttEngineType.fromName(null))
    }

    @Test
    fun `test startDownload, isDownloaded, and deleteModel lifecycle`() = testScope.runTest {
        val manager = DefaultOfflineAsrModelManager(mockContext, testScope)
        val modelId = "sherpa_onnx_zipformer_es"

        assertFalse(manager.isModelDownloaded(modelId))

        // Start download and wait for completion
        manager.startDownload(modelId)
        manager.getActiveJob(modelId)?.join()

        // Verify it marked as downloaded
        assertTrue(manager.isModelDownloaded(modelId))
        val state = manager.downloadStates.value[modelId]
        assertTrue(state is AsrModelDownloadState.Downloaded)

        // Delete model
        val deleted = manager.deleteModel(modelId)
        assertTrue(deleted)
        assertFalse(manager.isModelDownloaded(modelId))
    }
}

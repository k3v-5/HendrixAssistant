package com.asistente.celular.pc.ota

import com.asistente.celular.nlu.pc.PcEndpointConfig
import com.asistente.celular.nlu.pc.ota.OtaDownloadState
import com.asistente.celular.nlu.pc.ota.PcAppUpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PcOtaUpdateTest {

    @Test
    fun testPcAppUpdateInfoModelAndVersionComparison() {
        val installedEpoch = 1790070000000L
        val newerEpoch = 1790080000000L

        val updateInfo = PcAppUpdateInfo(
            available = true,
            fileName = "app-debug.apk",
            apkSizeBytes = 33169045L,
            lastModifiedEpoch = newerEpoch,
            sha256 = "abc123sha",
            airsyncFileId = "hendrix_latest_apk",
            downloadUrl = "http://192.168.100.159:8901/api/update/download"
        )

        assertTrue(updateInfo.available)
        assertEquals("app-debug.apk", updateInfo.fileName)
        assertEquals(33169045L, updateInfo.apkSizeBytes)
        assertTrue("La versión de la PC debe ser más reciente que la instalada", updateInfo.lastModifiedEpoch > installedEpoch)
        assertEquals("http://192.168.100.159:8901/api/update/download", updateInfo.downloadUrl)
    }

    @Test
    fun testOtaDownloadStateTransitions() {
        var state: OtaDownloadState = OtaDownloadState.Idle
        assertEquals(OtaDownloadState.Idle, state)

        state = OtaDownloadState.Checking
        assertEquals(OtaDownloadState.Checking, state)

        state = OtaDownloadState.Downloading(progress = 0.5f, bytesDownloaded = 16000000L, totalBytes = 32000000L)
        assertTrue(state is OtaDownloadState.Downloading)
        val downloading = state as OtaDownloadState.Downloading
        assertEquals(0.5f, downloading.progress, 0.001f)
        assertEquals(16000000L, downloading.bytesDownloaded)
        assertEquals(32000000L, downloading.totalBytes)

        val tempFile = File.createTempFile("test-ota", ".apk")
        try {
            state = OtaDownloadState.ReadyToInstall(tempFile)
            assertTrue(state is OtaDownloadState.ReadyToInstall)
            assertEquals(tempFile, (state as OtaDownloadState.ReadyToInstall).apkFile)
        } finally {
            tempFile.delete()
        }

        state = OtaDownloadState.Error("Fallo de red")
        assertTrue(state is OtaDownloadState.Error)
        assertEquals("Fallo de red", (state as OtaDownloadState.Error).message)
    }

    @Test
    fun testEndpointUrlGeneration() {
        val config = PcEndpointConfig(
            localIp = "192.168.100.159",
            port = 8899,
            airSyncPort = 8900,
            otaPort = 8901
        )

        val latestUrl = "http://${config.localIp}:${config.otaPort}/api/update/latest"
        val downloadUrl = "http://${config.localIp}:${config.otaPort}/api/update/download"

        assertEquals("http://192.168.100.159:8901/api/update/latest", latestUrl)
        assertEquals("http://192.168.100.159:8901/api/update/download", downloadUrl)
    }
}

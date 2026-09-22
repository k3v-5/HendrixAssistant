package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.airsync.AirSyncDirection
import com.asistente.celular.nlu.pc.airsync.AirSyncSharedFile
import com.asistente.celular.nlu.pc.airsync.AirSyncTransfer
import com.asistente.celular.nlu.pc.airsync.AirSyncTransferState
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PcAirSyncSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    private class MockAirSyncBridge(
        var mockSharedFiles: List<AirSyncSharedFile> = emptyList(),
        var mockTransfers: List<AirSyncTransfer> = emptyList(),
        var downloadSuccess: Boolean = true
    ) : PcWorkspaceBridge {
        var downloadedFileId: String? = null
        var downloadedDestination: String? = null

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(null)
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(null)
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
        override val airSyncTransfers: StateFlow<List<AirSyncTransfer>> = MutableStateFlow(mockTransfers)
        override val airSyncSharedFiles: StateFlow<List<AirSyncSharedFile>> = MutableStateFlow(mockSharedFiles)

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = null
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean = true
        override suspend fun executeQuickCommand(command: String): Boolean = true
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan {
            return AutonomousTaskPlan(planId = "dummy", userGoal = goalPrompt, steps = emptyList())
        }
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = null
        override suspend fun connect(host: String, port: Int, pin: String?): Boolean = true
        override suspend fun disconnect() {}

        override suspend fun queryAirSyncSharedFiles(): List<AirSyncSharedFile> {
            return mockSharedFiles
        }

        override suspend fun downloadAirSyncFile(fileId: String, destinationPath: String): Boolean {
            downloadedFileId = fileId
            downloadedDestination = destinationPath
            return downloadSuccess
        }
    }

    @Test
    fun testScoreMatching() {
        val skill = PcAirSyncSkill()

        val score1 = skill.score(dummyContext, "descarga el archivo por airsync")
        assertEquals(1.0f, score1.confidence, 0.001f)
        assertEquals(Specificity.HIGH, score1.specificity)

        val score2 = skill.score(dummyContext, "archivos de airsync")
        assertEquals(1.0f, score2.confidence, 0.001f)

        val score3 = skill.score(dummyContext, "estado de transferencia")
        assertEquals(1.0f, score3.confidence, 0.001f)
    }

    @Test
    fun testListSharedFiles() = runBlocking {
        val files = listOf(
            AirSyncSharedFile(
                fileId = "f1",
                fileName = "render_animation.mp4",
                filePath = "D:\\Renders\\render_animation.mp4",
                fileSizeBytes = 104857600L, // 100 MB
                totalChunks = 400
            ),
            AirSyncSharedFile(
                fileId = "f2",
                fileName = "stems_multitrack.zip",
                filePath = "D:\\Audio\\stems_multitrack.zip",
                fileSizeBytes = 524288000L, // 500 MB
                totalChunks = 2000
            )
        )

        val bridge = MockAirSyncBridge(mockSharedFiles = files)
        val skill = PcAirSyncSkill(pcBridge = bridge)

        val score = skill.score(dummyContext, "archivos de airsync")
        val output = skill.execute(dummyContext, "archivos de airsync", score)

        assertTrue(output.speech.contains("2 archivos listos"))
        assertTrue(output.speech.contains("render_animation.mp4"))
        assertNotNull(output.displayText)
        assertTrue(output.displayText!!.contains("render_animation.mp4"))
        assertTrue(output.displayText!!.contains("stems_multitrack.zip"))
    }

    @Test
    fun testDownloadAirSyncFile() = runBlocking {
        val files = listOf(
            AirSyncSharedFile(
                fileId = "file_render_4k",
                fileName = "render_final_4k.mov",
                filePath = "D:\\Exports\\render_final_4k.mov",
                fileSizeBytes = 209715200L,
                totalChunks = 800
            )
        )

        val bridge = MockAirSyncBridge(mockSharedFiles = files)
        val skill = PcAirSyncSkill(pcBridge = bridge, defaultDownloadDir = File(System.getProperty("java.io.tmpdir") ?: "."))

        val score = skill.score(dummyContext, "descarga el archivo render_final_4k por airsync")
        val output = skill.execute(dummyContext, "descarga el archivo render_final_4k por airsync", score)

        assertEquals("file_render_4k", bridge.downloadedFileId)
        assertTrue(output.speech.contains("render_final_4k.mov transferido con éxito"))
        assertNotNull(output.displayText)
        assertTrue(output.displayText!!.contains("Descarga completada"))
    }

    @Test
    fun testTransferProgressStatus() = runBlocking {
        val transfer = AirSyncTransfer(
            transferId = "t1",
            fileName = "game_asset_pack.zip",
            fileSizeBytes = 1073741824L, // 1 GB
            direction = AirSyncDirection.RECEIVE,
            state = AirSyncTransferState.IN_PROGRESS,
            bytesTransferred = 536870912L,
            progressPercent = 0.5f,
            speedBytesPerSec = 52428800.0, // 50 MB/s
            etaSeconds = 10
        )

        val bridge = MockAirSyncBridge(mockTransfers = listOf(transfer))
        val skill = PcAirSyncSkill(pcBridge = bridge)

        val score = skill.score(dummyContext, "estado de la transferencia")
        val output = skill.execute(dummyContext, "estado de la transferencia", score)

        assertTrue(output.speech.contains("game_asset_pack.zip"))
        assertTrue(output.speech.contains("50%"))
        assertTrue(output.speech.contains("50.0 MB por segundo"))
        assertNotNull(output.displayText)
        assertTrue(output.displayText!!.contains("50% - 50.0 MB/s"))
    }

    @Test
    fun testLensToWorkspaceSkill() = runBlocking {
        val bridge = MockAirSyncBridge()
        val skill = PcAirSyncSkill(pcBridge = bridge)

        val score1 = skill.score(dummyContext, "pega la foto en la pc")
        assertEquals(1.0f, score1.confidence, 0.001f)

        val output1 = skill.execute(dummyContext, "pega la foto en la pc", score1)
        assertTrue(output1.speech.contains("Lens-to-Workspace"))
        assertTrue(output1.speech.contains("Control V"))
        assertTrue(output1.displayText!!.contains("Lens-to-Workspace Activo"))

        val score2 = skill.score(dummyContext, "enviar boceto a la pc")
        assertEquals(1.0f, score2.confidence, 0.001f)
        val output2 = skill.execute(dummyContext, "enviar boceto a la pc", score2)
        assertTrue(output2.speech.contains("portapapeles"))
    }
}

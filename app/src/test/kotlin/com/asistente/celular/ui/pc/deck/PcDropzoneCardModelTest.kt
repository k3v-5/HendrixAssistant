package com.asistente.celular.ui.pc.deck

import com.asistente.celular.nlu.pc.dropzone.PcDropzoneFile
import com.asistente.celular.service.DropzoneNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el modelo y formateo de entregables de Dropzone.
 */
class PcDropzoneCardModelTest {

    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", DropzoneNotificationHelper.formatFileSize(0L))
        assertEquals("500 B", DropzoneNotificationHelper.formatFileSize(500L))
        assertEquals("1 KB", DropzoneNotificationHelper.formatFileSize(1024L))
        assertEquals("45 KB", DropzoneNotificationHelper.formatFileSize(45 * 1024L))
        assertEquals("3.5 MB", DropzoneNotificationHelper.formatFileSize((3.5 * 1024 * 1024).toLong()))
        assertEquals("1.25 GB", DropzoneNotificationHelper.formatFileSize((1.25 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testPcDropzoneFileProperties() {
        val file = PcDropzoneFile(
            fileName = "carro_futurista_001.png",
            category = "blender_renders",
            sizeBytes = 4194304L, // 4 MB
            relativePath = "Blender/Renders/carro_futurista_001.png",
            timestamp = 1700000000000L
        )

        assertEquals("carro_futurista_001.png", file.fileName)
        assertEquals("blender_renders", file.category)
        assertEquals(4194304L, file.sizeBytes)
        assertTrue(file.relativePath.startsWith("Blender"))
        assertEquals("4.0 MB", DropzoneNotificationHelper.formatFileSize(file.sizeBytes))
    }
}

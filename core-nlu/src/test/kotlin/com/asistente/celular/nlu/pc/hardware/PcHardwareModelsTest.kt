package com.asistente.celular.nlu.pc.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcHardwareModelsTest {

    @Test
    fun testHardwareTelemetryAndThresholds() {
        val normalHw = PcHardwareTelemetry(
            gpuName = "NVIDIA GeForce RTX 4080",
            gpuUsagePercent = 85.5f,
            gpuTempCelsius = 68,
            vramUsedMb = 12000L,
            vramTotalMb = 16384L,
            cpuUsagePercent = 42.0f
        )

        assertFalse(normalHw.isGpuOverheating)
        assertTrue(normalHw.vramUsagePercent > 70f)

        val hotHw = normalHw.copy(gpuTempCelsius = 86)
        assertTrue(hotHw.isGpuOverheating)
    }

    @Test
    fun testRenderWatchdogEvent() {
        val event = PcRenderWatchdogEvent(
            processName = "blender.exe",
            status = "COMPLETED",
            durationSeconds = 720L,
            peakGpuTemp = 74,
            autoSuspendTriggered = true,
            message = "Render de animación finalizado exitosamente."
        )

        assertEquals("blender.exe", event.processName)
        assertEquals("COMPLETED", event.status)
        assertEquals(720L, event.durationSeconds)
        assertTrue(event.autoSuspendTriggered)
    }
}

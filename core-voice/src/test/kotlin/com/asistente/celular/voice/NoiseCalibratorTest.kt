package com.asistente.celular.voice

import com.asistente.celular.voice.acoustic.NoiseCalibrator
import com.asistente.celular.voice.acoustic.NoiseProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseCalibratorTest {

    @Test
    fun testQuietEnvironmentCalibration() {
        val calibrator = NoiseCalibrator(windowHistorySize = 5)
        // Muestras de dormitorio silencioso (~ -52 dBFS)
        repeat(5) {
            val result = calibrator.feedRmsSample(-52.0f)
            assertEquals(NoiseProfile.QUIET, result.profile)
            assertEquals(-52.0f, result.noiseFloorDb, 0.5f)
            assertEquals(10.0f, result.silenceMarginDb, 0.1f)
            assertEquals(-42.0f, result.recommendedVoiceThresholdDb, 0.5f)
        }
    }

    @Test
    fun testModerateEnvironmentCalibration() {
        val calibrator = NoiseCalibrator(windowHistorySize = 5)
        // Muestras de oficina con murmullos (~ -36 dBFS)
        repeat(5) {
            val result = calibrator.feedRmsSample(-36.0f)
            assertEquals(NoiseProfile.MODERATE, result.profile)
            assertEquals(12.0f, result.silenceMarginDb, 0.1f)
            assertEquals(-24.0f, result.recommendedVoiceThresholdDb, 0.5f)
        }
    }

    @Test
    fun testNoisyStreetAdaptation() {
        val calibrator = NoiseCalibrator(windowHistorySize = 3)
        // De silencioso a calle ruidosa
        calibrator.feedRmsSample(-50.0f)
        calibrator.feedRmsSample(-22.0f)
        val result = calibrator.feedRmsSample(-20.0f)

        // El piso de ruido promedio sube y el perfil cambia
        assertTrue(result.noiseFloorDb > -35.0f)
        assertEquals(NoiseProfile.MODERATE, result.profile)

        // Luego de más muestras ruidosas
        calibrator.feedRmsSample(-19.0f)
        calibrator.feedRmsSample(-20.0f)
        val noisyResult = calibrator.feedRmsSample(-21.0f)
        assertEquals(NoiseProfile.NOISY, noisyResult.profile)
        assertEquals(14.0f, noisyResult.silenceMarginDb, 0.1f)
    }
}

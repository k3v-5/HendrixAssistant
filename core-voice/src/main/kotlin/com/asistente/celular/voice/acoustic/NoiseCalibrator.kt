package com.asistente.celular.voice.acoustic

/**
 * Perfiles acústicos basados en el nivel medio de decibelios RMS del entorno.
 */
enum class NoiseProfile(val displayName: String) {
    QUIET("Silencioso"),
    MODERATE("Moderado"),
    NOISY("Ruidoso"),
    VERY_NOISY("Muy ruidoso")
}

/**
 * Resultado de calibración acústica dinámica.
 */
data class AcousticCalibrationResult(
    val noiseFloorDb: Float,
    val profile: NoiseProfile,
    val recommendedVoiceThresholdDb: Float,
    val silenceMarginDb: Float
)

/**
 * Calibrador acústico dinámico en tiempo real.
 * Mide el piso de ruido ambiental (noise floor) y calibra los umbrales de detección
 * para evitar falsos positivos en entornos ruidosos y mantener alta sensibilidad en entornos silenciosos.
 */
class NoiseCalibrator(
    private val windowHistorySize: Int = 10
) {
    private val noiseSamples = mutableListOf<Float>()

    /**
     * Alimenta un buffer PCM de silencio o ruido de fondo para calibración.
     */
    fun feedSilenceSample(buffer: ShortArray, readSize: Int = buffer.size): AcousticCalibrationResult {
        val rms = AcousticFeatureExtractor.calculateRmsDb(buffer, readSize)
        return feedRmsSample(rms)
    }

    /**
     * Alimenta un valor RMS medido directamente.
     */
    fun feedRmsSample(rmsDb: Float): AcousticCalibrationResult {
        if (noiseSamples.size >= windowHistorySize) {
            noiseSamples.removeAt(0)
        }
        noiseSamples.add(rmsDb)

        val averageNoise = noiseSamples.average().toFloat()
        val profile = when {
            averageNoise < -45.0f -> NoiseProfile.QUIET
            averageNoise < -28.0f -> NoiseProfile.MODERATE
            averageNoise < -18.0f -> NoiseProfile.NOISY
            else -> NoiseProfile.VERY_NOISY
        }

        // Margen dinámico sobre el piso de ruido para detectar voz humana
        val margin = when (profile) {
            NoiseProfile.QUIET -> 10.0f
            NoiseProfile.MODERATE -> 12.0f
            NoiseProfile.NOISY -> 14.0f
            NoiseProfile.VERY_NOISY -> 16.0f
        }

        val recommendedThreshold = (averageNoise + margin).coerceAtMost(-10.0f)

        return AcousticCalibrationResult(
            noiseFloorDb = averageNoise,
            profile = profile,
            recommendedVoiceThresholdDb = recommendedThreshold,
            silenceMarginDb = margin
        )
    }

    fun getEstimatedFloor(): Float = if (noiseSamples.isEmpty()) -50.0f else noiseSamples.average().toFloat()

    fun reset() {
        noiseSamples.clear()
    }
}

package com.asistente.celular.voice.biometrics

/**
 * Perfil acústico que almacena la huella vocal comprimida del usuario.
 */
data class VoiceprintProfile(
    val speakerId: String,
    val speakerName: String,
    val acousticCentroid: FloatArray,
    val energyThreshold: Float = 0.05f,
    val sampleCount: Int = 1,
    val createdAtEpoch: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VoiceprintProfile) return false
        return speakerId == other.speakerId
    }

    override fun hashCode(): Int {
        return speakerId.hashCode()
    }
}

/**
 * Resultado de verificación biométrica de voz en tiempo real.
 */
data class BiometricMatchResult(
    val isMatch: Boolean,
    val confidence: Float,
    val identifiedSpeaker: String?,
    val details: String = ""
)

/**
 * Contrato para el motor de biometría e identificación de voz offline.
 * Diseñado con escalabilidad preventiva para soportar modelos ONNX de embeddings
 * (e.g., CAM++ o ResNet) o extractores de características espectrales ligeras.
 */
interface VoiceBiometricsEngine {
    fun extractVoiceprint(pcmData: ShortArray): FloatArray
    suspend fun enrollSpeaker(speakerId: String, speakerName: String, pcmSamples: List<ShortArray>): VoiceprintProfile
    suspend fun verifySpeaker(pcmData: ShortArray, targetSpeakerId: String? = null): BiometricMatchResult
    fun getRegisteredProfiles(): List<VoiceprintProfile>
    fun deleteProfile(speakerId: String): Boolean
}

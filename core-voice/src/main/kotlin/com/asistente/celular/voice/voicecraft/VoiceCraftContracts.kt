package com.asistente.celular.voice.voicecraft

data class VoiceProfileCraftConfig(
    val voiceId: String = "hendrix_default",
    val pitchSemitones: Float = 0.0f, // -6.0 to +6.0
    val speechRateMultiplier: Float = 1.0f, // 0.7 to 1.5
    val formantShiftFactor: Float = 1.0f, // 0.85 to 1.15
    val warmthBoostDb: Float = 2.0f,
    val styleName: String = "Natural Cálido"
)

/**
 * Contrato para el estudio de personalización tímbrica y modulación de voz local de Hendrix.
 */
interface VoiceCraftStudioController {
    fun applyProfile(config: VoiceProfileCraftConfig)
    fun getCurrentProfile(): VoiceProfileCraftConfig
    fun getAvailablePresets(): List<VoiceProfileCraftConfig>
    suspend fun previewVoice(sampleText: String): Boolean
}

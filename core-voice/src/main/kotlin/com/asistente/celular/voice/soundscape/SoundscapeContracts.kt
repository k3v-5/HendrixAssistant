package com.asistente.celular.voice.soundscape

enum class SoundscapeType {
    BROWN_NOISE,
    WHITE_NOISE,
    BINAURAL_ALPHA, // ~10 Hz diferencia (concentración y enfoque)
    BINAURAL_DELTA, // ~2 Hz diferencia (sueño profundo)
    RAIN_PROCEDURAL,
    CAMPFIRE_PROCEDURAL
}

data class SoundscapeState(
    val isPlaying: Boolean = false,
    val activeType: SoundscapeType? = null,
    val volume: Float = 0.7f,
    val remainingMinutes: Int? = null
)

/**
 * Contrato para el motor de generación matemática procedural de paisajes sonoros y frecuencias binaurales.
 */
interface SoundscapeAudioEngine {
    fun startSoundscape(type: SoundscapeType, durationMinutes: Int? = null): SoundscapeState
    fun stopSoundscape(): Boolean
    fun setVolume(volume: Float)
    fun getCurrentState(): SoundscapeState
}

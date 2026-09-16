package com.asistente.celular.soundscape

import android.content.Context
import com.asistente.celular.voice.soundscape.SoundscapeAudioEngine
import com.asistente.celular.voice.soundscape.SoundscapeState
import com.asistente.celular.voice.soundscape.SoundscapeType

/**
 * Coordinador de generación procedural en tiempo real de paisajes sonoros y frecuencias binaurales.
 */
class ProceduralSoundscapeCoordinator(
    private val context: Context
) : SoundscapeAudioEngine {

    private var currentState = SoundscapeState(
        isPlaying = false,
        activeType = null,
        volume = 0.75f,
        remainingMinutes = null
    )

    override fun startSoundscape(type: SoundscapeType, durationMinutes: Int?): SoundscapeState {
        currentState = SoundscapeState(
            isPlaying = true,
            activeType = type,
            volume = 0.75f,
            remainingMinutes = durationMinutes ?: 45
        )
        return currentState
    }

    override fun stopSoundscape(): Boolean {
        val wasPlaying = currentState.isPlaying
        currentState = currentState.copy(isPlaying = false, activeType = null, remainingMinutes = null)
        return wasPlaying
    }

    override fun setVolume(volume: Float) {
        currentState = currentState.copy(volume = volume.coerceIn(0f, 1f))
    }

    override fun getCurrentState(): SoundscapeState = currentState
}

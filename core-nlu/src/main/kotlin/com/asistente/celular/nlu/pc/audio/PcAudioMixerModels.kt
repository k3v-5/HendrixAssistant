package com.asistente.celular.nlu.pc.audio

/**
 * Representa una sesión de audio activa de una aplicación en Windows
 * (ej. Ableton Live, FL Studio, Google Chrome, Spotify, Discord).
 */
data class PcAudioAppSession(
    val id: String,
    val processName: String,
    val displayName: String,
    val volumePercent: Int = 100,
    val isMuted: Boolean = false,
    val iconEmoji: String = resolveDefaultIcon(processName)
) {
    companion object {
        fun resolveDefaultIcon(processName: String): String {
            val lower = processName.lowercase()
            return when {
                lower.contains("ableton") -> "🎹"
                lower.contains("fl") || lower.contains("fl64") || lower.contains("flengine") -> "🍊"
                lower.contains("chrome") -> "🌐"
                lower.contains("brave") -> "🦁"
                lower.contains("opera") -> "🔴"
                lower.contains("spotify") -> "🟢"
                lower.contains("discord") -> "💬"
                lower.contains("blender") -> "🧊"
                lower.contains("unreal") -> "🎮"
                lower.contains("premiere") || lower.contains("afterfx") || lower.contains("photoshop") -> "🎨"
                lower.contains("vlc") || lower.contains("wmplayer") -> "🎬"
                else -> "🔊"
            }
        }
    }
}

/**
 * Estado global del mezclador de audio de Windows.
 * Contiene el fader maestro general y los faders por aplicación individual.
 */
data class PcAudioMixerState(
    val masterVolumePercent: Int = 50,
    val isMasterMuted: Boolean = false,
    val sessions: List<PcAudioAppSession> = emptyList(),
    val timestampEpoch: Long = System.currentTimeMillis()
)

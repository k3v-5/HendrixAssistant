package com.asistente.celular.nlu.pc.audio

/**
 * Estado en tiempo real del monitoreo y streaming inalámbrico de audio desde la PC.
 */
data class PcAudioStreamState(
    val isStreaming: Boolean = false,
    val sampleRate: Int = 24000,
    val channels: Int = 1,
    val currentRms: Float = 0.0f,
    val currentPeak: Float = 0.0f,
    val driverName: String = "unknown"
)

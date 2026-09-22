package com.asistente.celular.pc.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Reproductor de audio inalámbrico de baja latencia (< 30ms) para Hendrix Studio.
 * Recibe tramas binarias PCM 16-bit por WebSocket y las escribe directamente en un AudioTrack
 * dedicado en modo streaming, calculando niveles RMS en tiempo real para el vúmetro.
 */
class PcAudioStreamPlayer {

    companion object {
        private const val TAG = "PcAudioStreamPlayer"
        const val DEFAULT_SAMPLE_RATE = 24000
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentVolume = 1.0f

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    private val _currentRms = MutableStateFlow(0.0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    private val _currentPeak = MutableStateFlow(0.0f)
    val currentPeak: StateFlow<Float> = _currentPeak.asStateFlow()

    @Synchronized
    fun start(sampleRate: Int = DEFAULT_SAMPLE_RATE, channels: Int = 1): Boolean {
        if (isPlaying) return true

        try {
            val channelConfig = if (channels == 2) {
                AudioFormat.CHANNEL_OUT_STEREO
            } else {
                AudioFormat.CHANNEL_OUT_MONO
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSize = max(minBufferSize, sampleRate * channels * 2 / 10) // ~100ms de buffer

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(currentVolume)
            audioTrack?.play()
            isPlaying = true
            _isPlayingState.value = true
            Log.i(TAG, "AudioTrack iniciado correctamente (${sampleRate}Hz, ${channels}ch)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando AudioTrack: ${e.message}", e)
            stop()
            return false
        }
    }

    @Synchronized
    fun stop() {
        isPlaying = false
        _isPlayingState.value = false
        _currentRms.value = 0.0f
        _currentPeak.value = 0.0f

        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error deteniendo AudioTrack: ${e.message}")
        }
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando AudioTrack: ${e.message}")
        } finally {
            audioTrack = null
        }
    }

    fun setVolume(volume: Float) {
        val clamped = max(0.0f, min(1.0f, volume))
        currentVolume = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Error ajustando volumen: ${e.message}")
        }
    }

    fun writePcmChunk(pcmBytes: ByteArray, offset: Int = 0, length: Int = pcmBytes.size) {
        val track = audioTrack
        if (!isPlaying || track == null || length <= 0) return

        // 1. Calcular RMS y pico en las muestras de 16 bits para el vúmetro
        try {
            val sampleCount = length / 2
            if (sampleCount > 0) {
                var sumSquares = 0.0
                var peak = 0.0f
                val buffer = ByteBuffer.wrap(pcmBytes, offset, length).order(ByteOrder.LITTLE_ENDIAN)

                for (i in 0 until sampleCount) {
                    val sampleShort = buffer.short
                    val normalized = abs(sampleShort.toFloat() / 32768.0f)
                    sumSquares += (normalized * normalized).toDouble()
                    if (normalized > peak) {
                        peak = normalized
                    }
                }

                val rms = sqrt(sumSquares / sampleCount).toFloat()
                _currentRms.value = rms
                _currentPeak.value = peak
            }
        } catch (e: Exception) {
            // Ignorar errores de cálculo en métricas
        }

        // 2. Escribir audio de forma no bloqueante
        try {
            track.write(pcmBytes, offset, length, AudioTrack.WRITE_NON_BLOCKING)
        } catch (e: Exception) {
            Log.w(TAG, "Error escribiendo en AudioTrack: ${e.message}")
        }
    }
}

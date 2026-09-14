package com.asistente.celular.voice.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Capturador de audio de micrófono en formato estándar 16 kHz, 16-bit Mono.
 * Compatible directamente con los motores de Sherpa-ONNX y Silero VAD.
 */
class AudioRecordSource(
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    val active: Boolean
        get() = isRecording.get()

    @SuppressLint("MissingPermission")
    fun start(onBuffer: (FloatArray, Int) -> Unit) {
        if (isRecording.get()) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, sampleRate / 10) // ~100ms por chunk

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord no pudo inicializarse.")
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingThread = Thread({
                val shortBuffer = ShortArray(bufferSize)
                val floatBuffer = FloatArray(bufferSize)

                while (isRecording.get()) {
                    val readSamples = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (readSamples > 0) {
                        // Convertir PCM 16-bit short (-32768 a 32767) a Float normalizado (-1.0f a 1.0f)
                        for (i in 0 until readSamples) {
                            floatBuffer[i] = shortBuffer[i] / 32768.0f
                        }
                        onBuffer(floatBuffer, readSamples)
                    }
                }
            }, "AudioRecordSourceThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando captura de micrófono: ${e.message}", e)
            stop()
        }
    }

    fun stop() {
        if (!isRecording.compareAndSet(true, false)) return

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join(500)
            recordingThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo AudioRecord: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "AudioRecordSource"
    }
}

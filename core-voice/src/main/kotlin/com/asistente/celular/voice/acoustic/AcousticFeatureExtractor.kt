package com.asistente.celular.voice.acoustic

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Estados acústicos y emocionales detectables a partir del flujo de audio PCM crudo.
 */
enum class AcousticEmotion {
    CALM,
    NORMAL,
    EXCITED_OR_URGENT,
    WHISPER
}

/**
 * Extractor de características acústicas en tiempo real sobre buffers PCM de 16 bits.
 * Permite detectar susurro (Whisper Mode) y modulación de urgencia o calma sin requerir modelos pesados.
 */
object AcousticFeatureExtractor {

    /**
     * Calcula la energía RMS (Root Mean Square) en decibelios relativos a escala completa (dBFS).
     * Valores típicos: Silencio (-60 a -50 dBFS), Susurro (-45 a -28 dBFS), Voz normal (-24 a -14 dBFS), Grito (> -10 dBFS).
     */
    fun calculateRmsDb(buffer: ShortArray, readSize: Int = buffer.size): Float {
        if (readSize <= 0) return -100f
        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        val meanSquare = sumSquares / readSize
        val rms = sqrt(meanSquare)
        if (rms <= 0.0001) return -100f
        val dbfs = 20.0 * log10(rms / 32768.0)
        return dbfs.toFloat()
    }

    /**
     * Calcula la tasa de cruce por cero (Zero Crossing Rate - ZCR).
     * En el habla susurrada, la turbulencia sin cuerdas vocales genera una ZCR notablemente alta.
     */
    fun calculateZeroCrossingRate(buffer: ShortArray, readSize: Int = buffer.size): Float {
        if (readSize <= 1) return 0f
        var crossings = 0
        for (i in 1 until readSize) {
            val prev = buffer[i - 1]
            val curr = buffer[i]
            if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / (readSize - 1)
    }

    /**
     * Determina si el audio corresponde a habla susurrada.
     */
    fun isWhisper(buffer: ShortArray, readSize: Int = buffer.size): Boolean {
        val rms = calculateRmsDb(buffer, readSize)
        val zcr = calculateZeroCrossingRate(buffer, readSize)

        // Susurro: Energía atenuada (-42 a -25 dBFS) con componente fricativa no vocalizada (alta tasa de cruce por cero)
        return rms in -45.0f..-25.0f && zcr > 0.09f
    }

    /**
     * Clasifica el tono acústico general del segmento de audio.
     */
    fun classifyAcousticState(buffer: ShortArray, readSize: Int = buffer.size): AcousticEmotion {
        val rms = calculateRmsDb(buffer, readSize)
        val zcr = calculateZeroCrossingRate(buffer, readSize)

        return when {
            isWhisper(buffer, readSize) -> AcousticEmotion.WHISPER
            rms > -13.0f -> AcousticEmotion.EXCITED_OR_URGENT
            rms < -35.0f -> AcousticEmotion.CALM
            else -> AcousticEmotion.NORMAL
        }
    }
}

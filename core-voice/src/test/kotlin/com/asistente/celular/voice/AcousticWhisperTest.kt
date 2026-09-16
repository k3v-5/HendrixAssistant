package com.asistente.celular.voice

import com.asistente.celular.voice.acoustic.AcousticEmotion
import com.asistente.celular.voice.acoustic.AcousticFeatureExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcousticWhisperTest {

    @Test
    fun testSilenceDetection() {
        val emptyPcm = ShortArray(1600) { 0 }
        val rms = AcousticFeatureExtractor.calculateRmsDb(emptyPcm)
        assertEquals(-100.0f, rms, 0.1f)

        val classification = AcousticFeatureExtractor.classifyAcousticState(emptyPcm)
        assertEquals(AcousticEmotion.CALM, classification)
    }

    @Test
    fun testWhisperClassification() {
        // Generar señal de baja energía pero alta frecuencia (típica de voz susurrada / sibilante)
        val whisperPcm = ShortArray(1600) { i ->
            val sign = if (i % 3 == 0) -1 else 1
            // Amplitud moderada/baja (~600 de 32767) -> aprox -35 dBFS
            (600 * sign).toShort()
        }

        val rms = AcousticFeatureExtractor.calculateRmsDb(whisperPcm)
        val zcr = AcousticFeatureExtractor.calculateZeroCrossingRate(whisperPcm)

        assertTrue("RMS debe estar atenuado para susurro (< -25 dBFS)", rms < -25.0f)
        assertTrue("ZCR debe ser elevado para susurro (> 0.09)", zcr > 0.09f)
        assertTrue(AcousticFeatureExtractor.isWhisper(whisperPcm))
        assertEquals(AcousticEmotion.WHISPER, AcousticFeatureExtractor.classifyAcousticState(whisperPcm))
    }

    @Test
    fun testNormalVoiceClassification() {
        // Generar tono sinusoidal de mayor amplitud a baja frecuencia (voz vocalizada normal)
        val normalPcm = ShortArray(1600) { i ->
            val angle = 2.0 * Math.PI * i / 80.0 // frecuencia fundamental baja
            (12000.0 * Math.sin(angle)).toInt().toShort()
        }

        val rms = AcousticFeatureExtractor.calculateRmsDb(normalPcm)
        val zcr = AcousticFeatureExtractor.calculateZeroCrossingRate(normalPcm)

        assertTrue("RMS debe ser fuerte para voz normal (> -25 dBFS)", rms > -25.0f)
        assertFalse(AcousticFeatureExtractor.isWhisper(normalPcm))
    }
}

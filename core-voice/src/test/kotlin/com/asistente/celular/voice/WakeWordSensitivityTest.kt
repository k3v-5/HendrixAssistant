package com.asistente.celular.voice

import com.asistente.celular.voice.kws.WakeWordSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordSensitivityTest {

    @Test
    fun testSensitivityParametersOrder() {
        // En LOW, se requiere mayor energía RMS para disparar la detección (entornos ruidosos)
        assertTrue(WakeWordSensitivity.LOW.voiceRmsThreshold > WakeWordSensitivity.MEDIUM.voiceRmsThreshold)
        assertTrue(WakeWordSensitivity.MEDIUM.voiceRmsThreshold > WakeWordSensitivity.HIGH.voiceRmsThreshold)

        // En LOW, se requieren más frames consecutivos para confirmar que es voz real y no un pico transitorio
        assertTrue(WakeWordSensitivity.LOW.requiredVoiceFrames > WakeWordSensitivity.MEDIUM.requiredVoiceFrames)
        assertTrue(WakeWordSensitivity.MEDIUM.requiredVoiceFrames > WakeWordSensitivity.HIGH.requiredVoiceFrames)

        // En LOW, el enfriamiento es más largo para evitar re-disparos por ruido sostenido
        assertTrue(WakeWordSensitivity.LOW.cooldownMillis > WakeWordSensitivity.MEDIUM.cooldownMillis)
        assertTrue(WakeWordSensitivity.MEDIUM.cooldownMillis > WakeWordSensitivity.HIGH.cooldownMillis)
    }

    @Test
    fun testFromNameParsing() {
        assertEquals(WakeWordSensitivity.LOW, WakeWordSensitivity.fromName("LOW"))
        assertEquals(WakeWordSensitivity.LOW, WakeWordSensitivity.fromName("low"))
        assertEquals(WakeWordSensitivity.MEDIUM, WakeWordSensitivity.fromName("medium"))
        assertEquals(WakeWordSensitivity.HIGH, WakeWordSensitivity.fromName("HIGH"))
        // Fallback predeterminado para valores desconocidos o nulos
        assertEquals(WakeWordSensitivity.MEDIUM, WakeWordSensitivity.fromName("INVALID_UNKNOWN"))
        assertEquals(WakeWordSensitivity.MEDIUM, WakeWordSensitivity.fromName(null))
        assertEquals(WakeWordSensitivity.HIGH, WakeWordSensitivity.fromName(null, default = WakeWordSensitivity.HIGH))
    }

    @Test
    fun testDisplayNames() {
        assertTrue(WakeWordSensitivity.LOW.displayName.contains("Baja"))
        assertTrue(WakeWordSensitivity.MEDIUM.displayName.contains("Media"))
        assertTrue(WakeWordSensitivity.HIGH.displayName.contains("Alta"))
    }
}

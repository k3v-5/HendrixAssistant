package com.asistente.celular.nlu.pc.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcAudioMixerModelsTest {

    @Test
    fun testAppSessionIconResolution() {
        assertEquals("🎹", PcAudioAppSession.resolveDefaultIcon("Ableton Live 11 Suite.exe"))
        assertEquals("🍊", PcAudioAppSession.resolveDefaultIcon("FL64.exe"))
        assertEquals("🌐", PcAudioAppSession.resolveDefaultIcon("chrome.exe"))
        assertEquals("🟢", PcAudioAppSession.resolveDefaultIcon("Spotify.exe"))
        assertEquals("💬", PcAudioAppSession.resolveDefaultIcon("Discord.exe"))
        assertEquals("🧊", PcAudioAppSession.resolveDefaultIcon("blender.exe"))
        assertEquals("🎮", PcAudioAppSession.resolveDefaultIcon("UnrealEditor.exe"))
        assertEquals("🔊", PcAudioAppSession.resolveDefaultIcon("unknown_process.exe"))
    }

    @Test
    fun testAudioMixerState() {
        val abletonSession = PcAudioAppSession(
            id = "ableton_1",
            processName = "Ableton Live 11 Suite.exe",
            displayName = "Ableton Live",
            volumePercent = 85,
            isMuted = false
        )
        val chromeSession = PcAudioAppSession(
            id = "chrome_1",
            processName = "chrome.exe",
            displayName = "Google Chrome",
            volumePercent = 50,
            isMuted = true
        )

        val mixer = PcAudioMixerState(
            masterVolumePercent = 70,
            isMasterMuted = false,
            sessions = listOf(abletonSession, chromeSession)
        )

        assertEquals(70, mixer.masterVolumePercent)
        assertFalse(mixer.isMasterMuted)
        assertEquals(2, mixer.sessions.size)
        assertEquals("🎹", mixer.sessions[0].iconEmoji)
        assertEquals(85, mixer.sessions[0].volumePercent)
        assertTrue(mixer.sessions[1].isMuted)
    }
}

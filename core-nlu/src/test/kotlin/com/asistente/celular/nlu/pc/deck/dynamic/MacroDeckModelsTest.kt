package com.asistente.celular.nlu.pc.deck.dynamic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroDeckModelsTest {

    @Test
    fun testAllProfilesRegistered() {
        val profiles = MacroDeckProfileRegistry.getAllProfiles()
        assertTrue("Debe contener al menos 5 perfiles predefinidos", profiles.size >= 5)

        val blender = MacroDeckProfileRegistry.BLENDER_PROFILE
        assertEquals("profile_blender", blender.id)
        assertTrue(blender.controls.isNotEmpty())

        val ableton = MacroDeckProfileRegistry.ABLETON_PROFILE
        assertEquals("profile_ableton", ableton.id)
        assertTrue(ableton.controls.isNotEmpty())

        val dev = MacroDeckProfileRegistry.DEV_PROFILE
        assertEquals("profile_code", dev.id)
        assertTrue(dev.controls.isNotEmpty())
    }

    @Test
    fun testFindProfileForProcess() {
        assertEquals(
            MacroDeckProfileRegistry.BLENDER_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("blender.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.BLENDER_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("C:\\Program Files\\Blender Foundation\\Blender 4.0\\blender.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.DEV_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("Code.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.DEV_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("idea64.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.DEV_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("windowsterminal.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.ABLETON_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("Ableton Live 11 Suite.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.PREMIERE_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("Adobe Premiere Pro.exe").id
        )

        assertEquals(
            MacroDeckProfileRegistry.PREMIERE_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("davinci resolve.exe").id
        )

        // Fallback
        assertEquals(
            MacroDeckProfileRegistry.DESKTOP_PROFILE.id,
            MacroDeckProfileRegistry.findProfileForProcess("notepad.exe").id
        )
    }

    @Test
    fun testControlProperties() {
        val devProfile = MacroDeckProfileRegistry.DEV_PROFILE
        val buttons: List<MacroDeckControl.MacroButton> = devProfile.controls.filterIsInstance<MacroDeckControl.MacroButton>()
        val buildBtn = buttons.firstOrNull { it.id == "dev_build" }
        assertNotNull(buildBtn)
        assertEquals("Compilar", buildBtn!!.label)
        assertTrue(buildBtn.action is MacroDeckAction.QuickCommandAction)

        val videoProfile = MacroDeckProfileRegistry.VIDEO_EDIT_PROFILE
        val jogWheels: List<MacroDeckControl.MacroJogWheel> = videoProfile.controls.filterIsInstance<MacroDeckControl.MacroJogWheel>()
        val jog = jogWheels.firstOrNull()
        assertNotNull(jog)
        assertEquals("Timeline Jog / Shuttle", jog!!.label)
    }
}

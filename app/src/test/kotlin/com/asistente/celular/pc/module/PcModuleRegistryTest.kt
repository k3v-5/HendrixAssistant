package com.asistente.celular.pc.module

import com.asistente.celular.nlu.pc.module.PcModuleCategory
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.nlu.pc.module.PcModuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el registro de módulos de PC.
 * Valida la integridad de las definiciones declarativas, categorías y macros de atajos.
 */
class PcModuleRegistryTest {

    @Test
    fun testAllModulesRegistered() {
        val modules = PcModuleRegistry.ALL_MODULES
        assertEquals(17, modules.size)

        val ids = modules.map { it.id }.toSet()
        assertTrue(ids.contains(PcModuleId.ABLETON_LIVE))
        assertTrue(ids.contains(PcModuleId.FL_STUDIO))
        assertTrue(ids.contains(PcModuleId.ADOBE_CREATIVE))
        assertTrue(ids.contains(PcModuleId.BLENDER))
        assertTrue(ids.contains(PcModuleId.UNREAL_ENGINE))
        assertTrue(ids.contains(PcModuleId.WEB_BROWSERS))
        assertTrue(ids.contains(PcModuleId.CLIPBOARD_MANAGER))
        assertTrue(ids.contains(PcModuleId.AUDIO_MIXER))
        assertTrue(ids.contains(PcModuleId.STUDIO_SCENES))
        assertTrue(ids.contains(PcModuleId.PROJECT_BROWSER))
        assertTrue(ids.contains(PcModuleId.HARDWARE_WATCHDOG))
        assertTrue(ids.contains(PcModuleId.SCREEN_COPILOT))
        assertTrue(ids.contains(PcModuleId.CUSTOM_PLUGINS))
        assertTrue(ids.contains(PcModuleId.WIRELESS_AUDIO_MONITOR))
        assertTrue(ids.contains(PcModuleId.AUTOMATED_ROUTINES))
        assertTrue(ids.contains(PcModuleId.AIRSYNC_P2P))
        assertTrue(ids.contains(PcModuleId.WORKSPACE_TERMINAL_MEMORY))
    }

    @Test
    fun testModuleCategoriesAndMacros() {
        val fl = PcModuleRegistry.findById(PcModuleId.FL_STUDIO)
        assertNotNull(fl)
        assertEquals(PcModuleCategory.AUDIO_DAW, fl?.category)
        assertTrue(fl!!.quickMacros.any { it.actionId == "PLAY_PAUSE" })
        assertTrue(fl.quickMacros.any { it.actionId == "RECORD" })

        val blender = PcModuleRegistry.findById(PcModuleId.BLENDER)
        assertNotNull(blender)
        assertEquals(PcModuleCategory.THREE_D_VFX, blender?.category)
        assertTrue(blender!!.quickMacros.any { it.actionId == "RENDER_IMAGE" })
        assertTrue(blender.quickMacros.any { it.actionId == "RENDER_ANIM" })

        val unreal = PcModuleRegistry.findById(PcModuleId.UNREAL_ENGINE)
        assertNotNull(unreal)
        assertEquals(PcModuleCategory.GAME_ENGINE, unreal?.category)
        assertTrue(unreal!!.quickMacros.any { it.actionId == "PLAY_IN_EDITOR" })

        val adobe = PcModuleRegistry.findById(PcModuleId.ADOBE_CREATIVE)
        assertNotNull(adobe)
        assertEquals(PcModuleCategory.CREATIVE_DESIGN, adobe?.category)
        assertTrue(adobe!!.quickMacros.any { it.actionId == "RAZOR_TOOL" })

        val browser = PcModuleRegistry.findById(PcModuleId.WEB_BROWSERS)
        assertNotNull(browser)
        assertEquals(PcModuleCategory.WEB_INTERNET, browser?.category)
        assertTrue(browser!!.quickMacros.any { it.actionId == "OPEN_CHROME" })
        assertTrue(browser.quickMacros.any { it.actionId == "OPEN_BRAVE" })
        assertTrue(browser!!.quickMacros.any { it.actionId == "SEARCH_GOOGLE" })
        assertTrue(browser.quickMacros.any { it.actionId == "SEARCH_YOUTUBE" })

        val copilot = PcModuleRegistry.findById(PcModuleId.SCREEN_COPILOT)
        assertNotNull(copilot)
        assertEquals(PcModuleCategory.SYSTEM_TOOLS, copilot?.category)
        assertTrue(copilot!!.quickMacros.any { it.actionId == "COPILOT_DIAGNOSE" })

        val plugins = PcModuleRegistry.findById(PcModuleId.CUSTOM_PLUGINS)
        assertNotNull(plugins)
        assertEquals(PcModuleCategory.SYSTEM_TOOLS, plugins?.category)
        assertTrue(plugins!!.quickMacros.any { it.actionId == "PLUGINS_REFRESH" })
    }
}

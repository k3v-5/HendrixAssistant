package com.asistente.celular.nlu.pc.project

import org.junit.Assert.assertEquals
import org.junit.Test

class PcProjectModelsTest {

    @Test
    fun testProjectCategoryResolution() {
        assertEquals(PcProjectCategory.AUDIO_DAW, PcProjectItem.resolveCategory(".als"))
        assertEquals(PcProjectCategory.AUDIO_DAW, PcProjectItem.resolveCategory("flp"))
        assertEquals(PcProjectCategory.THREE_D_VFX, PcProjectItem.resolveCategory("blend"))
        assertEquals(PcProjectCategory.THREE_D_VFX, PcProjectItem.resolveCategory(".uproject"))
        assertEquals(PcProjectCategory.VIDEO_DESIGN, PcProjectItem.resolveCategory("prproj"))
        assertEquals(PcProjectCategory.VIDEO_DESIGN, PcProjectItem.resolveCategory("aep"))
        assertEquals(PcProjectCategory.CODE_DEV, PcProjectItem.resolveCategory("py"))
        assertEquals(PcProjectCategory.OTHER, PcProjectItem.resolveCategory("txt"))
    }

    @Test
    fun testProjectItemProperties() {
        val proj = PcProjectItem(
            id = "synth_beat",
            name = "Synthwave Neon 2026",
            path = "D:/Musica/Ableton/Synthwave Neon 2026.als",
            category = PcProjectCategory.AUDIO_DAW,
            extension = "als",
            sizeBytes = 2500000L
        )

        assertEquals("Synthwave Neon 2026", proj.name)
        assertEquals("als", proj.extension)
        assertEquals("🎹", proj.iconEmoji)
        assertEquals(2500000L, proj.sizeBytes)
    }
}

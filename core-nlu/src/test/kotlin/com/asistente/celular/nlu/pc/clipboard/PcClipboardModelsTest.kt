package com.asistente.celular.nlu.pc.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcClipboardModelsTest {

    @Test
    fun testSha256ChecksumAndLengthIntegrity() {
        val sampleText = "Render ultra-fotorrealista en Blender 3D con iluminación Cycles y 4000 samples."
        val payload = PcClipboardPayload.fromText(sampleText)

        assertEquals(sampleText, payload.text)
        assertEquals(sampleText.length, payload.charCount)
        assertEquals(64, payload.checksumSha256.length) // SHA-256 en hex son 64 caracteres

        // Verificación de determinismo
        val secondPayload = PcClipboardPayload.fromText(sampleText)
        assertEquals(payload.checksumSha256, secondPayload.checksumSha256)

        // Verificación ante alteración de texto
        val alteredPayload = PcClipboardPayload.fromText(sampleText + " extra")
        assertNotEquals(payload.checksumSha256, alteredPayload.checksumSha256)
    }

    @Test
    fun testPcSnippetItem() {
        val snippet = PcSnippetItem(
            id = "blender_render",
            title = "Blender Cycles Render Prompt",
            content = "bpy.ops.render.render(write_still=True)",
            iconEmoji = "🧊"
        )

        assertEquals("blender_render", snippet.id)
        assertEquals("Blender Cycles Render Prompt", snippet.title)
        assertEquals("🧊", snippet.iconEmoji)
        assertTrue(snippet.content.contains("bpy.ops.render"))
    }
}

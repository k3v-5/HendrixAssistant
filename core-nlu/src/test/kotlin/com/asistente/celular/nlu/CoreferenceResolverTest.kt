package com.asistente.celular.nlu

import com.asistente.celular.nlu.context.ConversationSessionTracker
import com.asistente.celular.nlu.context.CoreferenceResolver
import com.asistente.celular.nlu.context.DialogEntity
import com.asistente.celular.nlu.context.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreferenceResolverTest {

    @Test
    fun testResolveLightPronoun() {
        val tracker = ConversationSessionTracker()
        tracker.recordTurn(
            userInput = "prende el foco de la sala",
            primaryEntity = DialogEntity(
                name = "foco de la sala",
                type = EntityType.LIGHT,
                attributes = mapOf("color" to "rojo", "brightness" to "80")
            )
        )

        val resolvedTurnOff = CoreferenceResolver.resolve("apágala", tracker)
        assertTrue(resolvedTurnOff.wasModified)
        assertEquals("apaga el foco de la sala", resolvedTurnOff.resolvedText)

        val resolvedTenue = CoreferenceResolver.resolve("hazla más tenue", tracker)
        assertTrue(resolvedTenue.wasModified)
        assertEquals("baja el brillo del foco de la sala", resolvedTenue.resolvedText)
    }

    @Test
    fun testResolveVolumePronoun() {
        val tracker = ConversationSessionTracker()
        tracker.recordTurn(
            userInput = "pon el volumen al 50",
            primaryEntity = DialogEntity(
                name = "volumen multimedia",
                type = EntityType.VOLUME_STREAM,
                attributes = mapOf("percent" to "50")
            )
        )

        val resolvedSubir = CoreferenceResolver.resolve("súbele", tracker)
        assertTrue(resolvedSubir.wasModified)
        assertEquals("sube el volumen", resolvedSubir.resolvedText)

        val resolvedBajar = CoreferenceResolver.resolve("bájale", tracker)
        assertTrue(resolvedBajar.wasModified)
        assertEquals("baja el volumen", resolvedBajar.resolvedText)
    }

    @Test
    fun testResolveMediaPronoun() {
        val tracker = ConversationSessionTracker()
        tracker.recordTurn(
            userInput = "reproduce Imagine Dragons en Spotify",
            primaryEntity = DialogEntity(
                name = "Spotify Track",
                type = EntityType.MEDIA_TRACK,
                attributes = mapOf("app" to "spotify")
            )
        )

        val resolvedPause = CoreferenceResolver.resolve("paúsala", tracker)
        assertTrue(resolvedPause.wasModified)
        assertEquals("pausa la música", resolvedPause.resolvedText)

        val resolvedResume = CoreferenceResolver.resolve("continúala", tracker)
        assertTrue(resolvedResume.wasModified)
        assertEquals("reanuda la música", resolvedResume.resolvedText)
    }

    @Test
    fun testUnmodifiedInputWhenNoCoreference() {
        val tracker = ConversationSessionTracker()
        val res = CoreferenceResolver.resolve("enciende la linterna", tracker)
        assertFalse(res.wasModified)
        assertEquals("enciende la linterna", res.resolvedText)
    }
}

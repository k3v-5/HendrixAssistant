package com.asistente.celular.voice.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StreamingTtsBufferTest {

    private lateinit var buffer: StreamingTtsBuffer

    @Before
    fun setUp() {
        buffer = StreamingTtsBuffer(minSentenceLength = 15)
    }

    @Test
    fun `test streaming tokens emit sentence when delimiter reached`() {
        // Enviar tokens secuenciales
        val res1 = buffer.append("Hola Kevin, ")
        assertTrue(res1.isEmpty())

        val res2 = buffer.append("espero que estés teniendo un gran día. ")
        assertEquals(1, res2.size)
        assertEquals("Hola Kevin, espero que estés teniendo un gran día.", res2[0])

        val res3 = buffer.append("¿En qué te puedo ayudar hoy? ")
        assertEquals(1, res3.size)
        assertEquals("¿En qué te puedo ayudar hoy?", res3[0])
    }

    @Test
    fun `test decimal numbers do not split prematurely`() {
        val res1 = buffer.append("La temperatura de la CPU es de 55.8 grados celsius en la PC. ")
        assertEquals(1, res1.size)
        assertTrue(res1[0].contains("55.8 grados"))
    }

    @Test
    fun `test flush returns remaining text`() {
        buffer.append("Este es un texto final sin punto")
        val flushed = buffer.flush()
        assertEquals("Este es un texto final sin punto", flushed)

        // Siguiente llamada a flush debe ser nula
        assertNull(buffer.flush())
    }

    @Test
    fun `test clear empties the buffer`() {
        buffer.append("Texto temporal pendiente")
        buffer.clear()
        assertNull(buffer.flush())
    }
}

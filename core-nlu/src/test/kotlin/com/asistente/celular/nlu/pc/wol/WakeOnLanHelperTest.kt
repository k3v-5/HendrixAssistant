package com.asistente.celular.nlu.pc.wol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para WakeOnLanHelper.
 * Valida validación sintáctica de MAC, parsing binario y estructura del Magic Packet de 102 bytes.
 */
class WakeOnLanHelperTest {

    @Test
    fun testValidMacAddresses() {
        assertTrue(WakeOnLanHelper.isValidMac("D8:5E:D3:2D:01:52"))
        assertTrue(WakeOnLanHelper.isValidMac("d8:5e:d3:2d:01:52"))
        assertTrue(WakeOnLanHelper.isValidMac("D8-5E-D3-2D-01-52"))
        assertTrue(WakeOnLanHelper.isValidMac("D85ED32D0152"))
        assertTrue(WakeOnLanHelper.isValidMac("00:11:22:33:44:55"))
    }

    @Test
    fun testInvalidMacAddresses() {
        assertFalse(WakeOnLanHelper.isValidMac(null))
        assertFalse(WakeOnLanHelper.isValidMac(""))
        assertFalse(WakeOnLanHelper.isValidMac("   "))
        assertFalse(WakeOnLanHelper.isValidMac("D8:5E:D3:2D:01")) // Muy corta
        assertFalse(WakeOnLanHelper.isValidMac("D8:5E:D3:2D:01:52:AA")) // Muy larga
        assertFalse(WakeOnLanHelper.isValidMac("GG:5E:D3:2D:01:52")) // Caracteres no hexadecimales
    }

    @Test
    fun testParseMacBytes() {
        val mac = "D8:5E:D3:2D:01:52"
        val bytes = WakeOnLanHelper.parseMacBytes(mac)

        assertEquals(6, bytes.size)
        assertEquals(0xD8.toByte(), bytes[0])
        assertEquals(0x5E.toByte(), bytes[1])
        assertEquals(0xD3.toByte(), bytes[2])
        assertEquals(0x2D.toByte(), bytes[3])
        assertEquals(0x01.toByte(), bytes[4])
        assertEquals(0x52.toByte(), bytes[5])
    }

    @Test
    fun testBuildMagicPacketStructure() {
        val mac = "00:1A:2B:3C:4D:5E"
        val packet = WakeOnLanHelper.buildMagicPacket(mac)

        // 1. Longitud exacta: 102 bytes
        assertEquals(102, packet.size)

        // 2. Primeros 6 bytes en 0xFF (-1)
        for (i in 0 until 6) {
            assertEquals(0xFF.toByte(), packet[i])
        }

        // 3. 16 repeticiones exactas de los 6 bytes de la MAC
        val expectedMacBytes = byteArrayOf(
            0x00.toByte(), 0x1A.toByte(), 0x2B.toByte(),
            0x3C.toByte(), 0x4D.toByte(), 0x5E.toByte()
        )

        for (repeat in 0 until 16) {
            val chunk = packet.copyOfRange(6 + repeat * 6, 6 + (repeat + 1) * 6)
            assertArrayEquals("Fallo en repetición $repeat", expectedMacBytes, chunk)
        }
    }
}

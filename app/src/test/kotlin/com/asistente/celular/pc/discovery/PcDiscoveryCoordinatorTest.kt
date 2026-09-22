package com.asistente.celular.pc.discovery

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcDiscoveryCoordinatorTest {

    @Test
    fun testDiscoveredPcEndpointModel() {
        val endpoint = DiscoveredPcEndpoint(
            hostname = "DEV-WORKSTATION",
            ipAddress = "192.168.1.150",
            wsPort = 8765,
            airsyncPort = 8766,
            macAddress = "00:1A:2B:3C:4D:5E",
            lastSeenTimestamp = 123456789L
        )

        assertEquals("DEV-WORKSTATION", endpoint.hostname)
        assertEquals("192.168.1.150", endpoint.ipAddress)
        assertEquals(8765, endpoint.wsPort)
        assertEquals(8766, endpoint.airsyncPort)
        assertEquals("00:1A:2B:3C:4D:5E", endpoint.macAddress)
        assertEquals(123456789L, endpoint.lastSeenTimestamp)
    }

    @Test
    fun testDiscoveryJsonPayloadParsing() {
        val jsonStr = """
            {
                "service": "hendrix-workspace",
                "hostname": "MY-PC",
                "ip": "192.168.1.80",
                "ws_port": 8765,
                "airsync_port": 8766,
                "mac": "AA:BB:CC:DD:EE:FF"
            }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        assertEquals("hendrix-workspace", json.getString("service"))
        assertEquals("MY-PC", json.getString("hostname"))
        assertEquals("192.168.1.80", json.getString("ip"))
        assertEquals(8765, json.getInt("ws_port"))
        assertEquals(8766, json.getInt("airsync_port"))
        assertEquals("AA:BB:CC:DD:EE:FF", json.getString("mac"))
    }

    @Test
    fun testDiscoveryJsonFallbackToSenderIpWhenLocalhost() {
        val jsonStr = """
            {
                "service": "hendrix-workspace",
                "hostname": "DESKTOP-TEST",
                "ip": "127.0.0.1",
                "ws_port": 8765,
                "airsync_port": 8766,
                "mac": "11:22:33:44:55:66"
            }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val reportedIp = json.optString("ip", "")
        val senderIp = "192.168.1.99"
        val effectiveIp = if (reportedIp.isNotBlank() && reportedIp != "127.0.0.1") reportedIp else senderIp

        assertEquals("192.168.1.99", effectiveIp)
    }

    @Test
    fun testMacValidation() {
        val validMac = "00:1B:44:11:3A:B7"
        val invalidMac = "not-a-mac"

        assertTrue(com.asistente.celular.nlu.pc.wol.WakeOnLanHelper.isValidMac(validMac))
        assertTrue(!com.asistente.celular.nlu.pc.wol.WakeOnLanHelper.isValidMac(invalidMac))
    }
}

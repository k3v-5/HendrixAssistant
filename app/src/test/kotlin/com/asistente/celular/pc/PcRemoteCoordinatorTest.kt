package com.asistente.celular.pc

import com.asistente.celular.nlu.pc.PcEndpointConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PcRemoteCoordinatorTest {

    @Test
    fun testEndpointConfigDefaultsAndCopy() {
        val config = PcEndpointConfig(
            hostname = "DESKTOP-M7D4K1T",
            localIp = "192.168.100.159",
            port = 8899,
            pin = "123456",
            macAddress = "24:4B:FE:58:6F:EC"
        )

        assertEquals("DESKTOP-M7D4K1T", config.hostname)
        assertEquals("192.168.100.159", config.localIp)
        assertEquals(8899, config.port)
        assertEquals("123456", config.pin)
        assertEquals("24:4B:FE:58:6F:EC", config.macAddress)

        val updated = config.copy(
            localIp = "192.168.100.200",
            pin = "654321"
        )
        assertEquals("192.168.100.200", updated.localIp)
        assertEquals("654321", updated.pin)
        assertEquals(8899, updated.port)
    }

    @Test
    fun testUrlNormalizationLogic() {
        fun normalize(host: String, port: Int): String {
            val cleanHost = host.trim()
            val isDirectUrl = cleanHost.startsWith("ws://", ignoreCase = true) || cleanHost.startsWith("wss://", ignoreCase = true)
            return if (isDirectUrl) {
                cleanHost
            } else {
                val stripped = cleanHost.removePrefix("http://").removePrefix("https://").substringBefore("/")
                val parsedHost = if (stripped.contains(":") && !stripped.contains("[")) stripped.substringBefore(":") else stripped
                val parsedPort = if (stripped.contains(":") && !stripped.contains("[")) stripped.substringAfter(":").toIntOrNull() ?: port else port
                "ws://$parsedHost:$parsedPort/ws"
            }
        }

        assertEquals("ws://192.168.100.159:8899/ws", normalize("192.168.100.159", 8899))
        assertEquals("ws://192.168.100.159:8899/ws", normalize("192.168.100.159:8899", 8899))
        assertEquals("ws://192.168.100.159:9000/ws", normalize("192.168.100.159:9000", 8899))
        assertEquals("ws://192.168.100.159:8899/ws", normalize("http://192.168.100.159:8899", 8899))
        assertEquals("ws://192.168.100.159:8899/ws", normalize("ws://192.168.100.159:8899/ws", 8899))
        assertEquals("wss://custom-tunnel.trycloudflare.com/ws", normalize("wss://custom-tunnel.trycloudflare.com/ws", 8899))
    }
}

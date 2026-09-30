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
    fun testBuildWebSocketUrl() {
        // LAN sin puerto especificado -> usa fallbackPort (8899)
        assertEquals("ws://192.168.100.159:8899/ws", PcRemoteCoordinator.buildWebSocketUrl("192.168.100.159", 8899))
        // LAN con puerto explícito
        assertEquals("ws://192.168.100.159:9000/ws", PcRemoteCoordinator.buildWebSocketUrl("192.168.100.159:9000", 8899))
        // LAN con esquema http://
        assertEquals("ws://192.168.100.159:8899/ws", PcRemoteCoordinator.buildWebSocketUrl("http://192.168.100.159:8899", 8899))
        // LAN con esquema ws://
        assertEquals("ws://192.168.100.159:8899/ws", PcRemoteCoordinator.buildWebSocketUrl("ws://192.168.100.159:8899/ws", 8899))

        // Cloudflare Tunnels: JAMÁS deben llevar :8899 y deben usar wss://
        assertEquals("wss://custom-tunnel.trycloudflare.com/ws", PcRemoteCoordinator.buildWebSocketUrl("wss://custom-tunnel.trycloudflare.com/ws", 8899))
        assertEquals("wss://hendrix-pc.trycloudflare.com/ws", PcRemoteCoordinator.buildWebSocketUrl("https://hendrix-pc.trycloudflare.com", 8899))
        assertEquals("wss://hendrix-pc.trycloudflare.com/ws", PcRemoteCoordinator.buildWebSocketUrl("https://hendrix-pc.trycloudflare.com/", 8899))
        assertEquals("wss://hendrix-pc.trycloudflare.com/ws", PcRemoteCoordinator.buildWebSocketUrl("hendrix-pc.trycloudflare.com", 8899))

        // Ngrok u otros dominios de túnel conocidos
        assertEquals("wss://hendrix.ngrok.io/ws", PcRemoteCoordinator.buildWebSocketUrl("hendrix.ngrok.io", 8899))
        assertEquals("wss://hendrix.ngrok.io/ws", PcRemoteCoordinator.buildWebSocketUrl("https://hendrix.ngrok.io", 8899))

        // Dominio personalizado con puerto explícito
        assertEquals("wss://my-domain.org:8443/ws", PcRemoteCoordinator.buildWebSocketUrl("https://my-domain.org:8443", 8899))
    }
}

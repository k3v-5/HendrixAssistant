package com.asistente.celular.skills.pc

import com.asistente.celular.nlu.pc.AutonomousTaskPlan
import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcEndpointConfig
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcOperationMode
import com.asistente.celular.nlu.pc.PcSystemTelemetry
import com.asistente.celular.nlu.pc.PcWorkspaceBridge
import com.asistente.celular.nlu.pc.TransportType
import com.asistente.celular.nlu.pc.WindowBounds
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.ui.PcWorkspaceUiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias que valida la arquitectura de transporte híbrido (LAN / WAN),
 * conmutación por error (auto-failover), cálculos de precisión para la Lupa HD
 * y el flujo de extremo a extremo de SkillOutput y AssistantUiPayload.
 */
class PcTransportAutoFailoverTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy context")
        override val isConnectedToInternet: Boolean = true
        override val previousOutput: SkillOutput? = null
    }

    /**
     * Puente simulador con control de fallos en LAN y disponibilidad de WAN.
     */
    private class HybridMockPcBridge(
        var simulateLanFailure: Boolean = false,
        var simulateWanFailure: Boolean = false
    ) : PcWorkspaceBridge {

        var connectedHost: String? = null
        var activeTransportType: TransportType = TransportType.LAN_DIRECT

        override val currentMode: StateFlow<PcOperationMode> = MutableStateFlow(PcOperationMode.INTERACTIVE_SNAPSHOT)
        override val telemetry: StateFlow<PcSystemTelemetry?> = MutableStateFlow(
            PcSystemTelemetry(
                hostname = "Alienware-PC",
                activeTransportType = TransportType.GLOBAL_TUNNEL_WAN,
                isWanRelayActive = true,
                roundTripLatencyMs = 45L,
                activeWindowBounds = WindowBounds(left = 100, top = 80, width = 1200, height = 800)
            )
        )
        override val latestSnapshot: StateFlow<ByteArray?> = MutableStateFlow(byteArrayOf(10, 20, 30, 40))
        override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun connect(host: String, port: Int, pin: String?): Boolean {
            val isWan = host.startsWith("http") || host.contains("trycloudflare.com")
            if (!isWan && simulateLanFailure) {
                return false
            }
            if (isWan && simulateWanFailure) {
                return false
            }
            connectedHost = host
            activeTransportType = if (isWan) TransportType.GLOBAL_TUNNEL_WAN else TransportType.LAN_DIRECT
            return true
        }

        override suspend fun disconnect() {
            connectedHost = null
        }

        override suspend fun setOperationMode(mode: PcOperationMode): Boolean = true
        override suspend fun requestSnapshot(cropToActiveWindow: Boolean): ByteArray? = latestSnapshot.value
        override suspend fun sendInteraction(action: PcInteractionAction): Boolean = true
        override suspend fun typeTextDirectly(text: String): Boolean = true
        override suspend fun executeQuickCommand(command: String): Boolean = true
        override suspend fun planAutonomousGoal(goalPrompt: String): AutonomousTaskPlan =
            AutonomousTaskPlan(planId = "plan_wan", userGoal = goalPrompt, steps = emptyList())
        override suspend fun executeApprovedPlan(planId: String): Boolean = true
        override suspend fun queryTelemetry(): PcSystemTelemetry? = telemetry.value
    }

    @Test
    fun testEndpointConfigIntegrityAndDefaults() {
        val defaultConfig = PcEndpointConfig()
        assertEquals("PC-Host", defaultConfig.hostname)
        assertEquals("192.168.1.100", defaultConfig.localIp)
        assertEquals(8899, defaultConfig.port)
        assertEquals(8900, defaultConfig.airSyncPort)
        assertEquals(75, defaultConfig.snapshotQuality)
        assertEquals(3000L, defaultConfig.telemetryIntervalMs)
        assertTrue(defaultConfig.autoPasteDefault)
        assertNull(defaultConfig.remoteTunnelUrl)
        assertFalse(defaultConfig.isPaired)

        val pairedConfig = defaultConfig.copy(
            hostname = "Workstation-Office",
            localIp = "192.168.0.50",
            remoteTunnelUrl = "https://office-tunnel.trycloudflare.com",
            deviceToken = "auth_tok_abc123",
            pin = "654321",
            isPaired = true,
            airSyncPort = 8905,
            snapshotQuality = 90,
            telemetryIntervalMs = 1000L,
            autoPasteDefault = false
        )

        assertEquals("Workstation-Office", pairedConfig.hostname)
        assertEquals("192.168.0.50", pairedConfig.localIp)
        assertEquals("https://office-tunnel.trycloudflare.com", pairedConfig.remoteTunnelUrl)
        assertEquals("auth_tok_abc123", pairedConfig.deviceToken)
        assertEquals("654321", pairedConfig.pin)
        assertTrue(pairedConfig.isPaired)
        assertEquals(8905, pairedConfig.airSyncPort)
        assertEquals(90, pairedConfig.snapshotQuality)
        assertEquals(1000L, pairedConfig.telemetryIntervalMs)
        assertFalse(pairedConfig.autoPasteDefault)
    }

    @Test
    fun testAutoConnectionPrefersLanWhenAvailable() = runBlocking {
        val bridge = HybridMockPcBridge(simulateLanFailure = false)
        val config = PcEndpointConfig(
            localIp = "192.168.1.55",
            remoteTunnelUrl = "https://hendrix-remote.trycloudflare.com"
        )

        // Simular lógica de connectAuto
        val lanSuccess = bridge.connect(config.localIp, config.port, null)
        assertTrue(lanSuccess)
        assertEquals("192.168.1.55", bridge.connectedHost)
        assertEquals(TransportType.LAN_DIRECT, bridge.activeTransportType)
    }

    @Test
    fun testAutoFailoverToWanWhenLanFails() = runBlocking {
        val bridge = HybridMockPcBridge(simulateLanFailure = true) // Fallo en LAN (ej. en la calle con datos 4G)
        val config = PcEndpointConfig(
            localIp = "192.168.1.55",
            remoteTunnelUrl = "https://hendrix-remote.trycloudflare.com"
        )

        // 1. Intento por LAN falla
        val lanSuccess = bridge.connect(config.localIp, config.port, null)
        assertFalse(lanSuccess)

        // 2. Conmutación automática a WAN
        val tunnelUrl = config.remoteTunnelUrl
        assertNotNull(tunnelUrl)
        val wanSuccess = bridge.connect(tunnelUrl!!, config.port, null)
        assertTrue(wanSuccess)
        assertEquals("https://hendrix-remote.trycloudflare.com", bridge.connectedHost)
        assertEquals(TransportType.GLOBAL_TUNNEL_WAN, bridge.activeTransportType)
    }

    @Test
    fun testLoupeCoordinateMathAndBoundaryClamping() {
        val containerWidth = 1080f
        val containerHeight = 1920f
        val scale = 1.0f
        val offsetX = 0f
        val offsetY = 0f

        // Caso 1: Toque centrado perfectamente (540, 960)
        val touchCenterX = 540f
        val touchCenterY = 960f
        val normX = ((touchCenterX - offsetX) / (containerWidth * scale)).coerceIn(0f, 1f)
        val normY = ((touchCenterY - offsetY) / (containerHeight * scale)).coerceIn(0f, 1f)
        assertEquals(0.5f, normX, 0.001f)
        assertEquals(0.5f, normY, 0.001f)

        // Caso 2: Toque fuera de los límites a la izquierda/arriba (coordenadas negativas)
        val touchOutOfBoundsX = -50f
        val touchOutOfBoundsY = -100f
        val clampedNormX = ((touchOutOfBoundsX - offsetX) / (containerWidth * scale)).coerceIn(0f, 1f)
        val clampedNormY = ((touchOutOfBoundsY - offsetY) / (containerHeight * scale)).coerceIn(0f, 1f)
        assertEquals(0.0f, clampedNormX, 0.001f)
        assertEquals(0.0f, clampedNormY, 0.001f)

        // Caso 3: Cálculo de recorte de muestra a 3.5x
        val bmpWidth = 1920
        val bmpHeight = 1080
        val mag = 3.5f
        val sampleX = normX * bmpWidth
        val sampleY = normY * bmpHeight
        val cropW = (bmpWidth / (scale * mag)).toInt().coerceAtLeast(10)
        val cropH = (bmpHeight / (scale * mag)).toInt().coerceAtLeast(10)

        assertEquals(960f, sampleX, 0.001f)
        assertEquals(540f, sampleY, 0.001f)
        assertTrue(cropW > 0 && cropW < bmpWidth)
        assertTrue(cropH > 0 && cropH < bmpHeight)

        val cropOffsetX = (sampleX - cropW / 2).toInt().coerceIn(0, bmpWidth - cropW)
        val cropOffsetY = (sampleY - cropH / 2).toInt().coerceIn(0, bmpHeight - cropH)
        assertTrue(cropOffsetX >= 0 && cropOffsetX + cropW <= bmpWidth)
        assertTrue(cropOffsetY >= 0 && cropOffsetY + cropH <= bmpHeight)
    }

    @Test
    fun testWindowBoundsNormalization() {
        val window = WindowBounds(left = 192, top = 108, width = 960, height = 540)
        val normLeft = (window.left.toFloat() / 1920f).coerceIn(0f, 1f)
        val normTop = (window.top.toFloat() / 1080f).coerceIn(0f, 1f)
        val normWidth = (window.width.toFloat() / 1920f).coerceIn(0.05f, 1f)
        val normHeight = (window.height.toFloat() / 1080f).coerceIn(0.05f, 1f)

        assertEquals(0.1f, normLeft, 0.001f)
        assertEquals(0.1f, normTop, 0.001f)
        assertEquals(0.5f, normWidth, 0.001f)
        assertEquals(0.5f, normHeight, 0.001f)
    }

    @Test
    fun testFullSkillExecutionWithWanTelemetryPayload() = runBlocking {
        val bridge = HybridMockPcBridge()
        val skill = PcControlSkill(bridge)

        val score = skill.score(dummyContext, "controlar pc")
        assertTrue(score.isMatch)

        val output = skill.execute(dummyContext, "controlar pc", score)
        assertNotNull(output.payload)
        assertTrue(output.payload is PcWorkspaceUiPayload)

        val payload = output.payload as PcWorkspaceUiPayload
        assertEquals("Alienware-PC", payload.hostname)
        assertEquals(TransportType.GLOBAL_TUNNEL_WAN, payload.telemetry?.activeTransportType)
        assertEquals(45L, payload.telemetry?.roundTripLatencyMs)
        assertTrue(payload.telemetry?.isWanRelayActive == true)
        assertEquals(1200, payload.telemetry?.activeWindowBounds?.width)
    }
}

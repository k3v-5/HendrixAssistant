package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.nlu.smarthome.SmartProtocol
import com.asistente.celular.nlu.ui.SmartBulbUiPayload
import com.asistente.celular.skills.smarthome.SmartHomeSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartHomeAdvancedTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class MockSmartHomeRepository : SmartHomeRepository {
        val testBulb = SmartDevice(
            id = "test_bulb_xiaomi",
            name = "Foco Xiaomi",
            aliases = listOf("foco", "luz", "foco sala", "foco xiaomi"),
            type = DeviceType.LIGHT,
            ipAddress = "192.168.1.105",
            port = 55443,
            protocol = SmartProtocol.YEELIGHT_LAN,
            properties = mapOf("power" to "on", "bright" to "80")
        )

        private val _devices = MutableStateFlow(listOf(testBulb))
        override val devices: StateFlow<List<SmartDevice>> = _devices.asStateFlow()

        var lastExecutedAction: DeviceAction? = null
        var lastTargetName: String? = null

        override suspend fun addOrUpdateDevice(device: SmartDevice) {
            _devices.value = _devices.value + device
        }

        override suspend fun removeDevice(id: String) {
            _devices.value = _devices.value.filter { it.id != id }
        }

        override suspend fun getDeviceById(id: String): SmartDevice? = _devices.value.find { it.id == id }

        override suspend fun findDeviceByName(nameOrAlias: String?): SmartDevice? {
            if (nameOrAlias.isNullOrBlank()) return _devices.value.firstOrNull()
            return _devices.value.firstOrNull { it.matchesName(nameOrAlias) }
        }

        override suspend fun discoverDevices(): List<SmartDevice> = _devices.value

        override suspend fun executeAction(targetName: String?, action: DeviceAction): DeviceActionResult {
            lastTargetName = targetName
            lastExecutedAction = action
            return DeviceActionResult(success = true, message = "Efecto aplicado correctamente")
        }
    }

    @Test
    fun testCandleModeFlow() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrases = listOf("modo vela", "efecto vela", "activa el modo vela en el foco", "luz de vela")
        for (phrase in phrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con modo vela: '$phrase'", score.isMatch)
            assertEquals("effect", score.capturedSlots["action"])
            assertEquals("candle", score.capturedSlots["effect_type"])

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)
            assertTrue("Debe enviar StartColorFlow", repo.lastExecutedAction is DeviceAction.StartColorFlow)

            val payload = output.payload as? SmartBulbUiPayload
            assertNotNull(payload)
            assertEquals("candle", payload?.activeMode)
        }
    }

    @Test
    fun testPartyModeFlow() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrases = listOf("modo fiesta", "efecto fiesta", "pon la luz en modo fiesta", "modo discoteca")
        for (phrase in phrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con modo fiesta: '$phrase'", score.isMatch)
            assertEquals("effect", score.capturedSlots["action"])
            assertEquals("party", score.capturedSlots["effect_type"])

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)
            assertTrue("Debe enviar StartColorFlow", repo.lastExecutedAction is DeviceAction.StartColorFlow)

            val payload = output.payload as? SmartBulbUiPayload
            assertNotNull(payload)
            assertEquals("party", payload?.activeMode)
        }
    }

    @Test
    fun testNightLightMode() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrases = listOf("luz de noche", "modo noche en el foco", "luz de luna")
        for (phrase in phrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con luz de noche: '$phrase'", score.isMatch)
            assertEquals("effect", score.capturedSlots["action"])
            assertEquals("night", score.capturedSlots["effect_type"])

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)

            val payload = output.payload as? SmartBulbUiPayload
            assertNotNull(payload)
            assertEquals("night", payload?.activeMode)
            assertEquals(1, payload?.brightness)
        }
    }

    @Test
    fun testStopFlowEffect() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrases = listOf("deten el efecto", "para el efecto", "detener modo fiesta", "detener modo vela")
        for (phrase in phrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con detener efecto: '$phrase'", score.isMatch)
            assertEquals("effect", score.capturedSlots["action"])
            assertEquals("stop_flow", score.capturedSlots["effect_type"])

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)
            assertEquals(DeviceAction.StopColorFlow, repo.lastExecutedAction)

            val payload = output.payload as? SmartBulbUiPayload
            assertNotNull(payload)
            assertEquals("normal", payload?.activeMode)
        }
    }

    @Test
    fun testExpandedColors() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val colorChecks = listOf(
            "luz color turquesa" to 0x40E0D0,
            "pon el foco lila" to 0xC8A2C8,
            "luz magenta" to 0xFF00FF,
            "cambia la luz a celeste" to 0x87CEEB,
            "foco ambar" to 0xFFBF00,
            "luz salmon" to 0xFA8072
        )

        for ((phrase, expectedRgb) in colorChecks) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir con color: '$phrase'", score.isMatch)

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)
            assertEquals(DeviceAction.SetColor(expectedRgb), repo.lastExecutedAction)

            val payload = output.payload as? SmartBulbUiPayload
            assertNotNull(payload)
            assertEquals(expectedRgb, payload?.colorRgb)
        }
    }
}

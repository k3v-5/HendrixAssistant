package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.nlu.smarthome.SmartProtocol
import com.asistente.celular.skills.smarthome.SmartHomeSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartHomeSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class MockSmartHomeRepository : SmartHomeRepository {
        val testBulb = SmartDevice(
            id = "test_bulb_1",
            name = "Foco Sala",
            aliases = listOf("foco", "luz", "foco sala", "luz de la sala"),
            type = DeviceType.LIGHT,
            ipAddress = "192.168.1.50",
            port = 55443,
            protocol = SmartProtocol.YEELIGHT_LAN,
            properties = mapOf("power" to "off", "bright" to "100")
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
            return DeviceActionResult(success = true, message = "Acción ejecutada")
        }
    }

    @Test
    fun testTurnOnPatternsMatchAndExecute() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrases = listOf(
            "prende el foco",
            "enciende el foco",
            "prende la luz",
            "enciende la luz",
            "prender las luces",
            "enciende la lampara"
        )

        for (phrase in phrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir: '$phrase'", score.isMatch)
            assertEquals("turn_on", score.capturedSlots["action"])

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)
            assertEquals(DeviceAction.TurnOn, repo.lastExecutedAction)
        }
    }

    @Test
    fun testTurnOffPatternsMatchAndExecute() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrases = listOf(
            "apaga el foco",
            "apaga la luz",
            "apagar el foco",
            "apaga las luces",
            "apaga la lampara",
            "apaga mi foco",
            "se apaga mi foco",
            "apaga el foco del cuarto"
        )

        for (phrase in phrases) {
            val score = skill.score(dummyContext, phrase)
            assertTrue("Debe coincidir: '$phrase'", score.isMatch)
            assertEquals("turn_off", score.capturedSlots["action"])

            val output = skill.execute(dummyContext, phrase, score)
            assertTrue(output.success)
            assertEquals(DeviceAction.TurnOff, repo.lastExecutedAction)
        }
    }

    @Test
    fun testBrightnessControl() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        val phrase = "pon el foco al 50%"
        val score = skill.score(dummyContext, phrase)
        assertTrue(score.isMatch)
        assertEquals("brightness", score.capturedSlots["action"])

        val output = skill.execute(dummyContext, phrase, score)
        assertTrue(output.success)
        assertEquals(DeviceAction.SetBrightness(50), repo.lastExecutedAction)
    }

    @Test
    fun testColorControl() = runBlocking {
        val repo = MockSmartHomeRepository()
        val skill = SmartHomeSkill(repo)

        // Color Rojo
        val scoreRed = skill.score(dummyContext, "cambia la luz a rojo")
        assertTrue(scoreRed.isMatch)
        val outputRed = skill.execute(dummyContext, "cambia la luz a rojo", scoreRed)
        assertTrue(outputRed.success)
        assertEquals(DeviceAction.SetColor(0xFF0000), repo.lastExecutedAction)

        // Luz Cálida
        val scoreWarm = skill.score(dummyContext, "pon luz calida")
        assertTrue(scoreWarm.isMatch)
        val outputWarm = skill.execute(dummyContext, "pon luz calida", scoreWarm)
        assertTrue(outputWarm.success)
        assertEquals(DeviceAction.SetColorTemperature(2700), repo.lastExecutedAction)
    }
}

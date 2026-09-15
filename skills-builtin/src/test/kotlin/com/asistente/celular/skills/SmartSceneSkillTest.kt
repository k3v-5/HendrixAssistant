package com.asistente.celular.skills

import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.nlu.smarthome.SmartProtocol
import com.asistente.celular.skills.smarthome.SmartSceneSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSceneSkillTest {

    private val dummyContext = object : SkillContext {
        override val androidContext: android.content.Context get() = error("Dummy")
        override val isConnectedToInternet: Boolean = false
        override val previousOutput: SkillOutput? = null
    }

    private class MockSmartHomeRepo : SmartHomeRepository {
        val bulb = SmartDevice(
            id = "b1",
            name = "Foco Xiaomi",
            type = DeviceType.LIGHT,
            ipAddress = "192.168.1.10",
            port = 55443,
            protocol = SmartProtocol.YEELIGHT_LAN
        )
        private val _devices = MutableStateFlow(listOf(bulb))
        override val devices: StateFlow<List<SmartDevice>> = _devices.asStateFlow()

        val executedActions = mutableListOf<DeviceAction>()

        override suspend fun addOrUpdateDevice(device: SmartDevice) {}
        override suspend fun removeDevice(id: String) {}
        override suspend fun getDeviceById(id: String): SmartDevice? = bulb
        override suspend fun findDeviceByName(nameOrAlias: String?): SmartDevice? = bulb
        override suspend fun executeAction(nameOrAlias: String?, action: DeviceAction): DeviceActionResult {
            executedActions.add(action)
            return DeviceActionResult(success = true, message = "OK")
        }
        override suspend fun discoverDevices(): List<SmartDevice> = listOf(bulb)
    }

    @Test
    fun testReadingSceneScoreAndExecute() = runBlocking {
        val repo = MockSmartHomeRepo()
        val skill = SmartSceneSkill(repo)

        val score = skill.score(dummyContext, "modo lectura")
        assertTrue(score.confidence >= 0.90f)
        assertEquals("reading", score.capturedSlots["scene_id"])

        val output = skill.execute(dummyContext, "modo lectura", score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("Modo lectura activado"))
        // Debe haber ejecutado TurnOn, SetColorTemperature(2700), SetBrightness(40)
        assertTrue(repo.executedActions.contains(DeviceAction.TurnOn))
        assertTrue(repo.executedActions.contains(DeviceAction.SetBrightness(40)))
        assertTrue(repo.executedActions.contains(DeviceAction.SetColorTemperature(2700)))
    }

    @Test
    fun testNightSceneScoreAndExecute() = runBlocking {
        val repo = MockSmartHomeRepo()
        val skill = SmartSceneSkill(repo)

        val score = skill.score(dummyContext, "buenas noches")
        assertTrue(score.confidence >= 0.90f)
        assertEquals("night", score.capturedSlots["scene_id"])

        val output = skill.execute(dummyContext, "buenas noches", score)
        assertTrue(output.success)
        assertTrue(output.speech.contains("Buenas noches"))
        assertTrue(repo.executedActions.contains(DeviceAction.TurnOff))
    }

    @Test
    fun testCinemaSceneScore() {
        val repo = MockSmartHomeRepo()
        val skill = SmartSceneSkill(repo)

        val score = skill.score(dummyContext, "activa el modo cine")
        assertTrue(score.confidence >= 0.90f)
        assertEquals("cinema", score.capturedSlots["scene_id"])
    }
}

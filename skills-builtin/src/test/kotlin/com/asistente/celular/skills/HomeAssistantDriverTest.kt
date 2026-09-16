package com.asistente.celular.skills

import com.asistente.celular.nlu.smarthome.DeviceAction
import com.asistente.celular.nlu.smarthome.DeviceActionResult
import com.asistente.celular.nlu.smarthome.DeviceType
import com.asistente.celular.nlu.smarthome.SmartDevice
import com.asistente.celular.nlu.smarthome.SmartHomeRepository
import com.asistente.celular.nlu.smarthome.SmartProtocol
import com.asistente.celular.skills.smarthome.AmbientSceneComposer
import com.asistente.celular.skills.smarthome.HomeAssistantDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAssistantDriverTest {

    @Test
    fun testSupportedProtocolIsHomeAssistant() {
        val driver = HomeAssistantDriver()
        assertEquals(SmartProtocol.HOME_ASSISTANT, driver.supportedProtocol)
    }

    @Test
    fun testExecuteActionRequiresEntityId() = runBlocking {
        val driver = HomeAssistantDriver(defaultBaseUrl = "http://127.0.0.1:8123")
        val invalidDevice = SmartDevice(
            id = "",
            name = "Luz Desconocida",
            ipAddress = "127.0.0.1",
            protocol = SmartProtocol.HOME_ASSISTANT,
            metadata = emptyMap()
        )

        val result = driver.executeAction(invalidDevice, DeviceAction.TurnOn)
        assertFalse(result.success)
        assertTrue(result.message.contains("entity_id"))
    }

    @Test
    fun testAmbientSceneComposerPresets() {
        assertEquals(4, AmbientSceneComposer.ALL_AMBIENT_PRESETS.size)
        val rainStudy = AmbientSceneComposer.PRESET_RAIN_STUDY
        assertEquals("Lluvia para Estudio", rainStudy.name)
        assertTrue(rainStudy.enableDnd == true)
        assertTrue(rainStudy.lightAction is DeviceAction.SetColorTemperature)
    }
}

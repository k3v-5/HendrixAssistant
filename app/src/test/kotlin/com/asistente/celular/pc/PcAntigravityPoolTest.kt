package com.asistente.celular.pc

import com.asistente.celular.nlu.pc.AntigravityChat
import com.asistente.celular.nlu.pc.AntigravityProfile
import com.asistente.celular.nlu.pc.NightTaskStatus
import com.asistente.celular.nlu.pc.module.PcModuleId
import com.asistente.celular.nlu.pc.module.PcModuleRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PcAntigravityPoolTest {

    @Test
    fun testModuleRegistryContainsAntigravityProfiles() {
        val module = PcModuleRegistry.ANTIGRAVITY_PROFILES_MODULE
        assertNotNull(module)
        assertEquals(PcModuleId.ANTIGRAVITY_PROFILES, module.id)
        assertEquals("Antigravity & Gemini Pool", module.name)
        assertTrue(module.isEnabledByDefault)
        assertTrue(PcModuleRegistry.ALL_MODULES.any { it.id == PcModuleId.ANTIGRAVITY_PROFILES })
    }

    @Test
    fun testAntigravityProfileModel() {
        val profile = AntigravityProfile(
            name = "cuenta1",
            email = "kevin@gmail.com",
            isActive = true,
            inCooldown = false,
            cooldownRemainingSeconds = 0,
            usageCount = 15,
            hasCredential = true
        )

        assertEquals("cuenta1", profile.name)
        assertEquals("kevin@gmail.com", profile.email)
        assertTrue(profile.isActive)
        assertFalse(profile.inCooldown)
        assertEquals(0, profile.cooldownRemainingSeconds)
        assertEquals(15, profile.usageCount)
        assertTrue(profile.hasCredential)

        val rotated = profile.copy(
            isActive = false,
            inCooldown = true,
            cooldownRemainingSeconds = 7200
        )
        assertFalse(rotated.isActive)
        assertTrue(rotated.inCooldown)
        assertEquals(7200, rotated.cooldownRemainingSeconds)
    }

    @Test
    fun testNightTaskStatusModel() {
        val status = NightTaskStatus(
            status = "ROTATING",
            goal = "Refactorizar e implementar tests de NLU",
            workspace = "f:/Dev/AsistenteCelular",
            activeProfile = "cuenta2",
            currentTurns = 40,
            totalTurns = 85,
            rotationsCount = 2,
            loopRecoveriesCount = 1,
            startedAt = "2026-10-01T22:00:00",
            lastIncident = "Límite de cuota alcanzado en cuenta1"
        )

        assertEquals("ROTATING", status.status)
        assertEquals("Refactorizar e implementar tests de NLU", status.goal)
        assertEquals("cuenta2", status.activeProfile)
        assertEquals(40, status.currentTurns)
        assertEquals(85, status.totalTurns)
        assertEquals(2, status.rotationsCount)
        assertEquals(1, status.loopRecoveriesCount)
        assertEquals("2026-10-01T22:00:00", status.startedAt)
        assertNotNull(status.lastIncident)
    }

    @Test
    fun testAntigravityChatModel() {
        val chat = AntigravityChat(
            conversationId = "conv-abc-123",
            title = "Integración Hendrix Android",
            preview = "Generando tarjeta Compose para pool multicuenta...",
            lastModifiedEpoch = 1790859461000L,
            stepCount = 42,
            projectId = "HendrixAssistant",
            workspaceUri = "f:/Dev/AsistenteCelular"
        )

        assertEquals("conv-abc-123", chat.conversationId)
        assertEquals("Integración Hendrix Android", chat.title)
        assertEquals(42, chat.stepCount)
        assertEquals("HendrixAssistant", chat.projectId)
    }

    @Test
    fun testParseProfileListJson() {
        val jsonStr = """
            {
                "type": "AG_PROFILE_LIST_RESP",
                "profiles": [
                    {
                        "name": "cuenta1",
                        "email": "dev1@gmail.com",
                        "isActive": true,
                        "inCooldown": false,
                        "cooldownRemainingSeconds": 0,
                        "usageCount": 12,
                        "hasCredential": true
                    },
                    {
                        "name": "cuenta5",
                        "email": "dev5@gmail.com",
                        "isActive": false,
                        "inCooldown": true,
                        "cooldownRemainingSeconds": 1800,
                        "usageCount": 40,
                        "hasCredential": true
                    }
                ],
                "activeProfile": "cuenta1",
                "activeChats": [
                    {
                        "conversationId": "chat-999",
                        "title": "Refactor de Motor NLU",
                        "preview": "Generando tests unitarios...",
                        "lastModifiedEpoch": 1790859461000,
                        "stepCount": 10
                    }
                ]
            }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val pArr = json.getJSONArray("profiles")
        assertEquals(2, pArr.length())

        val p1 = pArr.getJSONObject(0)
        assertEquals("cuenta1", p1.getString("name"))
        assertTrue(p1.getBoolean("isActive"))
        assertTrue(p1.getBoolean("hasCredential"))

        val p2 = pArr.getJSONObject(1)
        assertEquals("cuenta5", p2.getString("name"))
        assertTrue(p2.getBoolean("inCooldown"))
        assertEquals(1800, p2.getInt("cooldownRemainingSeconds"))

        val cArr = json.getJSONArray("activeChats")
        assertEquals(1, cArr.length())
        val c1 = cArr.getJSONObject(0)
        assertEquals("chat-999", c1.getString("conversationId"))
        assertEquals("Refactor de Motor NLU", c1.getString("title"))
    }

    @Test
    fun testNightTaskEventJsonParsing() {
        val jsonStr = """
            {
                "type": "NIGHT_TASK_EVENT",
                "eventType": "NIGHT_TASK_ROTATING",
                "taskState": {
                    "status": "ROTATING",
                    "goal": "Supervisión continua",
                    "activeProfile": "cuenta2",
                    "currentTurns": 40,
                    "totalTurns": 80,
                    "rotationsCount": 1,
                    "loopRecoveriesCount": 0
                },
                "details": {
                    "reason": "QUOTA_EXHAUSTED",
                    "previousProfile": "cuenta1",
                    "nextProfile": "cuenta2"
                }
            }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        assertEquals("NIGHT_TASK_EVENT", json.getString("type"))
        assertEquals("NIGHT_TASK_ROTATING", json.getString("eventType"))

        val taskState = json.getJSONObject("taskState")
        assertEquals("ROTATING", taskState.getString("status"))
        assertEquals(1, taskState.getInt("rotationsCount"))
        assertEquals(80, taskState.getInt("totalTurns"))

        val details = json.getJSONObject("details")
        assertEquals("QUOTA_EXHAUSTED", details.getString("reason"))
        assertEquals("cuenta1", details.getString("previousProfile"))
        assertEquals("cuenta2", details.getString("nextProfile"))
    }
}

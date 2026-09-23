package com.asistente.celular.ui.pc

import com.asistente.celular.nlu.pc.PcActionType
import com.asistente.celular.nlu.pc.PcInteractionAction
import com.asistente.celular.nlu.pc.PcWindowInfo
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para el modelo y contratos del Gestor Flotante de Ventanas de Windows (Window Switcher).
 */
class PcWindowSwitcherTest {

    @Test
    fun testPcWindowInfoProperties() {
        val window = PcWindowInfo(
            hwnd = 592342L,
            title = "Sin título* - Ableton Live 12 Suite",
            process = "Ableton Live 12 Suite.exe",
            pid = 12345,
            isForeground = true
        )

        assertEquals(592342L, window.hwnd)
        assertEquals("Sin título* - Ableton Live 12 Suite", window.title)
        assertEquals("Ableton Live 12 Suite.exe", window.process)
        assertEquals(12345, window.pid)
        assertTrue(window.isForeground)
    }

    @Test
    fun testFocusWindowActionCreation() {
        val action = PcInteractionAction(
            type = PcActionType.FOCUS_WINDOW,
            hwnd = 7799882L
        )

        assertEquals(PcActionType.FOCUS_WINDOW, action.type)
        assertEquals(7799882L, action.hwnd)
    }

    @Test
    fun testWindowListSorting() {
        val w1 = PcWindowInfo(hwnd = 1L, title = "WhatsApp", process = "brave.exe", isForeground = false)
        val w2 = PcWindowInfo(hwnd = 2L, title = "Ableton Live", process = "ableton.exe", isForeground = true)
        val w3 = PcWindowInfo(hwnd = 3L, title = "Terminal", process = "cmd.exe", isForeground = false)

        val list = listOf(w1, w2, w3)
        val sorted = list.sortedWith(
            compareByDescending<PcWindowInfo> { it.isForeground }
                .thenBy { it.process.lowercase() }
        )

        // La ventana en primer plano debe quedar en la primera posición
        assertEquals(2L, sorted[0].hwnd)
        assertTrue(sorted[0].isForeground)
        assertEquals("ableton.exe", sorted[0].process)

        assertEquals(1L, sorted[1].hwnd)
        assertEquals(3L, sorted[2].hwnd)
    }

    @Test
    fun testOpenWindowsJsonParsing() {
        val jsonString = """
            {
                "type": "OPEN_WINDOWS_LIST",
                "requestId": "req123",
                "windows": [
                    {
                        "hwnd": 592342,
                        "title": "Ableton Live 12 Suite",
                        "process": "ableton.exe",
                        "pid": 2040,
                        "isForeground": true
                    },
                    {
                        "hwnd": 66522,
                        "title": "WhatsApp - Brave",
                        "process": "brave.exe",
                        "pid": 8810,
                        "isForeground": false
                    }
                ]
            }
        """.trimIndent()

        val json = JSONObject(jsonString)
        assertEquals("OPEN_WINDOWS_LIST", json.getString("type"))
        assertEquals("req123", json.getString("requestId"))

        val arr = json.getJSONArray("windows")
        assertEquals(2, arr.length())

        val list = mutableListOf<PcWindowInfo>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                PcWindowInfo(
                    hwnd = obj.getLong("hwnd"),
                    title = obj.getString("title"),
                    process = obj.getString("process"),
                    pid = obj.optInt("pid", 0),
                    isForeground = obj.optBoolean("isForeground", false)
                )
            )
        }

        assertEquals(2, list.size)
        assertEquals(592342L, list[0].hwnd)
        assertEquals("Ableton Live 12 Suite", list[0].title)
        assertTrue(list[0].isForeground)

        assertEquals(66522L, list[1].hwnd)
        assertEquals("brave.exe", list[1].process)
        assertFalse(list[1].isForeground)
    }
}

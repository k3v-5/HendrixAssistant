package com.asistente.celular.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeSensitivityTest {

    @Test
    fun `fromName parses valid names ignoring case`() {
        assertEquals(ShakeSensitivity.GENTLE, ShakeSensitivity.fromName("GENTLE"))
        assertEquals(ShakeSensitivity.GENTLE, ShakeSensitivity.fromName("gentle"))
        assertEquals(ShakeSensitivity.NORMAL, ShakeSensitivity.fromName("Normal"))
        assertEquals(ShakeSensitivity.VIGOROUS, ShakeSensitivity.fromName("vigorous"))
    }

    @Test
    fun `fromName defaults to NORMAL on null or unknown value`() {
        assertEquals(ShakeSensitivity.NORMAL, ShakeSensitivity.fromName(null))
        assertEquals(ShakeSensitivity.NORMAL, ShakeSensitivity.fromName(""))
        assertEquals(ShakeSensitivity.NORMAL, ShakeSensitivity.fromName("UNKNOWN_MODE"))
    }

    @Test
    fun `thresholds and peaks escalate with sensitivity level`() {
        assertTrue(ShakeSensitivity.GENTLE.threshold < ShakeSensitivity.NORMAL.threshold)
        assertTrue(ShakeSensitivity.NORMAL.threshold < ShakeSensitivity.VIGOROUS.threshold)

        assertEquals(2, ShakeSensitivity.GENTLE.requiredShakes)
        assertEquals(2, ShakeSensitivity.NORMAL.requiredShakes)
        assertEquals(3, ShakeSensitivity.VIGOROUS.requiredShakes)

        assertTrue(ShakeSensitivity.GENTLE.debounceMillis > 0)
        assertTrue(ShakeSensitivity.NORMAL.debounceMillis > 0)
        assertTrue(ShakeSensitivity.VIGOROUS.debounceMillis > 0)
    }
}

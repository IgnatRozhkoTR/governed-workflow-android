package com.ignatrozhko.governedworkflow.discovery

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PanelFingerprintTest {
    private val json = Json

    @Test
    fun tokenConfigured_shouldIdentifyPanel_whenAuthStatusShape() {
        val body = """{"configured": true, "setup_command": "python3 admin-panel/backend/app.py auth-token"}"""

        assertEquals(true, PanelFingerprint.tokenConfigured(json, body))
    }

    @Test
    fun tokenConfigured_shouldReturnNull_whenOtherJsonService() {
        assertNull(PanelFingerprint.tokenConfigured(json, """{"configured": true}"""))
    }

    @Test
    fun tokenConfigured_shouldReturnNull_whenNotJson() {
        assertNull(PanelFingerprint.tokenConfigured(json, "<html>router login</html>"))
    }
}

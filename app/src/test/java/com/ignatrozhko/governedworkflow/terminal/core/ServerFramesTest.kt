package com.ignatrozhko.governedworkflow.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerFramesTest {

    @Test
    fun errorMessage_shouldExtractBackendRefusal() {
        assertEquals(
            "No tmux session. Use Start or Resume first.",
            ServerFrames.errorMessage("""{"error": "No tmux session. Use Start or Resume first."}"""),
        )
    }

    @Test
    fun errorMessage_shouldIgnoreTerminalOutput() {
        assertNull(ServerFrames.errorMessage("\u001b[H\u001b[2J$ "))
        assertNull(ServerFrames.errorMessage("{not json"))
        assertNull(ServerFrames.errorMessage("""{"resize": [80, 24]}"""))
    }

    @Test
    fun terminalSize_shouldEncodeResizeControlMessage() {
        assertEquals("""{"resize":[120,40]}""", TerminalSize(120, 40).controlMessage())
    }
}

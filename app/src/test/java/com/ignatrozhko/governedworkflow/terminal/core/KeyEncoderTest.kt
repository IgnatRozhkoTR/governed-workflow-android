package com.ignatrozhko.governedworkflow.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyEncoderTest {
    private val esc = "\u001b"
    private val ctrl = Modifiers(ctrl = true)
    private val alt = Modifiers(alt = true)
    private val shift = Modifiers(shift = true)

    private fun key(key: TerminalKey, modifiers: Modifiers = Modifiers.NONE, applicationCursor: Boolean = false) =
        KeyEncoder.encodeKey(key, modifiers, applicationCursor)

    @Test
    fun encodeKey_shouldSendCsiArrows_inNormalCursorMode() {
        assertEquals("$esc[A", key(TerminalKey.Up))
        assertEquals("$esc[B", key(TerminalKey.Down))
        assertEquals("$esc[C", key(TerminalKey.Right))
        assertEquals("$esc[D", key(TerminalKey.Left))
        assertEquals("$esc[H", key(TerminalKey.Home))
        assertEquals("$esc[F", key(TerminalKey.End))
    }

    @Test
    fun encodeKey_shouldSendSs3Arrows_inApplicationCursorMode() {
        assertEquals("${esc}OA", key(TerminalKey.Up, applicationCursor = true))
        assertEquals("${esc}OH", key(TerminalKey.Home, applicationCursor = true))
    }

    @Test
    fun encodeKey_shouldAddModifierParameter_whenArrowModified() {
        assertEquals("$esc[1;5C", key(TerminalKey.Right, ctrl))
        assertEquals("$esc[1;3D", key(TerminalKey.Left, alt, applicationCursor = true))
        assertEquals("$esc[1;2A", key(TerminalKey.Up, shift))
        assertEquals("$esc[1;8B", key(TerminalKey.Down, Modifiers(ctrl = true, alt = true, shift = true)))
    }

    @Test
    fun encodeKey_shouldSendTildeSequences_forPagingKeys() {
        assertEquals("$esc[5~", key(TerminalKey.PageUp))
        assertEquals("$esc[6~", key(TerminalKey.PageDown))
        assertEquals("$esc[3~", key(TerminalKey.Delete))
        assertEquals("$esc[2~", key(TerminalKey.Insert))
        assertEquals("$esc[5;5~", key(TerminalKey.PageUp, ctrl))
    }

    @Test
    fun encodeKey_shouldEncodeEditingKeys() {
        assertEquals("\r", key(TerminalKey.Enter))
        assertEquals("\t", key(TerminalKey.Tab))
        assertEquals("$esc[Z", key(TerminalKey.Tab, shift))
        assertEquals(esc, key(TerminalKey.Escape))
        assertEquals("\u007f", key(TerminalKey.Backspace))
        assertEquals("\b", key(TerminalKey.Backspace, ctrl))
        assertEquals("$esc\r", key(TerminalKey.Enter, alt))
    }

    @Test
    fun encodeText_shouldMapCtrlLetterToControlCode() {
        assertEquals("\u0003", KeyEncoder.encodeText("c", ctrl))
        assertEquals("\u0003", KeyEncoder.encodeText("C", ctrl))
        assertEquals("\u0004", KeyEncoder.encodeText("d", ctrl))
    }

    @Test
    fun encodeText_shouldApplyModifiersToFirstCharacterOnly() {
        assertEquals("\u0003at", KeyEncoder.encodeText("cat", ctrl))
        assertEquals("${esc}bc", KeyEncoder.encodeText("bc", alt))
        assertEquals("$esc\u0018", KeyEncoder.encodeText("x", Modifiers(ctrl = true, alt = true)))
    }

    @Test
    fun encodeText_shouldTranslateNewlineToCarriageReturn() {
        assertEquals("echo hi\r", KeyEncoder.encodeText("echo hi\n", Modifiers.NONE))
    }

    @Test
    fun encodeText_shouldKeepSurrogatePairsIntact_withAlt() {
        assertEquals("$esc😀x", KeyEncoder.encodeText("😀x", alt))
    }

    @Test
    fun controlCharacter_shouldCoverSymbolControls() {
        assertEquals("\u0000", KeyEncoder.controlCharacter(" "))
        assertEquals("\u001b", KeyEncoder.controlCharacter("["))
        assertEquals("\u001c", KeyEncoder.controlCharacter("\\"))
        assertEquals("\u001f", KeyEncoder.controlCharacter("/"))
        assertEquals("\u007f", KeyEncoder.controlCharacter("?"))
        assertNull(KeyEncoder.controlCharacter("é"))
    }
}

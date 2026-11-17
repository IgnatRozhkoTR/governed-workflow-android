package com.ignatrozhko.governedworkflow.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class StickyModifiersTest {

    @Test
    fun tap_shouldCycleOffOnceLockedOff() {
        val once = StickyModifiers().tap(StickyKey.Ctrl)
        val locked = once.tap(StickyKey.Ctrl)

        assertEquals(StickyLevel.Once, once.ctrl)
        assertEquals(StickyLevel.Locked, locked.ctrl)
        assertEquals(StickyLevel.Off, locked.tap(StickyKey.Ctrl).ctrl)
    }

    @Test
    fun afterKeyPress_shouldReleaseOnce_andKeepLocked() {
        val armed = StickyModifiers(ctrl = StickyLevel.Once, alt = StickyLevel.Locked)

        val after = armed.afterKeyPress()

        assertEquals(Modifiers(ctrl = true, alt = true), armed.active)
        assertEquals(StickyModifiers(ctrl = StickyLevel.Off, alt = StickyLevel.Locked), after)
        assertEquals(Modifiers(alt = true), after.active)
    }

    @Test
    fun stickyCtrl_shouldProduceCtrlC_whenNextTypedKeyIsC() {
        val sticky = StickyModifiers().tap(StickyKey.Ctrl)

        assertEquals("\u0003", KeyEncoder.encodeText("c", sticky.active))
        assertEquals("c", KeyEncoder.encodeText("c", sticky.afterKeyPress().active))
    }

    @Test
    fun stickyAlt_shouldModifyArrowKey() {
        val sticky = StickyModifiers().tap(StickyKey.Alt)

        assertEquals("\u001b[1;3A", KeyEncoder.encodeKey(TerminalKey.Up, sticky.active, applicationCursor = false))
    }
}

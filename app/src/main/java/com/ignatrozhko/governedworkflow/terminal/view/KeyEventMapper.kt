package com.ignatrozhko.governedworkflow.terminal.view

import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.ignatrozhko.governedworkflow.terminal.core.Modifiers
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalKey

/** Routes Android key events (hardware keyboards and IMEs that send raw keys) to terminal input. */
internal object KeyEventMapper {
    private val SPECIAL_KEYS = mapOf(
        KeyEvent.KEYCODE_ENTER to TerminalKey.Enter,
        KeyEvent.KEYCODE_NUMPAD_ENTER to TerminalKey.Enter,
        KeyEvent.KEYCODE_DEL to TerminalKey.Backspace,
        KeyEvent.KEYCODE_FORWARD_DEL to TerminalKey.Delete,
        KeyEvent.KEYCODE_INSERT to TerminalKey.Insert,
        KeyEvent.KEYCODE_TAB to TerminalKey.Tab,
        KeyEvent.KEYCODE_ESCAPE to TerminalKey.Escape,
        KeyEvent.KEYCODE_DPAD_UP to TerminalKey.Up,
        KeyEvent.KEYCODE_DPAD_DOWN to TerminalKey.Down,
        KeyEvent.KEYCODE_DPAD_LEFT to TerminalKey.Left,
        KeyEvent.KEYCODE_DPAD_RIGHT to TerminalKey.Right,
        KeyEvent.KEYCODE_MOVE_HOME to TerminalKey.Home,
        KeyEvent.KEYCODE_MOVE_END to TerminalKey.End,
        KeyEvent.KEYCODE_PAGE_UP to TerminalKey.PageUp,
        KeyEvent.KEYCODE_PAGE_DOWN to TerminalKey.PageDown,
    )
    private const val MODIFIER_META_MASK = KeyEvent.META_CTRL_MASK or KeyEvent.META_ALT_MASK

    /** Returns false for keys the terminal does not handle (e.g. Back, volume), leaving them to the system. */
    fun dispatch(event: KeyEvent, input: TerminalInput): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return SPECIAL_KEYS.containsKey(event.keyCode)
        val modifiers = Modifiers(ctrl = event.isCtrlPressed, alt = event.isAltPressed, shift = event.isShiftPressed)
        SPECIAL_KEYS[event.keyCode]?.let { key ->
            input.pressKey(key, modifiers)
            return true
        }
        val codePoint = event.getUnicodeChar(event.metaState and MODIFIER_META_MASK.inv())
        if (codePoint == 0 || codePoint and KeyCharacterMap.COMBINING_ACCENT != 0) return false
        input.typeText(String(Character.toChars(codePoint)), modifiers.copy(shift = false))
        return true
    }
}

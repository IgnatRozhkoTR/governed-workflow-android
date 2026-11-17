package com.ignatrozhko.governedworkflow.terminal.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** Keyboard input arriving from the IME, a hardware keyboard or the extra-keys row. */
interface TerminalInput {
    fun typeText(text: String, keyModifiers: Modifiers = Modifiers.NONE)
    fun pressKey(key: TerminalKey, keyModifiers: Modifiers = Modifiers.NONE)
    fun sendRaw(data: String)
}

/** Combines key input with the sticky Ctrl/Alt state and writes the encoded bytes to the session. */
class TerminalInputController(
    private val session: TerminalSession,
    private val stickyModifiers: MutableStateFlow<StickyModifiers>,
) : TerminalInput {

    override fun typeText(text: String, keyModifiers: Modifiers) {
        if (text.isEmpty()) return
        session.send(KeyEncoder.encodeText(text, consumeSticky() + keyModifiers))
    }

    override fun pressKey(key: TerminalKey, keyModifiers: Modifiers) {
        session.send(KeyEncoder.encodeKey(key, consumeSticky() + keyModifiers, session.applicationCursor))
    }

    override fun sendRaw(data: String) {
        session.send(data)
    }

    private fun consumeSticky(): Modifiers = stickyModifiers.getAndUpdate { it.afterKeyPress() }.active
}

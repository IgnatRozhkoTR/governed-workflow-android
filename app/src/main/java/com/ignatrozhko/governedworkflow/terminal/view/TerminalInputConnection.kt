package com.ignatrozhko.governedworkflow.terminal.view

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import com.ignatrozhko.governedworkflow.terminal.core.ComposingTextTracker
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalKey
import com.ignatrozhko.governedworkflow.terminal.core.TextEdit

/**
 * IME bridge in the spirit of Termux: the editable stays (nearly) empty so keyboards never try to
 * autocorrect terminal content; composing text is mirrored eagerly and committed text is sent as typed.
 */
internal class TerminalInputConnection(
    view: View,
    private val input: () -> TerminalInput?,
) : BaseInputConnection(view, true) {
    private val composing = ComposingTextTracker()

    override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
        apply(composing.compose(text.toString()))
        return super.setComposingText(text, newCursorPosition)
    }

    override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
        apply(composing.commit(text.toString()))
        editable?.clear()
        return true
    }

    override fun finishComposingText(): Boolean {
        composing.finish()
        super.finishComposingText()
        editable?.clear()
        return true
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (composing.hasComposition) {
            composing.finish()
            editable?.clear()
        }
        repeat(beforeLength) { input()?.pressKey(TerminalKey.Backspace) }
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        val terminalInput = input() ?: return false
        KeyEventMapper.dispatch(event, terminalInput)
        return true
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        input()?.pressKey(TerminalKey.Enter)
        return true
    }

    private fun apply(edit: TextEdit) {
        if (edit.isEmpty) return
        val terminalInput = input() ?: return
        repeat(edit.deleteCount) { terminalInput.sendRaw(BACKSPACE) }
        terminalInput.typeText(edit.insert)
    }

    private companion object {
        const val BACKSPACE = "\u007f"
    }
}

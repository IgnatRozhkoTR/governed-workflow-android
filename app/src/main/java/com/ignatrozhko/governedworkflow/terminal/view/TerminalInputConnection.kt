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
 * Multi-line text, such as a dictated paragraph, goes to [onInsertedText] so it arrives as a paste and a
 * line break inside it does not submit the prompt early.
 */
internal class TerminalInputConnection(
    view: View,
    private val input: TerminalInput,
    private val onInsertedText: (String) -> Unit,
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
        repeat(beforeLength) { input.pressKey(TerminalKey.Backspace) }
        return true
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        KeyEventMapper.dispatch(event, input)
        return true
    }

    override fun performEditorAction(actionCode: Int): Boolean {
        input.pressKey(TerminalKey.Enter)
        return true
    }

    private fun apply(edit: TextEdit) {
        if (edit.isEmpty) return
        repeat(edit.deleteCount) { input.sendRaw(BACKSPACE) }
        if (isMultiLine(edit.insert)) onInsertedText(edit.insert) else input.typeText(edit.insert)
    }

    /** A lone line break is the keyboard's Enter key and must submit; only text spanning lines is pasted. */
    private fun isMultiLine(text: String): Boolean {
        val content = text.trim('\n', '\r')
        return content.isNotEmpty() && text.any { it == '\n' || it == '\r' }
    }

    private companion object {
        const val BACKSPACE = "\u007f"
    }
}

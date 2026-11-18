package com.ignatrozhko.governedworkflow.terminal.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.ui.common.readClipboardText

/**
 * The terminal's text field as the system sees it. Being a real, unobscured EditText is what lets keyboards, dictation
 * apps such as Wispr Flow and accessibility services recognise an ordinary focused editable field: they look for that
 * node to show their UI and to insert text through paste or set-text. The field never keeps any text; typing goes
 * through [TerminalInputConnection] and inserted text is handed to [onInsertedText] for the pty.
 */
@SuppressLint("ViewConstructor")
internal class TerminalInputField(
    context: Context,
    private val input: TerminalInput,
    private val onInsertedText: (String) -> Unit,
) : EditText(context) {

    init {
        background = null
        setTextColor(Color.TRANSPARENT)
        setHintTextColor(Color.TRANSPARENT)
        hint = ACCESSIBLE_NAME
        isCursorVisible = false
        isSaveEnabled = false
        isLongClickable = false
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        inputType = TERMINAL_INPUT_TYPE
        imeOptions = TERMINAL_IME_OPTIONS
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = TERMINAL_INPUT_TYPE
        outAttrs.imeOptions = TERMINAL_IME_OPTIONS
        return TerminalInputConnection(this, input, onInsertedText)
    }

    /** The field is always empty, so "replace the text with X" from an accessibility service means "insert X". */
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action != AccessibilityNodeInfo.ACTION_SET_TEXT) return super.performAccessibilityAction(action, arguments)
        val text = arguments?.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE)?.toString()
        if (!text.isNullOrEmpty()) onInsertedText(text)
        return true
    }

    /** Covers the paste menu and accessibility ACTION_PASTE, which the framework routes through here. */
    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id != android.R.id.paste && id != android.R.id.pasteAsPlainText) return super.onTextContextMenuItem(id)
        val text = readClipboardText(context)
        if (text.isNotEmpty()) onInsertedText(text)
        return true
    }

    private companion object {
        const val ACCESSIBLE_NAME = "Terminal input"

        /**
         * Plain text without suggestions: no autocorrect rewriting what is sent to the shell. Deliberately not a
         * password variation and without IME_FLAG_NO_PERSONALIZED_LEARNING, which make keyboards switch to
         * incognito and dictation apps treat the field as sensitive and stay away.
         */
        const val TERMINAL_INPUT_TYPE = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        const val TERMINAL_IME_OPTIONS = EditorInfo.IME_FLAG_NO_FULLSCREEN or
            EditorInfo.IME_FLAG_NO_EXTRACT_UI or
            EditorInfo.IME_ACTION_NONE
    }
}

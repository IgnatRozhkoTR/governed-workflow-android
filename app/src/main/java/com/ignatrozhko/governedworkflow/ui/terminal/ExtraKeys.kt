package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.annotation.DrawableRes
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.terminal.core.Modifiers
import com.ignatrozhko.governedworkflow.terminal.core.StickyKey
import com.ignatrozhko.governedworkflow.terminal.core.TerminalKey

sealed interface ExtraKeyAction {
    data class Key(val key: TerminalKey, val modifiers: Modifiers = Modifiers.NONE) : ExtraKeyAction
    data class Text(val text: String) : ExtraKeyAction
    /** Sent verbatim, ignoring armed sticky modifiers, for the shortcut buttons that already encode one. */
    data class Raw(val data: String) : ExtraKeyAction
    data class Sticky(val key: StickyKey) : ExtraKeyAction
    data object Paste : ExtraKeyAction
    data object ToggleKeyboard : ExtraKeyAction
}

/** A key shows [iconRes] when set, otherwise [label]; the label doubles as the icon's content description. */
data class ExtraKey(
    val label: String,
    val action: ExtraKeyAction,
    val isRepeatable: Boolean = false,
    @param:DrawableRes val iconRes: Int? = null,
)

object ExtraKeyLayout {
    private const val ESC = "\u001b"
    private const val CTRL_C = "\u0003"

    private val escape = ExtraKey("Esc", ExtraKeyAction.Key(TerminalKey.Escape))
    private val shiftTab = ExtraKey("⇧Tab", ExtraKeyAction.Key(TerminalKey.Tab, Modifiers(shift = true)))
    private val slash = ExtraKey("/", ExtraKeyAction.Text("/"))
    private val dash = ExtraKey("-", ExtraKeyAction.Text("-"))
    private val pipe = ExtraKey("|", ExtraKeyAction.Text("|"))
    private val tilde = ExtraKey("~", ExtraKeyAction.Text("~"))
    private val home = ExtraKey("Home", ExtraKeyAction.Key(TerminalKey.Home))
    private val up = ExtraKey("↑", ExtraKeyAction.Key(TerminalKey.Up), isRepeatable = true)
    private val end = ExtraKey("End", ExtraKeyAction.Key(TerminalKey.End))
    private val pageUp = ExtraKey("PgUp", ExtraKeyAction.Key(TerminalKey.PageUp), isRepeatable = true)
    private val paste = ExtraKey("Paste", ExtraKeyAction.Paste, iconRes = R.drawable.ic_paste)
    private val tab = ExtraKey("Tab", ExtraKeyAction.Key(TerminalKey.Tab))
    private val ctrl = ExtraKey("Ctrl", ExtraKeyAction.Sticky(StickyKey.Ctrl))
    private val alt = ExtraKey("Alt", ExtraKeyAction.Sticky(StickyKey.Alt))
    private val interrupt = ExtraKey("^C", ExtraKeyAction.Raw(CTRL_C))
    private val doubleEscape = ExtraKey("Esc²", ExtraKeyAction.Raw(ESC + ESC))
    private val enter = ExtraKey("↵", ExtraKeyAction.Key(TerminalKey.Enter))
    private val left = ExtraKey("←", ExtraKeyAction.Key(TerminalKey.Left), isRepeatable = true)
    private val down = ExtraKey("↓", ExtraKeyAction.Key(TerminalKey.Down), isRepeatable = true)
    private val right = ExtraKey("→", ExtraKeyAction.Key(TerminalKey.Right), isRepeatable = true)
    private val pageDown = ExtraKey("PgDn", ExtraKeyAction.Key(TerminalKey.PageDown), isRepeatable = true)
    private val keyboard = ExtraKey("Keyboard", ExtraKeyAction.ToggleKeyboard, iconRes = R.drawable.ic_keyboard)

    /** Two rows whose columns line up so the arrow keys form an inverted T. */
    val twoRows: List<List<ExtraKey>> = listOf(
        listOf(escape, shiftTab, slash, dash, pipe, tilde, home, up, end, pageUp, paste),
        listOf(tab, ctrl, alt, interrupt, doubleEscape, enter, left, down, right, pageDown, keyboard),
    )

    /** Landscape with the keyboard up leaves room for one row, so the most used keys come first. */
    val oneRow: List<List<ExtraKey>> = listOf(
        listOf(
            escape, tab, ctrl, alt, interrupt, left, up, down, right, enter, shiftTab,
            home, end, pageUp, pageDown, slash, dash, pipe, tilde, doubleEscape, paste, keyboard,
        ),
    )
}

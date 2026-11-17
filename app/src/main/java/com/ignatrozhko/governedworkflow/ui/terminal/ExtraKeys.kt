package com.ignatrozhko.governedworkflow.ui.terminal

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

data class ExtraKey(val label: String, val action: ExtraKeyAction, val isRepeatable: Boolean = false)

/** Two rows whose columns line up so the arrow keys form an inverted T. */
object ExtraKeyLayout {
    private const val ESC = "\u001b"
    private const val CTRL_C = "\u0003"

    val rows: List<List<ExtraKey>> = listOf(
        listOf(
            ExtraKey("Esc", ExtraKeyAction.Key(TerminalKey.Escape)),
            ExtraKey("⇧Tab", ExtraKeyAction.Key(TerminalKey.Tab, Modifiers(shift = true))),
            ExtraKey("/", ExtraKeyAction.Text("/")),
            ExtraKey("-", ExtraKeyAction.Text("-")),
            ExtraKey("|", ExtraKeyAction.Text("|")),
            ExtraKey("~", ExtraKeyAction.Text("~")),
            ExtraKey("Home", ExtraKeyAction.Key(TerminalKey.Home)),
            ExtraKey("↑", ExtraKeyAction.Key(TerminalKey.Up), isRepeatable = true),
            ExtraKey("End", ExtraKeyAction.Key(TerminalKey.End)),
            ExtraKey("PgUp", ExtraKeyAction.Key(TerminalKey.PageUp), isRepeatable = true),
            ExtraKey("Paste", ExtraKeyAction.Paste),
        ),
        listOf(
            ExtraKey("Tab", ExtraKeyAction.Key(TerminalKey.Tab)),
            ExtraKey("Ctrl", ExtraKeyAction.Sticky(StickyKey.Ctrl)),
            ExtraKey("Alt", ExtraKeyAction.Sticky(StickyKey.Alt)),
            ExtraKey("^C", ExtraKeyAction.Raw(CTRL_C)),
            ExtraKey("Esc²", ExtraKeyAction.Raw(ESC + ESC)),
            ExtraKey("↵", ExtraKeyAction.Key(TerminalKey.Enter)),
            ExtraKey("←", ExtraKeyAction.Key(TerminalKey.Left), isRepeatable = true),
            ExtraKey("↓", ExtraKeyAction.Key(TerminalKey.Down), isRepeatable = true),
            ExtraKey("→", ExtraKeyAction.Key(TerminalKey.Right), isRepeatable = true),
            ExtraKey("PgDn", ExtraKeyAction.Key(TerminalKey.PageDown), isRepeatable = true),
            ExtraKey("⌨", ExtraKeyAction.ToggleKeyboard),
        ),
    )
}

package com.ignatrozhko.governedworkflow.terminal.core

data class Modifiers(val ctrl: Boolean = false, val alt: Boolean = false, val shift: Boolean = false) {
    val isEmpty: Boolean get() = !ctrl && !alt && !shift

    operator fun plus(other: Modifiers) = Modifiers(ctrl || other.ctrl, alt || other.alt, shift || other.shift)

    companion object {
        val NONE = Modifiers()
    }
}

enum class TerminalKey { Escape, Tab, Enter, Backspace, Delete, Insert, Up, Down, Left, Right, Home, End, PageUp, PageDown }

/** Encodes keys the way xterm does, which is what tmux and Claude Code expect from the outer terminal. */
object KeyEncoder {
    private const val ESC = "\u001b"
    private const val DEL = "\u007f"
    private const val BACKSPACE_CTRL = "\b"

    private val CURSOR_FINALS = mapOf(
        TerminalKey.Up to 'A', TerminalKey.Down to 'B', TerminalKey.Right to 'C', TerminalKey.Left to 'D',
        TerminalKey.Home to 'H', TerminalKey.End to 'F',
    )
    private val TILDE_CODES = mapOf(
        TerminalKey.Insert to 2, TerminalKey.Delete to 3, TerminalKey.PageUp to 5, TerminalKey.PageDown to 6,
    )

    fun encodeKey(key: TerminalKey, modifiers: Modifiers, applicationCursor: Boolean): String {
        CURSOR_FINALS[key]?.let { return cursorSequence(it, modifiers, applicationCursor) }
        TILDE_CODES[key]?.let { return tildeSequence(it, modifiers) }
        return when (key) {
            TerminalKey.Tab -> if (modifiers.shift) "$ESC[Z" else altPrefixed("\t", modifiers)
            TerminalKey.Enter -> altPrefixed("\r", modifiers)
            TerminalKey.Escape -> altPrefixed(ESC, modifiers)
            TerminalKey.Backspace -> altPrefixed(if (modifiers.ctrl) BACKSPACE_CTRL else DEL, modifiers)
            else -> error("Unmapped terminal key $key")
        }
    }

    /** Applies Ctrl/Alt to the first character only; IMEs may commit whole words at once. */
    fun encodeText(text: String, modifiers: Modifiers): String {
        val normalized = text.replace('\n', '\r')
        if (normalized.isEmpty() || (!modifiers.ctrl && !modifiers.alt)) return normalized
        val firstLength = Character.charCount(normalized.codePointAt(0))
        val first = normalized.substring(0, firstLength)
        val rest = normalized.substring(firstLength)
        val controlled = if (modifiers.ctrl) controlCharacter(first) ?: first else first
        return altPrefixed(controlled, modifiers) + rest
    }

    /** The C0 control a terminal sends for Ctrl+[character], or null when Ctrl has no meaning for it. */
    fun controlCharacter(character: String): String? {
        if (character.length != 1) return null
        val c = character[0]
        val code = when (c) {
            in 'a'..'z' -> c - 'a' + 1
            in 'A'..'Z' -> c - 'A' + 1
            '@', ' ', '2' -> 0
            '[', '3' -> 27
            '\\', '4' -> 28
            ']', '5' -> 29
            '^', '6' -> 30
            '_', '-', '/', '7' -> 31
            '?', '8' -> 127
            else -> return null
        }
        return code.toChar().toString()
    }

    private fun cursorSequence(final: Char, modifiers: Modifiers, applicationCursor: Boolean): String = when {
        !modifiers.isEmpty -> "$ESC[1;${modifierParameter(modifiers)}$final"
        applicationCursor -> "${ESC}O$final"
        else -> "$ESC[$final"
    }

    private fun tildeSequence(code: Int, modifiers: Modifiers): String =
        if (modifiers.isEmpty) "$ESC[$code~" else "$ESC[$code;${modifierParameter(modifiers)}~"

    private fun modifierParameter(modifiers: Modifiers): Int =
        1 + (if (modifiers.shift) 1 else 0) + (if (modifiers.alt) 2 else 0) + (if (modifiers.ctrl) 4 else 0)

    private fun altPrefixed(sequence: String, modifiers: Modifiers): String = if (modifiers.alt) ESC + sequence else sequence
}

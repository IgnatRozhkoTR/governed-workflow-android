package com.ignatrozhko.governedworkflow.terminal.core

/** What to send so the terminal line matches the IME: erase [deleteCount] characters, then type [insert]. */
data class TextEdit(val deleteCount: Int, val insert: String) {
    val isEmpty: Boolean get() = deleteCount == 0 && insert.isEmpty()

    companion object {
        val NONE = TextEdit(0, "")
    }
}

/**
 * Mirrors IME composing text into the terminal eagerly, so words appear while being typed and
 * autocorrect or suggestion picks are applied as backspaces plus the replacement suffix.
 */
class ComposingTextTracker {
    private var sent = ""

    val hasComposition: Boolean get() = sent.isNotEmpty()

    fun compose(text: String): TextEdit = transitionTo(text)

    fun commit(text: String): TextEdit = transitionTo(text).also { sent = "" }

    fun finish() {
        sent = ""
    }

    private fun transitionTo(target: String): TextEdit {
        val edit = diff(sent, target)
        sent = target
        return edit
    }

    companion object {
        fun diff(from: String, to: String): TextEdit {
            var common = 0
            while (common < from.length && common < to.length && from[common] == to[common]) common++
            if (common > 0 && Character.isHighSurrogate(from[common - 1]) && common < from.length) common--
            val removed = from.substring(common)
            return TextEdit(removed.codePointCount(0, removed.length), to.substring(common))
        }
    }
}

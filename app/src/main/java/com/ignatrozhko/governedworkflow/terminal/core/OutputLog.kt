package com.ignatrozhko.governedworkflow.terminal.core

/**
 * Bounded history of terminal output, replayed into a freshly created emulator view so the screen and
 * scrollback survive the WebView being recreated. Not thread-safe; the owning session synchronises.
 */
class OutputLog(private val capacityChars: Int) {
    private val chunks = ArrayDeque<String>()
    private var size = 0

    init {
        require(capacityChars > 0)
    }

    fun append(chunk: String) {
        if (chunk.isEmpty()) return
        chunks.addLast(chunk)
        size += chunk.length
        trimToCapacity()
    }

    fun snapshot(): String = buildString(size) { chunks.forEach(::append) }

    fun clear() {
        chunks.clear()
        size = 0
    }

    private fun trimToCapacity() {
        while (size > capacityChars) {
            val head = chunks.removeFirst()
            size -= head.length
            if (size < capacityChars) keepTailFromLineStart(head.takeLast(capacityChars - size))
        }
    }

    /** Starting the retained tail at a line boundary avoids replaying half of an escape sequence. */
    private fun keepTailFromLineStart(tail: String) {
        val lineStart = tail.indexOf('\n')
        if (lineStart < 0) return
        val keep = tail.substring(lineStart + 1)
        if (keep.isNotEmpty()) {
            chunks.addFirst(keep)
            size += keep.length
        }
    }
}

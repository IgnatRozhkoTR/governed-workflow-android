package com.ignatrozhko.governedworkflow.data.diff

/** One rendered row of a file's unified diff, with the line numbers on each side that it occupies. */
data class DiffLine(
    val kind: DiffLineKind,
    val text: String,
    val oldNumber: Int? = null,
    val newNumber: Int? = null,
    val changedRange: IntRange? = null,
)

enum class DiffLineKind { Hunk, Context, Added, Removed, Note }

/**
 * Parses the per-file `git diff` text the panel returns. File headers before the first hunk are dropped;
 * git's own notes (`\ No newline at end of file`, `Binary files … differ`) are kept as [DiffLineKind.Note].
 */
object UnifiedDiffParser {
    private val HUNK_HEADER = Regex("""^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@.*""")

    fun parse(diff: String): List<DiffLine> {
        val lines = mutableListOf<DiffLine>()
        var oldNumber = 0
        var newNumber = 0
        var isInHunk = false
        for (raw in diff.lineSequence()) {
            val hunk = HUNK_HEADER.matchEntire(raw)
            when {
                hunk != null -> {
                    oldNumber = hunk.groupValues[1].toInt()
                    newNumber = hunk.groupValues[2].toInt()
                    isInHunk = true
                    lines += DiffLine(DiffLineKind.Hunk, raw)
                }
                raw.startsWith("Binary files ") -> lines += DiffLine(DiffLineKind.Note, raw)
                !isInHunk -> Unit
                raw.startsWith("+") -> lines += DiffLine(DiffLineKind.Added, raw.substring(1), newNumber = newNumber++)
                raw.startsWith("-") -> lines += DiffLine(DiffLineKind.Removed, raw.substring(1), oldNumber = oldNumber++)
                raw.startsWith(" ") -> lines += DiffLine(DiffLineKind.Context, raw.substring(1), oldNumber++, newNumber++)
                raw.startsWith("\\") -> lines += DiffLine(DiffLineKind.Note, raw.removePrefix("\\ "))
                raw.startsWith("diff --git") -> isInHunk = false
            }
        }
        return WordDiff.markChanges(lines)
    }
}

/** Pairs each removed line with the added line at the same position of its change block and marks the span that differs. */
private object WordDiff {
    fun markChanges(lines: List<DiffLine>): List<DiffLine> {
        val result = lines.toMutableList()
        var index = 0
        while (index < lines.size) {
            val removedStart = index
            while (index < lines.size && lines[index].kind == DiffLineKind.Removed) index++
            val addedStart = index
            while (index < lines.size && lines[index].kind == DiffLineKind.Added) index++
            val pairs = minOf(addedStart - removedStart, index - addedStart)
            for (offset in 0 until pairs) markPair(result, removedStart + offset, addedStart + offset)
            if (index == removedStart) index++
        }
        return result
    }

    private fun markPair(lines: MutableList<DiffLine>, removedIndex: Int, addedIndex: Int) {
        val removed = lines[removedIndex].text
        val added = lines[addedIndex].text
        val prefix = removed.commonPrefixWith(added).length
        val suffix = removed.substring(prefix).commonSuffixWith(added.substring(prefix)).length
        lines[removedIndex] = lines[removedIndex].copy(changedRange = rangeOrNull(prefix, removed.length - suffix))
        lines[addedIndex] = lines[addedIndex].copy(changedRange = rangeOrNull(prefix, added.length - suffix))
    }

    private fun rangeOrNull(start: Int, endExclusive: Int) = (start until endExclusive).takeIf { !it.isEmpty() }
}

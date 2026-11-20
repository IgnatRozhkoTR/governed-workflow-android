package com.ignatrozhko.governedworkflow.data.diff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedDiffParserTest {
    private val pricingDiff = """
        diff --git a/src/pricing.py b/src/pricing.py
        index f723803..be2ef4a 100644
        --- a/src/pricing.py
        +++ b/src/pricing.py
        @@ -122,4 +122,5 @@ def helper_39(value):
             return value * 39
        -    return value * 40
        +    return value * 40 + 1
        +    # tweak
         
    """.trimIndent()

    @Test
    fun parse_shouldDropFileHeaders_andKeepHunkHeader() {
        val lines = UnifiedDiffParser.parse(pricingDiff)

        assertEquals(DiffLineKind.Hunk, lines.first().kind)
        assertTrue(lines.none { it.text.startsWith("index ") || it.text.startsWith("--- ") })
    }

    @Test
    fun parse_shouldNumberEachSideIndependently_whenLinesAreAddedAndRemoved() {
        val lines = UnifiedDiffParser.parse(pricingDiff).drop(1)

        assertEquals(DiffLine(DiffLineKind.Context, "    return value * 39", 122, 122), lines[0])
        assertEquals(DiffLine(DiffLineKind.Removed, "    return value * 40", oldNumber = 123), lines[1])
        assertEquals(DiffLine(DiffLineKind.Added, "    return value * 40 + 1", newNumber = 123, changedRange = 21..24), lines[2])
        assertEquals(DiffLine(DiffLineKind.Added, "    # tweak", newNumber = 124), lines[3])
        assertEquals(DiffLine(DiffLineKind.Context, "", 124, 125), lines[4])
    }

    @Test
    fun parse_shouldRestartNumbering_atEachHunk() {
        val diff = "@@ -1,1 +1,1 @@\n-a\n+b\n@@ -40 +40,2 @@\n c\n+d"

        val numbered = UnifiedDiffParser.parse(diff).filter { it.kind != DiffLineKind.Hunk }

        assertEquals(listOf(null, 1, 40, 41), numbered.map { it.newNumber })
        assertEquals(listOf(1, null, 40, null), numbered.map { it.oldNumber })
    }

    @Test
    fun parse_shouldKeepGitNotes_asNoteLines() {
        val diff = "@@ -1 +1 @@\n-a\n\\ No newline at end of file\n+b"

        val note = UnifiedDiffParser.parse(diff)[2]

        assertEquals(DiffLine(DiffLineKind.Note, "No newline at end of file"), note)
    }

    @Test
    fun parse_shouldReturnSingleNote_forBinaryFile() {
        val diff = "diff --git a/logo.png b/logo.png\nBinary files a/logo.png and b/logo.png differ"

        assertEquals(listOf(DiffLine(DiffLineKind.Note, "Binary files a/logo.png and b/logo.png differ")), UnifiedDiffParser.parse(diff))
    }

    @Test
    fun parse_shouldReturnEmpty_forEmptyDiff() {
        assertEquals(emptyList<DiffLine>(), UnifiedDiffParser.parse(""))
    }
}

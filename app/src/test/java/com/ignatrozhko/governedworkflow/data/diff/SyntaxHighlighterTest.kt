package com.ignatrozhko.governedworkflow.data.diff

import org.junit.Assert.assertEquals
import org.junit.Test

class SyntaxHighlighterTest {
    @Test
    fun highlight_shouldMarkKeywordsStringsNumbersAndComment_forPython() {
        val line = """def f(x): return "a#b" + 42  # note"""

        val spans = SyntaxHighlighter.forPath("src/pricing.py").highlight(line)

        assertEquals(
            listOf("def" to SyntaxKind.Keyword, "return" to SyntaxKind.Keyword, "\"a#b\"" to SyntaxKind.StringLiteral,
                "42" to SyntaxKind.Number, "# note" to SyntaxKind.Comment),
            spans.map { line.substring(it.start, it.end) to it.kind },
        )
    }

    @Test
    fun highlight_shouldNotTreatDigitsInsideIdentifiers_asNumbers() {
        val spans = SyntaxHighlighter.forPath("Main.kt").highlight("val helper40 = 1")

        assertEquals(listOf(SyntaxKind.Keyword, SyntaxKind.Number), spans.map { it.kind })
    }

    @Test
    fun highlight_shouldRunStringToLineEnd_whenUnterminated() {
        val line = "val s = \"open"

        val string = SyntaxHighlighter.forPath("a.kt").highlight(line).last()

        assertEquals(SyntaxSpan(8, line.length, SyntaxKind.StringLiteral), string)
    }

    @Test
    fun highlight_shouldSkipEscapedQuotes_insideStrings() {
        val line = """x = "a\"b" """

        val string = SyntaxHighlighter.forPath("a.py").highlight(line).single()

        assertEquals("\"a\\\"b\"", line.substring(string.start, string.end))
    }

    @Test
    fun highlight_shouldReturnNoSpans_forUnknownFileType() {
        assertEquals(emptyList<SyntaxSpan>(), SyntaxHighlighter.forPath("README").highlight("if true \"x\" 1"))
    }
}

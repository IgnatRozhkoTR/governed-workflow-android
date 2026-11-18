package com.ignatrozhko.governedworkflow.ui.workspace.common

/** A run of markdown, or a fenced mermaid block lifted out of it so it can be drawn as a diagram. */
sealed interface MarkdownSegment {
    data class Text(val markdown: String) : MarkdownSegment
    data class Mermaid(val source: String) : MarkdownSegment
}

private val MERMAID_FENCE = Regex("^[ \\t]*```mermaid[^\\n]*\\n(.*?)\\n[ \\t]*```[ \\t]*$", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
private val LEADING_MARKERS = Regex("^(#{1,6}\\s+|>\\s*|[-*+]\\s+|\\d+[.)]\\s+|\\[[ xX]]\\s+)+")
private val LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")
private val EMPHASIS = Regex("\\*{1,3}|`+")

fun splitMermaidBlocks(markdown: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    var textStart = 0
    MERMAID_FENCE.findAll(markdown).forEach { fence ->
        markdown.substring(textStart, fence.range.first).takeIf { it.isNotBlank() }?.let { segments += MarkdownSegment.Text(it) }
        segments += MarkdownSegment.Mermaid(fence.groupValues[1].trimEnd())
        textStart = fence.range.last + 1
    }
    markdown.substring(textStart).takeIf { it.isNotBlank() }?.let { segments += MarkdownSegment.Text(it) }
    return segments
}

/** The first line of prose in [markdown] as plain text, for the one-line preview of a collapsed block. */
fun previewLine(markdown: String): String {
    val firstLine = markdown.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("```") && !it.startsWith("|") }
        .orEmpty()
    return firstLine.replace(LEADING_MARKERS, "").replace(LINK, "$1").replace(EMPHASIS, "").trim()
}

package com.ignatrozhko.governedworkflow.ui.changes

/** The inline preview is the note's first sentence-bearing line with markdown syntax stripped, like the web panel. */
object AnnotationPreview {
    private val LEADING_MARKUP = Regex("""^\s*(#{1,6}\s+|>\s*|[-*+]\s+|\d+\.\s+)""")
    private val INLINE_MARKUP = Regex("""[*_`~]+""")
    private val LINK = Regex("""\[([^\]]*)]\([^)]*\)""")

    fun of(markdown: String): String {
        val firstLine = markdown.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("```") }.orEmpty()
        return firstLine
            .replace(LEADING_MARKUP, "")
            .replace(LINK, "$1")
            .replace(INLINE_MARKUP, "")
            .trim()
    }
}

package com.ignatrozhko.governedworkflow.data.diff

/** A highlighted span within a single line. */
data class SyntaxSpan(val start: Int, val end: Int, val kind: SyntaxKind)

enum class SyntaxKind { Keyword, StringLiteral, Comment, Number }

/**
 * Line-local keyword/string/comment colouring. It deliberately ignores multi-line constructs: diffs show
 * fragments, so a full parser would mis-colour them anyway, and this stays cheap on long files.
 */
class SyntaxHighlighter private constructor(
    private val keywords: Set<String>,
    private val lineComment: String?,
    private val isEnabled: Boolean = true,
) {
    fun highlight(line: String): List<SyntaxSpan> {
        if (!isEnabled) return emptyList()
        val spans = mutableListOf<SyntaxSpan>()
        var index = 0
        while (index < line.length) {
            val char = line[index]
            index = when {
                lineComment != null && line.startsWith(lineComment, index) -> {
                    spans += SyntaxSpan(index, line.length, SyntaxKind.Comment)
                    line.length
                }
                char == '"' || char == '\'' || char == '`' -> stringEnd(line, index, char).also {
                    spans += SyntaxSpan(index, it, SyntaxKind.StringLiteral)
                }
                char.isDigit() && (index == 0 || !line[index - 1].isLetterOrDigit()) -> wordEnd(line, index).also {
                    spans += SyntaxSpan(index, it, SyntaxKind.Number)
                }
                char.isLetter() || char == '_' -> wordEnd(line, index).also { end ->
                    if (line.substring(index, end) in keywords) spans += SyntaxSpan(index, end, SyntaxKind.Keyword)
                }
                else -> index + 1
            }
        }
        return spans
    }

    private fun stringEnd(line: String, start: Int, quote: Char): Int {
        var index = start + 1
        while (index < line.length) {
            if (line[index] == '\\') index++ else if (line[index] == quote) return index + 1
            index++
        }
        return line.length
    }

    private fun wordEnd(line: String, start: Int): Int {
        var index = start
        while (index < line.length && (line[index].isLetterOrDigit() || line[index] == '_')) index++
        return index
    }

    companion object {
        private val C_STYLE = setOf(
            "abstract", "async", "await", "break", "case", "catch", "class", "const", "continue", "data", "default", "do",
            "else", "enum", "export", "extends", "false", "final", "finally", "for", "fun", "function", "if", "implements",
            "import", "in", "interface", "is", "let", "new", "null", "object", "override", "package", "private",
            "protected", "public", "return", "static", "super", "switch", "this", "throw", "true", "try", "val", "var",
            "void", "when", "while", "yield",
        )
        private val PYTHON = setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except",
            "False", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "None", "not", "or", "pass",
            "raise", "return", "True", "try", "while", "with", "yield",
        )
        private val SHELL = setOf("if", "then", "else", "elif", "fi", "for", "do", "done", "case", "esac", "function", "return", "local", "export")
        private val PLAIN = SyntaxHighlighter(emptySet(), null, isEnabled = false)

        /** Picks rules by file extension; unknown types are left uncoloured rather than guessed. */
        fun forPath(path: String): SyntaxHighlighter = when (path.substringAfterLast('.', "").lowercase()) {
            "kt", "kts", "java", "js", "jsx", "ts", "tsx", "go", "rs", "c", "h", "cpp", "swift", "scala", "dart", "cs" ->
                SyntaxHighlighter(C_STYLE, "//")
            "py" -> SyntaxHighlighter(PYTHON, "#")
            "sh", "bash", "zsh" -> SyntaxHighlighter(SHELL, "#")
            "yml", "yaml", "toml" -> SyntaxHighlighter(setOf("true", "false", "null"), "#")
            else -> PLAIN
        }
    }
}

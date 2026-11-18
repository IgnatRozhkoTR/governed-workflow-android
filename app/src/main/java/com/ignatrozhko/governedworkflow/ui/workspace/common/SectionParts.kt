package com.ignatrozhko.governedworkflow.ui.workspace.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

val SectionContentPadding = PaddingValues(16.dp)
val SectionSpacing = Arrangement.spacedBy(12.dp)

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            content()
        }
    }
}

/** Markdown whose fenced mermaid blocks are drawn as diagrams instead of code. */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val segments = remember(markdown) { splitMermaidBlocks(markdown) }
    val onlyText = segments.singleOrNull() as? MarkdownSegment.Text
    if (onlyText != null || segments.isEmpty()) {
        PlainMarkdown(onlyText?.markdown.orEmpty(), modifier)
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.Text -> PlainMarkdown(segment.markdown)
                is MarkdownSegment.Mermaid -> DiagramBlock("Diagram", segment.source, "markdown-diagram:${segment.source.hashCode()}")
            }
        }
    }
}

/** Markdown sized like the surrounding body text, so notes do not shout over the cards they sit in. */
@Composable
private fun PlainMarkdown(markdown: String, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val typography = markdownTypography(
        h1 = type.titleLarge,
        h2 = type.titleMedium,
        h3 = type.titleSmall,
        h4 = type.titleSmall,
        h5 = type.labelLarge,
        h6 = type.labelLarge,
        text = type.bodyMedium,
        paragraph = type.bodyMedium,
        ordered = type.bodyMedium,
        bullet = type.bodyMedium,
        list = type.bodyMedium,
        quote = type.bodyMedium,
    )
    Markdown(content = markdown, typography = typography, modifier = modifier.fillMaxWidth())
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), contentColor = color, shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun MonospaceBlock(text: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Text(text, Modifier.padding(10.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun SecondaryText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun EmptySection(message: String) {
    SecondaryText(message, Modifier.padding(24.dp))
}

fun countLabel(count: Int, singular: String, plural: String = "${singular}s") = "$count ${if (count == 1) singular else plural}"

/** "2 open comments" for a header subtitle, or null when nothing is open. */
fun openCommentsLabel(comments: List<Comment>): String? =
    comments.count { !it.resolved }.takeIf { it > 0 }?.let { countLabel(it, "open comment") }

/** Joins the non-blank parts of a header subtitle with the separator used across the app. */
fun subtitleOf(vararg parts: String?): String = parts.filterNot { it.isNullOrBlank() }.joinToString(" · ")

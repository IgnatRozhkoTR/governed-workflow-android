package com.ignatrozhko.governedworkflow.ui.workspace.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownSegmentsTest {
    @Test
    fun splitMermaidBlocks_shouldReturnSingleText_whenThereIsNoMermaidFence() {
        val markdown = "# Title\n\n```kotlin\nval x = 1\n```"

        assertEquals(listOf(MarkdownSegment.Text(markdown)), splitMermaidBlocks(markdown))
    }

    @Test
    fun splitMermaidBlocks_shouldLiftDiagramOut_keepingTextAroundItInOrder() {
        val markdown = "Before\n```mermaid\nflowchart LR\n  A --> B\n```\nAfter"

        val segments = splitMermaidBlocks(markdown)

        assertEquals(
            listOf(
                MarkdownSegment.Text("Before\n"),
                MarkdownSegment.Mermaid("flowchart LR\n  A --> B"),
                MarkdownSegment.Text("\nAfter"),
            ),
            segments,
        )
    }

    @Test
    fun splitMermaidBlocks_shouldReturnEachDiagram_whenFencesAreAdjacent() {
        val markdown = "```mermaid\ngraph TD\n```\n\n```mermaid\nsequenceDiagram\n```"

        assertEquals(
            listOf(MarkdownSegment.Mermaid("graph TD"), MarkdownSegment.Mermaid("sequenceDiagram")),
            splitMermaidBlocks(markdown),
        )
    }

    @Test
    fun splitMermaidBlocks_shouldReturnNothing_whenMarkdownIsBlank() {
        assertEquals(emptyList<MarkdownSegment>(), splitMermaidBlocks("  \n"))
    }

    @Test
    fun previewLine_shouldStripHeadingAndEmphasis_fromFirstNonBlankLine() {
        assertEquals("Order totals are computed twice", previewLine("\n\n## **Order totals** are `computed` twice\nmore"))
    }

    @Test
    fun previewLine_shouldKeepLinkTextAndDropListMarker() {
        assertEquals("See the pricing module", previewLine("- See the [pricing module](src/pricing.py)"))
    }

    @Test
    fun previewLine_shouldSkipCodeFencesAndTables() {
        assertEquals("Summary after code", previewLine("```mermaid\n| a | b |\nSummary after code"))
    }

    @Test
    fun previewLine_shouldKeepSnakeCaseIdentifiersIntact() {
        assertEquals("Touches order_total in pricing", previewLine("Touches order_total in pricing"))
    }
}

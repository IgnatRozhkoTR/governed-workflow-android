package com.ignatrozhko.governedworkflow.ui.changes

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnotationPreviewTest {
    @Test
    fun of_shouldStripHeadingAndInlineMarkup() {
        assertEquals("Retry is bounded on purpose", AnnotationPreview.of("## Retry is **bounded** on `purpose`\n\nmore"))
    }

    @Test
    fun of_shouldSkipBlankLinesAndCodeFences() {
        assertEquals("val x = 1", AnnotationPreview.of("\n```kotlin\nval x = 1\n```"))
    }

    @Test
    fun of_shouldKeepLinkText_andDropUrl() {
        assertEquals("See the RFC for details", AnnotationPreview.of("- See [the RFC](https://example.com) for details"))
    }

    @Test
    fun of_shouldReturnEmpty_forBlankNote() {
        assertEquals("", AnnotationPreview.of("  \n "))
    }
}

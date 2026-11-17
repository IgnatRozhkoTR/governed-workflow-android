package com.ignatrozhko.governedworkflow.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ComposingTextTrackerTest {

    @Test
    fun compose_shouldSendOnlyNewSuffix_whileTyping() {
        val tracker = ComposingTextTracker()

        assertEquals(TextEdit(0, "h"), tracker.compose("h"))
        assertEquals(TextEdit(0, "e"), tracker.compose("he"))
        assertEquals(TextEdit(0, "llo"), tracker.compose("hello"))
    }

    @Test
    fun compose_shouldEraseAndRetype_whenAutocorrectReplacesWord() {
        val tracker = ComposingTextTracker()
        tracker.compose("teh")

        assertEquals(TextEdit(2, "he"), tracker.commit("the"))
        assertFalse(tracker.hasComposition)
    }

    @Test
    fun compose_shouldEmitBackspace_whenComposingTextShrinks() {
        val tracker = ComposingTextTracker()
        tracker.compose("echo")

        assertEquals(TextEdit(1, ""), tracker.compose("ech"))
    }

    @Test
    fun commit_shouldOnlyAppendSpace_whenCommittedTextExtendsComposition() {
        val tracker = ComposingTextTracker()
        tracker.compose("ls")

        assertEquals(TextEdit(0, " "), tracker.commit("ls "))
        assertEquals(TextEdit(0, "x"), tracker.compose("x"))
    }

    @Test
    fun finish_shouldKeepTypedText_andStartNextWordFresh() {
        val tracker = ComposingTextTracker()
        tracker.compose("hi")

        tracker.finish()

        assertEquals(TextEdit(0, "yo"), tracker.compose("yo"))
    }

    @Test
    fun diff_shouldCountCodePoints_whenRemovingEmoji() {
        assertEquals(TextEdit(1, ""), ComposingTextTracker.diff("a😀", "a"))
        assertEquals(TextEdit(1, "😁"), ComposingTextTracker.diff("😀", "😁"))
    }
}

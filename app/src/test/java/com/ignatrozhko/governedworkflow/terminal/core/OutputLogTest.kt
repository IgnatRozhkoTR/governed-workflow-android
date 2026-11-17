package com.ignatrozhko.governedworkflow.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputLogTest {

    @Test
    fun snapshot_shouldConcatenateChunks_whenUnderCapacity() {
        val log = OutputLog(100)
        log.append("ls\r\n")
        log.append("a b\r\n")

        assertEquals("ls\r\na b\r\n", log.snapshot())
    }

    @Test
    fun append_shouldDropOldestLines_andStartTailAtLineBoundary() {
        val log = OutputLog(10)

        log.append("line-one\nline-two\n")
        log.append("x")

        assertEquals("line-two\nx", log.snapshot())
    }

    @Test
    fun append_shouldDropWholeOldChunks_whenNewOutputFillsCapacity() {
        val log = OutputLog(6)
        log.append("old\n")

        log.append("fresh\n")

        assertEquals("fresh\n", log.snapshot())
    }

    @Test
    fun append_shouldDiscardPartialChunk_whenTailHasNoNewline() {
        val log = OutputLog(5)
        log.append("abcdefgh")

        assertEquals("", log.snapshot())
    }

    @Test
    fun clear_shouldEmptyLog() {
        val log = OutputLog(10)
        log.append("abc")

        log.clear()

        assertEquals("", log.snapshot())
    }
}

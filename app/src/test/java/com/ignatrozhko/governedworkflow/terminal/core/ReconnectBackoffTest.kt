package com.ignatrozhko.governedworkflow.terminal.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectBackoffTest {

    @Test
    fun nextDelayMs_shouldDoubleUntilCap() {
        val backoff = ReconnectBackoff(initialDelayMs = 500, maxDelayMs = 15_000)

        val delays = List(8) { backoff.nextDelayMs() }

        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 15_000L, 15_000L), delays)
        assertEquals(8, backoff.failedAttempts)
    }

    @Test
    fun reset_shouldRestartFromInitialDelay() {
        val backoff = ReconnectBackoff()
        repeat(4) { backoff.nextDelayMs() }

        backoff.reset()

        assertEquals(0, backoff.failedAttempts)
        assertEquals(500L, backoff.nextDelayMs())
    }

    @Test
    fun nextDelayMs_shouldStayCapped_afterManyFailures() {
        val backoff = ReconnectBackoff()
        repeat(1_000) { backoff.nextDelayMs() }

        assertEquals(15_000L, backoff.nextDelayMs())
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_shouldReject_whenMaxBelowInitial() {
        ReconnectBackoff(initialDelayMs = 1_000, maxDelayMs = 10)
    }
}

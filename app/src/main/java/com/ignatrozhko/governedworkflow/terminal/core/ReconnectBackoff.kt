package com.ignatrozhko.governedworkflow.terminal.core

/** Exponential reconnect delays, capped; reset once a connection opens. */
class ReconnectBackoff(
    private val initialDelayMs: Long = 500,
    private val maxDelayMs: Long = 15_000,
    private val multiplier: Double = 2.0,
) {
    init {
        require(initialDelayMs > 0 && maxDelayMs >= initialDelayMs && multiplier >= 1.0)
    }

    var failedAttempts: Int = 0
        private set

    fun nextDelayMs(): Long {
        var delay = initialDelayMs.toDouble()
        repeat(failedAttempts) { delay = minOf(delay * multiplier, maxDelayMs.toDouble()) }
        failedAttempts++
        return delay.toLong().coerceAtMost(maxDelayMs)
    }

    fun reset() {
        failedAttempts = 0
    }
}

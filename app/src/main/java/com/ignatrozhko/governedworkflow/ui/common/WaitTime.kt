package com.ignatrozhko.governedworkflow.ui.common

import java.time.Duration
import java.time.LocalDateTime

/** Coarse "how long has this gate been holding": minutes under an hour, hours under a day, then days. */
fun waitLabel(since: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val minutes = Duration.between(since, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "<1 min"
        minutes < MINUTES_PER_HOUR -> "$minutes min"
        minutes < MINUTES_PER_DAY -> "${minutes / MINUTES_PER_HOUR} h"
        else -> "${minutes / MINUTES_PER_DAY} d"
    }
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * 60

package com.ignatrozhko.governedworkflow.data.api

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * When the workspace entered its current phase, from the latest history entry that moved into it. The panel writes
 * naive timestamps in its host's local time, which for a home Mac is the phone's zone too.
 */
fun WorkspaceState.currentPhaseSince(): LocalDateTime? =
    phaseHistory.lastOrNull { it.to == phase }?.time?.let(::parsePanelTime)

fun PhaseTransition.occurredAt(): LocalDateTime? = parsePanelTime(time)

private fun parsePanelTime(text: String): LocalDateTime? = try {
    LocalDateTime.parse(text)
} catch (naive: DateTimeParseException) {
    parseZonedOrNull(text)
}

/** An unreadable timestamp only hides the wait time, so it yields null instead of failing the state. */
private fun parseZonedOrNull(text: String): LocalDateTime? = try {
    OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
} catch (zoned: DateTimeParseException) {
    null
}

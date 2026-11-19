package com.ignatrozhko.governedworkflow.gates

import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import java.time.LocalDateTime

/**
 * A workspace currently blocked on a user gate; [label] is the ticket id when the workspace has one, else the branch.
 * [since] is when the gate started holding, when the phase history says so.
 */
data class WaitingGate(val workspace: WorkspaceRef, val phase: String, val label: String, val since: LocalDateTime? = null)

data class GateChanges(val appeared: List<WaitingGate>, val cleared: Set<WorkspaceRef>)

/**
 * Remembers which gates were already announced so each one notifies once: a gate counts as new when its
 * workspace was not waiting before or now waits at a different phase.
 */
class GateTracker {
    private var knownPhases: Map<WorkspaceRef, String> = emptyMap()

    fun update(current: List<WaitingGate>): GateChanges {
        val appeared = current.filter { knownPhases[it.workspace] != it.phase }
        val cleared = knownPhases.keys - current.map { it.workspace }.toSet()
        knownPhases = current.associate { it.workspace to it.phase }
        return GateChanges(appeared, cleared)
    }
}

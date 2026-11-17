package com.ignatrozhko.governedworkflow.ui.workspace

import com.ignatrozhko.governedworkflow.data.api.ProofStatus
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.reviewComments

/** Detail screens reachable from the Overview hub; one per web panel tab or settings area. */
enum class WorkspaceSection(val title: String) {
    PrePlanning("Pre-planning"),
    Plan("Plan"),
    Research("Research"),
    Progress("Progress"),
    Review("Review issues"),
    Criteria("Acceptance criteria"),
    Reflection("Reflection proposals"),
    Scratchpads("Scratchpads"),
    Comments("Open comments"),
    Phase("Phase control"),
    Settings("Settings"),
    ;

    /** One-line hub summary computed from the polled state; sections with their own endpoint have none. */
    fun summary(state: WorkspaceState): String? = when (this) {
        PrePlanning -> counted(state.discussions.size, "discussion")
        Plan -> state.planStatus?.let { "Plan $it · ${counted(state.plan?.execution?.size ?: 0, "sub-phase")}" } ?: "No plan yet"
        Research -> "${counted(state.research.size, "entry", "entries")} · " +
            "${state.research.count { it.proofStatus == ProofStatus.Pending }} awaiting proof"
        Progress -> "${counted(state.progress.size, "phase")} reported"
        Review -> "${state.reviewComments().count { !it.resolved }} open"
        Comments -> "${state.comments.values.sumOf { list -> list.count { !it.resolved } }} open"
        Phase -> "Current ${state.phase}"
        Settings -> listOf(state.workflowMode, state.reviewMode.replace('_', ' ')).filter { it.isNotEmpty() }.joinToString(" · ")
        Criteria, Reflection, Scratchpads -> null
    }
}

private fun counted(count: Int, singular: String, plural: String = "${singular}s") = "$count ${if (count == 1) singular else plural}"

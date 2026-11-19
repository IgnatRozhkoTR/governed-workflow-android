package com.ignatrozhko.governedworkflow.ui.workspace

import com.ignatrozhko.governedworkflow.data.api.ProofStatus
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.reviewComments

/**
 * Detail screens reachable from the Overview chip row; one per web panel tab or settings area. [chipLabel] is short
 * enough for a chip, [title] heads the section screen.
 */
enum class WorkspaceSection(val title: String, val chipLabel: String) {
    Plan("Plan", "Plan"),
    Review("Review issues", "Issues"),
    Criteria("Acceptance criteria", "Criteria"),
    Research("Research", "Research"),
    Progress("Progress", "Progress"),
    Comments("Open comments", "Comments"),
    PrePlanning("Pre-planning", "Pre-planning"),
    Reflection("Reflection proposals", "Reflection"),
    Scratchpads("Scratchpads", "Scratchpads"),
    Phase("Phase control", "Phase"),
    Settings("Settings", "Settings"),
    ;

    /** What still needs attention in the section, from the polled state; null when there is nothing to count. */
    fun openCount(state: WorkspaceState): Int? = when (this) {
        Review -> state.reviewComments().count { !it.resolved }
        Comments -> state.comments.values.sumOf { list -> list.count { !it.resolved } }
        Research -> state.research.count { it.proofStatus == ProofStatus.Pending }
        PrePlanning -> state.discussions.size
        Plan, Criteria, Progress, Reflection, Scratchpads, Phase, Settings -> null
    }?.takeIf { it > 0 }
}

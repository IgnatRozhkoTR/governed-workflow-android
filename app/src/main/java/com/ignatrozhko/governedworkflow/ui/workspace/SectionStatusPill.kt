package com.ignatrozhko.governedworkflow.ui.workspace

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.ProofStatus
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.reviewComments
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.workspace.sections.planStatusTone

private const val PLAN_GATE_FAMILY = "2."
private const val APPROVED = "approved"

/** The one-glance status in a section's app bar: plan review state, verified research or open review issues. */
@Composable
fun SectionStatusPill(section: WorkspaceSection, state: WorkspaceState) {
    val end = Modifier.padding(end = 12.dp)
    when (section) {
        WorkspaceSection.Plan -> state.planStatus?.let { status ->
            val isPlanGateWaiting = status != APPROVED && state.pendingGate?.phase?.startsWith(PLAN_GATE_FAMILY) == true
            Pill(status.replace('_', ' '), if (isPlanGateWaiting) PillTone.Gate else planStatusTone(status), end, hasDot = true)
        }
        WorkspaceSection.Research -> if (state.research.isNotEmpty()) {
            val verified = state.research.count { it.proofStatus == ProofStatus.Proven }
            Pill("$verified/${state.research.size} verified", PillTone.Line, end)
        }
        WorkspaceSection.Review -> state.reviewComments().count { !it.resolved }.takeIf { it > 0 }?.let { Pill("$it open", PillTone.Warn, end) }
        else -> Unit
    }
}

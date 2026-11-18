package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.Proposal
import com.ignatrozhko.governedworkflow.data.api.ProposalDecision
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.listProposals
import com.ignatrozhko.governedworkflow.data.api.resolveProposal
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.StatusPill
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

class ReflectionViewModel(workspace: WorkspaceRef, connectionManager: ConnectionManager, stateStore: WorkspaceStateStore) :
    SectionDataViewModel<List<Proposal>>(workspace, connectionManager, stateStore) {
    override suspend fun load(api: PanelApi) = api.listProposals(workspace).sortedBy { !it.isOpen }

    fun resolve(proposalId: Long, decision: ProposalDecision) =
        actAndReload(if (decision == ProposalDecision.MarkApplied) "Marked applied" else "Proposal rejected") {
            it.resolveProposal(workspace, proposalId, decision)
        }
}

@Composable
fun ReflectionSection(proposals: List<Proposal>, isBusy: Boolean, onResolve: (Long, ProposalDecision) -> Unit, modifier: Modifier = Modifier) {
    if (proposals.isEmpty()) {
        EmptySection("No reflection proposals.")
        return
    }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        items(proposals, key = { it.id }) { ProposalCard(it, isBusy, onResolve) }
    }
}

@Composable
private fun ProposalCard(proposal: Proposal, isBusy: Boolean, onResolve: (Long, ProposalDecision) -> Unit) {
    CollapsibleCard(
        title = proposal.title,
        memoryKey = "proposal:${proposal.id}",
        subtitle = subtitleOf(proposal.type, proposal.origin),
        badge = { StatusPill(proposal.status, proposalStatusColor(proposal.status)) },
    ) {
        MarkdownText(proposal.body)
        if (proposal.reason.isNotBlank()) SecondaryText("Why: ${proposal.reason}")
        if (proposal.isOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onResolve(proposal.id, ProposalDecision.MarkApplied) }, enabled = !isBusy) { Text("Mark applied") }
                OutlinedButton(onClick = { onResolve(proposal.id, ProposalDecision.Reject) }, enabled = !isBusy) { Text("Reject") }
            }
        }
    }
}

private fun proposalStatusColor(status: String) = when (status) {
    "executed" -> PanelColors.Green
    "rejected" -> PanelColors.Red
    else -> PanelColors.Amber
}

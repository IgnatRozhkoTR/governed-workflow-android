package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.api.ProofStatus
import com.ignatrozhko.governedworkflow.data.api.ResearchEntry
import com.ignatrozhko.governedworkflow.data.api.ResearchFinding
import com.ignatrozhko.governedworkflow.data.api.ResearchProof
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.commentsFor
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentThread
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.MonospaceBlock
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.StatusPill

@Composable
fun ResearchSection(state: WorkspaceState, viewModel: WorkspaceViewModel, isBusy: Boolean, modifier: Modifier = Modifier) {
    if (state.research.isEmpty()) {
        EmptySection("No research entries yet.")
        return
    }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        items(state.research, key = { it.id }) { entry -> ResearchCard(entry, state, viewModel, isBusy) }
    }
}

@Composable
private fun ResearchCard(entry: ResearchEntry, state: WorkspaceState, viewModel: WorkspaceViewModel, isBusy: Boolean) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.topic, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            ProofPill(entry.proofStatus)
        }
        MarkdownText(entry.summary)
        entry.provenNotes?.takeIf { it.isNotBlank() }?.let { SecondaryText("Review notes: $it") }
        entry.findings.forEach { finding ->
            FindingBlock(finding)
            val target = CommentTargets.finding(finding.summary)
            CommentThread(
                comments = state.commentsFor(CommentTargets.RESEARCH_SCOPE, target),
                onAdd = { viewModel.addComment(NewComment(CommentTargets.RESEARCH_SCOPE, target, it)) },
                onResolve = viewModel::resolveComment,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { viewModel.setResearchProven(entry.id, isProven = true) },
                enabled = !isBusy && entry.proofStatus != ProofStatus.Proven,
            ) { Text("Mark proven") }
            OutlinedButton(
                onClick = { viewModel.setResearchProven(entry.id, isProven = false) },
                enabled = !isBusy && entry.proofStatus != ProofStatus.Rejected,
            ) { Text("Not proven") }
        }
    }
}

@Composable
private fun ProofPill(status: ProofStatus) = when (status) {
    ProofStatus.Proven -> StatusPill("proven", PanelColors.Green)
    ProofStatus.Rejected -> StatusPill("not proven", PanelColors.Red)
    ProofStatus.Pending -> StatusPill("awaiting proof", PanelColors.Amber)
}

@Composable
private fun FindingBlock(finding: ResearchFinding) {
    Text(finding.summary, style = MaterialTheme.typography.bodyMedium)
    if (finding.details.isNotBlank()) SecondaryText(finding.details)
    finding.proof?.let { ProofBlock(it) }
}

@Composable
private fun ProofBlock(proof: ResearchProof) {
    val location = proof.file?.let { file ->
        if (proof.lineStart != null) "$file:${proof.lineStart}-${proof.lineEnd ?: proof.lineStart}" else file
    }
    (location ?: proof.url ?: proof.title)?.let { SecondaryText("Proof: $it") }
    proof.snippet?.takeIf { it.isNotBlank() }?.let { MonospaceBlock(it) }
}

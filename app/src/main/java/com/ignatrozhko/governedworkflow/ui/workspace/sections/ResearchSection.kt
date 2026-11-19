package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleSection
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentThread
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.MonospaceBlock
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.countLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.openCommentsLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.previewLine
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

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
    CollapsibleCard(
        title = entry.topic,
        memoryKey = "research:${entry.id}",
        subtitle = subtitleOf(countLabel(entry.findings.size, "finding"), previewLine(entry.summary)),
        badge = { ProofPill(entry.proofStatus) },
    ) {
        MarkdownText(entry.summary)
        entry.provenNotes?.takeIf { it.isNotBlank() }?.let { SecondaryText("Review notes: $it") }
        entry.findings.forEachIndexed { index, finding ->
            HorizontalDivider()
            FindingSection(finding, "research:${entry.id}:finding:$index", state, viewModel)
        }
        HorizontalDivider()
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
    ProofStatus.Proven -> Pill("proven", PillTone.Ok)
    ProofStatus.Rejected -> Pill("not proven", PillTone.Bad)
    ProofStatus.Pending -> Pill("awaiting proof", PillTone.Warn)
}

@Composable
private fun FindingSection(finding: ResearchFinding, memoryKey: String, state: WorkspaceState, viewModel: WorkspaceViewModel) {
    val target = CommentTargets.finding(finding.summary)
    val comments = state.commentsFor(CommentTargets.RESEARCH_SCOPE, target)
    CollapsibleSection(
        title = finding.summary,
        memoryKey = memoryKey,
        subtitle = subtitleOf(finding.proof?.let(::proofLocation), openCommentsLabel(comments)),
        titleStyle = MaterialTheme.typography.bodyMedium,
    ) {
        if (finding.details.isNotBlank()) MarkdownText(finding.details)
        finding.proof?.let { ProofBlock(it, "$memoryKey:snippet") }
        CommentThread(
            comments = comments,
            onAdd = { viewModel.addComment(NewComment(CommentTargets.RESEARCH_SCOPE, target, it)) },
            onResolve = viewModel::resolveComment,
        )
    }
}

@Composable
private fun ProofBlock(proof: ResearchProof, memoryKey: String) {
    proofLocation(proof)?.let { SecondaryText("Proof: $it") }
    val snippet = proof.snippet?.takeIf { it.isNotBlank() } ?: return
    CollapsibleSection(
        title = "Proof snippet",
        memoryKey = memoryKey,
        subtitle = countLabel(snippet.lines().size, "line"),
        titleStyle = MaterialTheme.typography.labelLarge,
    ) { MonospaceBlock(snippet) }
}

private fun proofLocation(proof: ResearchProof): String? {
    val fileLocation = proof.file?.let { file ->
        if (proof.lineStart != null) "$file:${proof.lineStart}-${proof.lineEnd ?: proof.lineStart}" else file
    }
    return fileLocation ?: proof.url ?: proof.title
}

package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.api.ProofStatus
import com.ignatrozhko.governedworkflow.data.api.ResearchEntry
import com.ignatrozhko.governedworkflow.data.api.ResearchFinding
import com.ignatrozhko.governedworkflow.data.api.ResearchProof
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.commentsFor
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
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
        subtitle = if (entry.findings.isEmpty()) previewLine(entry.summary) else countLabel(entry.findings.size, "finding"),
        badge = { ProofPill(entry.proofStatus) },
    ) {
        MarkdownText(entry.summary)
        entry.provenNotes?.takeIf { it.isNotBlank() }?.let { SecondaryText("Review notes: $it") }
        entry.findings.forEachIndexed { index, finding -> FindingBox(finding, "research:${entry.id}:finding:$index", state, viewModel) }
        ProofActions(entry, isBusy, viewModel)
    }
}

@Composable
private fun ProofActions(entry: ResearchEntry, isBusy: Boolean, viewModel: WorkspaceViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        OutlinedActionButton(
            "Not proven",
            { viewModel.setResearchProven(entry.id, isProven = false) },
            isEnabled = !isBusy && entry.proofStatus != ProofStatus.Rejected,
        )
        PrimaryButton(
            "Mark proven",
            { viewModel.setResearchProven(entry.id, isProven = true) },
            isEnabled = !isBusy && entry.proofStatus != ProofStatus.Proven,
            icon = rememberVectorPainter(Icons.Filled.Check),
        )
    }
}

@Composable
fun ProofPill(status: ProofStatus) = when (status) {
    ProofStatus.Proven -> Pill("verified", PillTone.Ok)
    ProofStatus.Rejected -> Pill("rejected", PillTone.Bad)
    ProofStatus.Pending -> Pill("pending", PillTone.Warn)
}

/** A finding sits inset in its topic card: title, where the proof lives, the details and the discussion about it. */
@Composable
private fun FindingBox(finding: ResearchFinding, memoryKey: String, state: WorkspaceState, viewModel: WorkspaceViewModel) {
    val target = CommentTargets.finding(finding.summary)
    val comments = state.commentsFor(CommentTargets.RESEARCH_SCOPE, target)
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(finding.summary, style = MaterialTheme.typography.bodyLarge)
        finding.proof?.let(::proofLocation)?.let { Text(it, style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (finding.details.isNotBlank()) MarkdownText(finding.details)
        finding.proof?.let { ProofSnippet(it, "$memoryKey:snippet") }
        CommentThread(
            comments = comments,
            onAdd = { viewModel.addComment(NewComment(CommentTargets.RESEARCH_SCOPE, target, it)) },
            onResolve = viewModel::resolveComment,
        )
    }
}

@Composable
private fun ProofSnippet(proof: ResearchProof, memoryKey: String) {
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

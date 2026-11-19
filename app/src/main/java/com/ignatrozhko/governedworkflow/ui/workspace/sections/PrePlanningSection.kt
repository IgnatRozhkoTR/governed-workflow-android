package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.Discussion
import com.ignatrozhko.governedworkflow.data.api.ProofStatus
import com.ignatrozhko.governedworkflow.data.api.ResearchEntry
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.common.PanelCard
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleSection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.TextInputDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.countLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.previewLine
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

/** Phase 1 output: research summaries, the impact analysis and the open discussions with Claude. */
@Composable
fun PrePlanningSection(state: WorkspaceState, viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {
    var isAsking by remember { mutableStateOf(false) }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        item(key = "research") { ResearchSummariesCard(state.research) }
        item(key = "impact") { ImpactAnalysisCard(state.impactAnalysis) }
        item(key = "discussions-label") { DiscussionsHeader(state.discussions, onAsk = { isAsking = true }) }
        items(state.discussions.filter { it.hidden == 0 }, key = { it.id }) { DiscussionCard(it, viewModel::replyToDiscussion) }
    }
    if (isAsking) {
        TextInputDialog(title = "New question", label = "Question for Claude", confirmLabel = "Post", onDismiss = { isAsking = false }) {
            isAsking = false
            viewModel.addDiscussion(it)
        }
    }
}

@Composable
private fun DiscussionsHeader(discussions: List<Discussion>, onAsk: () -> Unit) {
    val openCount = discussions.count { it.status == OPEN_STATUS }
    PanelCard {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Discussions", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (openCount > 0) Pill("$openCount open", PillTone.Warn)
            TextButton(onClick = onAsk) { Text("New question") }
        }
    }
}

@Composable
private fun ResearchSummariesCard(research: List<ResearchEntry>) {
    CollapsibleCard(title = "Research summaries", memoryKey = "preplanning:research", subtitle = countLabel(research.size, "topic")) {
        if (research.isEmpty()) SecondaryText("No research yet.")
        research.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            CollapsibleSection(
                title = entry.topic,
                memoryKey = "preplanning:research:${entry.id}",
                subtitle = previewLine(entry.summary),
                titleStyle = MaterialTheme.typography.bodyLarge,
                badge = { Pill(proofLabel(entry.proofStatus), proofTone(entry.proofStatus)) },
            ) { MarkdownText(entry.summary) }
        }
    }
}

@Composable
private fun ImpactAnalysisCard(analysis: Map<String, String>) {
    val filled = analysis.filterValues { it.isNotBlank() }
    CollapsibleCard(title = "Impact analysis", memoryKey = "preplanning:impact", subtitle = "${filled.size} of ${analysis.size} recorded") {
        if (filled.isEmpty()) SecondaryText("Not recorded yet.")
        filled.entries.forEachIndexed { index, (field, value) ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            CollapsibleSection(
                title = field.replace('_', ' ').replaceFirstChar { it.uppercase() },
                memoryKey = "preplanning:impact:$field",
                subtitle = previewLine(value),
                titleStyle = MaterialTheme.typography.bodyLarge,
            ) { MarkdownText(value) }
        }
    }
}

@Composable
private fun DiscussionCard(discussion: Discussion, onReply: (Long, String) -> Unit) {
    var isReplying by remember { mutableStateOf(false) }
    CollapsibleCard(
        title = previewLine(discussion.text).ifBlank { "Discussion" },
        memoryKey = "discussion:${discussion.id}",
        subtitle = subtitleOf(discussion.author, discussion.type, discussion.status, countLabel(discussion.replies.size, "reply", "replies")),
    ) {
        MarkdownText(discussion.text)
        discussion.replies.forEach { reply -> SecondaryText("↳ ${reply.author}: ${reply.text}") }
        TextButton(onClick = { isReplying = true }) { Text("Reply") }
    }
    if (isReplying) {
        TextInputDialog(title = "Reply", label = "Reply", confirmLabel = "Post", onDismiss = { isReplying = false }) {
            isReplying = false
            onReply(discussion.id, it)
        }
    }
}

private fun proofLabel(status: ProofStatus) = when (status) {
    ProofStatus.Proven -> "verified"
    ProofStatus.Pending -> "pending"
    ProofStatus.Rejected -> "rejected"
}

private fun proofTone(status: ProofStatus) = when (status) {
    ProofStatus.Proven -> PillTone.Ok
    ProofStatus.Pending -> PillTone.Warn
    ProofStatus.Rejected -> PillTone.Bad
}

private const val OPEN_STATUS = "open"

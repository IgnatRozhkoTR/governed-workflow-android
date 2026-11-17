package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ignatrozhko.governedworkflow.data.api.Discussion
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.TextInputDialog

/** Phase 1 output: research summaries, the impact analysis and the open discussions with Claude. */
@Composable
fun PrePlanningSection(state: WorkspaceState, viewModel: WorkspaceViewModel, modifier: Modifier = Modifier) {
    var isAsking by remember { mutableStateOf(false) }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        item {
            SectionCard(title = "Research summaries") {
                if (state.research.isEmpty()) SecondaryText("No research yet.")
                state.research.forEach { entry ->
                    Text(entry.topic, style = MaterialTheme.typography.bodyLarge)
                    SecondaryText(entry.summary)
                }
            }
        }
        item { ImpactAnalysisCard(state.impactAnalysis) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Discussions", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { isAsking = true }) { Text("New question") }
            }
            if (state.discussions.isEmpty()) SecondaryText("No discussions.")
        }
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
private fun ImpactAnalysisCard(analysis: Map<String, String>) {
    SectionCard(title = "Impact analysis") {
        val filled = analysis.filterValues { it.isNotBlank() }
        if (filled.isEmpty()) SecondaryText("Not recorded yet.")
        filled.forEach { (field, value) ->
            Text(field.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
            MarkdownText(value)
        }
    }
}

@Composable
private fun DiscussionCard(discussion: Discussion, onReply: (Long, String) -> Unit) {
    var isReplying by remember { mutableStateOf(false) }
    SectionCard {
        SecondaryText(listOf(discussion.author, discussion.type, discussion.status).filter { it.isNotEmpty() }.joinToString(" · "))
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

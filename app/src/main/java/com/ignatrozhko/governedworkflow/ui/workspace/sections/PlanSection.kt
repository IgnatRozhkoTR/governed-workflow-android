package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.api.PhaseScope
import com.ignatrozhko.governedworkflow.data.api.PlanDecision
import com.ignatrozhko.governedworkflow.data.api.PlanDiagram
import com.ignatrozhko.governedworkflow.data.api.PlanSubphase
import com.ignatrozhko.governedworkflow.data.api.PlanTask
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
fun PlanSection(state: WorkspaceState, viewModel: WorkspaceViewModel, isBusy: Boolean, modifier: Modifier = Modifier) {
    val plan = state.plan
    if (plan == null) {
        EmptySection("Claude has not written a plan yet.")
        return
    }
    val comments = PlanComments(state, viewModel)
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        item { PlanHeader(state.planStatus, plan.description, isBusy, viewModel::decidePlan, comments) }
        items(plan.execution, key = { it.id }) { SubphaseCard(it, comments) }
        if (plan.diagrams.isNotEmpty()) item { DiagramsCard(plan.diagrams) }
    }
}

/** Binds comment threads to the plan item keys the web panel uses. */
private class PlanComments(private val state: WorkspaceState, private val viewModel: WorkspaceViewModel) {
    @Composable
    fun Thread(target: String) {
        CommentThread(
            comments = state.commentsFor(CommentTargets.PLAN_SCOPE, target),
            onAdd = { viewModel.addComment(NewComment(CommentTargets.PLAN_SCOPE, target, it)) },
            onResolve = viewModel::resolveComment,
        )
    }
}

@Composable
private fun PlanHeader(
    status: String?,
    description: String,
    isBusy: Boolean,
    onDecide: (PlanDecision) -> Unit,
    comments: PlanComments,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Plan", style = MaterialTheme.typography.titleMedium)
            status?.let { StatusPill(it, planStatusColor(it)) }
        }
        MarkdownText(description)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onDecide(PlanDecision.Approve) }, enabled = !isBusy && status != APPROVED) { Text("Approve plan") }
            OutlinedButton(onClick = { onDecide(PlanDecision.Reject) }, enabled = !isBusy && status != REJECTED) { Text("Reject") }
        }
        comments.Thread(CommentTargets.WHOLE_PLAN)
    }
}

@Composable
private fun SubphaseCard(subphase: PlanSubphase, comments: PlanComments) {
    SectionCard(title = "${subphase.id} · ${subphase.name}") {
        ScopeLines(subphase.scope)
        subphase.tasks.forEach { TaskBlock(it, comments) }
        comments.Thread(CommentTargets.subphase(subphase.id))
    }
}

@Composable
fun ScopeLines(scope: PhaseScope) {
    if (scope.must.isNotEmpty()) SecondaryText("Must: ${scope.must.joinToString(", ")}")
    if (scope.may.isNotEmpty()) SecondaryText("May: ${scope.may.joinToString(", ")}")
}

@Composable
private fun TaskBlock(task: PlanTask, comments: PlanComments) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (task.status.isNotEmpty()) StatusPill(task.status, taskStatusColor(task.status))
    }
    if (task.agent.isNotEmpty()) SecondaryText(task.agent)
    if (task.description.isNotEmpty()) MarkdownText(task.description)
    if (task.files.isNotEmpty()) SecondaryText(task.files.joinToString("\n"))
    comments.Thread(CommentTargets.task(task.title))
}

/** Mermaid has no native renderer here, so diagrams stay readable as their source, collapsed by default. */
@Composable
private fun DiagramsCard(diagrams: List<PlanDiagram>) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "System diagrams (${diagrams.size})") {
        TextButton(onClick = { isExpanded = !isExpanded }) { Text(if (isExpanded) "Hide sources" else "Show mermaid sources") }
        if (isExpanded) {
            diagrams.forEach { diagram ->
                Text(diagram.title, style = MaterialTheme.typography.bodyMedium)
                MonospaceBlock(diagram.diagram)
            }
        }
    }
}

private const val APPROVED = "approved"
private const val REJECTED = "rejected"

fun planStatusColor(status: String) = when (status) {
    APPROVED -> PanelColors.Green
    REJECTED -> PanelColors.Red
    else -> PanelColors.Amber
}

fun taskStatusColor(status: String) = when (status) {
    "done", "completed" -> PanelColors.Green
    "in_progress" -> PanelColors.Amber
    else -> PanelColors.Indigo
}

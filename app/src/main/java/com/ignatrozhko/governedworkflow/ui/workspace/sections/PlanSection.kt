package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.api.PhaseOrder
import com.ignatrozhko.governedworkflow.data.api.PhaseScope
import com.ignatrozhko.governedworkflow.data.api.PlanDecision
import com.ignatrozhko.governedworkflow.data.api.PlanSubphase
import com.ignatrozhko.governedworkflow.data.api.PlanTask
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.commentsFor
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleSection
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentThread
import com.ignatrozhko.governedworkflow.ui.workspace.common.DiagramCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.StatusPill
import com.ignatrozhko.governedworkflow.ui.workspace.common.countLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.openCommentsLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.previewLine
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

/** Read top-down like the web panel's plan tab: what the plan is, how the system looks, then the steps. */
@Composable
fun PlanSection(state: WorkspaceState, viewModel: WorkspaceViewModel, isBusy: Boolean, modifier: Modifier = Modifier) {
    val plan = state.plan
    if (plan == null) {
        EmptySection("Claude has not written a plan yet.")
        return
    }
    val comments = PlanComments(state, viewModel)
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        item(key = "header") { PlanHeader(state.planStatus, plan.description, isBusy, viewModel::decidePlan, comments) }
        if (plan.diagrams.isNotEmpty()) item(key = "diagrams-label") { GroupLabel("System diagrams") }
        itemsIndexed(plan.diagrams, key = { index, _ -> "diagram:$index" }) { index, diagram ->
            DiagramCard(diagram.title.ifBlank { "Diagram ${index + 1}" }, diagram.diagram, "plan:diagram:$index")
        }
        if (state.scope.isNotEmpty()) item(key = "scope") { OverallScopeCard(state.scope, plan.execution) }
        if (plan.execution.isNotEmpty()) item(key = "subphases-label") { GroupLabel("Sub-phases") }
        items(plan.execution, key = { "subphase:${it.id}" }) { subphase ->
            SubphaseCard(subphase, state.scope[subphase.id] ?: subphase.scope, state.phase, comments)
        }
    }
}

/** Binds comment threads to the plan item keys the web panel uses. */
private class PlanComments(private val state: WorkspaceState, private val viewModel: WorkspaceViewModel) {
    fun openLabel(target: String): String? = openCommentsLabel(state.commentsFor(CommentTargets.PLAN_SCOPE, target))

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
private fun GroupLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (description.isNotBlank()) {
            CollapsibleSection("Description", "plan:description", subtitle = previewLine(description)) { MarkdownText(description) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onDecide(PlanDecision.Approve) }, enabled = !isBusy && status != APPROVED) { Text("Approve plan") }
            OutlinedButton(onClick = { onDecide(PlanDecision.Reject) }, enabled = !isBusy && status != REJECTED) { Text("Reject") }
        }
        comments.Thread(CommentTargets.WHOLE_PLAN)
    }
}

@Composable
private fun OverallScopeCard(scope: Map<String, PhaseScope>, subphases: List<PlanSubphase>) {
    val namesById = subphases.associate { it.id to it.name }
    val summary = subtitleOf(
        countLabel(scope.size, "phase"),
        "${scope.values.sumOf { it.must.size }} must",
        "${scope.values.sumOf { it.may.size }} may",
    )
    CollapsibleCard(title = "Scope", memoryKey = "plan:scope", subtitle = summary) {
        scope.entries.sortedWith(compareBy(PhaseOrder) { it.key }).forEach { (phase, phaseScope) ->
            Text(listOfNotNull(phase, namesById[phase]).joinToString(" "), style = MaterialTheme.typography.labelLarge)
            ScopeLists(phaseScope)
        }
    }
}

@Composable
private fun SubphaseCard(subphase: PlanSubphase, scope: PhaseScope, currentPhase: String, comments: PlanComments) {
    val doneCount = subphase.tasks.count { it.status == DONE }
    val subphaseTarget = CommentTargets.subphase(subphase.id)
    CollapsibleCard(
        title = "${subphase.id} ${subphase.name}",
        memoryKey = "plan:subphase:${subphase.id}",
        subtitle = subtitleOf("$doneCount of ${countLabel(subphase.tasks.size, "task")} done", comments.openLabel(subphaseTarget)),
        badge = { SubphaseBadge(isCurrent = currentPhase.startsWith("${subphase.id}."), doneCount, subphase.tasks.size) },
    ) {
        if (scope.must.isNotEmpty() || scope.may.isNotEmpty()) {
            CollapsibleSection(
                title = "Scope",
                memoryKey = "plan:subphase:${subphase.id}:scope",
                subtitle = "${scope.must.size} must · ${scope.may.size} may",
                titleStyle = MaterialTheme.typography.labelLarge,
            ) { ScopeLists(scope) }
        }
        subphase.tasks.forEachIndexed { index, task ->
            HorizontalDivider()
            TaskSection(task, "plan:subphase:${subphase.id}:task:$index", comments)
        }
        HorizontalDivider()
        comments.Thread(subphaseTarget)
    }
}

@Composable
private fun SubphaseBadge(isCurrent: Boolean, doneCount: Int, taskCount: Int) = when {
    isCurrent -> StatusPill("current", PanelColors.Amber)
    taskCount > 0 && doneCount == taskCount -> StatusPill("done", PanelColors.Green)
    doneCount > 0 -> StatusPill("in progress", PanelColors.Amber)
    else -> StatusPill("pending", PanelColors.Indigo)
}

@Composable
private fun ScopeLists(scope: PhaseScope) {
    ScopeList("Must", scope.must, PanelColors.Red)
    ScopeList("May", scope.may, MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ScopeList(label: String, paths: List<String>, color: Color) {
    if (paths.isEmpty()) return
    Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    paths.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
}

/** Each task is its own toggle: the title and status stay visible, the description, files and comments open on tap. */
@Composable
private fun TaskSection(task: PlanTask, memoryKey: String, comments: PlanComments) {
    val target = CommentTargets.task(task.title)
    CollapsibleSection(
        title = task.title,
        memoryKey = memoryKey,
        subtitle = subtitleOf(task.agent, comments.openLabel(target)),
        badge = { if (task.status.isNotEmpty()) StatusPill(task.status, taskStatusColor(task.status)) },
        titleStyle = MaterialTheme.typography.bodyMedium,
    ) {
        if (task.description.isNotEmpty()) MarkdownText(task.description)
        task.files.forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
        comments.Thread(target)
    }
}

private const val APPROVED = "approved"
private const val REJECTED = "rejected"
private const val DONE = "done"

fun planStatusColor(status: String) = when (status) {
    APPROVED -> PanelColors.Green
    REJECTED -> PanelColors.Red
    else -> PanelColors.Amber
}

fun taskStatusColor(status: String) = when (status) {
    DONE, "completed" -> PanelColors.Green
    "in_progress" -> PanelColors.Amber
    else -> PanelColors.Indigo
}

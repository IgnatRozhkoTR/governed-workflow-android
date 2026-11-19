package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.ReviewResolution
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.reviewComments
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.SegmentedChoice
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing

/** Review findings grouped by file; tapping one opens that file's diff at the commented lines. */
@Composable
fun ReviewSection(
    state: WorkspaceState,
    viewModel: WorkspaceViewModel,
    isBusy: Boolean,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowingResolved by rememberSaveable { mutableStateOf(false) }
    var isChoosingResolution by remember { mutableStateOf(false) }
    val allIssues = state.reviewComments()
    val openCount = allIssues.count { !it.resolved }
    val issues = allIssues.filter { it.resolved == isShowingResolved }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SegmentedChoice(listOf(false, true), isShowingResolved, { if (it) "Resolved" else "Open" }, { isShowingResolved = it }, Modifier.width(190.dp))
                if (openCount > 0) TextButton(onClick = { isChoosingResolution = true }, enabled = !isBusy) { Text("Resolve all ($openCount)") }
            }
        }
        if (issues.isEmpty()) item { EmptySection(if (isShowingResolved) "No resolved issues." else "No open review issues.") }
        items(issues.groupBy { it.filePath ?: it.target }.toList(), key = { it.first }) { (file, fileIssues) ->
            ReviewFileCard(file, fileIssues, viewModel::resolveComment) { onOpenFile(file) }
        }
    }
    if (isChoosingResolution) {
        ResolutionDialog(onDismiss = { isChoosingResolution = false }) {
            isChoosingResolution = false
            viewModel.resolveAllReviewIssues(it)
        }
    }
}

/** One card per file: the name and open count stay visible, the findings open on tap. */
@Composable
private fun ReviewFileCard(file: String, issues: List<Comment>, onResolve: (Long, Boolean) -> Unit, onOpenFile: () -> Unit) {
    val openCount = issues.count { !it.resolved }
    CollapsibleCard(
        title = file.substringAfterLast('/'),
        memoryKey = "review:file:$file",
        subtitle = file.substringBeforeLast('/', missingDelimiterValue = ""),
        titleStyle = MonoText.body,
        badge = { if (openCount > 0) Pill("$openCount open", PillTone.Warn) else Pill("resolved", PillTone.Ok) },
    ) {
        issues.forEach { CommentCard(it, onResolve, onOpen = onOpenFile) }
        TextButton(onClick = onOpenFile) { Text("Open diff") }
    }
}

@Composable
private fun ResolutionDialog(onDismiss: () -> Unit, onChoose: (ReviewResolution) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resolve all open issues as") },
        text = {
            Column {
                ReviewResolution.entries.forEach { resolution ->
                    OutlinedButton(onClick = { onChoose(resolution) }) { Text(resolution.label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

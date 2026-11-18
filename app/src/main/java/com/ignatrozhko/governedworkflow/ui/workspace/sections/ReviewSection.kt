package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.ReviewResolution
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.reviewComments
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

/** Review findings grouped by file; tapping one opens that file's diff at the commented lines. */
@Composable
fun ReviewSection(
    state: WorkspaceState,
    viewModel: WorkspaceViewModel,
    isBusy: Boolean,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHidingResolved by rememberSaveable { mutableStateOf(true) }
    var isChoosingResolution by remember { mutableStateOf(false) }
    val issues = state.reviewComments().filter { !isHidingResolved || !it.resolved }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = isHidingResolved, onClick = { isHidingResolved = !isHidingResolved }, label = { Text("Hide resolved") })
                TextButton(onClick = { isChoosingResolution = true }, enabled = !isBusy) { Text("Resolve all") }
            }
        }
        if (issues.isEmpty()) item { EmptySection("No review issues.") }
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
    CollapsibleCard(
        title = file.substringAfterLast('/'),
        memoryKey = "review:file:$file",
        subtitle = subtitleOf(file.substringBeforeLast('/', missingDelimiterValue = ""), "${issues.count { !it.resolved }} of ${issues.size} open"),
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

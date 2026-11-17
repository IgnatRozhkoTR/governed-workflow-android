package com.ignatrozhko.governedworkflow.ui.changes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.CommitInfo
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.DiffSource
import com.ignatrozhko.governedworkflow.data.api.ReviewResolution
import com.ignatrozhko.governedworkflow.data.api.reviewComments
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.LoadStateContent
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText

@Composable
fun ChangesTab(viewModel: ChangesViewModel, onManageServers: () -> Unit, modifier: Modifier = Modifier) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val openPath by viewModel.openPath.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var isHidingResolved by rememberSaveable { mutableStateOf(true) }
    val reviewComments = snapshot.state?.reviewComments().orEmpty().filter { !isHidingResolved || !it.resolved }
    val path = openPath
    val openFile = (files as? LoadState.Loaded)?.data?.firstOrNull { it.path == path }
    if (path != null) {
        BackHandler(onBack = viewModel::closeFile)
        if (openFile == null) {
            EmptySection("$path has no changes in this view.")
            return
        }
        FileDiffView(openFile, reviewComments.forFile(path), isHidingResolved, viewModel, { isHidingResolved = it }, modifier)
        return
    }
    Column(modifier.fillMaxSize()) {
        SourceSelector(viewModel)
        ReviewToolbar(isHidingResolved, { isHidingResolved = it }, viewModel)
        LoadStateContent(files, isRefreshing = false, onRefresh = viewModel::reload, onManageServers = onManageServers) { loaded ->
            FileList(loaded, reviewComments, viewModel::openFile)
        }
    }
}

fun List<Comment>.forFile(path: String) = filter { (it.filePath ?: it.target) == path }

@Composable
private fun SourceSelector(viewModel: ChangesViewModel) {
    val source by viewModel.source.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var isCommitMenuOpen by remember { mutableStateOf(false) }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(source == DiffSource.Branch, { viewModel.selectSource(DiffSource.Branch) }, { Text("Branch") }) }
        item { FilterChip(source == DiffSource.Uncommitted, { viewModel.selectSource(DiffSource.Uncommitted) }, { Text("Uncommitted") }) }
        item {
            Box {
                val commit = source as? DiffSource.Commit
                FilterChip(
                    selected = commit != null,
                    onClick = { isCommitMenuOpen = true },
                    label = {
                        val label = commit?.let { "${it.sha.take(SHORT_SHA)} ${it.subject}" } ?: "Commit…"
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                )
                CommitMenu(isCommitMenuOpen, history, onDismiss = { isCommitMenuOpen = false }) {
                    isCommitMenuOpen = false
                    viewModel.selectSource(DiffSource.Commit(it.fullSha, it.subject))
                }
            }
        }
    }
}

@Composable
private fun CommitMenu(isOpen: Boolean, history: LoadState<List<CommitInfo>>, onDismiss: () -> Unit, onPick: (CommitInfo) -> Unit) {
    DropdownMenu(expanded = isOpen, onDismissRequest = onDismiss) {
        val commits = (history as? LoadState.Loaded)?.data.orEmpty()
        if (commits.isEmpty()) DropdownMenuItem(text = { Text("No commits on this branch") }, onClick = onDismiss, enabled = false)
        commits.forEach { commit ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text("${commit.sha.take(SHORT_SHA)}  ${commit.subject}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        SecondaryText("${commit.authorName} · ${commit.authorDate.take(DATE_LENGTH)}")
                    }
                },
                onClick = { onPick(commit) },
            )
        }
    }
}

@Composable
private fun ReviewToolbar(isHidingResolved: Boolean, onHidingResolvedChange: (Boolean) -> Unit, viewModel: ChangesViewModel) {
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var isChoosingResolution by remember { mutableStateOf(false) }
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(isHidingResolved, { onHidingResolvedChange(!isHidingResolved) }, { Text("Hide resolved") })
        TextButton(onClick = { isChoosingResolution = true }, enabled = !isBusy) { Text("Resolve all issues") }
    }
    if (isChoosingResolution) {
        AlertDialog(
            onDismissRequest = { isChoosingResolution = false },
            title = { Text("Resolve all open review issues as") },
            text = {
                Column {
                    ReviewResolution.entries.forEach { resolution ->
                        OutlinedButton(onClick = {
                            isChoosingResolution = false
                            viewModel.resolveAllReviewIssues(resolution)
                        }) { Text(resolution.label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { isChoosingResolution = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun FileList(files: List<DiffFile>, comments: List<Comment>, onOpen: (String) -> Unit) {
    if (files.isEmpty()) {
        EmptySection("No changes.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(files, key = { it.path }) { file ->
            val openComments = comments.forFile(file.path).count { !it.resolved }
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(file.path) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(file.path.substringAfterLast('/'), style = MaterialTheme.typography.bodyLarge)
                    SecondaryText(file.oldPath?.let { "$it → ${file.path}" } ?: file.path)
                }
                if (openComments > 0) Text("$openComments open  ", style = MaterialTheme.typography.labelMedium, color = PanelColors.Amber)
                Text("+${file.additions}", color = PanelColors.Green, style = MaterialTheme.typography.labelMedium)
                Text(" −${file.deletions}", color = PanelColors.Red, style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
        }
    }
}

private const val SHORT_SHA = 7
private const val DATE_LENGTH = 10

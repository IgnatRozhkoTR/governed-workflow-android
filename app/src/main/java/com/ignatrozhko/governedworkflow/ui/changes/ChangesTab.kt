package com.ignatrozhko.governedworkflow.ui.changes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.api.CodeRepo
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.CommitInfo
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.DiffSource
import com.ignatrozhko.governedworkflow.data.api.ReviewResolution
import com.ignatrozhko.governedworkflow.data.api.reviewComments
import com.ignatrozhko.governedworkflow.ui.common.ActionFooter
import com.ignatrozhko.governedworkflow.ui.common.EmptyState
import com.ignatrozhko.governedworkflow.ui.common.ListRowLayout
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.LoadStateContent
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.RowGroup
import com.ignatrozhko.governedworkflow.ui.common.ResolutionDialog
import com.ignatrozhko.governedworkflow.ui.common.SegmentedChoice
import com.ignatrozhko.governedworkflow.ui.common.withBreakOpportunities
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText

@Composable
fun ChangesTab(viewModel: ChangesViewModel, onManageServers: () -> Unit, modifier: Modifier = Modifier) {
    val repos by viewModel.repos.collectAsStateWithLifecycle()
    val selectedRepo by viewModel.selectedRepo.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val openPath by viewModel.openPath.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var isHidingResolved by rememberSaveable { mutableStateOf(true) }
    val allReviewComments = snapshot.state?.reviewComments().orEmpty()
    val reviewComments = allReviewComments.filter { !isHidingResolved || !it.resolved }
    val path = openPath
    val openFile = (files as? LoadState.Loaded)?.data?.firstOrNull { it.path == path }
    if (path != null) {
        BackHandler(onBack = viewModel::closeFile)
        if (openFile == null) {
            EmptySection("$path has no changes in this view.")
            return
        }
        val allFiles = (files as? LoadState.Loaded)?.data.orEmpty()
        Column(modifier) {
            FileDiffView(openFile, reviewComments.forFile(path), isHidingResolved, viewModel, { isHidingResolved = it }, Modifier.weight(1f))
            FileNavigator(allFiles.indexOf(openFile), allFiles.size) { viewModel.openFile(allFiles[it].path) }
        }
        return
    }
    LoadStateContent(repos, isRefreshing = false, onRefresh = viewModel::reload, onManageServers = onManageServers, modifier = modifier) { repoList ->
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (repoList.size > 1) RepoPicker(repoList, selectedRepo, viewModel::selectRepo, Modifier.padding(top = 12.dp))
            if (selectedRepo == null) {
                EmptyState(
                    "Choose a repository",
                    message = "This workspace spans ${repoList.size} repositories; changes are shown one repository at a time.",
                    icon = painterResource(R.drawable.ic_branch),
                )
                return@Column
            }
            SourceSelector(viewModel, Modifier.padding(top = if (repoList.size > 1) 0.dp else 12.dp))
            LoadStateContent(files, isRefreshing = false, onRefresh = viewModel::reload, onManageServers = onManageServers, Modifier.weight(1f)) { loaded ->
                val openIssueCount = allReviewComments.count { !it.resolved }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { FileList(loaded, reviewComments, viewModel::openFile) }
                    item { ReviewLinks(isHidingResolved, { isHidingResolved = it }, openIssueCount, viewModel) }
                }
            }
        }
    }
}

@Composable
private fun FileNavigator(index: Int, count: Int, onOpen: (Int) -> Unit) {
    ActionFooter {
        OutlinedActionButton(
            "Previous",
            { onOpen(index - 1) },
            isEnabled = index > 0,
            icon = rememberVectorPainter(Icons.AutoMirrored.Filled.KeyboardArrowLeft),
        )
        Text("File ${index + 1} of $count", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        OutlinedActionButton("Next file", { onOpen(index + 1) }, isEnabled = index < count - 1)
    }
}

fun List<Comment>.forFile(path: String) = filter { (it.filePath ?: it.target) == path }

/** First control of a multi-repo workspace, so a diff is never requested without saying which repository. */
@Composable
private fun RepoPicker(repos: List<CodeRepo>, selected: CodeRepo?, onSelect: (CodeRepo) -> Unit, modifier: Modifier = Modifier) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(role = Role.DropdownList) { isMenuOpen = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_branch), contentDescription = null, Modifier.size(16.dp))
            Text(
                selected?.let { "Repository: ${it.name}" } ?: "Choose a repository",
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            selected?.let { repo ->
                SecondaryText("${repos.indexOf(repo) + 1} of ${repos.size}")
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            repos.forEach { repo ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(repo.name)
                            Text(repo.path.withBreakOpportunities(), style = MonoText.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = {
                        isMenuOpen = false
                        onSelect(repo)
                    },
                )
            }
        }
    }
}

private enum class SourceKind(val label: String) { Branch("Branch"), Uncommitted("Uncommitted"), Commit("Commit") }

private val DiffSource.kind: SourceKind
    get() = when (this) {
        DiffSource.Branch -> SourceKind.Branch
        DiffSource.Uncommitted -> SourceKind.Uncommitted
        is DiffSource.Commit -> SourceKind.Commit
    }

@Composable
private fun SourceSelector(viewModel: ChangesViewModel, modifier: Modifier = Modifier) {
    val source by viewModel.source.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var isCommitMenuOpen by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            SegmentedChoice(SourceKind.entries, source.kind, SourceKind::label, onSelect = { kind ->
                when (kind) {
                    SourceKind.Branch -> viewModel.selectSource(DiffSource.Branch)
                    SourceKind.Uncommitted -> viewModel.selectSource(DiffSource.Uncommitted)
                    SourceKind.Commit -> isCommitMenuOpen = true
                }
            })
            CommitMenu(isCommitMenuOpen, history, onDismiss = { isCommitMenuOpen = false }) {
                isCommitMenuOpen = false
                viewModel.selectSource(DiffSource.Commit(it.fullSha, it.subject))
            }
        }
        (source as? DiffSource.Commit)?.let { commit ->
            Text("${commit.sha.take(SHORT_SHA)}  ${commit.subject}", style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ReviewLinks(isHidingResolved: Boolean, onHidingResolvedChange: (Boolean) -> Unit, openIssueCount: Int, viewModel: ChangesViewModel) {
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var isChoosingResolution by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onHidingResolvedChange(!isHidingResolved) }) { Text(if (isHidingResolved) "Show resolved" else "Hide resolved") }
        if (openIssueCount > 0) {
            TextButton(onClick = { isChoosingResolution = true }, enabled = !isBusy) { Text("Resolve all issues ($openIssueCount)") }
        }
    }
    if (isChoosingResolution) {
        ResolutionDialog("Resolve all open review issues as", onDismiss = { isChoosingResolution = false }) {
            isChoosingResolution = false
            viewModel.resolveAllReviewIssues(it)
        }
    }
}

@Composable
private fun FileList(files: List<DiffFile>, comments: List<Comment>, onOpen: (String) -> Unit) {
    if (files.isEmpty()) {
        EmptyState("No changes", message = "Nothing differs in this view.")
        return
    }
    RowGroup(files) { file -> FileRow(file, comments.forFile(file.path).count { !it.resolved }) { onOpen(file.path) } }
}

@Composable
private fun FileRow(file: DiffFile, openComments: Int, onClick: () -> Unit) {
    ListRowLayout(
        onClick = onClick,
        showsChevron = false,
        trailing = {
            if (openComments > 0) {
                Icon(painterResource(R.drawable.ic_comment), contentDescription = "Open comments", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$openComments", style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("+${file.additions}", style = MonoText.small, color = Checkpoint.colors.ok)
            Text("−${file.deletions}", style = MonoText.small, color = MaterialTheme.colorScheme.error)
        },
    ) {
        Text(file.path.substringAfterLast('/').withBreakOpportunities(), style = MonoText.code, color = MaterialTheme.colorScheme.onSurface)
        val location = file.oldPath?.let { "from $it" } ?: file.path.substringBeforeLast('/', missingDelimiterValue = "")
        if (location.isNotEmpty()) Text(location.withBreakOpportunities(), style = MonoText.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private const val SHORT_SHA = 7
private const val DATE_LENGTH = 10

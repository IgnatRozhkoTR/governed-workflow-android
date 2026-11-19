package com.ignatrozhko.governedworkflow.ui.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.ui.common.EmptyState
import com.ignatrozhko.governedworkflow.ui.common.FieldLabel
import com.ignatrozhko.governedworkflow.ui.common.ListRowLayout
import com.ignatrozhko.governedworkflow.ui.common.LoadStateContent
import com.ignatrozhko.governedworkflow.ui.common.PanelTopBar
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.RowGroup
import com.ignatrozhko.governedworkflow.ui.common.Tag
import com.ignatrozhko.governedworkflow.ui.common.waitLabel
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

@Composable
fun WorkspacesScreen(
    title: String,
    onOpenWorkspace: (branch: String, hasGate: Boolean) -> Unit,
    onWorkspaceCreated: (String) -> Unit,
    onManageServers: () -> Unit,
    onBack: () -> Unit,
    viewModel: WorkspacesViewModel = viewModel(factory = WorkspacesViewModel.Factory),
) {
    val state by viewModel.workspaces.collectAsStateWithLifecycle()
    val creation by viewModel.creation.collectAsStateWithLifecycle()
    CreationFlow(creation, viewModel, onWorkspaceCreated)
    Scaffold(
        topBar = { PanelTopBar(title = title, onBack = onBack, onManageServers = onManageServers) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::startCreating,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New workspace", style = MaterialTheme.typography.labelLarge) },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LoadStateContent(
            state = state,
            isRefreshing = false,
            onRefresh = viewModel::reload,
            onManageServers = onManageServers,
            modifier = Modifier.padding(padding),
        ) { list -> WorkspaceGroups(list) { row -> onOpenWorkspace(row.summary.branch, row.gate != null) } }
    }
}

/** Workspaces holding at a gate come first under "Needs you", oldest wait on top; archived ones stay folded away. */
@Composable
private fun WorkspaceGroups(list: WorkspaceList, onOpen: (WorkspaceRow) -> Unit) {
    var isArchiveShown by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (list.rows.isEmpty()) item { EmptyState("No workspaces yet", message = "Start one with New workspace.") }
        if (list.waiting.isNotEmpty()) {
            item { FieldLabel("Needs you", Modifier.padding(start = 4.dp)) }
            item { RowGroup(list.waiting) { WorkspaceRowItem(it, list.phaseNames) { onOpen(it) } } }
        }
        if (list.live.isNotEmpty()) {
            if (list.waiting.isNotEmpty()) item { FieldLabel("Other workspaces", Modifier.padding(start = 4.dp, top = 4.dp)) }
            item { RowGroup(list.live) { WorkspaceRowItem(it, list.phaseNames) { onOpen(it) } } }
        }
        if (list.archived.isNotEmpty()) {
            item {
                TextButton(onClick = { isArchiveShown = !isArchiveShown }) {
                    Text(if (isArchiveShown) "Hide archived" else "Show archived (${list.archived.size})")
                }
            }
            if (isArchiveShown) {
                item { RowGroup(list.archived, Modifier.alpha(ARCHIVED_ALPHA)) { WorkspaceRowItem(it, list.phaseNames) { onOpen(it) } } }
            }
        }
    }
}

@Composable
private fun CreationFlow(creation: WorkspaceCreation, viewModel: WorkspacesViewModel, onWorkspaceCreated: (String) -> Unit) {
    when (creation) {
        is WorkspaceCreation.Editing -> NewWorkspaceSheet(creation, viewModel::create, viewModel::dismissCreation)
        is WorkspaceCreation.Created -> LaunchedEffect(creation) {
            viewModel.dismissCreation()
            onWorkspaceCreated(creation.branch)
        }
        WorkspaceCreation.Idle, WorkspaceCreation.Loading -> Unit
    }
}

@Composable
private fun WorkspaceRowItem(row: WorkspaceRow, phaseNames: PhaseNames, onClick: () -> Unit) {
    ListRowLayout(onClick) {
        Text(row.summary.branch, style = MonoText.body)
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag("Phase ${row.summary.phase}")
            WorkspaceStatus(row, phaseNames)
        }
    }
}

@Composable
private fun WorkspaceStatus(row: WorkspaceRow, phaseNames: PhaseNames) {
    val gate = row.gate
    when {
        gate != null -> {
            val name = phaseNames.nameFor(gate.phase) ?: "Gate ${gate.phase}"
            Pill(listOfNotNull(name, gate.since?.let(::waitLabel)).joinToString(" · "), PillTone.Gate, hasDot = true)
        }
        row.summary.isArchived -> Pill("Archived", PillTone.Neutral)
        row.summary.phase == NOT_STARTED_PHASE -> Pill("Not started", PillTone.Neutral)
        row.summary.phase.startsWith(DONE_PHASE) -> Pill("Done", PillTone.Ok)
        else -> Pill("Agent working", PillTone.Info)
    }
}

private const val NOT_STARTED_PHASE = "0"
private const val DONE_PHASE = "6"
private const val ARCHIVED_ALPHA = 0.6f
private val FAB_CLEARANCE = 96.dp

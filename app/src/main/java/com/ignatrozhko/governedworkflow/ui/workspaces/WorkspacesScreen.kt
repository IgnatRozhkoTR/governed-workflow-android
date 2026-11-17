package com.ignatrozhko.governedworkflow.ui.workspaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.ui.common.LoadStateContent
import com.ignatrozhko.governedworkflow.ui.common.PanelTopBar
import com.ignatrozhko.governedworkflow.ui.common.PhaseChip
import com.ignatrozhko.governedworkflow.ui.common.GateBadge

@Composable
fun WorkspacesScreen(
    title: String,
    onOpenWorkspace: (String) -> Unit,
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
                text = { Text("New") },
            )
        },
    ) { padding ->
        LoadStateContent(
            state = state,
            isRefreshing = false,
            onRefresh = viewModel::reload,
            onManageServers = onManageServers,
            modifier = Modifier.padding(padding),
        ) { rows ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (rows.isEmpty()) item { Text("This project has no workspaces yet.") }
                items(rows, key = { it.summary.id }) { row -> WorkspaceCard(row) { onOpenWorkspace(row.summary.branch) } }
            }
        }
    }
}

@Composable
private fun CreationFlow(creation: WorkspaceCreation, viewModel: WorkspacesViewModel, onWorkspaceCreated: (String) -> Unit) {
    when (creation) {
        is WorkspaceCreation.Editing -> CreateWorkspaceDialog(creation, viewModel::create, viewModel::dismissCreation)
        is WorkspaceCreation.Created -> LaunchedEffect(creation) {
            viewModel.dismissCreation()
            onWorkspaceCreated(creation.branch)
        }
        WorkspaceCreation.Idle, WorkspaceCreation.Loading -> Unit
    }
}

@Composable
private fun WorkspaceCard(row: WorkspaceRow, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().alpha(if (row.summary.isArchived) ARCHIVED_ALPHA else 1f).clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.summary.branch, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (row.summary.isArchived) {
                    Text("archived", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PhaseChip(row.summary.phase)
                if (row.gatePhase != null) GateBadge()
            }
        }
    }
}

private const val ARCHIVED_ALPHA = 0.5f

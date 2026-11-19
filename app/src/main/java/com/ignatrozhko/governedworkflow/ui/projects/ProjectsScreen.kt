package com.ignatrozhko.governedworkflow.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.data.api.Project
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.gates.WaitingGate
import com.ignatrozhko.governedworkflow.ui.common.EmptyState
import com.ignatrozhko.governedworkflow.ui.common.FieldLabel
import com.ignatrozhko.governedworkflow.ui.common.GateIcon
import com.ignatrozhko.governedworkflow.ui.common.ListRow
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.LoadStateContent
import com.ignatrozhko.governedworkflow.ui.common.PanelTopBar
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.RowGroup
import com.ignatrozhko.governedworkflow.ui.common.StatusDot
import com.ignatrozhko.governedworkflow.ui.common.waitLabel
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

@Composable
fun ProjectsScreen(
    onOpenProject: (Project) -> Unit,
    onOpenGate: (WorkspaceRef) -> Unit,
    onManageServers: () -> Unit,
    viewModel: ProjectsViewModel = viewModel(factory = ProjectsViewModel.Factory),
) {
    val state by viewModel.projects.collectAsStateWithLifecycle()
    val gatesState by viewModel.waitingGates.collectAsStateWithLifecycle()
    val gates = (gatesState as? LoadState.Loaded)?.data
    Scaffold(
        topBar = { PanelTopBar(title = "Projects", onBack = null, onManageServers = onManageServers, actions = { GateWatchToggle() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LoadStateContent(
            state = state,
            isRefreshing = false,
            onRefresh = viewModel::reload,
            onManageServers = onManageServers,
            modifier = Modifier.padding(padding),
        ) { projects ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (gates != null && gates.gates.isNotEmpty()) {
                    item { GateSummary(gates.gates.size) }
                    item { RowGroup(gates.gates) { gate -> WaitingGateRow(gate, gates) { onOpenGate(gate.workspace) } } }
                }
                item { FieldLabel("All projects", Modifier.padding(start = 4.dp, top = 8.dp)) }
                item { ProjectList(projects, gates, onOpenProject) }
            }
        }
    }
}

@Composable
private fun ProjectList(projects: List<Project>, gates: WaitingGates?, onOpenProject: (Project) -> Unit) {
    if (projects.isEmpty()) {
        EmptyState("No projects yet", message = "Register a project in the panel and it appears here.")
        return
    }
    RowGroup(projects) { project ->
        ListRow(project.name, onClick = { onOpenProject(project) }, subtitle = project.path) {
            val gateCount = gates?.countFor(project.id) ?: 0
            if (gateCount > 0) Pill(gateCount.toString(), PillTone.Gate, hasDot = true)
        }
    }
}

@Composable
private fun GateSummary(count: Int) {
    Row(
        Modifier.fillMaxWidth().background(Checkpoint.colors.gateSoft, MaterialTheme.shapes.extraLarge).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GateIcon()
        val text = if (count == 1) "1 gate is waiting on you" else "$count gates are waiting on you"
        Text(text, style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onGateSoft)
    }
}

@Composable
private fun WaitingGateRow(gate: WaitingGate, gates: WaitingGates, onClick: () -> Unit) {
    val phase = gates.phaseNames.nameFor(gate.phase) ?: "Gate ${gate.phase}"
    ListRow(
        title = gate.label,
        onClick = onClick,
        titleStyle = if (gate.label == gate.workspace.branch) MonoText.body else MaterialTheme.typography.bodyLarge,
        subtitle = listOfNotNull(phase, gate.since?.let(::waitLabel)).joinToString(" · "),
        subtitleColor = Checkpoint.colors.onGateSoft,
        leading = { StatusDot(Checkpoint.colors.gate) },
    )
}

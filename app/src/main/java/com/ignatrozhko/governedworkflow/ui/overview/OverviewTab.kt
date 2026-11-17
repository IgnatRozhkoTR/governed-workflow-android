package com.ignatrozhko.governedworkflow.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.data.api.PendingGate
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.GateBadge
import com.ignatrozhko.governedworkflow.ui.common.PhaseChip

@Composable
fun OverviewTab(viewModel: OverviewViewModel, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    val state = data.state
    if (state == null) {
        if (data.loadError != null) Text(data.loadError.orEmpty(), modifier = modifier.padding(24.dp)) else FullScreenProgress()
        return
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusCard(state, data.context?.let { "${it.ticketId} ${it.ticketName}".trim() }.orEmpty(), data.loadError)
        state.pendingGate?.let { gate -> GateCard(gate, isBusy, viewModel::approve, viewModel::reject) }
        YoloCard(state.yoloMode, isBusy, viewModel::setYolo)
        NotifyCard(isBusy, viewModel::notifyClaude)
    }
}

@Composable
private fun StatusCard(state: WorkspaceState, ticket: String, loadError: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PhaseChip(state.phase)
                if (state.pendingGate != null) GateBadge()
            }
            Text(state.branch, style = MaterialTheme.typography.titleMedium)
            if (ticket.isNotEmpty()) Text(ticket, style = MaterialTheme.typography.bodyMedium)
            LabeledValue("Status", state.status)
            state.sourceBranch?.let { LabeledValue("Source branch", it) }
            if (state.workflowMode.isNotEmpty()) LabeledValue("Workflow", state.workflowMode)
            state.planStatus?.let { LabeledValue("Plan", it) }
            loadError?.let { Text("Refresh failed: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GateCard(gate: PendingGate, isBusy: Boolean, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    var dialog by remember { mutableStateOf<GateDecision?>(null) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Phase ${gate.phase} is waiting for your approval", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (gate.acceptsCommitMessage) dialog = GateDecision.Approve else onApprove("")
                }, enabled = !isBusy) { Text("Approve") }
                OutlinedButton(onClick = { dialog = GateDecision.Reject }, enabled = !isBusy) { Text("Reject") }
            }
        }
    }
    dialog?.let { decision ->
        GateDialog(decision, onDismiss = { dialog = null }) { text ->
            dialog = null
            if (decision == GateDecision.Approve) onApprove(text) else onReject(text)
        }
    }
}

private enum class GateDecision(val title: String, val fieldLabel: String, val isTextRequired: Boolean) {
    Approve("Approve and commit", "Commit message (optional)", isTextRequired = false),
    Reject("Reject", "What should change?", isTextRequired = true),
}

@Composable
private fun GateDialog(decision: GateDecision, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(decision.title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(decision.fieldLabel) }, minLines = 3)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = !decision.isTextRequired || text.isNotBlank()) {
                Text(decision.title)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun YoloCard(isEnabled: Boolean, isBusy: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YOLO mode", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Auto-approve user gates without asking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isEnabled, onCheckedChange = onToggle, enabled = !isBusy)
        }
    }
}

@Composable
private fun NotifyCard(isBusy: Boolean, onNotify: (NotifyMessage) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Notify Claude", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NotifyMessage.entries.forEach { message ->
                    OutlinedButton(onClick = { onNotify(message) }, enabled = !isBusy) { Text(message.label) }
                }
            }
        }
    }
}

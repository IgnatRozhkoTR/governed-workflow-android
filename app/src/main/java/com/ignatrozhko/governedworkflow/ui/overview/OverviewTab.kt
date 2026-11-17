package com.ignatrozhko.governedworkflow.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.ignatrozhko.governedworkflow.data.api.PendingGate
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.WorkspaceContext
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.GateBadge
import com.ignatrozhko.governedworkflow.ui.common.PhaseChip
import com.ignatrozhko.governedworkflow.ui.workspace.NotifyMessage
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceSection
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.TextInputDialog

@Composable
fun OverviewTab(viewModel: WorkspaceViewModel, onOpenSection: (WorkspaceSection) -> Unit, modifier: Modifier = Modifier) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val context by viewModel.context.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val phaseNames by viewModel.phaseNames.collectAsStateWithLifecycle()
    val state = snapshot.state
    if (state == null) {
        if (snapshot.error != null) Text(snapshot.error.orEmpty(), modifier = modifier.padding(24.dp)) else FullScreenProgress()
        return
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TicketCard(state, context, phaseNames, snapshot.error)
        state.pendingGate?.let { gate -> GateCard(gate, isBusy, viewModel::approveGate, viewModel::rejectGate) }
        SectionsCard(state, onOpenSection)
        YoloCard(state.yoloMode, isBusy, viewModel::setYolo)
        NotifyCard(isBusy, viewModel::notifyClaude)
        ArchiveButton(isBusy, viewModel::archive)
    }
}

/** The top bar already names the branch, so the card only adds the ticket when the workspace has one. */
@Composable
private fun TicketCard(state: WorkspaceState, context: WorkspaceContext?, phaseNames: PhaseNames, loadError: String?) {
    val ticket = listOfNotNull(context?.ticketId?.ifBlank { null }, context?.ticketName?.ifBlank { null }).joinToString(" · ")
    var isDescriptionExpanded by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PhaseChip(state.phase)
                phaseNames.nameFor(state.phase)?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
                if (state.pendingGate != null) GateBadge()
            }
            if (ticket.isNotEmpty()) Text(ticket, style = MaterialTheme.typography.titleMedium)
            SecondaryText(workspaceFacts(state))
            context?.description?.takeIf { it.isNotBlank() }?.let { description ->
                if (isDescriptionExpanded) {
                    MarkdownText(description)
                } else {
                    Text(description, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { isDescriptionExpanded = !isDescriptionExpanded }) {
                    Text(if (isDescriptionExpanded) "Show less" else "Show description")
                }
            }
            loadError?.let { Text("Refresh failed: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun workspaceFacts(state: WorkspaceState): String = listOfNotNull(
    state.status.ifBlank { null },
    state.sourceBranch?.let { "from $it" },
    state.workflowMode.ifBlank { null },
    state.planStatus?.let { "plan $it" },
).joinToString(" · ")

@Composable
private fun SectionsCard(state: WorkspaceState, onOpenSection: (WorkspaceSection) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        WorkspaceSection.entries.forEachIndexed { index, section ->
            if (index > 0) HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().clickable { onOpenSection(section) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(section.title, style = MaterialTheme.typography.bodyLarge)
                    section.summary(state)?.let { SecondaryText(it) }
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
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
        TextInputDialog(
            title = decision.title,
            label = decision.fieldLabel,
            confirmLabel = decision.title,
            isTextRequired = decision.isTextRequired,
            onDismiss = { dialog = null },
        ) { text ->
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
private fun YoloCard(isEnabled: Boolean, isBusy: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YOLO mode", style = MaterialTheme.typography.titleSmall)
                SecondaryText("Auto-approve user gates without asking")
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

@Composable
private fun ArchiveButton(isBusy: Boolean, onArchive: () -> Unit) {
    var isConfirming by remember { mutableStateOf(false) }
    TextButton(onClick = { isConfirming = true }, enabled = !isBusy) {
        Text("Archive workspace", color = MaterialTheme.colorScheme.error)
    }
    if (isConfirming) {
        ConfirmDialog(
            title = "Archive workspace?",
            message = "The workspace leaves the active list. Its branch and history stay in git.",
            confirmLabel = "Archive",
            onDismiss = { isConfirming = false },
        ) {
            isConfirming = false
            onArchive()
        }
    }
}

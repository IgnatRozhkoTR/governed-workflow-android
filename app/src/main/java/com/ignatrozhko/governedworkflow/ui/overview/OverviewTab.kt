package com.ignatrozhko.governedworkflow.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.data.api.PendingGate
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.WorkspaceContext
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.currentPhaseSince
import com.ignatrozhko.governedworkflow.ui.common.ChoiceChip
import com.ignatrozhko.governedworkflow.ui.common.CountBadge
import com.ignatrozhko.governedworkflow.ui.common.DangerButton
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.GateCard
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.PanelCard
import com.ignatrozhko.governedworkflow.ui.common.PhaseStepper
import com.ignatrozhko.governedworkflow.ui.common.Tag
import com.ignatrozhko.governedworkflow.ui.common.ToggleSwitch
import com.ignatrozhko.governedworkflow.ui.common.waitLabel
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.workspace.NotifyMessage
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceSection
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText

/** The deciding screen: the waiting gate first, then where the run is, then the way into every section. */
@Composable
fun OverviewTab(
    viewModel: WorkspaceViewModel,
    onOpenSection: (WorkspaceSection) -> Unit,
    onOpenChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val context by viewModel.context.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val phaseNames by viewModel.phaseNames.collectAsStateWithLifecycle()
    val state = snapshot.state
    if (state == null) {
        if (snapshot.error != null) Text(snapshot.error.orEmpty(), modifier = modifier.padding(24.dp)) else FullScreenProgress()
        return
    }
    val openEvidence: (GateEvidence) -> Unit = { evidence ->
        when (evidence) {
            GateEvidence.Changes -> onOpenChanges()
            GateEvidence.Plan -> onOpenSection(WorkspaceSection.Plan)
            GateEvidence.Research -> onOpenSection(WorkspaceSection.Research)
        }
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.pendingGate?.let { gate -> PendingGateCard(gate, state, context, openEvidence) }
        PanelCard { PhaseStepper(workflowSteps(state, phaseNames), Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) }
        SectionChips(state, onOpenSection)
        DetailsCard(state, context, phaseNames, snapshot.error)
        YoloCard(state.yoloMode, isBusy, viewModel::setYolo)
        NotifyCard(isBusy, viewModel::notifyClaude)
        ArchiveButton(isBusy, viewModel::archive)
    }
}

@Composable
private fun PendingGateCard(gate: PendingGate, state: WorkspaceState, context: WorkspaceContext?, onReview: (GateEvidence) -> Unit) {
    val prompt = gatePrompt(gate)
    val holding = state.currentPhaseSince()?.let { " · holding ${waitLabel(it)}" }.orEmpty()
    GateCard(caption = "Gate ${gate.phase}$holding", question = prompt.question) {
        context?.ticketName?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = Checkpoint.colors.onGateSoft)
        }
        Row(
            Modifier.clickable { onReview(prompt.evidence) }.padding(top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(prompt.evidence.linkLabel, style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onGateSoft)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.size(20.dp), tint = Checkpoint.colors.onGateSoft)
        }
    }
}

/** Every section one tap away; sections with something open carry the count. */
@Composable
private fun SectionChips(state: WorkspaceState, onOpenSection: (WorkspaceSection) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WorkspaceSection.entries.forEach { section ->
            val count = section.openCount(state)
            ChoiceChip(
                section.chipLabel,
                isSelected = false,
                onClick = { onOpenSection(section) },
                trailing = count?.let { { CountBadge(it) } },
            )
        }
    }
}

/** Ticket, description and the workspace facts the app bar has no room for. */
@Composable
private fun DetailsCard(state: WorkspaceState, context: WorkspaceContext?, phaseNames: PhaseNames, loadError: String?) {
    val ticket = listOfNotNull(context?.ticketId?.ifBlank { null }, context?.ticketName?.ifBlank { null }).joinToString(" · ")
    var isDescriptionExpanded by rememberSaveable { mutableStateOf(false) }
    PanelCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Tag("Phase ${state.phase}")
                phaseNames.nameFor(state.phase)?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
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
private fun YoloCard(isEnabled: Boolean, isBusy: Boolean, onToggle: (Boolean) -> Unit) {
    PanelCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YOLO mode", style = MaterialTheme.typography.titleSmall)
                SecondaryText("Auto-approve user gates without asking")
            }
            ToggleSwitch(isEnabled, onToggle, isEnabled = !isBusy)
        }
    }
}

@Composable
private fun NotifyCard(isBusy: Boolean, onNotify: (NotifyMessage) -> Unit) {
    PanelCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Notify Claude", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NotifyMessage.entries.forEach { message ->
                    OutlinedActionButton(message.label, { onNotify(message) }, isEnabled = !isBusy, height = 36.dp)
                }
            }
        }
    }
}

@Composable
private fun ArchiveButton(isBusy: Boolean, onArchive: () -> Unit) {
    var isConfirming by remember { mutableStateOf(false) }
    DangerButton("Archive workspace", { isConfirming = true }, isEnabled = !isBusy)
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

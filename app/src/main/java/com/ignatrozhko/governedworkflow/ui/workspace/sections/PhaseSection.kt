package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.PhaseTransition
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.occurredAt
import com.ignatrozhko.governedworkflow.ui.common.ActionFooter
import com.ignatrozhko.governedworkflow.ui.common.PanelCard
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.common.waitLabel
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing

/** Manual phase override, as in the web panel's phase selector; history is listed below, newest first. */
@Composable
fun PhaseSection(
    state: WorkspaceState,
    phaseNames: PhaseNames,
    isBusy: Boolean,
    onSetPhase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chosenPhase by remember(state.phase) { mutableStateOf(state.phase) }
    var isConfirming by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
            item { SkipWarning() }
            item { PhaseList(state.availablePhases, chosenPhase, phaseNames, isBusy) { chosenPhase = it } }
            item { HistoryCard(state.phaseHistory.asReversed()) }
        }
        ActionFooter {
            Text("Current: ${state.phase}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton("Set phase", { isConfirming = true }, isEnabled = !isBusy && chosenPhase != state.phase)
        }
    }
    if (isConfirming) {
        ConfirmDialog(
            title = "Move to ${phaseNames.label(chosenPhase)}?",
            message = "The workspace jumps from ${state.phase} straight to $chosenPhase.",
            confirmLabel = "Move",
            onDismiss = { isConfirming = false },
        ) {
            isConfirming = false
            onSetPhase(chosenPhase)
        }
    }
}

@Composable
private fun SkipWarning() {
    val extra = Checkpoint.colors
    Row(
        Modifier.fillMaxWidth().background(extra.warnSoft, MaterialTheme.shapes.large).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_flag), contentDescription = null, Modifier.size(18.dp), tint = extra.onWarnSoft)
        Text("Jumping phases skips the workflow's checks. Use it to recover a stuck workspace.", style = MaterialTheme.typography.bodyMedium, color = extra.onWarnSoft)
    }
}

@Composable
private fun PhaseList(phases: List<String>, chosenPhase: String, phaseNames: PhaseNames, isBusy: Boolean, onChoose: (String) -> Unit) {
    PanelCard {
        phases.forEachIndexed { index, phase ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            PhaseRow(phase, phaseNames, isChosen = phase == chosenPhase, isEnabled = !isBusy) { onChoose(phase) }
        }
    }
}

@Composable
private fun PhaseRow(phase: String, phaseNames: PhaseNames, isChosen: Boolean, isEnabled: Boolean, onChoose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(isChosen, enabled = isEnabled, role = Role.RadioButton, onClick = onChoose).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChoiceMark(isChosen)
        Text(phase, Modifier.width(52.dp), style = MonoText.body)
        Text(phaseNames.nameFor(phase).orEmpty(), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (phaseNames.isUserGate(phase)) Pill("gate", PillTone.Gate)
    }
}

@Composable
private fun ChoiceMark(isChosen: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(24.dp).then(if (isChosen) Modifier.background(scheme.primary, CircleShape) else Modifier.border(1.5.dp, scheme.outline, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (isChosen) Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp), tint = scheme.onPrimary)
    }
}

@Composable
private fun HistoryCard(history: List<PhaseTransition>) {
    CollapsibleCard(title = "History", memoryKey = "phase:history", subtitle = "${history.size} changes", initiallyExpanded = true) {
        history.forEach { transition ->
            val ago = transition.occurredAt()?.let { "${waitLabel(it)} ago" } ?: transition.time
            Text("${transition.from} to ${transition.to} · $ago", style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

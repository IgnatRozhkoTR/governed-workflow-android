package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding

/** Manual phase override, as in the web panel's phase selector; history is listed below, newest first. */
@Composable
fun PhaseSection(
    state: WorkspaceState,
    phaseNames: PhaseNames,
    isBusy: Boolean,
    onSetPhase: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingPhase by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier, contentPadding = SectionContentPadding) {
        item { SecondaryText("Jumping phases skips the workflow's checks. Use it to recover a stuck workspace.") }
        items(state.availablePhases, key = { it }) { phase ->
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !isBusy && phase != state.phase) { pendingPhase = phase },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = phase == state.phase, onClick = null, modifier = Modifier.padding(12.dp))
                Text(phaseNames.label(phase), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (phaseNames.isUserGate(phase)) SecondaryText("gate")
            }
        }
        item { Text("History", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp)) }
        items(state.phaseHistory.asReversed()) { transition ->
            SecondaryText("${transition.from} → ${transition.to}   ${transition.time.take(TIMESTAMP_MINUTES)}")
        }
    }
    pendingPhase?.let { phase ->
        ConfirmDialog(
            title = "Move to ${phaseNames.label(phase)}?",
            message = "The workspace jumps from ${state.phase} straight to $phase.",
            confirmLabel = "Move",
            onDismiss = { pendingPhase = null },
        ) {
            pendingPhase = null
            onSetPhase(phase)
        }
    }
}

private const val TIMESTAMP_MINUTES = 16

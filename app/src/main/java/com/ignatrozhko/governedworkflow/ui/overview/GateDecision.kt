package com.ignatrozhko.governedworkflow.ui.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.ignatrozhko.governedworkflow.data.api.PendingGate
import com.ignatrozhko.governedworkflow.ui.common.ActionFooter
import com.ignatrozhko.governedworkflow.ui.common.LabeledField
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.PanelSheet
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton

private enum class GateSheet { Approve, Reject }

/**
 * Reject and Approve pinned above the tabs while a gate waits. Rejecting always asks what should change; approving
 * asks for an optional commit message only at commit gates and is otherwise immediate.
 */
@Composable
fun GateDecisionFooter(gate: PendingGate, isBusy: Boolean, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    var sheet by remember(gate.phase) { mutableStateOf<GateSheet?>(null) }
    ActionFooter {
        OutlinedActionButton("Reject", { sheet = GateSheet.Reject }, isEnabled = !isBusy)
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            "Approve",
            onClick = { if (gate.acceptsCommitMessage) sheet = GateSheet.Approve else onApprove("") },
            modifier = Modifier.weight(1.4f),
            isEnabled = !isBusy,
            icon = rememberVectorPainter(Icons.Filled.Check),
        )
    }
    when (sheet) {
        GateSheet.Approve -> ApproveCommitSheet(gate, onDismiss = { sheet = null }) {
            sheet = null
            onApprove(it)
        }
        GateSheet.Reject -> RejectSheet(gate, onDismiss = { sheet = null }) {
            sheet = null
            onReject(it)
        }
        null -> Unit
    }
}

@Composable
private fun ApproveCommitSheet(gate: PendingGate, onDismiss: () -> Unit, onApprove: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    PanelSheet("Approve the commit at gate ${gate.phase}", onDismiss) {
        LabeledField("Commit message (optional)", message, { message = it }, isMono = true, isSingleLine = false, minLines = 3)
        SheetActions(onDismiss, "Approve and commit", rememberVectorPainter(Icons.Filled.Check), isConfirmEnabled = true) { onApprove(message.trim()) }
    }
}

@Composable
private fun RejectSheet(gate: PendingGate, onDismiss: () -> Unit, onReject: (String) -> Unit) {
    var feedback by remember { mutableStateOf("") }
    PanelSheet("Request changes at gate ${gate.phase}", onDismiss) {
        LabeledField("Feedback for the agent", feedback, { feedback = it }, isSingleLine = false, minLines = 4)
        Text("The agent gets this note and reworks the phase.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SheetActions(onDismiss, "Send and reject", rememberVectorPainter(Icons.AutoMirrored.Filled.Send), isConfirmEnabled = feedback.isNotBlank()) {
            onReject(feedback.trim())
        }
    }
}

@Composable
private fun SheetActions(
    onDismiss: () -> Unit,
    confirmLabel: String,
    confirmIcon: Painter,
    isConfirmEnabled: Boolean,
    onConfirm: () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        OutlinedActionButton("Cancel", onDismiss)
        Spacer(Modifier.weight(1f))
        PrimaryButton(confirmLabel, onConfirm, isEnabled = isConfirmEnabled, icon = confirmIcon)
    }
}

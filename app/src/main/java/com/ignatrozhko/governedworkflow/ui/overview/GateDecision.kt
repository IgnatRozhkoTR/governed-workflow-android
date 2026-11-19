package com.ignatrozhko.governedworkflow.ui.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.ignatrozhko.governedworkflow.data.api.PendingGate
import com.ignatrozhko.governedworkflow.ui.common.ActionFooter
import com.ignatrozhko.governedworkflow.ui.common.DECISION_BUTTON_MIN_WIDTH
import com.ignatrozhko.governedworkflow.ui.common.LabeledField
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.PanelSheet
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton

private enum class GateSheet { Approve, Reject }

/**
 * Reject and Approve pinned above the tabs while a gate waits. Rejecting always asks what should change; approving
 * asks for an optional commit message only at commit gates and is otherwise immediate. The bar hides behind the
 * keyboard while its sheets stay open, since the sheet's own text field is what raises the keyboard.
 */
@Composable
fun GateDecisionFooter(
    gate: PendingGate,
    isBusy: Boolean,
    isAtScreenBottom: Boolean,
    isBarShown: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    var sheet by remember(gate.phase) { mutableStateOf<GateSheet?>(null) }
    var submitted by remember(gate.phase) { mutableStateOf<GateSheet?>(null) }
    val pending = submitted.takeIf { isBusy }
    LaunchedEffect(isBusy) {
        if (!isBusy && submitted != null) {
            submitted = null
            sheet = null
        }
    }
    val approve: (String) -> Unit = {
        submitted = GateSheet.Approve
        onApprove(it)
    }
    val reject: (String) -> Unit = {
        submitted = GateSheet.Reject
        onReject(it)
    }
    if (isBarShown) {
        ActionFooter(isAtScreenBottom = isAtScreenBottom) {
            OutlinedActionButton("Reject", { sheet = GateSheet.Reject }, isEnabled = !isBusy, isLoading = pending == GateSheet.Reject)
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                "Approve",
                onClick = { if (gate.acceptsCommitMessage) sheet = GateSheet.Approve else approve("") },
                modifier = Modifier.widthIn(min = DECISION_BUTTON_MIN_WIDTH),
                isEnabled = !isBusy,
                icon = rememberVectorPainter(Icons.Filled.Check),
                isLoading = pending == GateSheet.Approve,
            )
        }
    }
    when (sheet) {
        GateSheet.Approve -> ApproveCommitSheet(gate, isBusy, pending == GateSheet.Approve, onDismiss = { sheet = null }, onApprove = approve)
        GateSheet.Reject -> RejectSheet(gate, isBusy, pending == GateSheet.Reject, onDismiss = { sheet = null }, onReject = reject)
        null -> Unit
    }
}

@Composable
private fun ApproveCommitSheet(gate: PendingGate, isBusy: Boolean, isSubmitting: Boolean, onDismiss: () -> Unit, onApprove: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    PanelSheet("Approve the commit at gate ${gate.phase}", onDismiss) {
        LabeledField("Commit message (optional)", message, { message = it }, isMono = true, isSingleLine = false, minLines = 3, isEnabled = !isBusy)
        SheetActions(onDismiss, "Approve and commit", rememberVectorPainter(Icons.Filled.Check), isConfirmEnabled = true, isBusy, isSubmitting) { onApprove(message.trim()) }
    }
}

@Composable
private fun RejectSheet(gate: PendingGate, isBusy: Boolean, isSubmitting: Boolean, onDismiss: () -> Unit, onReject: (String) -> Unit) {
    var feedback by remember { mutableStateOf("") }
    PanelSheet("Request changes at gate ${gate.phase}", onDismiss) {
        LabeledField("Feedback for the agent", feedback, { feedback = it }, isSingleLine = false, minLines = 4, isEnabled = !isBusy)
        Text("The agent gets this note and reworks the phase.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SheetActions(onDismiss, "Send and reject", rememberVectorPainter(Icons.AutoMirrored.Filled.Send), isConfirmEnabled = feedback.isNotBlank(), isBusy, isSubmitting) {
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
    isBusy: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        OutlinedActionButton("Cancel", onDismiss, isEnabled = !isBusy)
        Spacer(Modifier.weight(1f))
        PrimaryButton(confirmLabel, onConfirm, isEnabled = isConfirmEnabled && !isBusy, icon = confirmIcon, isLoading = isSubmitting)
    }
}

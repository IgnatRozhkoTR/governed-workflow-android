package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ignatrozhko.governedworkflow.data.api.ReviewResolution

/** Centred dialog on the outlined surface from the design's components sheet: title, [content], then right-aligned [actions]. */
@Composable
fun PanelDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.padding(horizontal = 24.dp).widthIn(max = DIALOG_MAX_WIDTH).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                content()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) { actions() }
            }
        }
    }
}

/** [isDestructive] swaps the confirm button for the red outlined one, as on the archive dialog in the design. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
) {
    PanelDialog(
        title,
        onDismiss,
        actions = {
            OutlinedActionButton("Cancel", onDismiss)
            if (isDestructive) DangerButton(confirmLabel, onConfirm) else PrimaryButton(confirmLabel, onConfirm)
        },
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ResolutionDialog(title: String, onDismiss: () -> Unit, onChoose: (ReviewResolution) -> Unit) {
    PanelDialog(title, onDismiss, actions = { OutlinedActionButton("Cancel", onDismiss) }) {
        ReviewResolution.entries.forEach { resolution ->
            OutlinedActionButton(resolution.label, { onChoose(resolution) }, Modifier.fillMaxWidth())
        }
    }
}

private val DIALOG_MAX_WIDTH = 420.dp

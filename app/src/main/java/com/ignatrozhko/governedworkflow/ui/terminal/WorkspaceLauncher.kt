package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.ui.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.common.CenteredText
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress

/** Shown instead of the emulator while the workspace has no Claude tmux session to attach to. */
@Composable
fun WorkspaceLauncher(status: WorkspaceSessionStatus, isBusy: Boolean, onResume: () -> Unit, onStartFresh: () -> Unit) {
    if (status != WorkspaceSessionStatus.Missing) {
        FullScreenProgress()
        return
    }
    var isConfirmOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CenteredText("No Claude session is running for this workspace.")
        Button(onClick = onResume, enabled = !isBusy) { Text("Resume session") }
        OutlinedButton(onClick = { isConfirmOpen = true }, enabled = !isBusy) { Text("Start fresh") }
    }
    if (isConfirmOpen) {
        ConfirmDialog(
            title = "Start a fresh Claude session?",
            message = "Claude starts a new conversation instead of resuming the previous one.",
            confirmLabel = "Start fresh",
            onDismiss = { isConfirmOpen = false },
        ) {
            isConfirmOpen = false
            onStartFresh()
        }
    }
}

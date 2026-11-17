package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.terminal.core.TerminalConnectionState
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSession
import com.ignatrozhko.governedworkflow.terminal.view.TerminalHostEvents
import com.ignatrozhko.governedworkflow.terminal.view.TerminalHostView
import com.ignatrozhko.governedworkflow.ui.common.writeClipboardText

/** One emulator view bound to [session]; recreated per session so each keeps its own screen and scrollback. */
@Composable
fun TerminalPane(
    session: TerminalSession,
    input: TerminalInput,
    initialFontSize: Int,
    onFontSizeChanged: (Int) -> Unit,
    onHostView: (TerminalHostView?) -> Unit,
) {
    val context = LocalContext.current
    val connection by session.state.collectAsStateWithLifecycle()
    var isSelectionActive by remember { mutableStateOf(false) }
    val currentOnFontSizeChanged by rememberUpdatedState(onFontSizeChanged)
    val hostView = remember(session) {
        TerminalHostView(context, session, input, initialFontSize, object : TerminalHostEvents {
            override fun onSelectionChanged(hasSelection: Boolean) {
                isSelectionActive = hasSelection
            }

            override fun onFontSizeChanged(size: Int) = currentOnFontSizeChanged(size)

            override fun onClipboardCopy(text: String) = writeClipboardText(context, CLIP_LABEL, text)
        })
    }
    DisposableEffect(hostView) {
        onHostView(hostView)
        onDispose {
            onHostView(null)
            hostView.release()
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { hostView }, modifier = Modifier.fillMaxSize())
        ConnectionChip(connection, onRetry = session::reconnectNow, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        if (isSelectionActive) {
            SelectionBar(
                onCopy = { hostView.takeSelection { text -> if (text.isNotEmpty()) writeClipboardText(context, CLIP_LABEL, text) } },
                onCancel = hostView::clearSelection,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}

@Composable
private fun SelectionBar(onCopy: () -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    Row(modifier) {
        FilledTonalButton(onClick = onCopy) { Text("Copy") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun ConnectionChip(connection: TerminalConnectionState, onRetry: () -> Unit, modifier: Modifier) {
    val label = when (connection) {
        TerminalConnectionState.Connected, TerminalConnectionState.Idle -> return
        is TerminalConnectionState.Connecting -> "Connecting…"
        is TerminalConnectionState.Retrying -> "Reconnecting in ${(connection.delayMs + 999) / 1000}s — tap to retry"
        is TerminalConnectionState.Rejected -> connection.message
        TerminalConnectionState.Paused -> "Paused in background — tap to reconnect"
        TerminalConnectionState.Closed -> "Closed"
    }
    AssistChip(onClick = onRetry, label = { Text(label) }, modifier = modifier)
}

private const val CLIP_LABEL = "Terminal"

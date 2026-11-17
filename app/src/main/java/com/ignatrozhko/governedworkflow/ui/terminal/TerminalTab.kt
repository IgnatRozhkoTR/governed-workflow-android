package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import com.ignatrozhko.governedworkflow.terminal.view.TerminalHostView
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.readClipboardText

@Composable
fun TerminalTab(
    viewModel: TerminalViewModel,
    snackbarHostState: SnackbarHostState,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val sticky by viewModel.stickyModifiers.collectAsStateWithLifecycle()
    var hostView by remember { mutableStateOf<TerminalHostView?>(null) }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    val selected = state.selected
    val session = viewModel.sessionFor(selected)
    val input = remember(session) { viewModel.inputFor(session) }
    val isTerminalShown = selected !is TerminalTarget.WorkspaceSession ||
        state.workspaceStatus == WorkspaceSessionStatus.Live

    Column(modifier.fillMaxSize()) {
        if (!isCompact) {
            SessionBar(
                state = state,
                onSelect = viewModel::select,
                onRefresh = viewModel::refreshSessions,
                onKill = viewModel::killSelected,
                canKill = isTerminalShown,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val initialFontSize = fontSize
            when {
                !isTerminalShown -> WorkspaceLauncher(
                    status = state.workspaceStatus,
                    isBusy = state.isBusy,
                    onResume = viewModel::resumeWorkspaceSession,
                    onStartFresh = viewModel::startFreshWorkspaceSession,
                )
                initialFontSize == null -> FullScreenProgress()
                else -> key(selected.key) {
                    TerminalPane(session, input, initialFontSize, viewModel::setFontSize, onHostView = { hostView = it })
                }
            }
        }
        if (isTerminalShown) {
            ExtraKeysBar(sticky, onAction = { action -> perform(action, input, hostView, viewModel) })
        }
    }
}

private fun perform(action: ExtraKeyAction, input: TerminalInput, hostView: TerminalHostView?, viewModel: TerminalViewModel) {
    hostView?.ensureFocus()
    when (action) {
        is ExtraKeyAction.Key -> input.pressKey(action.key, action.modifiers)
        is ExtraKeyAction.Text -> input.typeText(action.text)
        is ExtraKeyAction.Raw -> input.sendRaw(action.data)
        is ExtraKeyAction.Sticky -> viewModel.tapSticky(action.key)
        ExtraKeyAction.Paste -> hostView?.let { it.paste(readClipboardText(it.context)) }
        ExtraKeyAction.ToggleKeyboard -> hostView?.toggleKeyboard()
    }
}

package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import com.ignatrozhko.governedworkflow.terminal.view.TerminalHostView
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.readClipboardText
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint

/** How much chrome surrounds the terminal; the screen picks it from orientation, keyboard and the full-screen toggle. */
enum class TerminalChrome {
    /** Slim top row and two key rows. */
    Normal,

    /** No top row; a small handle restores the chrome. */
    FullScreen,

    /** Landscape with the keyboard up: no top row and a single key row, so a few terminal lines stay visible. */
    KeyboardLandscape,
}

@Composable
fun TerminalTab(
    viewModel: TerminalViewModel,
    snackbarHostState: SnackbarHostState,
    chrome: TerminalChrome,
    gatePhase: String?,
    onOpenGate: () -> Unit,
    onBack: () -> Unit,
    onManageServers: () -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
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
        if (chrome == TerminalChrome.Normal) {
            TerminalTopRow(
                state = state,
                canKill = isTerminalShown,
                hasGate = gatePhase != null,
                onBack = onBack,
                onSelect = viewModel::select,
                onRefresh = viewModel::refreshSessions,
                onKill = viewModel::killSelected,
                onEnterFullScreen = { onFullScreenChange(true) },
                onManageServers = onManageServers,
            )
        }
        Column(Modifier.weight(1f).fillMaxWidth().terminalFrame(chrome)) {
            if (gatePhase != null && chrome == TerminalChrome.Normal) TerminalGateStrip(gatePhase, onOpenGate)
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
                if (chrome == TerminalChrome.FullScreen) {
                    ExitFullScreenHandle(Modifier.align(Alignment.TopEnd)) { onFullScreenChange(false) }
                }
            }
        }
        if (isTerminalShown) {
            val rows = if (chrome == TerminalChrome.KeyboardLandscape) ExtraKeyLayout.oneRow else ExtraKeyLayout.twoRows
            ExtraKeysBar(rows, sticky, onAction = { action -> perform(action, input, hostView, viewModel) })
        }
    }
}

/** A rounded, outlined box in normal chrome; edge to edge when every row of the terminal counts. */
@Composable
private fun Modifier.terminalFrame(chrome: TerminalChrome): Modifier {
    if (chrome != TerminalChrome.Normal) return this
    val shape = MaterialTheme.shapes.large
    return padding(start = 12.dp, end = 12.dp, bottom = 8.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outline, shape)
}

/** Header of the terminal box: the agent in this session is held at a gate, and Open goes to the decision. */
@Composable
private fun TerminalGateStrip(phase: String, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Checkpoint.colors.gateSoft)
            .clickable(onClickLabel = "Open", onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Gate $phase needs you", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onGateSoft)
        Text("Open", style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onGateSoft)
    }
}

/** Translucent so it does not hide the terminal text underneath. */
@Composable
private fun ExitFullScreenHandle(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.padding(4.dp).size(32.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Icon(painterResource(R.drawable.ic_fullscreen_exit), contentDescription = "Exit full screen", modifier = Modifier.padding(6.dp))
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

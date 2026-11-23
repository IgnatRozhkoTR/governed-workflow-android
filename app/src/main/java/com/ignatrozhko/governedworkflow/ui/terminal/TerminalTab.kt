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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import com.ignatrozhko.governedworkflow.terminal.view.TerminalHostView
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.StatusDot
import com.ignatrozhko.governedworkflow.ui.common.readClipboardText
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

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
    val keysMode by viewModel.extraKeysMode.collectAsStateWithLifecycle()
    var hostView by remember { mutableStateOf<TerminalHostView?>(null) }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    val selected = state.selected
    val session = viewModel.sessionFor(selected)
    val input = remember(session) { viewModel.inputFor(session) }
    val isTerminalShown = selected !is TerminalTarget.WorkspaceSession ||
        state.workspaceStatus == WorkspaceSessionStatus.Live

    Column(modifier.fillMaxSize()) {
        if (chrome.isHeaderShown) {
            TerminalTopRow(
                state = state,
                canKill = isTerminalShown,
                hasGate = gatePhase != null,
                keysMode = keysMode,
                onKeysModeChange = viewModel::setExtraKeysMode,
                onBack = onBack,
                onSelect = viewModel::select,
                onRefresh = viewModel::refreshSessions,
                onKill = viewModel::killSelected,
                onEnterFullScreen = { onFullScreenChange(true) },
                onManageServers = onManageServers,
            )
        }
        Column(Modifier.weight(1f).fillMaxWidth().terminalFrame(chrome)) {
            if (gatePhase != null && chrome.isHeaderShown) TerminalGateStrip(gatePhase, onOpenGate)
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
                if (chrome.isFullScreen) {
                    ExitFullScreenButton(hasGate = gatePhase != null, Modifier.align(Alignment.TopEnd)) { onFullScreenChange(false) }
                }
                val mode = keysMode
                if (isTerminalShown && mode != null) {
                    ExtraKeysHandle(chrome.showsKeys(mode), Modifier.align(Alignment.BottomEnd)) {
                        viewModel.setExtraKeysMode(mode.toggled(chrome.isKeyboardOpen))
                    }
                }
            }
        }
        if (isTerminalShown && keysMode?.let(chrome::showsKeys) == true) {
            ExtraKeysBar(chrome.keyRows, sticky, onAction = { action -> perform(action, input, hostView, viewModel) })
        }
    }
}

/** A rounded, outlined box under the header; edge to edge when every row of the terminal counts. */
@Composable
private fun Modifier.terminalFrame(chrome: TerminalChrome): Modifier {
    if (!chrome.isHeaderShown) return this
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

/** Translucent so it does not hide the output; the orange dot keeps a waiting gate visible while the gate strip is gone. */
@Composable
private fun ExitFullScreenButton(hasGate: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.padding(4.dp)) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(TOUCH_TARGET),
            shape = CircleShape,
            color = translucentSurface(),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(painterResource(R.drawable.ic_fullscreen_exit), contentDescription = "Exit full screen", modifier = Modifier.padding(10.dp))
        }
        if (hasGate) StatusDot(Checkpoint.colors.gate, Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}

/** Sits on the terminal's bottom edge right above the keys, so it stays in the same place whether they are shown or not. */
@Composable
private fun ExtraKeysHandle(isExpanded: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(width = HANDLE_WIDTH, height = TOUCH_TARGET)
            .clickable(onClickLabel = if (isExpanded) "Hide keys" else "Show keys", role = Role.Button, onClick = onClick)
            .padding(end = 6.dp, bottom = 4.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(
            Modifier.background(translucentSurface(), CircleShape).padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("keys", style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun translucentSurface() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = OVERLAY_ALPHA)

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

private val TOUCH_TARGET = 44.dp
private val HANDLE_WIDTH = 72.dp
private const val OVERLAY_ALPHA = 0.7f

package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.settings.ExtraKeysMode
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import com.ignatrozhko.governedworkflow.ui.common.ChoiceChip
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.StatusDot
import com.ignatrozhko.governedworkflow.ui.common.connectionStatus
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.common.ConfirmDialog

/**
 * Title row with back, connection dot, full screen and menu, then the sessions as one scrolling chip row. [hasGate] marks the
 * workspace's own session with the gate dot.
 */
@Composable
fun TerminalTopRow(
    state: TerminalUiState,
    canKill: Boolean,
    hasGate: Boolean,
    keysMode: ExtraKeysMode?,
    onKeysModeChange: (ExtraKeysMode) -> Unit,
    onBack: () -> Unit,
    onSelect: (TerminalTarget) -> Unit,
    onRefresh: () -> Unit,
    onKill: () -> Unit,
    onEnterFullScreen: () -> Unit,
    onManageServers: () -> Unit,
) {
    val connectionManager = LocalAppContainer.current.connectionManager
    val connection by connectionManager.state.collectAsStateWithLifecycle()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isKillConfirmOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text("Terminal", Modifier.padding(start = 4.dp, end = 10.dp), style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
        StatusDot(connectionStatus(connection).first)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onEnterFullScreen) { Icon(painterResource(R.drawable.ic_fullscreen), contentDescription = "Full screen") }
        Box {
            IconButton(onClick = { isMenuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Terminal actions") }
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                MenuItem("Full screen") {
                    isMenuOpen = false
                    onEnterFullScreen()
                }
                if (keysMode != null) {
                    DropdownMenuItem(
                        text = { Text("Keys only with keyboard") },
                        trailingIcon = { if (keysMode == ExtraKeysMode.WithKeyboard) Icon(Icons.Filled.Check, contentDescription = "On") },
                        onClick = {
                            isMenuOpen = false
                            onKeysModeChange(keysMode.withKeyboardToggled())
                        },
                    )
                }
                MenuItem("Refresh sessions") {
                    isMenuOpen = false
                    onRefresh()
                }
                DropdownMenuItem(
                    text = { Text("Kill ${state.selected.title} session") },
                    enabled = canKill && !state.isBusy,
                    onClick = {
                        isMenuOpen = false
                        isKillConfirmOpen = true
                    },
                )
                HorizontalDivider()
                MenuItem("Re-probe server URLs") {
                    isMenuOpen = false
                    connectionManager.refresh()
                }
                MenuItem("Manage servers") {
                    isMenuOpen = false
                    onManageServers()
                }
            }
        }
    }
    SessionChips(state, hasGate, onSelect)
    if (isKillConfirmOpen) {
        ConfirmDialog(
            title = "Kill ${state.selected.title}?",
            message = "The tmux session and everything running in it end on the panel host.",
            confirmLabel = "Kill",
            onDismiss = { isKillConfirmOpen = false },
            isDestructive = true,
        ) {
            isKillConfirmOpen = false
            onKill()
        }
    }
}

@Composable
private fun SessionChips(state: TerminalUiState, hasGate: Boolean, onSelect: (TerminalTarget) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.targets, key = { it.key }) { target ->
            val isGateSession = hasGate && target is TerminalTarget.WorkspaceSession
            ChoiceChip(
                target.title,
                isSelected = target == state.selected,
                onClick = { onSelect(target) },
                leading = if (isGateSession) ({ StatusDot(Checkpoint.colors.gate) }) else null,
            )
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)
}

private val ROW_HEIGHT = 52.dp

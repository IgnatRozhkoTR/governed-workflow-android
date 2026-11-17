package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.StatusDot
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog

/** Everything the terminal needs above it in one slim row: back, connection dot, session chips and a menu. */
@Composable
fun TerminalTopRow(
    state: TerminalUiState,
    canKill: Boolean,
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
        StatusDot(connectionColor(connection))
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.targets, key = { it.key }) { target ->
                FilterChip(selected = target == state.selected, onClick = { onSelect(target) }, label = { Text(target.title) })
            }
        }
        Box {
            IconButton(onClick = { isMenuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Terminal actions") }
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                MenuItem("Full screen") {
                    isMenuOpen = false
                    onEnterFullScreen()
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
    if (isKillConfirmOpen) {
        ConfirmDialog(
            title = "Kill ${state.selected.title}?",
            message = "The tmux session and everything running in it end on the panel host.",
            confirmLabel = "Kill",
            onDismiss = { isKillConfirmOpen = false },
        ) {
            isKillConfirmOpen = false
            onKill()
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)
}

private fun connectionColor(connection: ConnectionState) = when (connection) {
    is ConnectionState.Connected -> PanelColors.Green
    is ConnectionState.Probing -> PanelColors.Amber
    ConnectionState.NoServer, is ConnectionState.TokenRejected, is ConnectionState.Unreachable -> PanelColors.Red
}

private val ROW_HEIGHT = 44.dp

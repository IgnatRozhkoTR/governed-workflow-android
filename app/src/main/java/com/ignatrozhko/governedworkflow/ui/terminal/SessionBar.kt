package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget

/** Chips for the workspace's Claude session and every other tmux session on the host, as in the web panel. */
@Composable
fun SessionBar(
    state: TerminalUiState,
    onSelect: (TerminalTarget) -> Unit,
    onRefresh: () -> Unit,
    onKill: () -> Unit,
    canKill: Boolean,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var isKillConfirmOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.targets, key = { it.key }) { target ->
                FilterChip(selected = target == state.selected, onClick = { onSelect(target) }, label = { Text(target.title) })
            }
        }
        IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh sessions") }
        Box {
            IconButton(onClick = { isMenuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Session actions") }
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Kill ${state.selected.title} session") },
                    enabled = canKill && !state.isBusy,
                    onClick = {
                        isMenuOpen = false
                        isKillConfirmOpen = true
                    },
                )
            }
        }
    }
    if (isKillConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isKillConfirmOpen = false },
            title = { Text("Kill ${state.selected.title}?") },
            text = { Text("The tmux session and everything running in it end on the panel host.") },
            confirmButton = {
                TextButton(onClick = {
                    isKillConfirmOpen = false
                    onKill()
                }) { Text("Kill") }
            },
            dismissButton = { TextButton(onClick = { isKillConfirmOpen = false }) { Text("Cancel") } },
        )
    }
}

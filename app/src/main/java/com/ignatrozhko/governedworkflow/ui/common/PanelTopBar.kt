package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome
import com.ignatrozhko.governedworkflow.data.server.ServerUrls
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Top bar that always shows which panel URL is in use and lets the user pin another candidate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onManageServers: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val connectionManager = LocalAppContainer.current.connectionManager
    val connection by connectionManager.state.collectAsStateWithLifecycle()
    var isMenuOpen by remember { mutableStateOf(false) }
    var switchError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(switchError) {
        if (switchError != null) {
            delay(SWITCH_ERROR_VISIBLE_MS)
            switchError = null
        }
    }

    TopAppBar(
        title = {
            Box {
                Column(Modifier.clickable { isMenuOpen = true }) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ConnectionSubtitle(connection, switchError)
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    CandidateMenuItems(connection) { url ->
                        isMenuOpen = false
                        scope.launch {
                            val outcome = connectionManager.switchTo(url)
                            switchError = (outcome as? ProbeOutcome.Unreachable)?.let { "Switch failed: ${it.reason}" }
                                ?: if (outcome == ProbeOutcome.Unauthorized) "Switch failed: token rejected" else null
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Re-probe all URLs") }, onClick = {
                        isMenuOpen = false
                        switchError = null
                        connectionManager.refresh()
                    })
                    DropdownMenuItem(text = { Text("Manage servers") }, onClick = {
                        isMenuOpen = false
                        onManageServers()
                    })
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            }
        },
        actions = actions,
    )
}

@Composable
private fun CandidateMenuItems(connection: ConnectionState, onPick: (String) -> Unit) {
    val server = connection.serverOrNull() ?: return
    val activeUrl = (connection as? ConnectionState.Connected)?.baseUrl
    Text(
        server.name,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
    server.urls.forEach { url ->
        DropdownMenuItem(
            text = { Text(url) },
            onClick = { onPick(url) },
            leadingIcon = { if (url == activeUrl) Icon(Icons.Filled.Check, contentDescription = "Active") },
        )
    }
}

@Composable
private fun ConnectionSubtitle(connection: ConnectionState, switchError: String?) {
    val (color, label) = when (connection) {
        ConnectionState.NoServer -> MaterialTheme.colorScheme.onSurfaceVariant to "No server selected"
        is ConnectionState.Probing -> PanelColors.Amber to "Connecting to ${connection.server.name}…"
        is ConnectionState.Connected -> PanelColors.Green to ServerUrls.displayHost(connection.baseUrl)
        is ConnectionState.TokenRejected -> PanelColors.Red to "Token rejected by ${ServerUrls.displayHost(connection.baseUrl)}"
        is ConnectionState.Unreachable -> PanelColors.Red to "${connection.server.name} unreachable"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(color)
        Spacer(Modifier.width(6.dp))
        Text(
            switchError ?: label,
            style = MaterialTheme.typography.labelMedium,
            color = if (switchError != null) PanelColors.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Switch URL", modifier = Modifier.size(18.dp))
    }
}

@Composable
fun StatusDot(color: Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

fun ConnectionState.serverOrNull() = when (this) {
    ConnectionState.NoServer -> null
    is ConnectionState.Probing -> server
    is ConnectionState.Connected -> server
    is ConnectionState.TokenRejected -> server
    is ConnectionState.Unreachable -> server
}

private const val SWITCH_ERROR_VISIBLE_MS = 5_000L

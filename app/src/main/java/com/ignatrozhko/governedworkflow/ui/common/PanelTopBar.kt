package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome
import com.ignatrozhko.governedworkflow.data.server.ServerUrls
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App bar with the screen title over a mono subtitle. Without a [subtitle] it shows which panel URL is in use; the
 * overflow menu lets the user pin another candidate URL, re-probe or manage servers.
 */
@Composable
fun PanelTopBar(
    title: String,
    onBack: (() -> Unit)?,
    onManageServers: () -> Unit,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val connection by LocalAppContainer.current.connectionManager.state.collectAsStateWithLifecycle()
    var switchError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(switchError) {
        if (switchError != null) {
            delay(SWITCH_ERROR_VISIBLE_MS)
            switchError = null
        }
    }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = TOP_BAR_HEIGHT).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), maxLines = TITLE_MAX_LINES, overflow = TextOverflow.Ellipsis)
            if (subtitle != null && switchError == null) {
                Text(subtitle, style = MonoText.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ConnectionSubtitle(connection, switchError)
            }
        }
        actions()
        ConnectionMenu(connection, onManageServers) { switchError = it }
    }
}

@Composable
private fun ConnectionMenu(connection: ConnectionState, onManageServers: () -> Unit, onSwitchError: (String?) -> Unit) {
    val connectionManager = LocalAppContainer.current.connectionManager
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isMenuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Connection menu") }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            CandidateMenuItems(connection) { url ->
                isMenuOpen = false
                scope.launch {
                    val outcome = connectionManager.switchTo(url)
                    onSwitchError((outcome as? ProbeOutcome.Unreachable)?.let { "Switch failed: ${it.reason}" }
                        ?: if (outcome == ProbeOutcome.Unauthorized) "Switch failed: token rejected" else null)
                }
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Re-probe all URLs") }, onClick = {
                isMenuOpen = false
                onSwitchError(null)
                connectionManager.refresh()
            })
            DropdownMenuItem(text = { Text("Manage servers") }, onClick = {
                isMenuOpen = false
                onManageServers()
            })
        }
    }
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
    val (color, label) = connectionStatus(connection)
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(if (switchError != null) MaterialTheme.colorScheme.error else color, Modifier.size(6.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            switchError ?: label,
            style = MonoText.caption,
            color = if (switchError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Dot color and one-line description of the connection, shared by the app bar and the terminal header. */
@Composable
fun connectionStatus(connection: ConnectionState): Pair<Color, String> = when (connection) {
    ConnectionState.NoServer -> MaterialTheme.colorScheme.onSurfaceVariant to "No server selected"
    is ConnectionState.Probing -> Checkpoint.colors.warn to "Connecting to ${connection.server.name}…"
    is ConnectionState.Connected -> Checkpoint.colors.ok to "${connection.server.name} · ${ServerUrls.displayHost(connection.baseUrl)}"
    is ConnectionState.TokenRejected -> MaterialTheme.colorScheme.error to "Token rejected by ${ServerUrls.displayHost(connection.baseUrl)}"
    is ConnectionState.Unreachable -> MaterialTheme.colorScheme.error to "${connection.server.name} unreachable"
}

fun ConnectionState.serverOrNull() = when (this) {
    ConnectionState.NoServer -> null
    is ConnectionState.Probing -> server
    is ConnectionState.Connected -> server
    is ConnectionState.TokenRejected -> server
    is ConnectionState.Unreachable -> server
}

private const val SWITCH_ERROR_VISIBLE_MS = 5_000L

private val TOP_BAR_HEIGHT = 64.dp
private const val TITLE_MAX_LINES = 3

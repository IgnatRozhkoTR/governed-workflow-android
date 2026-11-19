package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome
import com.ignatrozhko.governedworkflow.data.server.ServerUrls
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

/** Full-screen explanation of why there is no usable panel connection, with the ways out. */
@Composable
fun ConnectionStatusPanel(onManageServers: () -> Unit) {
    val container = LocalAppContainer.current
    val connection by container.connectionManager.state.collectAsStateWithLifecycle()
    val isGateWatchEnabled by container.settings.isGateWatchEnabled.collectAsStateWithLifecycle(initialValue = false)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 36.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val state = connection) {
            is ConnectionState.Probing, is ConnectionState.Connected -> CircularProgressIndicator()
            ConnectionState.NoServer -> OfflineHeading("Add or select a server to start")
            is ConnectionState.TokenRejected -> {
                OfflineHeading("The panel rejected the saved token")
                ProbeLines(listOf("${ServerUrls.displayHost(state.baseUrl)} · token rejected"))
                Caption("Edit the server and paste a fresh token.")
            }
            is ConnectionState.Unreachable -> {
                OfflineHeading("No URL of ${state.server.name} answered")
                ProbeLines(state.results.map { "${ServerUrls.displayHost(it.url)} · ${probeReason(it.outcome)}" })
                if (isGateWatchEnabled) Caption("Gate notifications are paused until the server is back.")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedActionButton("Servers", onManageServers, icon = painterResource(R.drawable.ic_server))
            if (connection !is ConnectionState.NoServer) {
                PrimaryButton("Retry", container.connectionManager::refresh, icon = rememberVectorPainter(Icons.Filled.Refresh))
            }
        }
    }
}

private fun probeReason(outcome: ProbeOutcome) = (outcome as? ProbeOutcome.Unreachable)?.reason ?: "token rejected"

@Composable
private fun OfflineHeading(text: String) {
    Icon(painterResource(R.drawable.ic_wifi), contentDescription = null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error)
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), textAlign = TextAlign.Center)
}

@Composable
private fun ProbeLines(lines: List<String>) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        lines.forEach { Text(it, style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
fun CenteredText(text: String) {
    Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
}

@Composable
fun LoadFailure(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CenteredText(message)
        PrimaryButton("Retry", onRetry, icon = rememberVectorPainter(Icons.Filled.Refresh))
    }
}

@Composable
fun FullScreenProgress() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator() }
}

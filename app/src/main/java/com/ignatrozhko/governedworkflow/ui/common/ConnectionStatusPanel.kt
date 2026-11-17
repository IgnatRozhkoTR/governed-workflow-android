package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome

/** Full-screen explanation of why there is no usable panel connection, with the ways out. */
@Composable
fun ConnectionStatusPanel(onManageServers: () -> Unit) {
    val connectionManager = LocalAppContainer.current.connectionManager
    val connection by connectionManager.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val state = connection) {
            is ConnectionState.Probing, is ConnectionState.Connected -> CircularProgressIndicator()
            ConnectionState.NoServer -> CenteredText("Add or select a server to start.")
            is ConnectionState.TokenRejected -> CenteredText("The panel at ${state.baseUrl} rejected the saved token. Edit the server and paste a fresh token.")
            is ConnectionState.Unreachable -> {
                CenteredText("No URL of ${state.server.name} answered.")
                state.results.forEach { result ->
                    val reason = (result.outcome as? ProbeOutcome.Unreachable)?.reason ?: "token rejected"
                    Text("${result.url}: $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onManageServers) { Text("Servers") }
            if (connection !is ConnectionState.NoServer) Button(onClick = connectionManager::refresh) { Text("Retry") }
        }
    }
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
        Button(onClick = onRetry) { Text("Retry") }
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

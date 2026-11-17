package com.ignatrozhko.governedworkflow.ui.servers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.discovery.DiscoveredPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanScanScreen(
    onPanelChosen: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LanScanViewModel = viewModel(factory = LanScanViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan local network") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = { TextButton(onClick = viewModel::scan, enabled = !state.isScanning) { Text("Rescan") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ScanProgress(state) }
            items(state.found, key = { it.baseUrl }) { panel -> FoundPanelCard(panel) { onPanelChosen(panel.baseUrl) } }
        }
    }
}

@Composable
private fun ScanProgress(state: LanScanUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.subnetLabel?.let { subnet ->
            Text("Probing port 5111 on $subnet — ${state.probed}/${state.total}", style = MaterialTheme.typography.bodyMedium)
            if (state.total > 0) {
                LinearProgressIndicator(progress = { state.probed.toFloat() / state.total }, modifier = Modifier.fillMaxWidth())
            }
            if (!state.isScanning && state.found.isEmpty()) {
                Text("No panel answered. Check that the panel listens on 0.0.0.0 and the phone is on the same network.")
            }
        }
    }
}

@Composable
private fun FoundPanelCard(panel: DiscoveredPanel, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(panel.baseUrl, style = MaterialTheme.typography.titleMedium)
            Text(
                if (panel.isTokenConfigured) "Governed Workflow panel — tap to add with its token" else "Panel has no token configured yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

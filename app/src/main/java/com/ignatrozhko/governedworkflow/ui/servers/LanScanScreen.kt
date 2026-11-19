package com.ignatrozhko.governedworkflow.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.discovery.DiscoveredPanel
import com.ignatrozhko.governedworkflow.ui.common.EmptyState
import com.ignatrozhko.governedworkflow.ui.common.ListRow
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.common.RowGroup
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanScanScreen(
    onPanelChosen: (String) -> Unit,
    onEnterManually: () -> Unit,
    onBack: () -> Unit,
    viewModel: LanScanViewModel = viewModel(factory = LanScanViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan local network", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.found.isNotEmpty()) {
                RowGroup(state.found) { panel -> FoundPanelRow(panel) { onPanelChosen(panel.baseUrl) } }
            }
            ScanStatus(state, onRescan = viewModel::scan, onEnterManually = onEnterManually)
        }
    }
}

@Composable
private fun ScanStatus(state: LanScanUiState, onRescan: () -> Unit, onEnterManually: () -> Unit) {
    EmptyState(
        title = scanTitle(state),
        message = state.error ?: scanMessage(state),
        icon = painterResource(R.drawable.ic_wifi),
        actions = {
            if (!state.isScanning) OutlinedActionButton("Rescan", onRescan)
            PrimaryButton("Enter manually", onEnterManually)
        },
    )
    if (state.isScanning && state.total > 0) {
        LinearProgressIndicator(progress = { state.probed.toFloat() / state.total }, modifier = Modifier.fillMaxWidth())
    }
}

private fun scanTitle(state: LanScanUiState) = when {
    state.error != null -> "No network to scan"
    state.isScanning -> "Looking for panels"
    state.found.isEmpty() -> "No panel answered"
    else -> "Scan finished"
}

private fun scanMessage(state: LanScanUiState) = when {
    state.isScanning -> "Probing port 5111 on ${state.subnetLabel.orEmpty()}: ${state.probed} of ${state.total} addresses."
    state.found.isEmpty() -> "Check that the panel listens on 0.0.0.0 and the phone is on the same network."
    else -> "Pick a panel above or enter the address yourself."
}

@Composable
private fun FoundPanelRow(panel: DiscoveredPanel, onClick: () -> Unit) {
    ListRow(
        title = panel.baseUrl,
        onClick = onClick,
        titleStyle = MonoText.code,
        subtitle = if (panel.isTokenConfigured) "Governed Workflow panel. Tap to add it with its token." else "Panel has no token configured yet",
    )
}

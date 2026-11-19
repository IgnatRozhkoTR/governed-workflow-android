package com.ignatrozhko.governedworkflow.ui.servers


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.data.server.ServerEntry
import com.ignatrozhko.governedworkflow.ui.common.ActionFooter
import com.ignatrozhko.governedworkflow.ui.common.EmptyState
import com.ignatrozhko.governedworkflow.ui.common.ListRowLayout
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.common.RowGroup
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onServerChosen: () -> Unit,
    onAddManually: () -> Unit,
    onEdit: (String) -> Unit,
    onScanQr: () -> Unit,
    onScanNetwork: () -> Unit,
    onBack: () -> Unit,
    viewModel: ServersViewModel = viewModel(factory = ServersViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connection by LocalAppContainer.current.connectionManager.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ServerEntry?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servers", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)) },
                navigationIcon = {
                    if (state.selectedId != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { AddServerActions(onAddManually, onScanQr, onScanNetwork) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.servers.isNotEmpty()) {
                item {
                    RowGroup(state.servers) { server ->
                        ServerRow(
                            server = server,
                            status = if (server.id == state.selectedId) connection else null,
                            onOpen = { viewModel.select(server.id, onServerChosen) },
                            onEdit = { onEdit(server.id) },
                            onDelete = { pendingDelete = server },
                        )
                    }
                }
            }
            if (state.isLoaded) {
                item {
                    EmptyState(
                        title = if (state.servers.isEmpty()) "No servers yet" else "Add another server",
                        message = "Add a server by scanning the QR code from the web panel, or search your Wi-Fi network.",
                        icon = painterResource(R.drawable.ic_wifi),
                    )
                }
            }
        }
    }
    pendingDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${server.name}?") },
            text = { Text("The saved URLs and token are removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(server.id)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

/** [status] is the live connection for the selected server only; the others have not been probed. */
@Composable
private fun ServerRow(server: ServerEntry, status: ConnectionState?, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListRowLayout(onOpen, trailing = {
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit ${server.name}") }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Delete ${server.name}") }
    }, showsChevron = false) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(server.name, style = MaterialTheme.typography.titleSmall)
            status?.let { ConnectionPill(it) }
        }
        server.urls.forEach { url ->
            Text(url, style = MonoText.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionPill(connection: ConnectionState) {
    when (connection) {
        is ConnectionState.Connected -> Pill("connected", PillTone.Ok, hasDot = true)
        is ConnectionState.Probing -> Pill("connecting", PillTone.Warn, hasDot = true)
        is ConnectionState.TokenRejected -> Pill("token rejected", PillTone.Bad, hasDot = true)
        is ConnectionState.Unreachable -> Pill("unreachable", PillTone.Bad, hasDot = true)
        ConnectionState.NoServer -> Unit
    }
}

@Composable
private fun AddServerActions(onAddManually: () -> Unit, onScanQr: () -> Unit, onScanNetwork: () -> Unit) {
    ActionFooter(isAtScreenBottom = true) {
        OutlinedActionButton("Scan QR", onScanQr, icon = painterResource(R.drawable.ic_qr))
        OutlinedActionButton("Scan LAN", onScanNetwork, icon = painterResource(R.drawable.ic_wifi))
        Spacer(Modifier.weight(1f))
        PrimaryButton("Add", onAddManually, icon = rememberVectorPainter(Icons.Filled.Add))
    }
}

package com.ignatrozhko.governedworkflow.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome
import com.ignatrozhko.governedworkflow.ui.common.readClipboardText
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditorScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ServerEditorViewModel = viewModel(factory = ServerEditorViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.isScanningQr) {
        QrScanner(onScanned = viewModel::onQrScanned, onClose = { viewModel.setScanningQr(false) }, error = state.error)
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit server" else "Add server") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServerForm(state, viewModel)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TestResults(state)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = viewModel::testConnection, enabled = !state.isTesting) {
                    Text(if (state.isTesting) "Testing…" else "Test")
                }
                Button(onClick = { viewModel.save(onSaved) }, modifier = Modifier.weight(1f)) { Text("Save and connect") }
            }
        }
    }
}

@Composable
private fun ServerForm(state: ServerEditorUiState, viewModel: ServerEditorViewModel) {
    val context = LocalContext.current
    var isTokenVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = state.name,
        onValueChange = viewModel::onNameChange,
        label = { Text("Name") },
        placeholder = { Text("Mac") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.urlsText,
        onValueChange = viewModel::onUrlsChange,
        label = { Text("URLs, one per line, in order of preference") },
        placeholder = { Text("http://192.168.1.10:5111\nhttp://100.64.0.2:5111") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { viewModel.appendUrl(readClipboardText(context)) }) { Text("Paste URL") }
        TextButton(onClick = { viewModel.setScanningQr(true) }) { Text("Scan QR") }
    }
    OutlinedTextField(
        value = state.token,
        onValueChange = viewModel::onTokenChange,
        label = { Text("Admin token") },
        singleLine = true,
        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            TextButton(onClick = { isTokenVisible = !isTokenVisible }) { Text(if (isTokenVisible) "Hide" else "Show") }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = { viewModel.onTokenChange(readClipboardText(context)) }) { Text("Paste token") }
}

@Composable
private fun TestResults(state: ServerEditorUiState) {
    state.testResults.forEach { (url, outcome) ->
        val (color, label) = when (outcome) {
            ProbeOutcome.Reachable -> PanelColors.Green to "reachable"
            ProbeOutcome.Unauthorized -> PanelColors.Red to "token rejected"
            is ProbeOutcome.Unreachable -> PanelColors.Red to outcome.reason
        }
        Text("$url — $label", color = color, style = MaterialTheme.typography.bodySmall)
    }
}

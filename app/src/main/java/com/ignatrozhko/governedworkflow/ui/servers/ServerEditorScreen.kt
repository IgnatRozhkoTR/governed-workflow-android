package com.ignatrozhko.governedworkflow.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome
import com.ignatrozhko.governedworkflow.ui.common.ActionFooter
import com.ignatrozhko.governedworkflow.ui.common.LabeledField
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.common.readClipboardText
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

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
                title = { Text(if (state.isEditing) "Edit server" else "Add server", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            ActionFooter(Modifier.imePadding(), isAtScreenBottom = true) {
                OutlinedActionButton(if (state.isTesting) "Testing…" else "Test", viewModel::testConnection, isEnabled = !state.isTesting)
                PrimaryButton("Save and connect", { viewModel.save(onSaved) }, Modifier.weight(1f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ServerForm(state, viewModel)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            TestResults(state)
        }
    }
}

@Composable
private fun ServerForm(state: ServerEditorUiState, viewModel: ServerEditorViewModel) {
    val context = LocalContext.current
    var isTokenVisible by remember { mutableStateOf(false) }
    LabeledField("Name", state.name, viewModel::onNameChange, placeholder = "Mac")
    LabeledField(
        "URLs, one per line, in order of preference",
        state.urlsText,
        viewModel::onUrlsChange,
        placeholder = "http://192.168.1.10:5111\nhttp://100.64.0.2:5111",
        isMono = true,
        isSingleLine = false,
        minLines = 2,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedActionButton("Paste URL", { viewModel.appendUrl(readClipboardText(context)) }, icon = painterResource(R.drawable.ic_paste))
        OutlinedActionButton("Scan QR", { viewModel.setScanningQr(true) }, icon = painterResource(R.drawable.ic_qr))
    }
    LabeledField(
        "Admin token",
        state.token,
        viewModel::onTokenChange,
        isMono = true,
        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            TextButton(onClick = { isTokenVisible = !isTokenVisible }) { Text(if (isTokenVisible) "Hide" else "Show") }
        },
    )
    OutlinedActionButton("Paste token", { viewModel.onTokenChange(readClipboardText(context)) }, icon = painterResource(R.drawable.ic_paste))
}

@Composable
private fun TestResults(state: ServerEditorUiState) {
    state.testResults.forEach { (url, outcome) ->
        val (tone, label) = when (outcome) {
            ProbeOutcome.Reachable -> PillTone.Ok to "reachable"
            ProbeOutcome.Unauthorized -> PillTone.Bad to "token rejected"
            is ProbeOutcome.Unreachable -> PillTone.Bad to "unreachable"
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(url, Modifier.weight(1f), style = MonoText.small)
            Pill(label, tone)
        }
        (outcome as? ProbeOutcome.Unreachable)?.let { SecondaryReason(it.reason) }
    }
}

@Composable
private fun SecondaryReason(reason: String) {
    Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

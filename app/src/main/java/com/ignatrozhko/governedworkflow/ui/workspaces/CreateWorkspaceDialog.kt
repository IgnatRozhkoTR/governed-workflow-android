package com.ignatrozhko.governedworkflow.ui.workspaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.NewWorkspace

/** Name, source branch and YOLO/fast, like the web panel's form; the rest is configured in the new workspace's Settings. */
@Composable
fun CreateWorkspaceDialog(editing: WorkspaceCreation.Editing, onCreate: (NewWorkspace) -> Unit, onDismiss: () -> Unit) {
    var branch by remember { mutableStateOf("") }
    var sourceBranch by remember(editing.sourceBranches) { mutableStateOf(defaultSourceBranch(editing.sourceBranches)) }
    var isYolo by remember { mutableStateOf(false) }
    var isFast by remember { mutableStateOf(false) }
    val canCreate = branch.isNotBlank() && sourceBranch != null && !editing.isSubmitting
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch name") }, singleLine = true)
                SourceBranchPicker(editing.sourceBranches, sourceBranch) { sourceBranch = it }
                ModeToggle("YOLO mode", isOnByDefault = editing.defaults.yoloModeDefault, isYolo) { isYolo = it }
                ModeToggle("Fast mode", isOnByDefault = editing.defaults.fastModeDefault, isFast) { isFast = it }
                editing.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { sourceBranch?.let { onCreate(NewWorkspace(branch.trim(), it, isYolo, isFast)) } }, enabled = canCreate) {
                Text(if (editing.isSubmitting) "Creating…" else "Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The web panel's choice: `develop` when the repository has it, otherwise whatever branch the server lists first. */
private fun defaultSourceBranch(branches: List<String>): String? =
    branches.firstOrNull { it == PREFERRED_SOURCE_BRANCH } ?: branches.firstOrNull()

private const val PREFERRED_SOURCE_BRANCH = "develop"

@Composable
private fun SourceBranchPicker(branches: List<String>, selected: String?, onSelect: (String) -> Unit) {
    if (selected == null) {
        Text("No branches to start from", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        return
    }
    var isExpanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("From", Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { isExpanded = true }) { Text(selected, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                branches.forEach { branch ->
                    DropdownMenuItem(text = { Text(branch) }, onClick = { onSelect(branch); isExpanded = false })
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(label: String, isOnByDefault: Boolean, isChecked: Boolean, onChange: (Boolean) -> Unit) {
    if (isOnByDefault) {
        Text("$label is on by default for this project", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onChange)
    }
}

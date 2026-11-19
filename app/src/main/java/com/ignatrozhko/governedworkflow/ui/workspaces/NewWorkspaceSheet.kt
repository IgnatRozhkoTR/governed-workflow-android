package com.ignatrozhko.governedworkflow.ui.workspaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.api.NewWorkspace
import com.ignatrozhko.governedworkflow.ui.common.LabeledField
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.PanelSheet
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.common.ToggleSwitch
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

/** Name, source branch and YOLO/fast, like the web panel's form; the rest is configured in the new workspace's Settings. */
@Composable
fun NewWorkspaceSheet(editing: WorkspaceCreation.Editing, onCreate: (NewWorkspace) -> Unit, onDismiss: () -> Unit) {
    var branch by remember { mutableStateOf("") }
    var sourceBranch by remember(editing.sourceBranches) { mutableStateOf(defaultSourceBranch(editing.sourceBranches)) }
    var isYolo by remember { mutableStateOf(false) }
    var isFast by remember { mutableStateOf(false) }
    val canCreate = branch.isNotBlank() && sourceBranch != null && !editing.isSubmitting
    PanelSheet("New workspace", onDismiss) {
        LabeledField("Branch name", branch, { branch = it }, placeholder = "feature/my-branch", isMono = true)
        SourceBranchPicker(editing.sourceBranches, sourceBranch) { sourceBranch = it }
        ModeToggle("YOLO mode", isOnByDefault = editing.defaults.yoloModeDefault, isYolo) { isYolo = it }
        ModeToggle("Fast mode", isOnByDefault = editing.defaults.fastModeDefault, isFast) { isFast = it }
        editing.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth()) {
            OutlinedActionButton("Cancel", onDismiss)
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                if (editing.isSubmitting) "Creating…" else "Create",
                onClick = { sourceBranch?.let { onCreate(NewWorkspace(branch.trim(), it, isYolo, isFast)) } },
                isEnabled = canCreate,
                icon = rememberVectorPainter(Icons.Filled.Add),
            )
        }
    }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("From", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Box {
            BranchButton(selected, onClick = { isExpanded = true })
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                branches.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch, style = MonoText.code, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(branch); isExpanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun BranchButton(branch: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick,
        Modifier.widthIn(max = BRANCH_BUTTON_MAX_WIDTH).heightIn(min = 44.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(start = 14.dp, end = 10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_branch), contentDescription = null, Modifier.size(18.dp))
        Text(
            branch,
            Modifier.weight(1f, fill = false).padding(horizontal = 8.dp),
            style = MonoText.code,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose source branch")
    }
}

@Composable
private fun ModeToggle(label: String, isOnByDefault: Boolean, isChecked: Boolean, onChange: (Boolean) -> Unit) {
    if (isOnByDefault) {
        Text("$label is on by default for this project", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        ToggleSwitch(isChecked, onChange)
    }
}

private val BRANCH_BUTTON_MAX_WIDTH = 220.dp

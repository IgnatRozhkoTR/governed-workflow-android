package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.FAST_WORKFLOW_MODE
import com.ignatrozhko.governedworkflow.data.api.ModuleInfo
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.ProjectSettings
import com.ignatrozhko.governedworkflow.data.api.STANDARD_WORKFLOW_MODE
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.data.api.getClaudeCommand
import com.ignatrozhko.governedworkflow.data.api.getProjectSettings
import com.ignatrozhko.governedworkflow.data.api.listEnabledModules
import com.ignatrozhko.governedworkflow.data.api.listModules
import com.ignatrozhko.governedworkflow.data.api.setClaudeCommand
import com.ignatrozhko.governedworkflow.data.api.setEnabledModules
import com.ignatrozhko.governedworkflow.data.api.setReviewMode
import com.ignatrozhko.governedworkflow.data.api.setWorkflowMode
import com.ignatrozhko.governedworkflow.data.api.updateProjectSettings
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel

data class SettingsData(
    val project: ProjectSettings,
    val claudeCommand: String,
    val modules: List<ModuleInfo>,
    val enabledModules: Set<String>,
)

/** How the agentic review runs; the web panel's four review modes. */
enum class ReviewMode(val wireValue: String, val label: String) {
    Manual("manual", "Manual"),
    Integration("integration", "Integration"),
    FilesIntegration("files_integration", "Files + integration"),
    Full("full", "Full"),
}

class SettingsViewModel(workspace: WorkspaceRef, connectionManager: ConnectionManager, stateStore: WorkspaceStateStore) :
    SectionDataViewModel<SettingsData>(workspace, connectionManager, stateStore) {
    override suspend fun load(api: PanelApi) = SettingsData(
        project = api.getProjectSettings(workspace.projectId),
        claudeCommand = api.getClaudeCommand(workspace),
        modules = api.listModules(),
        enabledModules = api.listEnabledModules(),
    )

    fun setWorkflowMode(isFast: Boolean) = act(if (isFast) "Fast mode on" else "Fast mode off") {
        it.setWorkflowMode(workspace, if (isFast) FAST_WORKFLOW_MODE else STANDARD_WORKFLOW_MODE)
    }

    fun setReviewMode(mode: ReviewMode) = act("Review mode: ${mode.label}") { it.setReviewMode(workspace, mode.wireValue) }

    fun setYolo(enabled: Boolean) = act(if (enabled) "YOLO mode on" else "YOLO mode off") { it.setYolo(workspace, enabled) }

    fun updateProjectDefaults(settings: ProjectSettings) =
        actAndReload("Project defaults saved") { it.updateProjectSettings(workspace.projectId, settings) }

    fun setClaudeCommand(command: String) = actAndReload("Launch command saved") { it.setClaudeCommand(workspace, command) }

    fun setEnabledModules(moduleIds: Set<String>) = actAndReload("Modules saved") { it.setEnabledModules(moduleIds) }
}

@Composable
fun SettingsSection(state: WorkspaceState, data: SettingsData, viewModel: SettingsViewModel, isBusy: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "This workspace") {
            SwitchRow("Fast mode", "Skips research and planning gates", state.workflowMode == FAST_WORKFLOW_MODE, isBusy, viewModel::setWorkflowMode)
            SwitchRow("YOLO mode", "Auto-approve user gates", state.yoloMode, isBusy, viewModel::setYolo)
            Text("Review mode", style = MaterialTheme.typography.labelLarge)
            ReviewModeChips(state.reviewMode, isBusy, viewModel::setReviewMode)
            ClaudeCommandField(data.claudeCommand, isBusy, viewModel::setClaudeCommand)
        }
        ProjectDefaultsCard(data.project, isBusy, viewModel::updateProjectDefaults)
        ModulesCard(data.modules, data.enabledModules, isBusy, viewModel::setEnabledModules)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, isChecked: Boolean, isBusy: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            SecondaryText(subtitle)
        }
        Switch(checked = isChecked, onCheckedChange = onChange, enabled = !isBusy)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewModeChips(current: String, isBusy: Boolean, onSelect: (ReviewMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReviewMode.entries.forEach { mode ->
            FilterChip(selected = mode.wireValue == current, onClick = { onSelect(mode) }, enabled = !isBusy, label = { Text(mode.label) })
        }
    }
}

@Composable
private fun ClaudeCommandField(saved: String, isBusy: Boolean, onSave: (String) -> Unit) {
    var command by remember(saved) { mutableStateOf(saved) }
    OutlinedTextField(
        value = command,
        onValueChange = { command = it },
        label = { Text("Claude launch command") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = { onSave(command.trim()) }, enabled = !isBusy && command.trim() != saved && command.isNotBlank()) { Text("Save command") }
}

@Composable
private fun ProjectDefaultsCard(settings: ProjectSettings, isBusy: Boolean, onSave: (ProjectSettings) -> Unit) {
    SectionCard(title = "Project defaults for new workspaces") {
        SwitchRow("YOLO by default", "New workspaces start in YOLO mode", settings.yoloModeDefault, isBusy) {
            onSave(settings.copy(yoloModeDefault = it))
        }
        SwitchRow("Fast by default", "New workspaces start in fast mode", settings.fastModeDefault, isBusy) {
            onSave(settings.copy(fastModeDefault = it))
        }
        Text("Default review mode", style = MaterialTheme.typography.labelLarge)
        ReviewModeChips(settings.reviewModeDefault, isBusy) { onSave(settings.copy(reviewModeDefault = it.wireValue)) }
    }
}

/** Modules are a panel-wide setting; saving re-renders every project's Claude config, hence the confirmation. */
@Composable
private fun ModulesCard(modules: List<ModuleInfo>, enabled: Set<String>, isBusy: Boolean, onSave: (Set<String>) -> Unit) {
    var pending by remember { mutableStateOf<Set<String>?>(null) }
    SectionCard(title = "Modules (all projects)") {
        if (modules.isEmpty()) SecondaryText("No modules installed on this panel.")
        modules.forEach { module ->
            SwitchRow(module.name, module.description, module.id in enabled, isBusy) { isOn ->
                pending = if (isOn) enabled + module.id else enabled - module.id
            }
        }
    }
    pending?.let { moduleIds ->
        ConfirmDialog(
            title = "Change modules?",
            message = "Modules apply to every project on this panel; their Claude config is regenerated.",
            confirmLabel = "Apply",
            onDismiss = { pending = null },
        ) {
            pending = null
            onSave(moduleIds)
        }
    }
}

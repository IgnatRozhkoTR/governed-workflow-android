package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.ignatrozhko.governedworkflow.data.settings.ThemeMode
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.common.ChoiceChip
import com.ignatrozhko.governedworkflow.ui.common.FieldLabel
import com.ignatrozhko.governedworkflow.ui.common.LabeledField
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.PanelCard
import com.ignatrozhko.governedworkflow.ui.common.SegmentedChoice
import com.ignatrozhko.governedworkflow.ui.common.ToggleSwitch
import com.ignatrozhko.governedworkflow.ui.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel
import kotlinx.coroutines.launch

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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsGroup("This workspace") {
            SwitchRow("Fast mode", "Skips optional review gates", state.workflowMode == FAST_WORKFLOW_MODE, isBusy, viewModel::setWorkflowMode)
            SettingsDivider()
            SwitchRow("YOLO mode", "Auto-approve user gates", state.yoloMode, isBusy, viewModel::setYolo)
            SettingsDivider()
            ReviewModeRow("Review mode", state.reviewMode, isBusy, viewModel::setReviewMode)
            SettingsDivider()
            ClaudeCommandField(data.claudeCommand, isBusy, viewModel::setClaudeCommand)
        }
        ProjectDefaultsGroup(data.project, isBusy, viewModel::updateProjectDefaults)
        SettingsGroup("This app") { ThemeRow() }
        ModulesGroup(data.modules, data.enabledModules, isBusy, viewModel::setEnabledModules)
    }
}

@Composable
private fun SettingsGroup(caption: String, content: @Composable ColumnScope.() -> Unit) {
    FieldLabel(caption, Modifier.padding(start = 4.dp, top = 6.dp))
    PanelCard(content = content)
}

@Composable
private fun SettingsDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.outline)

@Composable
private fun SwitchRow(title: String, subtitle: String, isChecked: Boolean, isBusy: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            SecondaryText(subtitle)
        }
        ToggleSwitch(isChecked, onChange, isEnabled = !isBusy)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewModeRow(title: String, current: String, isBusy: Boolean, onSelect: (ReviewMode) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp - CHIP_TOUCH_OVERHANG)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp - CHIP_TOUCH_OVERHANG * 2)) {
            ReviewMode.entries.forEach { mode ->
                ChoiceChip(mode.label, isSelected = mode.wireValue == current, onClick = { if (!isBusy) onSelect(mode) })
            }
        }
    }
}

@Composable
private fun ThemeRow() {
    val settings = LocalAppContainer.current.settings
    val scope = rememberCoroutineScope()
    val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text("Theme", style = MaterialTheme.typography.bodyLarge)
            SecondaryText("Follow system, day or night")
        }
        SegmentedChoice(
            ThemeMode.entries,
            themeMode,
            ThemeMode::label,
            onSelect = { scope.launch { settings.setThemeMode(it) } },
            modifier = Modifier.width(THEME_CHOICE_WIDTH),
        )
    }
}

@Composable
private fun ClaudeCommandField(saved: String, isBusy: Boolean, onSave: (String) -> Unit) {
    var command by remember(saved) { mutableStateOf(saved) }
    val isChanged = command.isNotBlank() && command.trim() != saved
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabeledField("Claude launch command", command, { command = it }, isMono = true)
        if (isChanged) TextButton(onClick = { onSave(command.trim()) }, enabled = !isBusy) { Text("Save command") }
    }
}

@Composable
private fun ProjectDefaultsGroup(settings: ProjectSettings, isBusy: Boolean, onSave: (ProjectSettings) -> Unit) {
    SettingsGroup("Project defaults for new workspaces") {
        SwitchRow("YOLO by default", "New workspaces start in YOLO mode", settings.yoloModeDefault, isBusy) {
            onSave(settings.copy(yoloModeDefault = it))
        }
        SettingsDivider()
        SwitchRow("Fast by default", "New workspaces start in fast mode", settings.fastModeDefault, isBusy) {
            onSave(settings.copy(fastModeDefault = it))
        }
        SettingsDivider()
        ReviewModeRow("Default review mode", settings.reviewModeDefault, isBusy) { onSave(settings.copy(reviewModeDefault = it.wireValue)) }
    }
}

/** Modules are a panel-wide setting; saving re-renders every project's Claude config, hence the confirmation. */
@Composable
private fun ModulesGroup(modules: List<ModuleInfo>, enabled: Set<String>, isBusy: Boolean, onSave: (Set<String>) -> Unit) {
    var pending by remember { mutableStateOf<Set<String>?>(null) }
    SettingsGroup("Modules (all projects)") {
        if (modules.isEmpty()) SecondaryText("No modules installed on this panel.", Modifier.padding(16.dp))
        modules.forEachIndexed { index, module ->
            if (index > 0) SettingsDivider()
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

private val THEME_CHOICE_WIDTH = 176.dp
private val CHIP_TOUCH_OVERHANG = 6.dp

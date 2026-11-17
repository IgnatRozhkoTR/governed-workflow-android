package com.ignatrozhko.governedworkflow.ui.workspace

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.common.LoadStateContent
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.sections.CommentLocation
import com.ignatrozhko.governedworkflow.ui.workspace.sections.CommentsHubSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.CriteriaSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.CriteriaViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.sections.PhaseSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.PlanSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.PrePlanningSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ProgressSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ReflectionSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ReflectionViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ResearchSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ReviewSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ScratchpadsSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.ScratchpadsViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.sections.SettingsSection
import com.ignatrozhko.governedworkflow.ui.workspace.sections.SettingsViewModel

/** Navigation out of a section: to a file in the Changes tab, or to another section. */
class SectionNavigation(val openReviewFile: (String) -> Unit, val openSection: (WorkspaceSection) -> Unit, val manageServers: () -> Unit) {
    fun jumpTo(location: CommentLocation) = when (location) {
        is CommentLocation.ReviewFile -> openReviewFile(location.path)
        CommentLocation.Plan -> openSection(WorkspaceSection.Plan)
        CommentLocation.Research -> openSection(WorkspaceSection.Research)
    }
}

@Composable
fun SectionHost(
    section: WorkspaceSection,
    state: WorkspaceState,
    viewModel: WorkspaceViewModel,
    navigation: SectionNavigation,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val phaseNames by viewModel.phaseNames.collectAsStateWithLifecycle()
    when (section) {
        WorkspaceSection.PrePlanning -> PrePlanningSection(state, viewModel, modifier)
        WorkspaceSection.Plan -> PlanSection(state, viewModel, isBusy, modifier)
        WorkspaceSection.Research -> ResearchSection(state, viewModel, isBusy, modifier)
        WorkspaceSection.Progress -> ProgressSection(state, phaseNames, modifier)
        WorkspaceSection.Review -> ReviewSection(state, viewModel, isBusy, navigation.openReviewFile, modifier)
        WorkspaceSection.Comments -> CommentsHubSection(state, viewModel::resolveComment, navigation::jumpTo, modifier)
        WorkspaceSection.Phase -> PhaseSection(state, phaseNames, isBusy, viewModel::setPhase, modifier)
        WorkspaceSection.Criteria, WorkspaceSection.Reflection, WorkspaceSection.Scratchpads, WorkspaceSection.Settings ->
            DataSectionHost(section, state, viewModel.workspace, navigation, snackbarHostState, modifier)
    }
}

@Composable
private fun DataSectionHost(
    section: WorkspaceSection,
    state: WorkspaceState,
    workspace: WorkspaceRef,
    navigation: SectionNavigation,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
) {
    val container = LocalAppContainer.current
    val connectionManager = container.connectionManager
    val store = container.workspaceStateStore
    when (section) {
        WorkspaceSection.Criteria -> {
            val sectionViewModel = viewModel(key = section.name) { CriteriaViewModel(workspace, connectionManager, store) }
            DataSection(sectionViewModel, navigation, snackbarHostState, modifier) { criteria, isBusy ->
                CriteriaSection(criteria, isBusy, sectionViewModel::validate)
            }
        }
        WorkspaceSection.Reflection -> {
            val sectionViewModel = viewModel(key = section.name) { ReflectionViewModel(workspace, connectionManager, store) }
            DataSection(sectionViewModel, navigation, snackbarHostState, modifier) { proposals, isBusy ->
                ReflectionSection(proposals, isBusy, sectionViewModel::resolve)
            }
        }
        WorkspaceSection.Scratchpads -> {
            val sectionViewModel = viewModel(key = section.name) { ScratchpadsViewModel(workspace, connectionManager, store) }
            DataSection(sectionViewModel, navigation, snackbarHostState, modifier) { files, _ -> ScratchpadsSection(files, sectionViewModel) }
        }
        else -> {
            val sectionViewModel = viewModel(key = section.name) { SettingsViewModel(workspace, connectionManager, store) }
            DataSection(sectionViewModel, navigation, snackbarHostState, modifier) { data, isBusy ->
                SettingsSection(state, data, sectionViewModel, isBusy)
            }
        }
    }
}

@Composable
private fun <T> DataSection(
    viewModel: SectionDataViewModel<T>,
    navigation: SectionNavigation,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    content: @Composable (data: T, isBusy: Boolean) -> Unit,
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    LoadStateContent(data, isRefreshing = false, onRefresh = viewModel::reload, onManageServers = navigation.manageServers, modifier = modifier) {
        content(it, isBusy)
    }
}

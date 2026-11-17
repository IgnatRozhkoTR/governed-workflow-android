package com.ignatrozhko.governedworkflow.ui.workspaces

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.NewWorkspace
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.ProjectSettings
import com.ignatrozhko.governedworkflow.data.api.StateFetch
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.WorkspaceSummary
import com.ignatrozhko.governedworkflow.data.api.createWorkspace
import com.ignatrozhko.governedworkflow.data.api.getProjectSettings
import com.ignatrozhko.governedworkflow.data.api.listSourceBranches
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.ReloadTrigger
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import com.ignatrozhko.governedworkflow.ui.nav.WorkspacesRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class WorkspaceRow(val summary: WorkspaceSummary, val gatePhase: String?)

sealed interface WorkspaceCreation {
    data object Idle : WorkspaceCreation
    data object Loading : WorkspaceCreation
    data class Editing(
        val defaults: ProjectSettings,
        val sourceBranches: List<String>,
        val error: String? = null,
        val isSubmitting: Boolean = false,
    ) : WorkspaceCreation
    data class Created(val branch: String) : WorkspaceCreation
}

class WorkspacesViewModel(savedStateHandle: SavedStateHandle, private val connectionManager: ConnectionManager) : ViewModel() {
    private val projectId = savedStateHandle.toRoute<WorkspacesRoute>().projectId
    private val trigger = ReloadTrigger()

    val workspaces: StateFlow<LoadState<List<WorkspaceRow>>> = connectionManager
        .loadWhenConnected(trigger.reloads, ::loadRows)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    private val _creation = MutableStateFlow<WorkspaceCreation>(WorkspaceCreation.Idle)
    val creation: StateFlow<WorkspaceCreation> = _creation.asStateFlow()

    fun reload() = trigger.reload()

    /** Project defaults decide which toggles the create dialog offers: a default that is already on needs no switch. */
    fun startCreating() {
        _creation.value = WorkspaceCreation.Loading
        viewModelScope.launch {
            _creation.value = try {
                val api = connectionManager.requireApi()
                WorkspaceCreation.Editing(api.getProjectSettings(projectId), api.listSourceBranches(projectId))
            } catch (error: PanelException) {
                WorkspaceCreation.Editing(ProjectSettings(), emptyList(), error.message)
            }
        }
    }

    fun create(request: NewWorkspace) {
        val editing = _creation.value as? WorkspaceCreation.Editing ?: return
        _creation.value = editing.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            _creation.value = try {
                WorkspaceCreation.Created(connectionManager.requireApi().createWorkspace(projectId, request).branch)
            } catch (error: PanelException) {
                editing.copy(isSubmitting = false, error = error.message)
            }
        }
    }

    fun dismissCreation() {
        _creation.value = WorkspaceCreation.Idle
        reload()
    }

    private suspend fun loadRows(api: PanelApi): List<WorkspaceRow> {
        val summaries = api.listWorkspaces(projectId).sortedWith(compareBy({ it.isArchived }, { it.branch }))
        val permits = Semaphore(MAX_PARALLEL_STATE_FETCHES)
        return coroutineScope {
            summaries.map { summary ->
                async { WorkspaceRow(summary, if (summary.isArchived) null else permits.withPermit { pendingGate(api, summary) }) }
            }.awaitAll()
        }
    }

    /** The list endpoint omits gates, so each live workspace's state is read; a failed read only drops its badge. */
    private suspend fun pendingGate(api: PanelApi, summary: WorkspaceSummary): String? = try {
        (api.getState(WorkspaceRef(projectId, summary.branch), etag = null) as? StateFetch.Fresh)?.state?.pendingGate?.phase
    } catch (error: PanelException) {
        null
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val MAX_PARALLEL_STATE_FETCHES = 4

        val Factory = viewModelFactory {
            initializer { WorkspacesViewModel(createSavedStateHandle(), appContainer.connectionManager) }
        }
    }
}

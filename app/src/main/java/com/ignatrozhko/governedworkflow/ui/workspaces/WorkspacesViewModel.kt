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
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.StateFetch
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.WorkspaceSummary
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.ReloadTrigger
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import com.ignatrozhko.governedworkflow.ui.nav.WorkspacesRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class WorkspaceRow(val summary: WorkspaceSummary, val gatePhase: String?)

class WorkspacesViewModel(savedStateHandle: SavedStateHandle, connectionManager: ConnectionManager) : ViewModel() {
    private val projectId = savedStateHandle.toRoute<WorkspacesRoute>().projectId
    private val trigger = ReloadTrigger()

    val workspaces: StateFlow<LoadState<List<WorkspaceRow>>> = connectionManager
        .loadWhenConnected(trigger.reloads, ::loadRows)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    fun reload() = trigger.reload()

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

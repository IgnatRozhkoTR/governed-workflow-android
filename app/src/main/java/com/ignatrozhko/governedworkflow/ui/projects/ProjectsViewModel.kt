package com.ignatrozhko.governedworkflow.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.Project
import com.ignatrozhko.governedworkflow.data.api.listPhases
import com.ignatrozhko.governedworkflow.gates.GateScanner
import com.ignatrozhko.governedworkflow.gates.WaitingGate
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.ReloadTrigger
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Every gate waiting across all projects, oldest first, with the names needed to describe each phase. */
data class WaitingGates(val gates: List<WaitingGate>, val phaseNames: PhaseNames) {
    fun countFor(projectId: String) = gates.count { it.workspace.projectId == projectId }
}

class ProjectsViewModel(connectionManager: ConnectionManager) : ViewModel() {
    private val trigger = ReloadTrigger()
    private val scanner = GateScanner()

    val projects: StateFlow<LoadState<List<Project>>> = connectionManager
        .loadWhenConnected(trigger.reloads) { api -> api.listProjects().sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    /**
     * Scanned separately because reading every workspace state is slower than the project list it decorates. A rescan
     * keeps the previous gates on screen instead of blanking them until every workspace has been read again.
     */
    val waitingGates: StateFlow<LoadState<WaitingGates>> = connectionManager
        .loadWhenConnected(trigger.reloads, ::scanGates)
        .filterNot { it is LoadState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    fun reload() = trigger.reload()

    private suspend fun scanGates(api: PanelApi): WaitingGates {
        val gates = scanner.scan(api).sortedWith(compareBy(nullsLast()) { it.since })
        return WaitingGates(gates, PhaseNames(api.listPhases()))
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { ProjectsViewModel(appContainer.connectionManager) }
        }
    }
}

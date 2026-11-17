package com.ignatrozhko.governedworkflow.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.Project
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.ReloadTrigger
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProjectsViewModel(connectionManager: ConnectionManager) : ViewModel() {
    private val trigger = ReloadTrigger()

    val projects: StateFlow<LoadState<List<Project>>> = connectionManager
        .loadWhenConnected(trigger.reloads) { api -> api.listProjects().sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    fun reload() = trigger.reload()

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { ProjectsViewModel(appContainer.connectionManager) }
        }
    }
}

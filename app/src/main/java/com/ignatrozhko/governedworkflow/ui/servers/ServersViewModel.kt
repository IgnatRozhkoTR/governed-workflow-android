package com.ignatrozhko.governedworkflow.ui.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ignatrozhko.governedworkflow.data.server.ServerEntry
import com.ignatrozhko.governedworkflow.data.server.ServerRepository
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServersUiState(val servers: List<ServerEntry> = emptyList(), val selectedId: String? = null, val isLoaded: Boolean = false)

class ServersViewModel(private val repository: ServerRepository) : ViewModel() {

    val uiState: StateFlow<ServersUiState> = combine(repository.servers, repository.selectedServerId) { servers, selected ->
        ServersUiState(servers, selected, isLoaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ServersUiState())

    fun select(id: String, onSelected: () -> Unit) {
        viewModelScope.launch {
            repository.select(id)
            onSelected()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { ServersViewModel(appContainer.serverRepository) }
        }
    }
}

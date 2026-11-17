package com.ignatrozhko.governedworkflow.ui.workspace.common

import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.ReloadTrigger
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** A section backed by its own endpoint rather than the polled state; it reloads after each successful action. */
abstract class SectionDataViewModel<T>(
    workspace: WorkspaceRef,
    connectionManager: ConnectionManager,
    stateStore: WorkspaceStateStore,
) : WorkspaceActionsViewModel(workspace, connectionManager, stateStore) {
    private val trigger = ReloadTrigger()

    val data: StateFlow<LoadState<T>> = connectionManager
        .loadWhenConnected(trigger.reloads) { load(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    protected abstract suspend fun load(api: PanelApi): T

    fun reload() = trigger.reload()

    protected fun actAndReload(successMessage: String, action: suspend (PanelApi) -> Unit) =
        act(successMessage, onSuccess = ::reload, action = action)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

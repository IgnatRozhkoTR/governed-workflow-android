package com.ignatrozhko.governedworkflow.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.StateFetch
import com.ignatrozhko.governedworkflow.data.api.WorkspaceContext
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class OverviewData(
    val state: WorkspaceState? = null,
    val context: WorkspaceContext? = null,
    val loadError: String? = null,
)

/** Workspace status and gate actions, mirroring the VS Code extension's workspace panel. */
class OverviewViewModel(private val workspace: WorkspaceRef, private val connectionManager: ConnectionManager) : ViewModel() {
    private val pollNow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _isBusy = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val data: StateFlow<OverviewData> = connectionManager.state
        .pollWhileConnected()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OverviewData())

    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun approve(commitMessage: String) = act("Approved") { it.approve(workspace, commitMessage) }

    fun reject(comments: String) = act("Rejected") { it.reject(workspace, comments) }

    fun setYolo(enabled: Boolean) = act(if (enabled) "YOLO mode on" else "YOLO mode off") { it.setYolo(workspace, enabled) }

    fun notifyClaude(message: NotifyMessage) = act("Claude notified") { it.notifyClaude(workspace, message.text) }

    private fun act(successMessage: String, action: suspend (PanelApi) -> Unit) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val outcome = try {
                action(connectionManager.requireApi())
                successMessage
            } catch (error: PanelException) {
                error.message.orEmpty()
            }
            _isBusy.value = false
            _messages.send(outcome)
            pollNow.tryEmit(Unit)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<ConnectionState>.pollWhileConnected(): Flow<OverviewData> = transformLatest { connection ->
        if (connection !is ConnectionState.Connected) return@transformLatest
        var current = OverviewData(context = loadContext(connection.api))
        var etag: String? = null
        while (true) {
            current = try {
                when (val fetch = connection.api.getState(workspace, etag)) {
                    StateFetch.NotModified -> current.copy(loadError = null)
                    is StateFetch.Fresh -> current.copy(state = fetch.state, loadError = null).also { etag = fetch.etag }
                }
            } catch (error: PanelException) {
                current.copy(loadError = error.message)
            }
            emit(current)
            withTimeoutOrNull(POLL_INTERVAL_MS) { pollNow.first() }
        }
    }

    /** The ticket is decoration: without it the overview still works, so a failure only hides the ticket line. */
    private suspend fun loadContext(api: PanelApi): WorkspaceContext? = try {
        api.getContext(workspace)
    } catch (error: PanelException) {
        null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 3_000L
    }
}

/** The two canned nudges the VS Code extension sends into the Claude session. */
enum class NotifyMessage(val label: String, val text: String) {
    Comments("Check comments", "I left review comments for you to address. Check workspace_get_comments for details."),
    Proceed("Go", "You can proceed. Continue with the current task."),
}

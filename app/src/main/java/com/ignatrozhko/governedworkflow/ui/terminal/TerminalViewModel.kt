package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.connection.NetworkMonitor
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.settings.AppSettings
import com.ignatrozhko.governedworkflow.data.settings.ExtraKeysMode
import com.ignatrozhko.governedworkflow.terminal.core.StickyKey
import com.ignatrozhko.governedworkflow.terminal.core.StickyModifiers
import com.ignatrozhko.governedworkflow.terminal.core.TerminalConnectionState
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInputController
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSession
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSocketOpener
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkspaceSessionStatus { Checking, Missing, Live }

data class TerminalUiState(
    val selected: TerminalTarget,
    val targets: List<TerminalTarget>,
    val workspaceStatus: WorkspaceSessionStatus = WorkspaceSessionStatus.Checking,
    val isBusy: Boolean = false,
)

/**
 * Owns the workspace screen's terminal sessions: which tmux sessions exist, which one is shown, and keeping
 * their sockets alive across URL switches, network changes and short trips to the background.
 */
class TerminalViewModel(
    workspace: WorkspaceRef,
    private val connectionManager: ConnectionManager,
    networkMonitor: NetworkMonitor,
    private val settings: AppSettings,
    opener: TerminalSocketOpener,
) : ViewModel(), DefaultLifecycleObserver {
    private val workspaceTarget = TerminalTarget.WorkspaceSession(workspace)
    private val pool = TerminalSessionPool(opener, viewModelScope)
    private val _uiState = MutableStateFlow(TerminalUiState(selected = workspaceTarget, targets = listOf(workspaceTarget)))
    private val _messages = Channel<String>(Channel.BUFFERED)
    private var backgroundPauseJob: Job? = null

    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()
    val messages: Flow<String> = _messages.receiveAsFlow()
    val stickyModifiers = MutableStateFlow(StickyModifiers())
    val fontSize: StateFlow<Int?> = settings.terminalFontSize.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val extraKeysMode: StateFlow<ExtraKeysMode?> = settings.extraKeysMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        followActivePanel()
        viewModelScope.launch { networkMonitor.changes.collect { pool.retryWaiting() } }
        viewModelScope.launch {
            pool.sessionFor(workspaceTarget).state.filterIsInstance<TerminalConnectionState.Rejected>().collect { refreshSessions() }
        }
    }

    fun sessionFor(target: TerminalTarget): TerminalSession = pool.sessionFor(target)

    fun inputFor(session: TerminalSession) = TerminalInputController(session, stickyModifiers)

    fun tapSticky(key: StickyKey) = stickyModifiers.update { it.tap(key) }

    fun setFontSize(size: Int) {
        viewModelScope.launch { settings.setTerminalFontSize(size) }
    }

    fun setExtraKeysMode(mode: ExtraKeysMode) {
        viewModelScope.launch { settings.setExtraKeysMode(mode) }
    }

    fun select(target: TerminalTarget) {
        _uiState.update { it.copy(selected = target) }
        if (target is TerminalTarget.NamedSession || _uiState.value.workspaceStatus == WorkspaceSessionStatus.Live) {
            pool.sessionFor(target).connect()
        }
    }

    fun refreshSessions() {
        viewModelScope.launch {
            runAction { api ->
                val status = api.terminalStatus(workspaceTarget.workspace)
                val others = api.listTerminalSessions().map { it.name }.filter { it != status.session }
                applySessions(status.exists, others.map(TerminalTarget::NamedSession))
            }
        }
    }

    /** Attaches to the workspace's Claude session, creating it with `--resume` semantics when missing. */
    fun resumeWorkspaceSession() = launchAction { api ->
        api.terminalResume(workspaceTarget.workspace)
        markWorkspaceLive()
    }

    /** Kills any running Claude session of this workspace and starts a new conversation. */
    fun startFreshWorkspaceSession() = launchAction { api ->
        api.terminalStart(workspaceTarget.workspace)
        markWorkspaceLive()
    }

    fun killSelected() {
        when (val target = _uiState.value.selected) {
            is TerminalTarget.WorkspaceSession -> launchAction { api ->
                api.terminalKill(target.workspace)
                _uiState.update { it.copy(workspaceStatus = WorkspaceSessionStatus.Missing) }
            }
            is TerminalTarget.NamedSession -> launchAction { api ->
                api.killTerminalSession(target.name)
                _uiState.update { it.copy(selected = workspaceTarget, targets = it.targets - target) }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        backgroundPauseJob?.cancel()
        pool.resumePaused()
        pool.retryWaiting()
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundPauseJob = viewModelScope.launch {
            delay(BACKGROUND_GRACE_MS)
            pool.pauseActive()
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        pool.closeAll()
    }

    /** Re-lists sessions whenever a panel becomes reachable and moves live sockets when its URL changes. */
    private fun followActivePanel() {
        viewModelScope.launch {
            var previousUrl: String? = null
            connectionManager.state
                .map { (it as? ConnectionState.Connected)?.baseUrl }
                .distinctUntilChanged()
                .collect { url ->
                    if (url == null) return@collect
                    if (previousUrl != null) pool.reconnectActive()
                    previousUrl = url
                    refreshSessions()
                }
        }
    }

    private fun applySessions(isWorkspaceLive: Boolean, others: List<TerminalTarget>) {
        val status = if (isWorkspaceLive) WorkspaceSessionStatus.Live else WorkspaceSessionStatus.Missing
        _uiState.update { state ->
            val selected = state.selected.takeIf { it == workspaceTarget || it in others } ?: workspaceTarget
            state.copy(selected = selected, targets = listOf(workspaceTarget) + others, workspaceStatus = status)
        }
        if (isWorkspaceLive) pool.sessionFor(workspaceTarget).connect()
        val selected = _uiState.value.selected
        if (selected is TerminalTarget.NamedSession) pool.sessionFor(selected).connect()
    }

    private fun markWorkspaceLive() {
        _uiState.update { it.copy(selected = workspaceTarget, workspaceStatus = WorkspaceSessionStatus.Live) }
        pool.sessionFor(workspaceTarget).reconnectNow()
    }

    private fun launchAction(action: suspend (PanelApi) -> Unit) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            runAction(action)
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun runAction(action: suspend (PanelApi) -> Unit) {
        try {
            action(connectionManager.requireApi())
        } catch (error: PanelException) {
            _messages.send(error.message.orEmpty())
        }
    }

    private companion object {
        const val BACKGROUND_GRACE_MS = 2 * 60 * 1000L
    }
}

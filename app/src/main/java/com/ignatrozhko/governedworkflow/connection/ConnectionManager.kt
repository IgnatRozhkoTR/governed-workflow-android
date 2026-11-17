package com.ignatrozhko.governedworkflow.connection

import com.ignatrozhko.governedworkflow.data.api.NotConnectedException
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.server.ServerCredentials
import com.ignatrozhko.governedworkflow.data.server.ServerEntry
import com.ignatrozhko.governedworkflow.data.server.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface ConnectionState {
    data object NoServer : ConnectionState
    data class Probing(val server: ServerEntry) : ConnectionState
    data class Connected(val server: ServerEntry, val baseUrl: String, val api: PanelApi) : ConnectionState
    data class TokenRejected(val server: ServerEntry, val baseUrl: String) : ConnectionState
    data class Unreachable(val server: ServerEntry, val results: List<CandidateResult>) : ConnectionState
}

/**
 * Keeps the selected server bound to its best reachable base URL, re-probing on launch, on every
 * default-network change and on demand; a manual switch holds until the next network change.
 */
class ConnectionManager(
    private val scope: CoroutineScope,
    private val repository: ServerRepository,
    private val probe: PanelProbe,
    private val networkMonitor: NetworkMonitor,
    private val apiFactory: (baseUrl: String, token: String) -> PanelApi,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.NoServer)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var credentials: ServerCredentials? = null
    private var boundCredentials: ServerCredentials? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        scope.launch {
            combine(
                repository.selectedCredentials,
                networkMonitor.changes.onStart { emit(Unit) },
                refreshRequests.onStart { emit(Unit) },
            ) { selected, _, _ -> selected }
                .collectLatest(::connect)
        }
    }

    fun refresh() {
        refreshRequests.tryEmit(Unit)
    }

    /** Pins a specific candidate URL if it answers with the stored token. */
    suspend fun switchTo(baseUrl: String): ProbeOutcome {
        val current = credentials ?: throw NotConnectedException()
        val outcome = probe.probe(baseUrl, current.token)
        if (outcome == ProbeOutcome.Reachable) {
            boundCredentials = current
            _state.value = ConnectionState.Connected(current.server, baseUrl, apiFactory(baseUrl, current.token))
        }
        return outcome
    }

    fun requireApi(): PanelApi =
        (state.value as? ConnectionState.Connected)?.api ?: throw NotConnectedException()

    /** For background work that can start in a cold process, before the first probe has bound a URL. */
    suspend fun awaitApi(timeoutMillis: Long): PanelApi =
        withTimeoutOrNull(timeoutMillis) { state.filterIsInstance<ConnectionState.Connected>().first().api }
            ?: throw NotConnectedException()

    private suspend fun connect(selected: ServerCredentials?) {
        credentials = selected
        if (selected == null) {
            _state.value = ConnectionState.NoServer
            return
        }
        val current = _state.value
        val isAlreadyServing = current is ConnectionState.Connected && current.server.id == selected.server.id
        if (!isAlreadyServing) _state.value = ConnectionState.Probing(selected.server)
        val selection = CandidateSelector.select(selected.server.urls) { url -> probe.probe(url, selected.token) }
        _state.value = toState(selected, selection)
    }

    private fun toState(selected: ServerCredentials, selection: CandidateSelection): ConnectionState = when (selection) {
        is CandidateSelection.Chosen -> reuseOrCreate(selected, selection.url)
        is CandidateSelection.TokenRejected -> ConnectionState.TokenRejected(selected.server, selection.url)
        is CandidateSelection.NoneReachable -> ConnectionState.Unreachable(selected.server, selection.results)
    }

    /** Re-probes that land on the same binding keep the same state instance so collectors see no change. */
    private fun reuseOrCreate(selected: ServerCredentials, url: String): ConnectionState.Connected {
        val current = _state.value
        if (current is ConnectionState.Connected && current.baseUrl == url && boundCredentials == selected) return current
        boundCredentials = selected
        return ConnectionState.Connected(selected.server, url, apiFactory(url, selected.token))
    }
}

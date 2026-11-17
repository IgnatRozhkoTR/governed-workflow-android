package com.ignatrozhko.governedworkflow.data.workspace

import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.StateFetch
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

data class StateSnapshot(val state: WorkspaceState? = null, val error: String? = null)

/**
 * One ETag poller per workspace, shared by every section that renders it, so opening several sections
 * costs one request every few seconds; polling stops shortly after the last screen stops observing.
 */
class WorkspaceStateStore(private val connectionManager: ConnectionManager, private val scope: CoroutineScope) {
    private val pollers = ConcurrentHashMap<WorkspaceRef, Poller>()

    fun observe(workspace: WorkspaceRef): StateFlow<StateSnapshot> = poller(workspace).snapshots

    fun refresh(workspace: WorkspaceRef) {
        poller(workspace).pollNow.tryEmit(Unit)
    }

    private fun poller(workspace: WorkspaceRef): Poller = pollers.getOrPut(workspace) { Poller(workspace) }

    private inner class Poller(private val workspace: WorkspaceRef) {
        val pollNow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val snapshots: StateFlow<StateSnapshot> = connectionManager.state
            .pollWhileConnected()
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), StateSnapshot())

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun Flow<ConnectionState>.pollWhileConnected(): Flow<StateSnapshot> = transformLatest { connection ->
            if (connection !is ConnectionState.Connected) return@transformLatest
            var current = StateSnapshot()
            var etag: String? = null
            while (true) {
                current = try {
                    when (val fetch = connection.api.getState(workspace, etag)) {
                        StateFetch.NotModified -> current.copy(error = null)
                        is StateFetch.Fresh -> StateSnapshot(fetch.state).also { etag = fetch.etag }
                    }
                } catch (error: PanelException) {
                    current.copy(error = error.message)
                }
                emit(current)
                withTimeoutOrNull(POLL_INTERVAL_MS) { pollNow.first() }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 3_000L
    }
}

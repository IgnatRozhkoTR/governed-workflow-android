package com.ignatrozhko.governedworkflow.ui.common

import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

sealed interface LoadState<out T> {
    data object Offline : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val data: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

/** Loads with the active panel whenever the binding or [reloads] change; [LoadState.Offline] while not connected. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> ConnectionManager.loadWhenConnected(reloads: Flow<Int>, load: suspend (PanelApi) -> T): Flow<LoadState<T>> =
    combine(state, reloads) { connection, _ -> connection }
        .transformLatest { connection ->
            if (connection !is ConnectionState.Connected) {
                emit(LoadState.Offline)
                return@transformLatest
            }
            emit(LoadState.Loading)
            emit(
                try {
                    LoadState.Loaded(load(connection.api))
                } catch (error: PanelException) {
                    LoadState.Failed(error.message.orEmpty())
                },
            )
        }

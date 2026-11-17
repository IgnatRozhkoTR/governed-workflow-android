package com.ignatrozhko.governedworkflow.ui.changes

import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.Annotation
import com.ignatrozhko.governedworkflow.data.api.CommitInfo
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.DiffSource
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.deleteAnnotation
import com.ignatrozhko.governedworkflow.data.api.getDiff
import com.ignatrozhko.governedworkflow.data.api.getHistory
import com.ignatrozhko.governedworkflow.data.api.listAnnotations
import com.ignatrozhko.governedworkflow.data.api.updateAnnotation
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.ReloadTrigger
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import com.ignatrozhko.governedworkflow.ui.workspace.common.WorkspaceActionsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The Changes tab: which diff is shown, its files, the open file and that file's annotations. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangesViewModel(workspace: WorkspaceRef, connectionManager: ConnectionManager, stateStore: WorkspaceStateStore) :
    WorkspaceActionsViewModel(workspace, connectionManager, stateStore) {
    private val diffReloads = ReloadTrigger()
    private val annotationReloads = ReloadTrigger()
    private val _source = MutableStateFlow<DiffSource>(DiffSource.Branch)
    private val _openPath = MutableStateFlow<String?>(null)

    val source: StateFlow<DiffSource> = _source.asStateFlow()
    val openPath: StateFlow<String?> = _openPath.asStateFlow()

    val files: StateFlow<LoadState<List<DiffFile>>> = _source
        .flatMapLatest { source -> connectionManager.loadWhenConnected(diffReloads.reloads) { it.getDiff(workspace, source) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    val history: StateFlow<LoadState<List<CommitInfo>>> = connectionManager
        .loadWhenConnected(diffReloads.reloads) { it.getHistory(workspace) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    val annotations: StateFlow<List<Annotation>> = combine(_openPath, _source) { path, source -> path to source }
        .flatMapLatest { (path, source) ->
            if (path == null) flowOf(LoadState.Loaded(emptyList())) else loadAnnotations(path, source)
        }
        .map { (it as? LoadState.Loaded)?.data.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Annotations are optional context: a file the server cannot anchor them to simply shows none. */
    private fun loadAnnotations(path: String, source: DiffSource) =
        connectionManager.loadWhenConnected(annotationReloads.reloads) {
            it.listAnnotations(workspace, path, (source as? DiffSource.Commit)?.sha)
        }

    fun selectSource(source: DiffSource) {
        _source.value = source
        _openPath.value = null
    }

    fun openFile(path: String) {
        _openPath.value = path
    }

    fun closeFile() {
        _openPath.value = null
    }

    fun reload() = diffReloads.reload()

    fun editAnnotation(annotationId: Long, text: String) =
        act("Annotation saved", onSuccess = annotationReloads::reload) { it.updateAnnotation(workspace, annotationId, text) }

    fun deleteAnnotation(annotationId: Long) =
        act("Annotation deleted", onSuccess = annotationReloads::reload) { it.deleteAnnotation(workspace, annotationId) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

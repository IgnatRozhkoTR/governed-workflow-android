package com.ignatrozhko.governedworkflow.ui.changes

import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.Annotation
import com.ignatrozhko.governedworkflow.data.api.CodeRepo
import com.ignatrozhko.governedworkflow.data.api.CommitInfo
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.DiffSource
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.deleteAnnotation
import com.ignatrozhko.governedworkflow.data.api.getDiff
import com.ignatrozhko.governedworkflow.data.api.getHistory
import com.ignatrozhko.governedworkflow.data.api.listAnnotations
import com.ignatrozhko.governedworkflow.data.api.listRepos
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The Changes tab: which repository and diff are shown, their files, the open file and that file's annotations. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangesViewModel(workspace: WorkspaceRef, connectionManager: ConnectionManager, stateStore: WorkspaceStateStore) :
    WorkspaceActionsViewModel(workspace, connectionManager, stateStore) {
    private val repoReloads = ReloadTrigger()
    private val diffReloads = ReloadTrigger()
    private val annotationReloads = ReloadTrigger()
    private val _chosenRepoPath = MutableStateFlow<String?>(null)
    private val _source = MutableStateFlow<DiffSource>(DiffSource.Branch)
    private val _openPath = MutableStateFlow<String?>(null)

    val source: StateFlow<DiffSource> = _source.asStateFlow()
    val openPath: StateFlow<String?> = _openPath.asStateFlow()

    val repos: StateFlow<LoadState<List<CodeRepo>>> = connectionManager
        .loadWhenConnected(repoReloads.reloads) { it.listRepos(workspace) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    /** Empty while the repositories load, and while a multi-repo workspace waits for the user to pick one. */
    val selectedRepo: StateFlow<CodeRepo?> = combine(repos, _chosenRepoPath) { repos, chosenPath ->
        (repos as? LoadState.Loaded)?.data?.let { resolveRepo(it, chosenPath) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val files: StateFlow<LoadState<List<DiffFile>>> = combine(selectedRepo.filterNotNull(), _source, ::Pair)
        .flatMapLatest { (repo, source) -> connectionManager.loadWhenConnected(diffReloads.reloads) { it.getDiff(workspace, repo, source) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    val history: StateFlow<LoadState<List<CommitInfo>>> = selectedRepo.filterNotNull()
        .flatMapLatest { repo -> connectionManager.loadWhenConnected(diffReloads.reloads) { it.getHistory(workspace, repo) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LoadState.Loading)

    val annotations: StateFlow<List<Annotation>> = combine(_openPath, _source, selectedRepo, ::Triple)
        .flatMapLatest { (path, source, repo) ->
            if (path == null || repo == null) flowOf(LoadState.Loaded(emptyList())) else loadAnnotations(repo, path, source)
        }
        .map { (it as? LoadState.Loaded)?.data.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** Annotations are optional context: a file the server cannot anchor them to simply shows none. */
    private fun loadAnnotations(repo: CodeRepo, path: String, source: DiffSource) =
        connectionManager.loadWhenConnected(annotationReloads.reloads) {
            it.listAnnotations(workspace, repo, path, (source as? DiffSource.Commit)?.sha)
        }

    /**
     * A single-repo workspace defaults to its own root; a multi-repo one has no root repository, so it defaults only
     * when exactly one repository is attached and otherwise waits for a choice instead of asking the panel blind.
     */
    private fun resolveRepo(repos: List<CodeRepo>, chosenPath: String?): CodeRepo? =
        repos.firstOrNull { it.path == chosenPath } ?: repos.singleOrNull() ?: repos.firstOrNull { it.queryValue == null }

    /** Commits belong to one repository, so a commit picked in another falls back to the branch diff. */
    fun selectRepo(repo: CodeRepo) {
        _chosenRepoPath.value = repo.path
        _source.value = DiffSource.Branch
        _openPath.value = null
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

    fun reload() = if (repos.value is LoadState.Loaded) diffReloads.reload() else repoReloads.reload()

    fun editAnnotation(annotationId: Long, text: String) =
        act("Annotation saved", onSuccess = annotationReloads::reload) { it.updateAnnotation(workspace, annotationId, text) }

    fun deleteAnnotation(annotationId: Long) =
        act("Annotation deleted", onSuccess = annotationReloads::reload) { it.deleteAnnotation(workspace, annotationId) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

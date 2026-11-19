package com.ignatrozhko.governedworkflow.ui.workspace.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.ReviewResolution
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.addComment
import com.ignatrozhko.governedworkflow.data.api.resolveAllReviewIssues
import com.ignatrozhko.governedworkflow.data.api.resolveComment
import com.ignatrozhko.governedworkflow.data.workspace.StateSnapshot
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base for workspace sections: the shared polled state plus one-at-a-time mutations that report their
 * outcome as a snackbar message and re-poll; busy lasts until that poll has published, so the change is on screen when buttons re-enable.
 */
abstract class WorkspaceActionsViewModel(
    val workspace: WorkspaceRef,
    protected val connectionManager: ConnectionManager,
    private val stateStore: WorkspaceStateStore,
) : ViewModel() {
    private val _isBusy = MutableStateFlow(false)
    private val _messages = Channel<String>(Channel.BUFFERED)

    val snapshot: StateFlow<StateSnapshot> = stateStore.observe(workspace)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()
    val messages: Flow<String> = _messages.receiveAsFlow()

    protected fun act(successMessage: String, onSuccess: () -> Unit = {}, action: suspend (PanelApi) -> Unit) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            val outcome = try {
                action(connectionManager.requireApi())
                onSuccess()
                successMessage
            } catch (error: PanelException) {
                error.message.orEmpty()
            }
            _messages.send(outcome)
            stateStore.refreshAndAwait(workspace)
            _isBusy.value = false
        }
    }

    fun addComment(comment: NewComment) = act("Comment added") { it.addComment(workspace, comment) }

    fun resolveComment(commentId: Long, isResolved: Boolean) =
        act(if (isResolved) "Resolved" else "Reopened") { it.resolveComment(workspace, commentId, isResolved) }

    /** Resolves a batch one by one, as the web panel's per-file "resolve all" does; the first failure stops the rest. */
    fun resolveComments(commentIds: List<Long>) = act("Resolved ${commentIds.size} comments") { api ->
        commentIds.forEach { api.resolveComment(workspace, it, isResolved = true) }
    }

    fun resolveAllReviewIssues(resolution: ReviewResolution) {
        val workspaceId = snapshot.value.state?.workspaceId ?: return
        act("All review issues resolved") { it.resolveAllReviewIssues(workspaceId, resolution) }
    }
}

package com.ignatrozhko.governedworkflow.ui.workspace

import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.PlanDecision
import com.ignatrozhko.governedworkflow.data.api.WorkspaceContext
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.addDiscussion
import com.ignatrozhko.governedworkflow.data.api.archiveWorkspace
import com.ignatrozhko.governedworkflow.data.api.listPhases
import com.ignatrozhko.governedworkflow.data.api.replyToDiscussion
import com.ignatrozhko.governedworkflow.data.api.setPhase
import com.ignatrozhko.governedworkflow.data.api.setPlanStatus
import com.ignatrozhko.governedworkflow.data.api.setResearchProven
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.loadWhenConnected
import com.ignatrozhko.governedworkflow.ui.workspace.common.WorkspaceActionsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Mutations that act on the polled workspace state: gates, plan, phase, research and discussions. */
class WorkspaceViewModel(
    workspace: WorkspaceRef,
    connectionManager: ConnectionManager,
    stateStore: WorkspaceStateStore,
) : WorkspaceActionsViewModel(workspace, connectionManager, stateStore) {
    private val _context = MutableStateFlow<WorkspaceContext?>(null)
    private val _isArchived = MutableStateFlow(false)
    private val _phaseNames = MutableStateFlow(PhaseNames(emptyList()))

    val context: StateFlow<WorkspaceContext?> = _context.asStateFlow()
    val isArchived: StateFlow<Boolean> = _isArchived.asStateFlow()
    val phaseNames: StateFlow<PhaseNames> = _phaseNames.asStateFlow()

    init {
        viewModelScope.launch {
            _context.value = loadOrNull { it.getContext(workspace) }
            loadOrNull { it.listPhases() }?.let { _phaseNames.value = PhaseNames(it) }
        }
    }

    /**
     * Ticket and phase names only decorate the screens, so a failure leaves them out instead of blocking.
     * The screen can open before the first probe binds a server (a notification tap on a cold start), so the
     * load waits for a connection instead of giving up on the first try.
     */
    private suspend fun <T> loadOrNull(load: suspend (PanelApi) -> T): T? =
        connectionManager.loadWhenConnected(flowOf(0), load)
            .first { it is LoadState.Loaded || it is LoadState.Failed }
            .let { (it as? LoadState.Loaded)?.data }

    fun approveGate(commitMessage: String) = act("Approved") { it.approve(workspace, commitMessage) }

    fun rejectGate(comments: String) = act("Rejected") { it.reject(workspace, comments) }

    fun setYolo(enabled: Boolean) = act(if (enabled) "YOLO mode on" else "YOLO mode off") { it.setYolo(workspace, enabled) }

    fun notifyClaude(message: NotifyMessage) = act("Claude notified") { it.notifyClaude(workspace, message.text) }

    fun decidePlan(decision: PlanDecision) = act(if (decision == PlanDecision.Approve) "Plan approved" else "Plan rejected") {
        it.setPlanStatus(workspace, decision)
    }

    fun setPhase(phase: String) = act("Moved to phase $phase") { it.setPhase(workspace, phase) }

    fun setResearchProven(researchId: Long, isProven: Boolean) =
        act(if (isProven) "Marked proven" else "Marked not proven") { it.setResearchProven(workspace, researchId, isProven) }

    fun addDiscussion(text: String) = act("Question posted") { it.addDiscussion(workspace, text) }

    fun replyToDiscussion(discussionId: Long, text: String) = act("Reply posted") { it.replyToDiscussion(workspace, discussionId, text) }

    fun archive() = act("Workspace archived", onSuccess = { _isArchived.value = true }) { it.archiveWorkspace(workspace) }
}

/** The two canned nudges the VS Code extension sends into the Claude session. */
enum class NotifyMessage(val label: String, val text: String) {
    Comments("Check comments", "I left review comments for you to address. Check workspace_get_comments for details."),
    Proceed("Go", "You can proceed. Continue with the current task."),
}

package com.ignatrozhko.governedworkflow.gates

import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.StateFetch
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Reads every live workspace of every project and returns the ones waiting on a user gate. */
class GateScanner {
    suspend fun scan(api: PanelApi): List<WaitingGate> {
        val workspaces = api.listProjects().flatMap { project ->
            api.listWorkspaces(project.id).filterNot { it.isArchived }.map { WorkspaceRef(project.id, it.branch) }
        }
        val permits = Semaphore(MAX_PARALLEL_STATE_FETCHES)
        return coroutineScope {
            workspaces.map { workspace -> async { permits.withPermit { waitingGate(api, workspace) } } }.awaitAll().filterNotNull()
        }
    }

    /** One unreadable workspace must not hide the gates of all the others, so its failure only drops it from this scan. */
    private suspend fun waitingGate(api: PanelApi, workspace: WorkspaceRef): WaitingGate? = try {
        val gate = (api.getState(workspace, etag = null) as? StateFetch.Fresh)?.state?.pendingGate
        gate?.let { WaitingGate(workspace, it.phase, ticketLabel(api, workspace)) }
    } catch (error: PanelException) {
        null
    }

    /** The ticket id only makes the notification friendlier; the branch identifies the workspace just as well. */
    private suspend fun ticketLabel(api: PanelApi, workspace: WorkspaceRef): String = try {
        api.getContext(workspace).ticketId.ifBlank { workspace.branch }
    } catch (error: PanelException) {
        workspace.branch
    }

    private companion object {
        const val MAX_PARALLEL_STATE_FETCHES = 4
    }
}

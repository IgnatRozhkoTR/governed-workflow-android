package com.ignatrozhko.governedworkflow.gates

import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.StateFetch
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.currentPhaseSince
import com.ignatrozhko.governedworkflow.data.api.listPhases
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads the live workspaces of every project and returns the ones waiting on a user gate. Only workspaces whose listed
 * phase is a gate phase are read, and a state the panel reports as unchanged keeps the result of the previous scan.
 */
class GateScanner {
    private val previous = ConcurrentHashMap<ScanKey, ScannedState>()

    suspend fun scan(api: PanelApi): List<WaitingGate> {
        val phaseNames = PhaseNames(api.listPhases())
        val permits = Semaphore(MAX_PARALLEL_REQUESTS)
        return coroutineScope {
            val candidates = api.listProjects()
                .map { project -> async { permits.withPermit { gateCandidates(api, project.id, phaseNames) } } }
                .awaitAll()
                .flatten()
            previous.keys.retainAll(candidates.map { ScanKey(api.baseUrl, it) }.toSet())
            candidates.map { workspace -> async { permits.withPermit { waitingGate(api, workspace) } } }.awaitAll().filterNotNull()
        }
    }

    private suspend fun gateCandidates(api: PanelApi, projectId: String, phaseNames: PhaseNames): List<WorkspaceRef> =
        api.listWorkspaces(projectId)
            .filter { !it.isArchived && phaseNames.canHoldGate(it.phase) }
            .map { WorkspaceRef(projectId, it.branch) }

    /** One unreadable workspace must not hide the gates of all the others, so its failure only drops it from this scan. */
    private suspend fun waitingGate(api: PanelApi, workspace: WorkspaceRef): WaitingGate? = try {
        val key = ScanKey(api.baseUrl, workspace)
        when (val fetch = api.getState(workspace, previous[key]?.etag)) {
            StateFetch.NotModified -> previous[key]?.gate
            is StateFetch.Fresh -> {
                val gate = fetch.state.pendingGate?.let {
                    WaitingGate(workspace, it.phase, ticketLabel(api, workspace), fetch.state.currentPhaseSince())
                }
                fetch.etag?.let { previous[key] = ScannedState(it, gate) } ?: previous.remove(key)
                gate
            }
        }
    } catch (error: PanelException) {
        null
    }

    /** The ticket id only makes the notification friendlier; the branch identifies the workspace just as well. */
    private suspend fun ticketLabel(api: PanelApi, workspace: WorkspaceRef): String = try {
        api.getContext(workspace).ticketId.ifBlank { workspace.branch }
    } catch (error: PanelException) {
        workspace.branch
    }

    private data class ScanKey(val baseUrl: HttpUrl, val workspace: WorkspaceRef)

    private class ScannedState(val etag: String, val gate: WaitingGate?)

    private companion object {
        const val MAX_PARALLEL_REQUESTS = 6
    }
}

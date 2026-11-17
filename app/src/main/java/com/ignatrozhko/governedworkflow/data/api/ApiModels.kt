package com.ignatrozhko.governedworkflow.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class WorkspaceRef(val projectId: String, val branch: String)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val path: String = "",
    @SerialName("project_type") val projectType: String = "single",
)

@Serializable
internal data class ProjectsResponse(val projects: List<Project>)

@Serializable
data class WorkspaceSummary(
    val id: String,
    @SerialName("workspace_id") val workspaceId: Long,
    val branch: String,
    val phase: String,
    val status: String,
    @SerialName("working_dir") val workingDir: String = "",
    val created: String = "",
) {
    val isArchived: Boolean get() = status == ARCHIVED_STATUS

    private companion object {
        const val ARCHIVED_STATUS = "archived"
    }
}

@Serializable
internal data class WorkspacesResponse(val workspaces: List<WorkspaceSummary>)

@Serializable
data class PendingGate(
    val phase: String,
    @SerialName("accepts_commit_message") val acceptsCommitMessage: Boolean = false,
)

/** The subset of `GET /api/ws/<project>/<branch>/state` the mobile client renders. */
@Serializable
data class WorkspaceState(
    val phase: String,
    val status: String = "",
    val branch: String = "",
    @SerialName("source_branch") val sourceBranch: String? = null,
    @SerialName("workflow_mode") val workflowMode: String = "",
    @SerialName("plan_status") val planStatus: String? = null,
    @SerialName("yolo_mode") val yoloMode: Boolean = false,
    @SerialName("pending_gate") val pendingGate: PendingGate? = null,
)

sealed interface StateFetch {
    data object NotModified : StateFetch
    data class Fresh(val state: WorkspaceState, val etag: String?) : StateFetch
}

@Serializable
data class WorkspaceContext(
    @SerialName("ticket_id") val ticketId: String = "",
    @SerialName("ticket_name") val ticketName: String = "",
)

@Serializable
data class TerminalStatus(val session: String, val exists: Boolean)

@Serializable
data class TerminalStartResponse(val session: String, val status: String)

@Serializable
data class TerminalSessionInfo(
    val name: String,
    val attached: Boolean = false,
    val command: String = "",
)

@Serializable
data class ServerInfo(
    val hostname: String = "",
    val user: String = "",
    val platform: String = "",
)

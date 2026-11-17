package com.ignatrozhko.governedworkflow.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl

/** Workflow endpoints behind the workspace sections: plan, phase, research, discussions, criteria, proposals. */

suspend fun PanelApi.setPlanStatus(workspace: WorkspaceRef, status: PlanDecision) {
    post(workspace, "plan-status", buildJsonObject { put("status", status.wireValue) })
}

enum class PlanDecision(val wireValue: String) { Approve("approved"), Reject("rejected") }

suspend fun PanelApi.setPhase(workspace: WorkspaceRef, phase: String) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "phase"), buildJsonObject { put("phase", phase) })
}

suspend fun PanelApi.listPhases(): List<PhaseInfo> =
    get(PanelPaths.api(baseUrl, "api/phases/available"), PhasesResponse.serializer()).phases

suspend fun PanelApi.setResearchProven(workspace: WorkspaceRef, researchId: Long, isProven: Boolean) {
    post(workspace, "research/$researchId/prove", buildJsonObject { put("proven", isProven) })
}

suspend fun PanelApi.addDiscussion(workspace: WorkspaceRef, text: String) {
    post(workspace, "context/discussions", buildJsonObject { put("text", text) })
}

suspend fun PanelApi.replyToDiscussion(workspace: WorkspaceRef, discussionId: Long, text: String) {
    post(workspace, "context/discussions/$discussionId/reply", buildJsonObject { put("text", text) })
}

suspend fun PanelApi.listCriteria(workspace: WorkspaceRef): List<Criterion> =
    get(PanelPaths.workspace(baseUrl, workspace, "criteria"), CriteriaResponse.serializer()).criteria

suspend fun PanelApi.validateCriterion(workspace: WorkspaceRef, criterionId: Long, isPassed: Boolean) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "criteria/$criterionId/validate"), buildJsonObject { put("passed", isPassed) })
}

suspend fun PanelApi.listProposals(workspace: WorkspaceRef): List<Proposal> =
    get(PanelPaths.workspace(baseUrl, workspace, "proposals"), ProposalsResponse.serializer()).proposals

suspend fun PanelApi.resolveProposal(workspace: WorkspaceRef, proposalId: Long, decision: ProposalDecision) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "proposals/$proposalId/resolve"), buildJsonObject { put("status", decision.wireValue) })
}

/** The web panel offers exactly these two outcomes for an open proposal. */
enum class ProposalDecision(val wireValue: String) { MarkApplied("executed"), Reject("rejected") }

suspend fun PanelApi.listScratchpads(workspace: WorkspaceRef): List<ScratchpadFile> =
    get(PanelPaths.workspace(baseUrl, workspace, "scratchpads"), ScratchpadsResponse.serializer()).files

suspend fun PanelApi.readScratchpad(workspace: WorkspaceRef, name: String): String {
    val url = PanelPaths.workspace(baseUrl, workspace, "scratchpads/content").withQuery("name" to name)
    return get(url, ScratchpadContent.serializer()).content
}

suspend fun PanelApi.setWorkflowMode(workspace: WorkspaceRef, mode: String) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "workflow-mode"), buildJsonObject { put("mode", mode) })
}

suspend fun PanelApi.setReviewMode(workspace: WorkspaceRef, mode: String) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "review-mode"), buildJsonObject { put("mode", mode) })
}

suspend fun PanelApi.getProjectSettings(projectId: String): ProjectSettings =
    get(PanelPaths.project(baseUrl, projectId, "settings"), ProjectSettings.serializer())

suspend fun PanelApi.updateProjectSettings(projectId: String, settings: ProjectSettings) {
    val body = buildJsonObject {
        put("yolo_mode_default", settings.yoloModeDefault)
        put("fast_mode_default", settings.fastModeDefault)
        put("review_mode_default", settings.reviewModeDefault)
    }
    send("PUT", PanelPaths.project(baseUrl, projectId, "settings"), body)
}

/** Local and remote branches merged, in the server's order: the candidates a new workspace can start from. */
suspend fun PanelApi.listSourceBranches(projectId: String): List<String> {
    val branches = get(PanelPaths.project(baseUrl, projectId, "branches"), BranchesResponse.serializer())
    return (branches.local + branches.remote).distinct()
}

suspend fun PanelApi.createWorkspace(projectId: String, request: NewWorkspace): CreatedWorkspace {
    val body = buildJsonObject {
        put("branch", request.branch)
        put("source", request.sourceBranch)
        if (request.isYolo) put("yolo_mode", true)
        if (request.isFast) put("workflow_mode", FAST_WORKFLOW_MODE)
    }
    val result = send("POST", PanelPaths.project(baseUrl, projectId, "workspaces"), body)
    return decode(result, CreatedWorkspace.serializer())
}

/** The web panel's create form minus repo attachment: name, source branch and the two toggles; the rest uses project defaults. */
data class NewWorkspace(val branch: String, val sourceBranch: String, val isYolo: Boolean, val isFast: Boolean)

const val FAST_WORKFLOW_MODE = "fast"
const val STANDARD_WORKFLOW_MODE = "standard"

suspend fun PanelApi.archiveWorkspace(workspace: WorkspaceRef) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "archive"), PanelApi.EMPTY_BODY)
}

internal suspend fun PanelApi.post(workspace: WorkspaceRef, suffix: String, body: JsonObject) {
    send("POST", PanelPaths.workspace(baseUrl, workspace, suffix), body)
}

internal fun HttpUrl.withQuery(vararg parameters: Pair<String, String?>): HttpUrl =
    newBuilder().apply { parameters.forEach { (name, value) -> if (value != null) addQueryParameter(name, value) } }.build()

suspend fun PanelApi.getClaudeCommand(workspace: WorkspaceRef): String =
    get(PanelPaths.workspace(baseUrl, workspace, "command"), LaunchConfig.serializer()).claudeCommand

suspend fun PanelApi.setClaudeCommand(workspace: WorkspaceRef, command: String) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "command"), buildJsonObject { put("claude_command", command) })
}

suspend fun PanelApi.listModules(): List<ModuleInfo> =
    get(PanelPaths.api(baseUrl, "api/modules"), ModulesResponse.serializer()).modules

suspend fun PanelApi.listEnabledModules(): Set<String> =
    get(PanelPaths.api(baseUrl, "api/modules/enabled"), EnabledModulesResponse.serializer()).modules.toSet()

/** The server re-renders every project's Claude config after this call, so it affects all projects at once. */
suspend fun PanelApi.setEnabledModules(moduleIds: Set<String>) {
    val body = buildJsonObject { put("modules", JsonArray(moduleIds.map(::JsonPrimitive))) }
    send("POST", PanelPaths.api(baseUrl, "api/modules/enabled"), body)
}

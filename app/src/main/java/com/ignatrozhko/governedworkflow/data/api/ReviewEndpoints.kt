package com.ignatrozhko.governedworkflow.data.api

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Code review endpoints: diffs, history, line comments and annotations. */

suspend fun PanelApi.listRepos(workspace: WorkspaceRef): List<CodeRepo> =
    get(PanelPaths.workspace(baseUrl, workspace, "repos"), ReposResponse.serializer()).repos

suspend fun PanelApi.getDiff(workspace: WorkspaceRef, repo: CodeRepo, source: DiffSource): List<DiffFile> {
    val url = PanelPaths.workspace(baseUrl, workspace, "diff").withQuery(
        "mode" to source.modeParameter,
        "commit" to (source as? DiffSource.Commit)?.sha,
        "repo" to repo.queryValue,
    )
    return get(url, DiffResponse.serializer()).files
}

/** Which changes the Changes tab shows; the web panel's three modes. */
sealed interface DiffSource {
    val modeParameter: String?

    data object Uncommitted : DiffSource {
        override val modeParameter = "uncommitted"
    }

    data object Branch : DiffSource {
        override val modeParameter: String? = null
    }

    data class Commit(val sha: String, val subject: String) : DiffSource {
        override val modeParameter = "commit"
    }
}

suspend fun PanelApi.getHistory(workspace: WorkspaceRef, repo: CodeRepo): List<CommitInfo> {
    val url = PanelPaths.workspace(baseUrl, workspace, "history").withQuery("repo" to repo.queryValue)
    return get(url, HistoryResponse.serializer()).commits
}

suspend fun PanelApi.listComments(workspace: WorkspaceRef, includeResolved: Boolean): List<Comment> {
    val url = PanelPaths.workspace(baseUrl, workspace, "comments").withQuery("resolved" to if (includeResolved) "true" else null)
    return get(url, CommentsResponse.serializer()).comments
}

suspend fun PanelApi.addComment(workspace: WorkspaceRef, comment: NewComment) {
    val body = buildJsonObject {
        put("scope", comment.scope)
        put("target", comment.target)
        put("text", comment.text)
        comment.line?.let { line ->
            put("file_path", comment.target)
            put("line_start", line)
            put("line_end", line)
        }
    }
    post(workspace, "comments", body)
}

/** Scope and target follow the web panel's keys: `plan`/`task:<title>`, `research`/`Finding: …`, `review`/<file path>. */
data class NewComment(val scope: String, val target: String, val text: String, val line: Int? = null)

suspend fun PanelApi.resolveComment(workspace: WorkspaceRef, commentId: Long, isResolved: Boolean) {
    send("PUT", PanelPaths.workspace(baseUrl, workspace, "comments/$commentId/resolve"), buildJsonObject { put("resolved", isResolved) })
}

suspend fun PanelApi.resolveAllReviewIssues(workspaceId: Long, resolution: ReviewResolution) {
    val url = PanelPaths.api(baseUrl, "api/workspaces/$workspaceId/review-issues/resolve-all")
    send("POST", url, buildJsonObject { put("resolution", resolution.wireValue) })
}

enum class ReviewResolution(val wireValue: String, val label: String) {
    Fixed("fixed", "Fixed"),
    OutOfScope("out_of_scope", "Out of scope"),
    FalsePositive("false_positive", "False positive"),
}

/** Annotations are re-anchored to the working tree, or to [commitSha]'s content when a single commit is shown. */
suspend fun PanelApi.listAnnotations(workspace: WorkspaceRef, repo: CodeRepo, filePath: String, commitSha: String?): List<Annotation> {
    val url = PanelPaths.workspace(baseUrl, workspace, "annotations")
        .withQuery("file" to filePath, "ref" to commitSha, "repo" to repo.queryValue)
    return get(url, AnnotationsResponse.serializer()).annotations
}

suspend fun PanelApi.updateAnnotation(workspace: WorkspaceRef, annotationId: Long, text: String) {
    send("PATCH", PanelPaths.workspace(baseUrl, workspace, "annotations/$annotationId"), buildJsonObject { put("text", text) })
}

suspend fun PanelApi.deleteAnnotation(workspace: WorkspaceRef, annotationId: Long) {
    send("DELETE", PanelPaths.workspace(baseUrl, workspace, "annotations/$annotationId"), PanelApi.EMPTY_BODY)
}

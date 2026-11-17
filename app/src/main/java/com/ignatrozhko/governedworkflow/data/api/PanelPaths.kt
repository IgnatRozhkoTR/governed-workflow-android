package com.ignatrozhko.governedworkflow.data.api

import okhttp3.HttpUrl

/** URL building that mirrors the backend routes; branch names keep their `/` as path separators. */
internal object PanelPaths {
    private const val CLAUDE_SESSION_KIND = "claude"

    fun api(base: HttpUrl, path: String): HttpUrl = base.newBuilder().addPathSegments(path).build()

    fun projectWorkspaces(base: HttpUrl, projectId: String): HttpUrl =
        base.newBuilder().addPathSegments("api/projects").addPathSegment(projectId).addPathSegment("workspaces").build()

    fun project(base: HttpUrl, projectId: String, suffix: String): HttpUrl =
        base.newBuilder().addPathSegments("api/projects").addPathSegment(projectId).addPathSegments(suffix).build()

    fun workspace(base: HttpUrl, workspace: WorkspaceRef, suffix: String): HttpUrl =
        base.newBuilder()
            .addPathSegments("api/ws")
            .addPathSegment(workspace.projectId)
            .addBranchSegments(workspace.branch)
            .addPathSegments(suffix)
            .build()

    fun terminalSessionKill(base: HttpUrl, name: String): HttpUrl =
        base.newBuilder().addPathSegments("api/terminal/sessions").addPathSegment(name).addPathSegment("kill").build()

    /**
     * The backend also routes `/ws/terminal/<project>/<path:branch>/<kind>`, so a branch containing `/`
     * would be split into branch and kind; naming the default kind keeps the full branch intact.
     */
    fun workspaceTerminalSocket(base: HttpUrl, workspace: WorkspaceRef, token: String): HttpUrl =
        base.newBuilder()
            .addPathSegments("ws/terminal")
            .addPathSegment(workspace.projectId)
            .addBranchSegments(workspace.branch)
            .addPathSegment(CLAUDE_SESSION_KIND)
            .addQueryParameter("token", token)
            .build()

    fun namedTerminalSocket(base: HttpUrl, sessionName: String, token: String): HttpUrl =
        base.newBuilder()
            .addPathSegments("ws/terminal-session")
            .addPathSegment(sessionName)
            .addQueryParameter("token", token)
            .build()

    private fun HttpUrl.Builder.addBranchSegments(branch: String): HttpUrl.Builder = apply {
        branch.split('/').forEach(::addPathSegment)
    }
}

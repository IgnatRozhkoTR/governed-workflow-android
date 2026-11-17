package com.ignatrozhko.governedworkflow.terminal.core

import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import okhttp3.Request

/** A tmux session the backend can bridge over a WebSocket. */
sealed interface TerminalTarget {
    val key: String
    val title: String

    fun socketRequest(api: PanelApi): Request

    /** The workspace's own Claude session (`ws-<project>-<branch>`), created by Start or Resume. */
    data class WorkspaceSession(val workspace: WorkspaceRef) : TerminalTarget {
        override val key: String get() = "workspace:${workspace.projectId}/${workspace.branch}"
        override val title: String get() = "Claude"
        override fun socketRequest(api: PanelApi): Request = api.workspaceTerminalSocket(workspace)
    }

    /** Any other live tmux session on the panel host, attached by its exact name like the web panel's chips. */
    data class NamedSession(val name: String) : TerminalTarget {
        override val key: String get() = "session:$name"
        override val title: String get() = name
        override fun socketRequest(api: PanelApi): Request = api.namedTerminalSocket(name)
    }
}

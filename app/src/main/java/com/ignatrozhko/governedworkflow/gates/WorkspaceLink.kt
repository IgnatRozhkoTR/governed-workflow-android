package com.ignatrozhko.governedworkflow.gates

import android.content.Intent
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A request, usually from a notification, to open a workspace; the nav host consumes it once. */
class WorkspaceLinks {
    private val _pending = MutableStateFlow<WorkspaceRef?>(null)
    val pending: StateFlow<WorkspaceRef?> = _pending.asStateFlow()

    fun request(intent: Intent?) {
        intent?.let(WorkspaceLink::read)?.let { _pending.value = it }
    }

    fun consume() {
        _pending.value = null
    }
}

object WorkspaceLink {
    private const val EXTRA_PROJECT = "com.ignatrozhko.governedworkflow.PROJECT"
    private const val EXTRA_BRANCH = "com.ignatrozhko.governedworkflow.BRANCH"

    fun put(intent: Intent, workspace: WorkspaceRef): Intent =
        intent.putExtra(EXTRA_PROJECT, workspace.projectId).putExtra(EXTRA_BRANCH, workspace.branch)

    fun read(intent: Intent): WorkspaceRef? {
        val project = intent.getStringExtra(EXTRA_PROJECT) ?: return null
        val branch = intent.getStringExtra(EXTRA_BRANCH) ?: return null
        return WorkspaceRef(project, branch)
    }
}

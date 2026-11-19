package com.ignatrozhko.governedworkflow.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object ServersRoute

@Serializable
data class ServerEditorRoute(
    val serverId: String? = null,
    val prefillUrl: String? = null,
    val startWithQr: Boolean = false,
)

@Serializable
data object LanScanRoute

@Serializable
data object ProjectsRoute

@Serializable
data class WorkspacesRoute(val projectId: String, val projectName: String)

/** [opensOnDecision] lands on Overview, where a waiting gate is decided, instead of the terminal. */
@Serializable
data class WorkspaceRoute(
    val projectId: String,
    val branch: String,
    val initialSection: String? = null,
    val opensOnDecision: Boolean = false,
)

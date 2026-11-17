package com.ignatrozhko.governedworkflow.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ignatrozhko.governedworkflow.AppContainer
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.ui.projects.ProjectsScreen
import com.ignatrozhko.governedworkflow.ui.servers.LanScanScreen
import com.ignatrozhko.governedworkflow.ui.servers.ServerEditorScreen
import com.ignatrozhko.governedworkflow.ui.servers.ServersScreen
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceScreen
import com.ignatrozhko.governedworkflow.ui.workspace.WorkspaceSection
import com.ignatrozhko.governedworkflow.ui.workspaces.WorkspacesScreen
import kotlinx.coroutines.flow.first

@Composable
fun AppNavHost(container: AppContainer) {
    val startRoute by produceState<Any?>(initialValue = null) {
        value = if (container.serverRepository.selectedServerId.first() != null) ProjectsRoute else ServersRoute
    }
    val start = startRoute
    if (start == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val navController = rememberNavController()
    val linkedWorkspace by container.workspaceLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(linkedWorkspace) {
        linkedWorkspace?.let { workspace ->
            container.workspaceLinks.consume()
            navController.openLinkedWorkspace(workspace)
        }
    }
    NavHost(navController = navController, startDestination = start) {
        composable<ServersRoute> {
            ServersScreen(
                onServerChosen = navController::showProjectsAsRoot,
                onAddManually = { navController.navigate(ServerEditorRoute()) },
                onEdit = { id -> navController.navigate(ServerEditorRoute(serverId = id)) },
                onScanQr = { navController.navigate(ServerEditorRoute(startWithQr = true)) },
                onScanNetwork = { navController.navigate(LanScanRoute) },
                onBack = navController::popBackStack,
            )
        }
        composable<ServerEditorRoute> {
            ServerEditorScreen(onSaved = navController::showProjectsAsRoot, onBack = navController::popBackStack)
        }
        composable<LanScanRoute> {
            LanScanScreen(
                onPanelChosen = { url ->
                    navController.navigate(ServerEditorRoute(prefillUrl = url)) { popUpTo<LanScanRoute> { inclusive = true } }
                },
                onBack = navController::popBackStack,
            )
        }
        composable<ProjectsRoute> {
            ProjectsScreen(
                onOpenProject = { project -> navController.navigate(WorkspacesRoute(project.id, project.name)) },
                onManageServers = { navController.navigate(ServersRoute) },
            )
        }
        composable<WorkspacesRoute> { entry ->
            val route = entry.toRoute<WorkspacesRoute>()
            WorkspacesScreen(
                title = route.projectName,
                onOpenWorkspace = { branch -> navController.navigate(WorkspaceRoute(route.projectId, branch)) },
                onWorkspaceCreated = { branch ->
                    navController.navigate(WorkspaceRoute(route.projectId, branch, initialSection = WorkspaceSection.Settings.name))
                },
                onManageServers = { navController.navigate(ServersRoute) },
                onBack = navController::popBackStack,
            )
        }
        composable<WorkspaceRoute> { entry ->
            val route = entry.toRoute<WorkspaceRoute>()
            WorkspaceScreen(
                workspace = WorkspaceRef(route.projectId, route.branch),
                initialSection = route.initialSection?.let(WorkspaceSection::valueOf),
                onManageServers = { navController.navigate(ServersRoute) },
                onBack = navController::popBackStack,
            )
        }
    }
}

private fun NavHostController.showProjectsAsRoot() {
    navigate(ProjectsRoute) { popUpTo(graph.id) { inclusive = true } }
}

/**
 * Single-top matching compares only the destination, not its arguments, so it would reuse an open workspace's entry
 * (and its view models) for a different workspace. Only a link to the workspace already on top is a no-op.
 */
private fun NavHostController.openLinkedWorkspace(workspace: WorkspaceRef) {
    val top = currentBackStackEntry?.takeIf { it.destination.hasRoute<WorkspaceRoute>() }?.toRoute<WorkspaceRoute>()
    if (top?.projectId == workspace.projectId && top.branch == workspace.branch) return
    navigate(WorkspaceRoute(workspace.projectId, workspace.branch))
}

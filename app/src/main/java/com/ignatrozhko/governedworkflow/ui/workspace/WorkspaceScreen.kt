package com.ignatrozhko.governedworkflow.ui.workspace

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.ui.changes.ChangesTab
import com.ignatrozhko.governedworkflow.ui.changes.ChangesViewModel
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.PanelTopBar
import com.ignatrozhko.governedworkflow.ui.overview.OverviewTab
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalChrome
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalTab
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(workspace: WorkspaceRef, initialSection: WorkspaceSection?, onManageServers: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val workspaceViewModel = viewModel { WorkspaceViewModel(workspace, container.connectionManager, container.workspaceStateStore) }
    val changesViewModel = viewModel { ChangesViewModel(workspace, container.connectionManager, container.workspaceStateStore) }
    val terminalViewModel = viewModel {
        TerminalViewModel(workspace, container.connectionManager, container.networkMonitor, container.settings, container.terminalSocketOpener)
    }
    var tab by rememberSaveable { mutableStateOf(if (initialSection != null) WorkspaceTab.Overview else WorkspaceTab.Terminal) }
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var isTerminalFullScreen by rememberSaveable { mutableStateOf(false) }
    val openPath by changesViewModel.openPath.collectAsStateWithLifecycle()
    val isArchived by workspaceViewModel.isArchived.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    ShowMessages(workspaceViewModel.messages, snackbarHostState)
    ShowMessages(changesViewModel.messages, snackbarHostState)
    LaunchedEffect(isArchived) { if (isArchived) onBack() }
    BackHandler(enabled = tab == WorkspaceTab.Overview && section != null) { section = null }

    val isImeVisible = WindowInsets.isImeVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val terminalChrome = when {
        isLandscape && isImeVisible -> TerminalChrome.KeyboardLandscape
        isTerminalFullScreen -> TerminalChrome.FullScreen
        else -> TerminalChrome.Normal
    }
    val isTerminal = tab == WorkspaceTab.Terminal
    val isNavigationShown = !isImeVisible && !(isTerminal && terminalChrome != TerminalChrome.Normal)
    val navigation = SectionNavigation(
        openReviewFile = { path ->
            changesViewModel.openFile(path)
            tab = WorkspaceTab.Changes
        },
        openSection = { section = it },
        manageServers = onManageServers,
    )
    Scaffold(
        topBar = {
            if (!isTerminal) {
                PanelTopBar(
                    title = section?.title?.takeIf { tab == WorkspaceTab.Overview } ?: workspace.branch,
                    onBack = {
                        when {
                            tab == WorkspaceTab.Overview && section != null -> section = null
                            tab == WorkspaceTab.Changes && openPath != null -> changesViewModel.closeFile()
                            else -> onBack()
                        }
                    },
                    onManageServers = onManageServers,
                )
            }
        },
        bottomBar = { if (isNavigationShown && !isLandscape) WorkspaceNavigationBar(tab) { tab = it } },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (isNavigationShown && isLandscape) WorkspaceNavigationRail(tab) { tab = it }
            val contentModifier = Modifier.weight(1f).imePadding()
            when (tab) {
                WorkspaceTab.Overview -> OverviewContent(section, workspaceViewModel, navigation, snackbarHostState, contentModifier)
                WorkspaceTab.Changes -> ChangesTab(changesViewModel, onManageServers, contentModifier)
                WorkspaceTab.Terminal -> TerminalTab(
                    viewModel = terminalViewModel,
                    snackbarHostState = snackbarHostState,
                    chrome = terminalChrome,
                    onBack = onBack,
                    onManageServers = onManageServers,
                    onFullScreenChange = { isTerminalFullScreen = it },
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun OverviewContent(
    section: WorkspaceSection?,
    viewModel: WorkspaceViewModel,
    navigation: SectionNavigation,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
) {
    if (section == null) {
        OverviewTab(viewModel, navigation.openSection, modifier)
        return
    }
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val state = snapshot.state
    when {
        state != null -> SectionHost(section, state, viewModel, navigation, snackbarHostState, modifier)
        snapshot.error != null -> EmptySection(snapshot.error.orEmpty())
        else -> FullScreenProgress()
    }
}

@Composable
private fun ShowMessages(messages: Flow<String>, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(messages) { messages.collect { snackbarHostState.showSnackbar(it) } }
}

/** Landscape leaves too few terminal rows for a bottom bar, so tabs move to a side rail; with the keyboard up the chrome is dropped entirely. */
@Composable
private fun WorkspaceNavigationRail(selected: WorkspaceTab, onSelect: (WorkspaceTab) -> Unit) {
    NavigationRail {
        WorkspaceTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun WorkspaceNavigationBar(selected: WorkspaceTab, onSelect: (WorkspaceTab) -> Unit) {
    NavigationBar {
        WorkspaceTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

package com.ignatrozhko.governedworkflow.ui.workspace

import android.content.res.Configuration
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.PanelTopBar
import com.ignatrozhko.governedworkflow.ui.overview.OverviewTab
import com.ignatrozhko.governedworkflow.ui.overview.OverviewViewModel
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalTab
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(workspace: WorkspaceRef, onManageServers: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val overviewViewModel = viewModel { OverviewViewModel(workspace, container.connectionManager) }
    val terminalViewModel = viewModel {
        TerminalViewModel(workspace, container.connectionManager, container.networkMonitor, container.settings, container.terminalSocketOpener)
    }
    var tab by rememberSaveable { mutableStateOf(WorkspaceTab.Terminal) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isImeVisible = WindowInsets.isImeVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTerminalCompact = isLandscape && isImeVisible
    Scaffold(
        topBar = {
            if (!isTerminalCompact) PanelTopBar(title = workspace.branch, onBack = onBack, onManageServers = onManageServers)
        },
        bottomBar = { if (!isImeVisible && !isLandscape) WorkspaceNavigationBar(tab) { tab = it } },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (isLandscape) WorkspaceNavigationRail(tab) { tab = it }
            val contentModifier = Modifier.weight(1f).imePadding()
            when (tab) {
                WorkspaceTab.Overview -> OverviewTab(overviewViewModel, snackbarHostState, contentModifier)
                WorkspaceTab.Terminal -> TerminalTab(terminalViewModel, snackbarHostState, isTerminalCompact, contentModifier)
            }
        }
    }
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

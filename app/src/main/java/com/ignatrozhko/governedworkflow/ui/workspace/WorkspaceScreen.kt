package com.ignatrozhko.governedworkflow.ui.workspace

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.WorkspaceContext
import com.ignatrozhko.governedworkflow.connection.ConnectionState
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.ui.changes.ChangesTab
import com.ignatrozhko.governedworkflow.ui.changes.ChangesViewModel
import com.ignatrozhko.governedworkflow.ui.common.CountBadge
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.GateBar
import com.ignatrozhko.governedworkflow.ui.common.ImmersiveMode
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.common.OfflineBanner
import com.ignatrozhko.governedworkflow.ui.common.PanelTopBar
import com.ignatrozhko.governedworkflow.ui.overview.GateDecisionFooter
import com.ignatrozhko.governedworkflow.ui.overview.OverviewTab
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalChrome
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalTab
import com.ignatrozhko.governedworkflow.ui.terminal.TerminalViewModel
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.LocalExpansionMemory
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(
    workspace: WorkspaceRef,
    initialSection: WorkspaceSection?,
    opensOnDecision: Boolean,
    onManageServers: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val workspaceViewModel = viewModel { WorkspaceViewModel(workspace, container.connectionManager, container.workspaceStateStore) }
    val changesViewModel = viewModel { ChangesViewModel(workspace, container.connectionManager, container.workspaceStateStore) }
    val terminalViewModel = viewModel {
        TerminalViewModel(workspace, container.connectionManager, container.networkMonitor, container.settings, container.terminalSocketOpener)
    }
    val opensOnOverview = initialSection != null || opensOnDecision
    var tab by rememberSaveable { mutableStateOf(if (opensOnOverview) WorkspaceTab.Overview else WorkspaceTab.Terminal) }
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var isTerminalFullScreen by rememberSaveable { mutableStateOf(false) }
    val openPath by changesViewModel.openPath.collectAsStateWithLifecycle()
    val changedFiles by changesViewModel.files.collectAsStateWithLifecycle()
    val changedFileList = (changedFiles as? LoadState.Loaded)?.data.orEmpty()
    val changedFileCount = changedFileList.size
    val openFile = (changedFiles as? LoadState.Loaded)?.data?.firstOrNull { it.path == openPath }
    val isArchived by workspaceViewModel.isArchived.collectAsStateWithLifecycle()
    val snapshot by workspaceViewModel.snapshot.collectAsStateWithLifecycle()
    val context by workspaceViewModel.context.collectAsStateWithLifecycle()
    val isBusy by workspaceViewModel.isBusy.collectAsStateWithLifecycle()
    val pendingGate = snapshot.state?.pendingGate
    val connection by container.connectionManager.state.collectAsStateWithLifecycle()
    val isShowingSavedData = connection is ConnectionState.Unreachable && snapshot.state != null
    val snackbarHostState = remember { SnackbarHostState() }
    ShowMessages(workspaceViewModel.messages, snackbarHostState)
    ShowMessages(changesViewModel.messages, snackbarHostState)
    LaunchedEffect(isArchived) { if (isArchived) onBack() }
    BackHandler(enabled = tab == WorkspaceTab.Overview && section != null) { section = null }

    val isImeVisible = WindowInsets.isImeVisible
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val terminalChrome = TerminalChrome(isFullScreen = isTerminalFullScreen, isKeyboardOpen = isImeVisible, isLandscape = isLandscape)
    val isTerminal = tab == WorkspaceTab.Terminal
    val isOverviewRoot = tab == WorkspaceTab.Overview && section == null
    val isNavigationShown = !isImeVisible && !(isTerminal && !terminalChrome.isHeaderShown)
    ImmersiveMode(isImmersive = isTerminal && isTerminalFullScreen)
    BackHandler(enabled = isTerminal && isTerminalFullScreen) { isTerminalFullScreen = false }
    val openDecision = {
        section = null
        tab = WorkspaceTab.Overview
    }
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
                val title = screenTitle(tab, section, context, workspace, openFile)
                PanelTopBar(
                    title = title,
                    subtitle = screenSubtitle(title, tab, workspace, openFile),
                    onBack = {
                        when {
                            tab == WorkspaceTab.Overview && section != null -> section = null
                            tab == WorkspaceTab.Changes && openPath != null -> changesViewModel.closeFile()
                            else -> onBack()
                        }
                    },
                    onManageServers = onManageServers,
                    actions = {
                        val shownSection = section
                        val sectionState = snapshot.state
                        if (tab == WorkspaceTab.Overview && shownSection != null && sectionState != null) SectionStatusPill(shownSection, sectionState)
                    },
                )
            }
        },
        bottomBar = {
            Column {
                if (pendingGate != null && isOverviewRoot) {
                    GateDecisionFooter(
                        pendingGate,
                        isBusy,
                        isAtScreenBottom = !isNavigationShown || isLandscape,
                        isBarShown = !isImeVisible,
                        onApprove = workspaceViewModel::approveGate,
                        onReject = workspaceViewModel::rejectGate,
                    )
                }
                if (isNavigationShown && !isLandscape) WorkspaceNavigationBar(tab, changedFileCount) { tab = it }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (isTerminal && isTerminalFullScreen) Checkpoint.colors.terminalBg else MaterialTheme.colorScheme.background,
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (isNavigationShown && isLandscape) WorkspaceNavigationRail(tab, changedFileCount) { tab = it }
            Column(Modifier.weight(1f).imePadding()) {
                if (isShowingSavedData && !isTerminal) {
                    OfflineBanner(container.connectionManager::refresh, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                if (pendingGate != null && !isTerminal && !isOverviewRoot) {
                    GateBar("Gate ${pendingGate.phase} needs you", "Decide", openDecision, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
                val contentModifier = Modifier.weight(1f)
                when (tab) {
                    WorkspaceTab.Overview -> CompositionLocalProvider(LocalExpansionMemory provides workspaceViewModel.expansionMemory) {
                        OverviewContent(section, workspaceViewModel, navigation, changedFileList, { tab = WorkspaceTab.Changes }, snackbarHostState, contentModifier)
                    }
                    WorkspaceTab.Changes -> ChangesTab(changesViewModel, onManageServers, contentModifier)
                    WorkspaceTab.Terminal -> TerminalTab(
                        viewModel = terminalViewModel,
                        snackbarHostState = snackbarHostState,
                        chrome = terminalChrome,
                        gatePhase = pendingGate?.phase,
                        onOpenGate = openDecision,
                        onBack = onBack,
                        onManageServers = onManageServers,
                        onFullScreenChange = { isTerminalFullScreen = it },
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}

private fun screenTitle(tab: WorkspaceTab, section: WorkspaceSection?, context: WorkspaceContext?, workspace: WorkspaceRef, openFile: DiffFile?): String = when {
    tab == WorkspaceTab.Changes && openFile != null -> openFile.path.substringAfterLast('/')
    tab == WorkspaceTab.Changes -> WorkspaceTab.Changes.label
    tab == WorkspaceTab.Overview && section != null -> section.title
    else -> context?.let { listOf(it.ticketId.takeUnless { id -> id == workspace.branch }.orEmpty(), it.ticketName).filter(String::isNotBlank).joinToString(" ") }
        ?.takeIf(String::isNotBlank) ?: tab.label
}

private fun screenSubtitle(title: String, tab: WorkspaceTab, workspace: WorkspaceRef, openFile: DiffFile?): String? {
    if (tab != WorkspaceTab.Changes || openFile == null) return workspace.branch.takeUnless { it == title }
    val directory = openFile.path.substringBeforeLast('/', missingDelimiterValue = "")
    return listOf(directory, "+${openFile.additions} −${openFile.deletions}").filter(String::isNotEmpty).joinToString(" · ")
}

@Composable
private fun OverviewContent(
    section: WorkspaceSection?,
    viewModel: WorkspaceViewModel,
    navigation: SectionNavigation,
    changedFiles: List<DiffFile>,
    onOpenChanges: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
) {
    if (section == null) {
        OverviewTab(viewModel, navigation.openSection, changedFiles, onOpenChanges, modifier)
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
private fun WorkspaceNavigationRail(selected: WorkspaceTab, changedFileCount: Int, onSelect: (WorkspaceTab) -> Unit) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        WorkspaceTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { TabIcon(tab, changedFileCount) },
                label = { Text(tab.label, style = NAV_LABEL_STYLE) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun WorkspaceNavigationBar(selected: WorkspaceTab, changedFileCount: Int, onSelect: (WorkspaceTab) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            WorkspaceTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { TabIcon(tab, changedFileCount) },
                    label = { Text(tab.label, style = NAV_LABEL_STYLE) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TabIcon(tab: WorkspaceTab, changedFileCount: Int) {
    val icon = @Composable { Icon(painterResource(tab.iconRes), contentDescription = null) }
    if (tab != WorkspaceTab.Changes || changedFileCount == 0) {
        icon()
        return
    }
    BadgedBox(badge = { CountBadge(changedFileCount) }, content = { icon() })
}

private val NAV_LABEL_STYLE = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)

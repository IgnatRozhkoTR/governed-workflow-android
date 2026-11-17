package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Renders the shared offline/loading/failed states and hands loaded data to [content] inside pull-to-refresh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadStateContent(
    state: LoadState<T>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onManageServers: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        when (state) {
            LoadState.Offline -> ConnectionStatusPanel(onManageServers)
            LoadState.Loading -> FullScreenProgress()
            is LoadState.Failed -> LoadFailure(state.message, onRefresh)
            is LoadState.Loaded -> PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) { content(state.data) }
        }
    }
}

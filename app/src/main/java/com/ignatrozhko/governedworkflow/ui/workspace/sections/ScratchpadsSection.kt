package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.ScratchpadFile
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.listScratchpads
import com.ignatrozhko.governedworkflow.data.api.readScratchpad
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.common.FullScreenProgress
import com.ignatrozhko.governedworkflow.ui.common.LoadState
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OpenScratchpad(val file: ScratchpadFile, val content: LoadState<String>)

class ScratchpadsViewModel(workspace: WorkspaceRef, connectionManager: ConnectionManager, stateStore: WorkspaceStateStore) :
    SectionDataViewModel<List<ScratchpadFile>>(workspace, connectionManager, stateStore) {
    private val _openFile = MutableStateFlow<OpenScratchpad?>(null)
    private var openJob: Job? = null

    val openFile: StateFlow<OpenScratchpad?> = _openFile.asStateFlow()

    override suspend fun load(api: PanelApi) = api.listScratchpads(workspace)

    fun open(file: ScratchpadFile) {
        openJob?.cancel()
        _openFile.value = OpenScratchpad(file, LoadState.Loading)
        openJob = viewModelScope.launch {
            val content = try {
                LoadState.Loaded(connectionManager.requireApi().readScratchpad(workspace, file.name))
            } catch (error: PanelException) {
                LoadState.Failed(error.message.orEmpty())
            }
            _openFile.value = OpenScratchpad(file, content)
        }
    }

    fun close() {
        openJob?.cancel()
        _openFile.value = null
    }
}

@Composable
fun ScratchpadsSection(files: List<ScratchpadFile>, viewModel: ScratchpadsViewModel, modifier: Modifier = Modifier) {
    val openFile by viewModel.openFile.collectAsStateWithLifecycle()
    val shown = openFile
    if (shown != null) {
        BackHandler(onBack = viewModel::close)
        ScratchpadContent(shown, modifier)
        return
    }
    if (files.isEmpty()) {
        EmptySection("No scratchpad files.")
        return
    }
    LazyColumn(modifier) {
        items(files, key = { it.name }) { file ->
            Column(Modifier.fillMaxWidth().clickable { viewModel.open(file) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(file.title.ifBlank { file.name }, style = MaterialTheme.typography.bodyLarge)
                SecondaryText("${file.name} · ${file.updatedAt.take(TIMESTAMP_MINUTES).replace('T', ' ')}")
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ScratchpadContent(openFile: OpenScratchpad, modifier: Modifier) {
    when (val content = openFile.content) {
        is LoadState.Loaded -> Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(openFile.file.name, style = MaterialTheme.typography.labelLarge)
            MarkdownText(content.data)
        }
        is LoadState.Failed -> EmptySection(content.message)
        LoadState.Loading, LoadState.Offline -> FullScreenProgress()
    }
}

private const val TIMESTAMP_MINUTES = 16

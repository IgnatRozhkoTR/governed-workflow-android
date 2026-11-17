package com.ignatrozhko.governedworkflow.ui.servers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.ignatrozhko.governedworkflow.connection.PanelProbe
import com.ignatrozhko.governedworkflow.connection.ProbeOutcome
import com.ignatrozhko.governedworkflow.data.server.InvalidQrPayloadException
import com.ignatrozhko.governedworkflow.data.server.QrPayloadParser
import com.ignatrozhko.governedworkflow.data.server.ServerRepository
import com.ignatrozhko.governedworkflow.data.server.ServerUrls
import com.ignatrozhko.governedworkflow.ui.common.appContainer
import com.ignatrozhko.governedworkflow.ui.nav.ServerEditorRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerEditorUiState(
    val name: String = "",
    val urlsText: String = "",
    val token: String = "",
    val isScanningQr: Boolean = false,
    val isTesting: Boolean = false,
    val testResults: Map<String, ProbeOutcome> = emptyMap(),
    val error: String? = null,
    val isEditing: Boolean = false,
)

class ServerEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ServerRepository,
    private val probe: PanelProbe,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ServerEditorRoute>()
    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<ServerEditorUiState> = _uiState.asStateFlow()

    init {
        route.serverId?.let(::loadExisting)
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }

    fun onUrlsChange(value: String) = _uiState.update { it.copy(urlsText = value, error = null, testResults = emptyMap()) }

    fun onTokenChange(value: String) = _uiState.update { it.copy(token = value.trim(), error = null, testResults = emptyMap()) }

    fun appendUrl(raw: String) {
        val url = ServerUrls.normalize(raw)
        if (url == null) {
            _uiState.update { it.copy(error = "The clipboard does not contain a URL.") }
            return
        }
        _uiState.update { state ->
            val urls = ServerUrls.parseList(state.urlsText)
            state.copy(urlsText = (urls + url).distinct().joinToString("\n"), error = null)
        }
    }

    fun setScanningQr(isScanning: Boolean) = _uiState.update { it.copy(isScanningQr = isScanning) }

    /** Returns false when [text] is not a connection code, so the scanner keeps looking. */
    fun onQrScanned(text: String): Boolean = try {
        val payload = QrPayloadParser.parse(text)
        _uiState.update { state ->
            state.copy(
                name = payload.name ?: state.name.ifBlank { ServerUrls.displayHost(payload.urls.first()) },
                urlsText = payload.urls.joinToString("\n"),
                token = payload.token,
                isScanningQr = false,
                error = null,
                testResults = emptyMap(),
            )
        }
        true
    } catch (error: InvalidQrPayloadException) {
        _uiState.update { it.copy(error = error.message) }
        false
    }

    fun testConnection() {
        val urls = ServerUrls.parseList(_uiState.value.urlsText)
        val token = _uiState.value.token
        if (!validate(urls, token)) return
        _uiState.update { it.copy(isTesting = true, testResults = emptyMap()) }
        viewModelScope.launch {
            val results = urls.map { url -> async { url to probe.probe(url, token) } }.awaitAll().toMap()
            _uiState.update { it.copy(isTesting = false, testResults = results) }
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val urls = ServerUrls.parseList(state.urlsText)
        if (!validate(urls, state.token)) return
        val name = state.name.trim().ifEmpty { ServerUrls.displayHost(urls.first()) }
        viewModelScope.launch {
            val saved = repository.save(route.serverId, name, urls, state.token)
            repository.select(saved.id)
            onSaved()
        }
    }

    private fun validate(urls: List<String>, token: String): Boolean {
        val error = when {
            urls.isEmpty() -> "Enter at least one URL, e.g. http://192.168.1.10:5111"
            token.isEmpty() -> "Paste the admin token (python3 admin-panel/backend/app.py auth-token)."
            else -> null
        }
        _uiState.update { it.copy(error = error) }
        return error == null
    }

    private fun initialState(): ServerEditorUiState {
        val prefill = route.prefillUrl?.let(ServerUrls::normalize)
        return ServerEditorUiState(
            name = prefill?.let(ServerUrls::displayHost).orEmpty(),
            urlsText = prefill.orEmpty(),
            isScanningQr = route.startWithQr,
            isEditing = route.serverId != null,
        )
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val credentials = repository.credentials(id)
            _uiState.update {
                it.copy(
                    name = credentials.server.name,
                    urlsText = credentials.server.urls.joinToString("\n"),
                    token = credentials.token,
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ServerEditorViewModel(createSavedStateHandle(), appContainer.serverRepository, appContainer.panelProbe)
            }
        }
    }
}

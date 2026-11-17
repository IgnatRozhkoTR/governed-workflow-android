package com.ignatrozhko.governedworkflow

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.connection.NetworkMonitor
import com.ignatrozhko.governedworkflow.connection.PanelProbe
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.PanelJson
import com.ignatrozhko.governedworkflow.data.server.ServerRepository
import com.ignatrozhko.governedworkflow.data.server.TokenCipher
import com.ignatrozhko.governedworkflow.data.settings.AppSettings
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.discovery.LanScanner
import com.ignatrozhko.governedworkflow.gates.WorkspaceLinks
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSocketOpener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Process-wide dependencies, built once by [GovernedWorkflowApp]. */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = PanelJson
    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(DATA_STORE_NAME)
    }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** Terminal sockets stay open indefinitely; pings detect connections a network switch silently killed. */
    private val socketClient = httpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(SOCKET_PING_SECONDS, TimeUnit.SECONDS)
        .build()

    val serverRepository = ServerRepository(dataStore, TokenCipher(), json)
    val settings = AppSettings(dataStore)
    val networkMonitor = NetworkMonitor(context)
    val panelProbe = PanelProbe(httpClient)
    val lanScanner = LanScanner(httpClient, json)

    val connectionManager = ConnectionManager(
        scope = applicationScope,
        repository = serverRepository,
        probe = panelProbe,
        networkMonitor = networkMonitor,
        apiFactory = { baseUrl, token -> PanelApi(baseUrl.toHttpUrl(), token, httpClient, json) },
    ).also { it.start() }

    val workspaceStateStore = WorkspaceStateStore(connectionManager, applicationScope)
    val workspaceLinks = WorkspaceLinks()

    val terminalSocketOpener = TerminalSocketOpener { target, listener ->
        socketClient.newWebSocket(target.socketRequest(connectionManager.requireApi()), listener)
    }

    private companion object {
        const val DATA_STORE_NAME = "governed_workflow"
        const val CONNECT_TIMEOUT_SECONDS = 5L
        const val READ_TIMEOUT_SECONDS = 20L
        const val SOCKET_PING_SECONDS = 15L
    }
}

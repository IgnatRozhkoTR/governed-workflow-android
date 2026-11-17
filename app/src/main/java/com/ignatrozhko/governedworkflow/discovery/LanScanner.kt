package com.ignatrozhko.governedworkflow.discovery

import com.ignatrozhko.governedworkflow.data.api.awaitResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class DiscoveredPanel(val baseUrl: String, val isTokenConfigured: Boolean)

sealed interface ScanEvent {
    data class Progress(val probed: Int, val total: Int) : ScanEvent
    data class Found(val panel: DiscoveredPanel) : ScanEvent
}

/** Sweeps hosts for the panel's unauthenticated `/api/auth/status` endpoint with bounded parallelism. */
class LanScanner(baseClient: OkHttpClient, private val json: Json) {
    private val http = baseClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .callTimeout(CONNECT_TIMEOUT_MS + READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    fun scan(hosts: List<String>, port: Int = PANEL_PORT): Flow<ScanEvent> = channelFlow {
        val permits = Semaphore(MAX_PARALLEL_PROBES)
        val probed = AtomicInteger()
        send(ScanEvent.Progress(0, hosts.size))
        hosts.forEach { host ->
            launch {
                val panel = permits.withPermit { identify(host, port) }
                if (panel != null) send(ScanEvent.Found(panel))
                send(ScanEvent.Progress(probed.incrementAndGet(), hosts.size))
            }
        }
    }

    private suspend fun identify(host: String, port: Int): DiscoveredPanel? {
        val baseUrl = HttpUrl.Builder().scheme("http").host(host).port(port).build()
        val request = Request.Builder().url(baseUrl.newBuilder().addPathSegments("api/auth/status").build()).get().build()
        val result = try {
            http.newCall(request).awaitResult()
        } catch (error: IOException) {
            return null
        }
        if (result.code != HTTP_OK) return null
        val configured = PanelFingerprint.tokenConfigured(json, result.body) ?: return null
        return DiscoveredPanel(baseUrl.toString().trimEnd('/'), configured)
    }

    companion object {
        const val PANEL_PORT = 5111
        private const val MAX_PARALLEL_PROBES = 48
        private const val CONNECT_TIMEOUT_MS = 300L
        private const val READ_TIMEOUT_MS = 700L
        private const val HTTP_OK = 200
    }
}

/** Recognises the admin panel's auth-status payload: `{"configured": <bool>, "setup_command": "..."}`. */
internal object PanelFingerprint {
    fun tokenConfigured(json: Json, body: String): Boolean? {
        val fields = try {
            json.parseToJsonElement(body) as? JsonObject
        } catch (error: IllegalArgumentException) {
            null
        } ?: return null
        if (fields["setup_command"] !is JsonPrimitive) return null
        return (fields["configured"] as? JsonPrimitive)?.booleanOrNull
    }
}

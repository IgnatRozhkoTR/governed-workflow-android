package com.ignatrozhko.governedworkflow.connection

import com.ignatrozhko.governedworkflow.data.api.awaitResult
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Authenticated reachability check against the cheapest protected endpoint. */
class PanelProbe(baseClient: OkHttpClient) {
    private val http = baseClient.newBuilder()
        .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .callTimeout(PROBE_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS)
        .build()

    suspend fun probe(baseUrl: String, token: String): ProbeOutcome {
        val url = baseUrl.toHttpUrlOrNull()?.newBuilder()?.addPathSegments("api/server-info")?.build()
            ?: return ProbeOutcome.Unreachable("Invalid URL")
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").get().build()
        return try {
            val result = http.newCall(request).awaitResult()
            when (result.code) {
                HTTP_OK -> ProbeOutcome.Reachable
                HTTP_UNAUTHORIZED -> ProbeOutcome.Unauthorized
                else -> ProbeOutcome.Unreachable("HTTP ${result.code}")
            }
        } catch (error: IOException) {
            ProbeOutcome.Unreachable(error.message ?: error.javaClass.simpleName)
        }
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 2_500L
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
    }
}

package com.ignatrozhko.governedworkflow.data.server

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ServerUrls {
    private const val DEFAULT_SCHEME = "http://"
    private val LIST_SEPARATORS = Regex("[\\s,]+")

    /**
     * Turns user input such as `192.168.1.5:5111` or `http://host:5111/` into a canonical base URL
     * (`scheme://host:port`), or null when it cannot address an HTTP server.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else DEFAULT_SCHEME + trimmed
        val url = withScheme.toHttpUrlOrNull() ?: return null
        return url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString().trimEnd('/')
    }

    fun parseList(text: String): List<String> =
        text.split(LIST_SEPARATORS).mapNotNull(::normalize).distinct()

    fun displayHost(baseUrl: String): String {
        val url: HttpUrl = baseUrl.toHttpUrlOrNull() ?: return baseUrl
        return "${url.host}:${url.port}"
    }
}

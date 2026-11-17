package com.ignatrozhko.governedworkflow.data.server

import kotlinx.serialization.Serializable

/** A saved admin panel; [urls] are candidate base URLs in the user's order of preference. */
@Serializable
data class ServerEntry(
    val id: String,
    val name: String,
    val urls: List<String>,
)

/** A server together with its decrypted admin token, ready to authenticate requests. */
data class ServerCredentials(
    val server: ServerEntry,
    val token: String,
)

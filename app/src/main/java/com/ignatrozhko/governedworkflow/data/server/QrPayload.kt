package com.ignatrozhko.governedworkflow.data.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Connection details the panel encodes as `{"urls": [...], "token": "...", "name": "..."}`. */
data class QrPayload(
    val name: String?,
    val urls: List<String>,
    val token: String,
)

class InvalidQrPayloadException(message: String) : Exception(message)

object QrPayloadParser {
    @Serializable
    private data class RawPayload(
        val urls: List<String> = emptyList(),
        val token: String = "",
        val name: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): QrPayload {
        val raw = try {
            json.decodeFromString<RawPayload>(text)
        } catch (error: SerializationException) {
            throw InvalidQrPayloadException("The QR code is not a Governed Workflow connection code.")
        } catch (error: IllegalArgumentException) {
            throw InvalidQrPayloadException("The QR code is not a Governed Workflow connection code.")
        }
        val urls = raw.urls.mapNotNull(ServerUrls::normalize).distinct()
        if (urls.isEmpty()) throw InvalidQrPayloadException("The QR code contains no valid server URL.")
        if (raw.token.isBlank()) throw InvalidQrPayloadException("The QR code contains no token.")
        return QrPayload(raw.name?.takeIf { it.isNotBlank() }, urls, raw.token.trim())
    }
}

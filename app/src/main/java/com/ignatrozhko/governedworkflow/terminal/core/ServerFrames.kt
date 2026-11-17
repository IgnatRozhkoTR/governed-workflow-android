package com.ignatrozhko.governedworkflow.terminal.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The backend refuses an attach by sending `{"error": "..."}` as the first frame and closing. */
object ServerFrames {
    private val json = Json

    fun errorMessage(firstFrame: String): String? {
        if (!firstFrame.startsWith("{")) return null
        val fields = try {
            json.parseToJsonElement(firstFrame) as? JsonObject
        } catch (error: IllegalArgumentException) {
            null
        } ?: return null
        val error = fields["error"] as? JsonPrimitive ?: return null
        return if (error.isString) error.content else null
    }
}

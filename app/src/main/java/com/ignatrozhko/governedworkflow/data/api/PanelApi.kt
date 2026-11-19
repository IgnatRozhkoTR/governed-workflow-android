package com.ignatrozhko.governedworkflow.data.api

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** REST client for one admin panel base URL; mirrors the contracts of the VS Code extension's ApiClient. */
class PanelApi(
    val baseUrl: HttpUrl,
    private val token: String,
    private val http: OkHttpClient,
    private val json: Json,
) {
    suspend fun serverInfo(): ServerInfo = get(PanelPaths.api(baseUrl, "api/server-info"), ServerInfo.serializer())

    suspend fun listProjects(): List<Project> =
        get(PanelPaths.api(baseUrl, "api/projects"), ProjectsResponse.serializer()).projects

    suspend fun listWorkspaces(projectId: String): List<WorkspaceSummary> =
        get(PanelPaths.projectWorkspaces(baseUrl, projectId), WorkspacesResponse.serializer()).workspaces

    suspend fun getState(workspace: WorkspaceRef, etag: String?): StateFetch {
        val request = authorized(PanelPaths.workspace(baseUrl, workspace, "state"))
            .apply { if (etag != null) header("If-None-Match", etag) }
            .get()
            .build()
        val result = execute(request)
        if (result.code == HTTP_NOT_MODIFIED) return StateFetch.NotModified
        return StateFetch.Fresh(decode(result, WorkspaceState.serializer()), result.etag)
    }

    suspend fun getContext(workspace: WorkspaceRef): WorkspaceContext =
        get(PanelPaths.workspace(baseUrl, workspace, "context"), WorkspaceContext.serializer())

    suspend fun approve(workspace: WorkspaceRef, commitMessage: String) {
        val body = buildJsonObject { if (commitMessage.isNotBlank()) put("commit_message", commitMessage) }
        send("POST", PanelPaths.workspace(baseUrl, workspace, "approve"), body)
    }

    suspend fun reject(workspace: WorkspaceRef, comments: String) {
        send("POST", PanelPaths.workspace(baseUrl, workspace, "reject"), buildJsonObject { put("comments", comments) })
    }

    suspend fun setYolo(workspace: WorkspaceRef, enabled: Boolean) {
        send("PUT", PanelPaths.workspace(baseUrl, workspace, "yolo"), buildJsonObject { put("enabled", enabled) })
    }

    suspend fun notifyClaude(workspace: WorkspaceRef, message: String) {
        send("POST", PanelPaths.workspace(baseUrl, workspace, "terminal/notify"), buildJsonObject { put("message", message) })
    }

    suspend fun terminalStatus(workspace: WorkspaceRef): TerminalStatus =
        get(PanelPaths.workspace(baseUrl, workspace, "terminal/status"), TerminalStatus.serializer())

    suspend fun terminalStart(workspace: WorkspaceRef): TerminalStartResponse =
        decode(send("POST", PanelPaths.workspace(baseUrl, workspace, "terminal/start"), EMPTY_BODY), TerminalStartResponse.serializer())

    suspend fun terminalResume(workspace: WorkspaceRef): TerminalStartResponse =
        decode(send("POST", PanelPaths.workspace(baseUrl, workspace, "terminal/resume"), EMPTY_BODY), TerminalStartResponse.serializer())

    suspend fun terminalKill(workspace: WorkspaceRef) {
        send("POST", PanelPaths.workspace(baseUrl, workspace, "terminal/kill"), buildJsonObject { put("kind", "claude") })
    }

    suspend fun listTerminalSessions(): List<TerminalSessionInfo> =
        get(PanelPaths.api(baseUrl, "api/terminal/sessions"), ListSerializer(TerminalSessionInfo.serializer()))

    suspend fun killTerminalSession(name: String) {
        send("POST", PanelPaths.terminalSessionKill(baseUrl, name), EMPTY_BODY)
    }

    /** WebSocket handshakes cannot rely on headers in browsers, so the backend reads the token from the query. */
    fun workspaceTerminalSocket(workspace: WorkspaceRef): Request =
        Request.Builder().url(PanelPaths.workspaceTerminalSocket(baseUrl, workspace, token)).build()

    fun namedTerminalSocket(sessionName: String): Request =
        Request.Builder().url(PanelPaths.namedTerminalSocket(baseUrl, sessionName, token)).build()

    internal suspend fun <T> get(url: HttpUrl, deserializer: DeserializationStrategy<T>): T =
        decode(execute(authorized(url).get().build()), deserializer)

    internal suspend fun send(method: String, url: HttpUrl, body: JsonObject): HttpResult {
        val requestBody = json.encodeToString(JsonObject.serializer(), body).toRequestBody(JSON_MEDIA_TYPE)
        return execute(authorized(url).method(method, requestBody).build())
    }

    private fun authorized(url: HttpUrl): Request.Builder =
        Request.Builder().url(url).header("Authorization", "Bearer $token").header("Accept", "application/json")

    private suspend fun execute(request: Request): HttpResult {
        val result = try {
            http.newCall(request).awaitResult()
        } catch (error: IOException) {
            throw PanelUnreachableException("Cannot reach the admin panel at $baseUrl: ${error.message}", error)
        }
        if (result.code == HTTP_UNAUTHORIZED) throw AuthRequiredException(errorMessage(result))
        if (result.code !in HTTP_SUCCESS && result.code != HTTP_NOT_MODIFIED) {
            throw HttpStatusException(result.code, errorCode(result), errorMessage(result))
        }
        return result
    }

    internal fun <T> decode(result: HttpResult, deserializer: DeserializationStrategy<T>): T = try {
        json.decodeFromString(deserializer, result.body)
    } catch (error: SerializationException) {
        throw MalformedResponseException("The admin panel returned an unexpected response.", error)
    } catch (error: IllegalArgumentException) {
        throw MalformedResponseException("The admin panel returned an unexpected response.", error)
    }

    private fun errorFields(result: HttpResult): JsonObject? = try {
        json.parseToJsonElement(result.body).jsonObject
    } catch (error: SerializationException) {
        null
    } catch (error: IllegalArgumentException) {
        null
    }

    private fun errorCode(result: HttpResult): String =
        (errorFields(result)?.get("error") as? JsonPrimitive)?.takeIf { it.isString }?.content ?: "http_${result.code}"

    private fun errorMessage(result: HttpResult): String {
        val fields = errorFields(result)
        val message = ERROR_MESSAGE_FIELDS.firstNotNullOfOrNull { name ->
            (fields?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
        }
        return message?.let { READABLE_ERROR_CODES[it] ?: it } ?: result.body.trim().ifEmpty { "HTTP ${result.code}" }
    }

    internal companion object {
        val EMPTY_BODY = JsonObject(emptyMap())
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_MODIFIED = 304
        private val HTTP_SUCCESS = 200..299
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private val ERROR_MESSAGE_FIELDS = listOf("details", "reason", "message", "error")
        private val READABLE_ERROR_CODES = mapOf(
            "repo_required" to "Choose a repository to see its changes.",
            "repo_not_found" to "This repository's checkout is missing on the panel host.",
            "invalid_repo" to "The panel rejected this repository path.",
        )
    }
}

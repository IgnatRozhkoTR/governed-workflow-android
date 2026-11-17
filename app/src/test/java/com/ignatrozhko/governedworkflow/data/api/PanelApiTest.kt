package com.ignatrozhko.governedworkflow.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class PanelApiTest {
    private val server = MockWebServer()
    private lateinit var api: PanelApi
    private val workspace = WorkspaceRef("qa-shop", "qa/prep-review")

    @Before
    fun setUp() {
        server.start()
        api = PanelApi(server.url("/"), "secret-token", OkHttpClient(), PanelJson)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun respond(code: Int, body: String = "", etag: String? = null) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body).apply { if (etag != null) setHeader("ETag", etag) })
    }

    @Test
    fun listProjects_shouldParseProjects_andSendBearerToken() = runTest {
        respond(200, """{"projects":[{"id":"qa-shop","name":"QA Shop","path":"/tmp/qa","registered":true,"project_type":"single"}]}""")

        val projects = api.listProjects()

        assertEquals(listOf(Project("qa-shop", "QA Shop", "/tmp/qa", "single")), projects)
        val request = server.takeRequest()
        assertEquals("/api/projects", request.path)
        assertEquals("Bearer secret-token", request.getHeader("Authorization"))
    }

    @Test
    fun listWorkspaces_shouldParseSummaries_andIgnoreUnknownFields() = runTest {
        respond(
            200,
            """{"workspaces":[{"id":"qa-prep-review","workspace_id":7,"branch":"qa/prep-review","phase":"1.4","status":"active",
               "session_id":null,"working_dir":"/w","created":"2026-01-01","workflow_mode":"full","sessions":[]},
               {"id":"qa-old","workspace_id":8,"branch":"qa/old","phase":"6","status":"archived"}]}""",
        )

        val workspaces = api.listWorkspaces("qa-shop")

        assertEquals(listOf("qa/prep-review", "qa/old"), workspaces.map { it.branch })
        assertEquals(listOf(false, true), workspaces.map { it.isArchived })
        assertEquals("/api/projects/qa-shop/workspaces", server.takeRequest().path)
    }

    @Test
    fun getState_shouldParseGate_andKeepBranchSlashesAsSegments() = runTest {
        respond(
            200,
            """{"phase":"1.4","status":"active","branch":"qa/prep-review","yolo_mode":false,
               "pending_gate":{"phase":"1.4","accepts_commit_message":false},"progress":{}}""",
            etag = "\"abc\"",
        )

        val fetch = api.getState(workspace, etag = null) as StateFetch.Fresh

        assertEquals(PendingGate("1.4", acceptsCommitMessage = false), fetch.state.pendingGate)
        assertEquals("\"abc\"", fetch.etag)
        assertEquals("/api/ws/qa-shop/qa/prep-review/state", server.takeRequest().path)
    }

    @Test
    fun getState_shouldReturnNotModified_andSendIfNoneMatch() = runTest {
        respond(304)

        val fetch = api.getState(workspace, etag = "\"abc\"")

        assertEquals(StateFetch.NotModified, fetch)
        assertEquals("\"abc\"", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun getState_shouldTreatMissingGateAsNull() = runTest {
        respond(200, """{"phase":"3.1.0","pending_gate":null,"yolo_mode":true}""")

        val state = (api.getState(workspace, null) as StateFetch.Fresh).state

        assertNull(state.pendingGate)
        assertTrue(state.yoloMode)
    }

    @Test
    fun getState_shouldDecodeFreshWorkspace_whenOptionalSectionsAreNullOrBlank() = runTest {
        respond(200, """{"phase":"0","impact_analysis":null,"plan":{"description":"","execution":[],"systemDiagram":""}}""")

        val state = (api.getState(workspace, null) as StateFetch.Fresh).state

        assertEquals("0", state.phase)
        assertTrue(state.impactAnalysis.isEmpty())
        assertEquals(emptyList<PlanDiagram>(), state.plan?.diagrams)
    }

    @Test
    fun getState_shouldReadBareDiagramString_asOneUntitledDiagram() = runTest {
        respond(200, """{"phase":"2.0","plan":{"systemDiagram":"graph TD; A-->B"}}""")

        val state = (api.getState(workspace, null) as StateFetch.Fresh).state

        assertEquals(listOf(PlanDiagram(title = "", diagram = "graph TD; A-->B")), state.plan?.diagrams)
    }

    @Test
    fun request_shouldThrowAuthRequired_when401() = runTest {
        respond(401, """{"error":"authentication_required","reason":"invalid token","configured":true}""")

        val error = runCatching { api.listProjects() }.exceptionOrNull()

        assertTrue(error is AuthRequiredException)
        assertEquals("invalid token", error?.message)
    }

    @Test
    fun request_shouldPreferDetailsOverError_whenBuildingMessage() = runTest {
        respond(409, """{"error":"gate_not_pending","details":"No gate is waiting"}""")

        try {
            api.approve(workspace, "")
            fail("Expected HttpStatusException")
        } catch (error: HttpStatusException) {
            assertEquals(409, error.status)
            assertEquals("gate_not_pending", error.code)
            assertEquals("No gate is waiting", error.message)
        }
    }

    @Test
    fun approve_shouldOmitCommitMessage_whenBlank_andAccept202() = runTest {
        respond(202, """{"status":"accepted"}""")

        api.approve(workspace, " ")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/ws/qa-shop/qa/prep-review/approve", request.path)
        assertEquals("{}", request.body.readUtf8())
    }

    @Test
    fun reject_shouldSendComments() = runTest {
        respond(200, "{}")

        api.reject(workspace, "Split the plan")

        assertEquals("""{"comments":"Split the plan"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun listProjects_shouldThrowMalformed_whenBodyIsNotExpectedJson() = runTest {
        respond(200, "<html></html>")

        assertTrue(runCatching { api.listProjects() }.exceptionOrNull() is MalformedResponseException)
    }

    @Test
    fun request_shouldThrowUnreachable_whenServerDown() = runTest {
        server.shutdown()

        assertTrue(runCatching { api.listProjects() }.exceptionOrNull() is PanelUnreachableException)
    }

    @Test
    fun workspaceTerminalSocket_shouldAppendKind_andToken() {
        val url = api.workspaceTerminalSocket(workspace).url

        assertEquals(listOf("ws", "terminal", "qa-shop", "qa", "prep-review", "claude"), url.pathSegments)
        assertEquals("secret-token", url.queryParameter("token"))
    }

    @Test
    fun listTerminalSessions_shouldParseArray() = runTest {
        respond(200, """[{"name":"ws-qa-shop-qa-fresh","attached":true,"command":"claude"},{"name":"qa-android-shell"}]""")

        val sessions = api.listTerminalSessions()

        assertEquals(listOf("ws-qa-shop-qa-fresh", "qa-android-shell"), sessions.map { it.name })
        assertEquals(listOf(true, false), sessions.map { it.attached })
    }
}

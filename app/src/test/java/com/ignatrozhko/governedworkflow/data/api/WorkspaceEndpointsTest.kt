package com.ignatrozhko.governedworkflow.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WorkspaceEndpointsTest {
    private val server = MockWebServer()
    private lateinit var api: PanelApi
    private val workspace = WorkspaceRef("qa-shop", "qa/commit-ready")

    @Before
    fun setUp() {
        server.start()
        api = PanelApi(server.url("/"), "secret-token", OkHttpClient(), PanelJson)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun respond(body: String = """{"ok":true}""") {
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    @Test
    fun getState_shouldParseRound2Fields() = runTest {
        respond(
            """{"phase":"3.1.3","status":"active","branch":"qa/commit-ready","workspace_id":42,"review_mode":"full",
               "available_phases":["2.0","3.1.3"],"phaseHistory":[{"from":"2.0","to":"3.1.3","time":"2026-11-10T10:00:00"}],
               "plan":{"description":"Do it","execution":[{"id":"3.1","name":"Pricing","scope":{"must":["src/pricing.py"],"may":[]},
                 "tasks":[{"title":"Tweak","status":"done","files":["src/pricing.py"]}]}],"systemDiagram":[{"title":"Flow","diagram":"graph TD"}]},
               "research":[{"id":5,"topic":"Pricing","summary":"S","proven":1,"findings":[{"summary":"F","proof":{"type":"code","file":"a.py","line_start":3}}]}],
               "progress":{"3.1":{"summary":"done","details":{"files":["a"]}}},
               "impact_analysis":{"affected_flows":"checkout"},
               "comments":{"review:src/pricing.py":[{"id":9,"scope":"review","target":"src/pricing.py","text":"why?","line_start":123}]}}""",
        )

        val state = (api.getState(workspace, etag = null) as StateFetch.Fresh).state

        assertEquals(42L, state.workspaceId)
        assertEquals(listOf("2.0", "3.1.3"), state.availablePhases)
        assertEquals("3.1.3", state.phaseHistory.single().to)
        assertEquals("Tweak", state.plan?.execution?.single()?.tasks?.single()?.title)
        assertEquals("graph TD", state.plan?.diagrams?.single()?.diagram)
        assertEquals(ProofStatus.Proven, state.research.single().proofStatus)
        assertEquals(3, state.research.single().findings.single().proof?.lineStart)
        assertEquals("checkout", state.impactAnalysis["affected_flows"])
        assertEquals(123, state.reviewComments().single().lineStart)
    }

    @Test
    fun addComment_shouldSendLineAnchor_forReviewLineComment() = runTest {
        respond()

        api.addComment(workspace, NewComment(CommentTargets.REVIEW_SCOPE, "src/pricing.py", "why?", line = 123))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/ws/qa-shop/qa/commit-ready/comments", request.path)
        assertEquals(
            """{"scope":"review","target":"src/pricing.py","text":"why?","file_path":"src/pricing.py","line_start":123,"line_end":123}""",
            request.body.readUtf8(),
        )
    }

    @Test
    fun getDiff_shouldQueryCommitMode_withSha() = runTest {
        respond("""{"files":[{"path":"src/pricing.py","additions":1,"deletions":1,"diff":"@@ -1 +1 @@"}],"mode":"commit"}""")

        val files = api.getDiff(workspace, DiffSource.Commit("abc123", "Tweak"))

        assertEquals("/api/ws/qa-shop/qa/commit-ready/diff?mode=commit&commit=abc123", server.takeRequest().path)
        assertEquals("src/pricing.py", files.single().path)
    }

    @Test
    fun getDiff_shouldOmitMode_forBranchDiff() = runTest {
        respond("""{"files":[]}""")

        api.getDiff(workspace, DiffSource.Branch)

        assertEquals("/api/ws/qa-shop/qa/commit-ready/diff", server.takeRequest().path)
    }

    @Test
    fun listAnnotations_shouldPassCommitAsRef_andEncodeFilePath() = runTest {
        respond("""{"annotations":[{"id":1,"file_path":"src/a b.py","line_start":2,"line_end":4,"text":"Note"}]}""")

        val annotations = api.listAnnotations(workspace, "src/a b.py", commitSha = "abc")

        assertEquals("/api/ws/qa-shop/qa/commit-ready/annotations?file=src%2Fa%20b.py&ref=abc", server.takeRequest().path)
        assertEquals(4, annotations.single().lineEnd)
    }

    @Test
    fun resolveProposal_shouldSendExecutedStatus_forMarkApplied() = runTest {
        respond()

        api.resolveProposal(workspace, 273, ProposalDecision.MarkApplied)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/ws/qa-shop/qa/commit-ready/proposals/273/resolve", request.path)
        assertEquals("""{"status":"executed"}""", request.body.readUtf8())
    }

    @Test
    fun createWorkspace_shouldSendOnlyEnabledToggles() = runTest {
        respond("""{"workspace":"qa-new","branch":"qa/new","working_dir":"/w"}""")

        val created = api.createWorkspace("qa-shop", NewWorkspace("qa/new", "main", isYolo = false, isFast = true))

        val request = server.takeRequest()
        assertEquals("/api/projects/qa-shop/workspaces", request.path)
        assertEquals("""{"branch":"qa/new","source":"main","workflow_mode":"fast"}""", request.body.readUtf8())
        assertEquals("qa/new", created.branch)
    }

    @Test
    fun listSourceBranches_shouldMergeLocalAndRemote_withoutDuplicates() = runTest {
        respond("""{"local":["main","develop"],"remote":["develop","release/1.0"]}""")

        val branches = api.listSourceBranches("qa-shop")

        assertEquals("/api/projects/qa-shop/branches", server.takeRequest().path)
        assertEquals(listOf("main", "develop", "release/1.0"), branches)
    }

    @Test
    fun resolveAllReviewIssues_shouldPostResolution_toWorkspaceIdRoute() = runTest {
        respond()

        api.resolveAllReviewIssues(42, ReviewResolution.OutOfScope)

        val request = server.takeRequest()
        assertEquals("/api/workspaces/42/review-issues/resolve-all", request.path)
        assertEquals("""{"resolution":"out_of_scope"}""", request.body.readUtf8())
    }

    @Test
    fun setEnabledModules_shouldPostModuleList() = runTest {
        respond("""{"status":"saved"}""")

        api.setEnabledModules(setOf("jira"))

        assertEquals("""{"modules":["jira"]}""", server.takeRequest().body.readUtf8())
    }
}

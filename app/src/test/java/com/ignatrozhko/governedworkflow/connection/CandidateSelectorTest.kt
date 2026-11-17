package com.ignatrozhko.governedworkflow.connection

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateSelectorTest {
    private val lan = "http://192.168.1.10:5111"
    private val tailnet = "http://100.64.0.2:5111"
    private val local = "http://10.0.2.2:5111"

    private fun probeWith(outcomes: Map<String, Pair<Long, ProbeOutcome>>): suspend (String) -> ProbeOutcome = { url ->
        val (latency, outcome) = outcomes.getValue(url)
        delay(latency)
        outcome
    }

    @Test
    fun select_shouldPreferEarlierCandidate_whenBothReachableAndLaterIsFaster() = runTest {
        val probe = probeWith(mapOf(lan to (800L to ProbeOutcome.Reachable), tailnet to (50L to ProbeOutcome.Reachable)))

        val selection = CandidateSelector.select(listOf(lan, tailnet), probe)

        assertEquals(CandidateSelection.Chosen(lan), selection)
        assertEquals(800L, currentTime)
    }

    @Test
    fun select_shouldFallBackToLaterCandidate_whenEarlierUnreachable() = runTest {
        val probe = probeWith(
            mapOf(
                lan to (2_500L to ProbeOutcome.Unreachable("timeout")),
                tailnet to (100L to ProbeOutcome.Reachable),
                local to (100L to ProbeOutcome.Reachable),
            ),
        )

        val selection = CandidateSelector.select(listOf(lan, tailnet, local), probe)

        assertEquals(CandidateSelection.Chosen(tailnet), selection)
    }

    @Test
    fun select_shouldProbeConcurrently_whenAllFail() = runTest {
        val probe = probeWith(
            mapOf(lan to (1_000L to ProbeOutcome.Unreachable("timeout")), tailnet to (1_000L to ProbeOutcome.Unreachable("refused"))),
        )

        val selection = CandidateSelector.select(listOf(lan, tailnet), probe)

        assertEquals(
            CandidateSelection.NoneReachable(
                listOf(CandidateResult(lan, ProbeOutcome.Unreachable("timeout")), CandidateResult(tailnet, ProbeOutcome.Unreachable("refused"))),
            ),
            selection,
        )
        assertEquals(1_000L, currentTime)
    }

    @Test
    fun select_shouldReportTokenRejected_whenOnlyUnauthorizedAnswers() = runTest {
        val probe = probeWith(mapOf(lan to (10L to ProbeOutcome.Unreachable("refused")), tailnet to (10L to ProbeOutcome.Unauthorized)))

        assertEquals(CandidateSelection.TokenRejected(tailnet), CandidateSelector.select(listOf(lan, tailnet), probe))
    }

    @Test
    fun select_shouldReportNoneReachable_whenNoCandidates() = runTest {
        assertEquals(CandidateSelection.NoneReachable(emptyList()), CandidateSelector.select(emptyList()) { ProbeOutcome.Reachable })
    }
}

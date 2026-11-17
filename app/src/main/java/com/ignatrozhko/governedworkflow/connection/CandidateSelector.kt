package com.ignatrozhko.governedworkflow.connection

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

sealed interface ProbeOutcome {
    data object Reachable : ProbeOutcome
    data object Unauthorized : ProbeOutcome
    data class Unreachable(val reason: String) : ProbeOutcome
}

data class CandidateResult(val url: String, val outcome: ProbeOutcome)

sealed interface CandidateSelection {
    data class Chosen(val url: String) : CandidateSelection
    data class TokenRejected(val url: String) : CandidateSelection
    data class NoneReachable(val results: List<CandidateResult>) : CandidateSelection
}

/**
 * Probes every candidate concurrently but honours the user's ordering: a candidate wins as soon as it is
 * reachable and every candidate listed before it has failed, so a slow LAN address still beats a fast
 * Tailscale one when both work.
 */
object CandidateSelector {

    suspend fun select(candidates: List<String>, probe: suspend (String) -> ProbeOutcome): CandidateSelection =
        coroutineScope {
            val probes = candidates.map { url -> url to async { probe(url) } }
            val failures = mutableListOf<CandidateResult>()
            for ((url, pending) in probes) {
                val outcome = pending.await()
                if (outcome == ProbeOutcome.Reachable) {
                    probes.forEach { (_, other) -> other.cancel() }
                    return@coroutineScope CandidateSelection.Chosen(url)
                }
                failures += CandidateResult(url, outcome)
            }
            val rejected = failures.firstOrNull { it.outcome == ProbeOutcome.Unauthorized }
            if (rejected != null) CandidateSelection.TokenRejected(rejected.url) else CandidateSelection.NoneReachable(failures)
        }
}

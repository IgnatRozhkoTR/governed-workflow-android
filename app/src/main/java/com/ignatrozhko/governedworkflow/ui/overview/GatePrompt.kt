package com.ignatrozhko.governedworkflow.ui.overview

import com.ignatrozhko.governedworkflow.data.api.PendingGate

/** Where the human should look before deciding a gate. */
enum class GateEvidence(val linkLabel: String) {
    Changes("Review the diff first"),
    Plan("Review the plan first"),
    Research("Review the research first"),
}

/** The question a gate asks, phrased by what is being approved, and the evidence to check first. */
data class GatePrompt(val question: String, val evidence: GateEvidence)

/** Gates are told apart by their phase family: 1.x preparation, 2.x plan, 3.x commit, 4.x final approval. */
fun gatePrompt(gate: PendingGate): GatePrompt = when {
    gate.acceptsCommitMessage -> GatePrompt("Approve this commit?", GateEvidence.Changes)
    gate.phase.startsWith(PREPARATION_FAMILY) -> GatePrompt("Ready to plan?", GateEvidence.Research)
    gate.phase.startsWith(PLAN_FAMILY) -> GatePrompt("Approve the plan?", GateEvidence.Plan)
    else -> GatePrompt("Approve the final result?", GateEvidence.Changes)
}

private const val PREPARATION_FAMILY = "1."
private const val PLAN_FAMILY = "2."

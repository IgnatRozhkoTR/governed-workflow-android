package com.ignatrozhko.governedworkflow.ui.overview

import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.common.Step
import com.ignatrozhko.governedworkflow.ui.common.StepState

/** The coarse stops of every run; each phase id belongs to one by its leading number. */
private enum class Station(val title: String, val majors: Set<Int>) {
    Prepare("Prepare", setOf(0, 1)),
    Plan("Plan", setOf(2)),
    Build("Build", setOf(3)),
    Review("Review", setOf(4)),
    Reflect("Reflect", setOf(5)),
    Done("Done", setOf(6)),
    ;

    companion object {
        fun of(phase: String): Station {
            val major = phase.substringBefore('.').toIntOrNull() ?: 0
            return entries.firstOrNull { major in it.majors } ?: Prepare
        }
    }
}

/**
 * The stepper rows for Overview: the stations already passed folded into one row, the current one with what it is
 * doing, the next one, and the rest folded again, so the card stays four rows tall at most.
 */
fun workflowSteps(state: WorkspaceState, phaseNames: PhaseNames): List<Step> {
    val current = Station.of(state.phase)
    if (current == Station.Done) return listOfNotNull(foldedStep(Station.entries.dropLast(1), StepState.Done), foldedStep(listOf(current), StepState.Done))
    val upcoming = Station.entries.drop(current.ordinal + 1)
    return listOfNotNull(
        foldedStep(Station.entries.take(current.ordinal), StepState.Done),
        currentStep(current, state, phaseNames),
        foldedStep(upcoming.take(1), StepState.Todo),
        foldedStep(upcoming.drop(1), StepState.Todo),
    )
}

private fun foldedStep(stations: List<Station>, state: StepState): Step? =
    stations.takeIf { it.isNotEmpty() }?.let { Step(it.joinToString(" · ") { station -> station.title }, null, state) }

private fun currentStep(station: Station, state: WorkspaceState, phaseNames: PhaseNames): Step {
    val phaseName = phaseNames.nameFor(state.phase)
    val title = if (station == Station.Build) "${station.title} · ${buildProgress(state)}" else station.title
    return if (state.pendingGate != null) {
        Step(title, "Waiting at ${phaseName?.lowercase() ?: "gate ${state.phase}"}", StepState.Gate)
    } else {
        Step(title, listOfNotNull(state.phase, phaseName).joinToString(" "), StepState.Running)
    }
}

/** "sub-phase 3.2 of 3.3": the implementation phase id carries the sub-phase, the plan says how many there are. */
private fun buildProgress(state: WorkspaceState): String {
    val subphase = state.phase.split('.').take(2).joinToString(".")
    val total = state.plan?.execution?.size ?: 0
    return if (total > 0) "sub-phase $subphase of 3.$total" else "sub-phase $subphase"
}

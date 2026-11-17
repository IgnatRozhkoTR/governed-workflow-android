package com.ignatrozhko.governedworkflow.gates

import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import org.junit.Assert.assertEquals
import org.junit.Test

class GateTrackerTest {
    private val tracker = GateTracker()
    private val shop = WorkspaceRef("qa-shop", "qa/commit-ready")
    private val multi = WorkspaceRef("qa-multi", "qa/fresh")

    private fun gate(workspace: WorkspaceRef, phase: String) = WaitingGate(workspace, phase, workspace.branch)

    @Test
    fun update_shouldAnnounceEveryGate_onFirstScan() {
        val changes = tracker.update(listOf(gate(shop, "3.1.3"), gate(multi, "1.4")))

        assertEquals(listOf(gate(shop, "3.1.3"), gate(multi, "1.4")), changes.appeared)
        assertEquals(emptySet<WorkspaceRef>(), changes.cleared)
    }

    @Test
    fun update_shouldNotAnnounceAgain_whenSameGateStillWaits() {
        tracker.update(listOf(gate(shop, "3.1.3")))

        val changes = tracker.update(listOf(gate(shop, "3.1.3")))

        assertEquals(emptyList<WaitingGate>(), changes.appeared)
    }

    @Test
    fun update_shouldAnnounce_whenWorkspaceWaitsAtANewPhase() {
        tracker.update(listOf(gate(shop, "3.1.3")))

        val changes = tracker.update(listOf(gate(shop, "3.2.3")))

        assertEquals(listOf(gate(shop, "3.2.3")), changes.appeared)
    }

    @Test
    fun update_shouldReportCleared_whenGateIsAnswered() {
        tracker.update(listOf(gate(shop, "3.1.3"), gate(multi, "1.4")))

        val changes = tracker.update(listOf(gate(multi, "1.4")))

        assertEquals(setOf(shop), changes.cleared)
        assertEquals(emptyList<WaitingGate>(), changes.appeared)
    }

    @Test
    fun update_shouldAnnounceAgain_whenGateReturnsAfterBeingCleared() {
        tracker.update(listOf(gate(shop, "3.1.3")))
        tracker.update(emptyList())

        val changes = tracker.update(listOf(gate(shop, "3.1.3")))

        assertEquals(listOf(gate(shop, "3.1.3")), changes.appeared)
    }
}

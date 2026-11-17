package com.ignatrozhko.governedworkflow.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseNamesTest {
    private val names = PhaseNames(
        listOf(
            PhaseInfo("1.4", "Preparation Review", isUserGate = true),
            PhaseInfo("3.x.1", "Verification"),
            PhaseInfo("3.x.3", "Commit Approval", isUserGate = true),
            PhaseInfo("6", "Done"),
        ),
    )

    @Test
    fun nameFor_shouldPreferExactId() {
        assertEquals("Preparation Review", names.nameFor("1.4"))
    }

    @Test
    fun nameFor_shouldMatchImplementationTemplate_forConcreteSubphase() {
        assertEquals("Verification", names.nameFor("3.2.1"))
        assertTrue(names.isUserGate("3.12.3"))
    }

    @Test
    fun nameFor_shouldNotMatchTemplate_whenSegmentCountDiffers() {
        assertNull(names.nameFor("3.2"))
        assertFalse(names.isUserGate("3.2.1.3"))
    }

    @Test
    fun label_shouldFallBackToId_whenPhaseIsUnknown() {
        assertEquals("9.9", names.label("9.9"))
        assertEquals("6 · Done", names.label("6"))
    }
}

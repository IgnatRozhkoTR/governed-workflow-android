package com.ignatrozhko.governedworkflow.data.api

/**
 * Resolves display names for concrete phase ids. The catalogue names implementation phases once as a
 * template (`3.x.1` "Verification"); a concrete id such as `3.2.1` matches it segment by segment.
 */
class PhaseNames(private val catalogue: List<PhaseInfo>) {
    fun nameFor(phaseId: String): String? = find(phaseId)?.name

    fun isUserGate(phaseId: String): Boolean = find(phaseId)?.isUserGate == true

    fun canHoldGate(phaseId: String): Boolean = find(phaseId)?.isUserGate ?: true

    fun label(phaseId: String): String = nameFor(phaseId)?.let { "$phaseId · $it" } ?: phaseId

    private fun find(phaseId: String): PhaseInfo? =
        catalogue.firstOrNull { it.id == phaseId } ?: catalogue.firstOrNull { matchesTemplate(it.id, phaseId) }

    private fun matchesTemplate(template: String, phaseId: String): Boolean {
        val templateParts = template.split('.')
        val idParts = phaseId.split('.')
        return TEMPLATE_WILDCARD in templateParts &&
            templateParts.size == idParts.size &&
            templateParts.zip(idParts).all { (expected, actual) -> expected == TEMPLATE_WILDCARD || expected == actual }
    }

    private companion object {
        const val TEMPLATE_WILDCARD = "x"
    }
}

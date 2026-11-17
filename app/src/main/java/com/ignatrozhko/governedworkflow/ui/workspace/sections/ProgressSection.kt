package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.ProgressEntry
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun ProgressSection(state: WorkspaceState, phaseNames: PhaseNames, modifier: Modifier = Modifier) {
    if (state.progress.isEmpty()) {
        EmptySection("No progress reported yet.")
        return
    }
    val entries = state.progress.entries.map { it.key to it.value }.sortedWith(compareBy(PhaseOrder) { it.first })
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        items(entries, key = { it.first }) { (phase, entry) -> ProgressCard(phaseNames.label(phase), entry) }
    }
}

/**
 * Progress keys are phase ids of mixed depth (`3.1` for a sub-phase, `3.1.0` for its steps), so they are ordered
 * segment by segment as numbers; plain string order would put `10` before `2`.
 */
private val PhaseOrder = Comparator<String> { first, second ->
    val firstParts = first.split('.')
    val secondParts = second.split('.')
    firstParts.zip(secondParts)
        .map { (a, b) -> compareSegments(a, b) }
        .firstOrNull { it != 0 }
        ?: firstParts.size.compareTo(secondParts.size)
}

private fun compareSegments(first: String, second: String): Int {
    val firstNumber = first.toIntOrNull()
    val secondNumber = second.toIntOrNull()
    return if (firstNumber != null && secondNumber != null) firstNumber.compareTo(secondNumber) else first.compareTo(second)
}

@Composable
private fun ProgressCard(title: String, entry: ProgressEntry) {
    SectionCard(title = title) {
        MarkdownText(entry.summary)
        entry.details?.forEach { (key, value) -> SecondaryText("${key.replace('_', ' ')}: ${value.display()}") }
    }
}

private fun JsonElement.display(): String = when (this) {
    is JsonPrimitive -> content
    is JsonArray -> joinToString(", ") { it.display() }
    else -> toString()
}

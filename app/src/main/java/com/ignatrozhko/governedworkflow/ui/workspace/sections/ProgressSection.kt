package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ignatrozhko.governedworkflow.data.api.PhaseNames
import com.ignatrozhko.governedworkflow.data.api.PhaseOrder
import com.ignatrozhko.governedworkflow.data.api.ProgressEntry
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.previewLine
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
        items(entries, key = { it.first }) { (phase, entry) -> ProgressCard(phase, phaseNames.label(phase), entry) }
    }
}

@Composable
private fun ProgressCard(phase: String, title: String, entry: ProgressEntry) {
    CollapsibleCard(title = title, memoryKey = "progress:$phase", subtitle = previewLine(entry.summary)) {
        MarkdownText(entry.summary)
        entry.details?.forEach { (key, value) -> SecondaryText("${key.replace('_', ' ')}: ${value.display()}") }
    }
}

private fun JsonElement.display(): String = when (this) {
    is JsonPrimitive -> content
    is JsonArray -> joinToString(", ") { it.display() }
    else -> toString()
}

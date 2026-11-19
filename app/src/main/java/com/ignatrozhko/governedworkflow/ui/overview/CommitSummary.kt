package com.ignatrozhko.governedworkflow.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.VerificationRun
import com.ignatrozhko.governedworkflow.ui.common.PanelCard
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleSection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.countLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

/** What the next commit contains and whether verification passed; rows appear only for what the panel has to show. */
@Composable
fun CommitSummaryCard(changedFiles: List<DiffFile>, verification: VerificationRun?, onOpenChanges: () -> Unit) {
    if (changedFiles.isEmpty() && verification == null) return
    PanelCard {
        if (changedFiles.isNotEmpty()) ChangesRow(changedFiles, onOpenChanges)
        if (changedFiles.isNotEmpty() && verification != null) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        if (verification != null) VerificationRow(verification)
    }
}

@Composable
private fun ChangesRow(files: List<DiffFile>, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Open changes", onClick = onOpen).heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Changes in this commit", style = MaterialTheme.typography.titleMedium)
            SecondaryText(subtitleOf(countLabel(files.size, "file"), "+${files.sumOf { it.additions }} −${files.sumOf { it.deletions }}"))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VerificationRow(run: VerificationRun) {
    CollapsibleSection(
        title = "Verification",
        memoryKey = "overview:verification",
        subtitle = "${run.passedSteps} of ${run.steps.size} steps passed",
        badge = { Pill(run.status, verificationTone(run.status)) },
        horizontalPadding = 16.dp,
    ) {
        run.steps.forEach { SecondaryText("${it.profileName} / ${it.stepName} · ${it.status}") }
    }
}

private fun verificationTone(status: String) = when (status) {
    "passed" -> PillTone.Ok
    "failed" -> PillTone.Bad
    else -> PillTone.Warn
}

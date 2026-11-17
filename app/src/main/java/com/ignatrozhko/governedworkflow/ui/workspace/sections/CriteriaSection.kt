package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.Criterion
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.listCriteria
import com.ignatrozhko.governedworkflow.data.api.validateCriterion
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.StatusPill

class CriteriaViewModel(workspace: WorkspaceRef, connectionManager: ConnectionManager, stateStore: WorkspaceStateStore) :
    SectionDataViewModel<List<Criterion>>(workspace, connectionManager, stateStore) {
    override suspend fun load(api: PanelApi) = api.listCriteria(workspace)

    fun validate(criterionId: Long, isPassed: Boolean) =
        actAndReload(if (isPassed) "Criterion accepted" else "Criterion rejected") { it.validateCriterion(workspace, criterionId, isPassed) }
}

@Composable
fun CriteriaSection(criteria: List<Criterion>, isBusy: Boolean, onValidate: (Long, Boolean) -> Unit, modifier: Modifier = Modifier) {
    if (criteria.isEmpty()) {
        EmptySection("No acceptance criteria.")
        return
    }
    LazyColumn(modifier, contentPadding = SectionContentPadding, verticalArrangement = SectionSpacing) {
        items(criteria, key = { it.id }) { CriterionCard(it, isBusy, onValidate) }
    }
}

/** Only custom criteria are checked by a person; test and lint criteria are validated by running them. */
@Composable
private fun CriterionCard(criterion: Criterion, isBusy: Boolean, onValidate: (Long, Boolean) -> Unit) {
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(criterion.type.replace('_', ' '), PanelColors.Indigo)
            ValidationPill(criterion.validated)
        }
        MarkdownText(criterion.description)
        criterion.validationMessage?.takeIf { it.isNotBlank() }?.let { SecondaryText(it) }
        criterion.details?.forEach { (key, value) -> SecondaryText("${key.replace('_', ' ')}: ${value.toString().trim('"')}") }
        if (criterion.type == CUSTOM_TYPE) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onValidate(criterion.id, true) }, enabled = !isBusy && criterion.validated != PASSED) { Text("Accept") }
                OutlinedButton(onClick = { onValidate(criterion.id, false) }, enabled = !isBusy && criterion.validated != FAILED) { Text("Reject") }
            }
        }
    }
}

@Composable
private fun ValidationPill(validated: Int) = when (validated) {
    PASSED -> StatusPill("passed", PanelColors.Green)
    FAILED -> StatusPill("failed", PanelColors.Red)
    else -> StatusPill("not validated", PanelColors.Amber)
}

private const val CUSTOM_TYPE = "custom"
private const val PASSED = 1
private const val FAILED = -1

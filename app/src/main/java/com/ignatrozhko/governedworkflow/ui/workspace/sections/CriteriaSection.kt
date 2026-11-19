package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.connection.ConnectionManager
import com.ignatrozhko.governedworkflow.data.api.Criterion
import com.ignatrozhko.governedworkflow.data.api.PanelApi
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import com.ignatrozhko.governedworkflow.data.api.listCriteria
import com.ignatrozhko.governedworkflow.data.api.validateCriterion
import com.ignatrozhko.governedworkflow.data.workspace.WorkspaceStateStore
import com.ignatrozhko.governedworkflow.ui.common.FieldLabel
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.Pill
import com.ignatrozhko.governedworkflow.ui.common.PillTone
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionDataViewModel
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionSpacing
import com.ignatrozhko.governedworkflow.ui.workspace.common.previewLine

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
    CollapsibleCard(
        title = previewLine(criterion.description).ifBlank { "Criterion ${criterion.id}" },
        memoryKey = "criterion:${criterion.id}",
        subtitle = criterion.type.replace('_', ' ').replaceFirstChar { it.uppercase() },
        titleStyle = MaterialTheme.typography.bodyLarge,
        badge = { ValidationPill(criterion.validated) },
    ) {
        MarkdownText(criterion.description)
        criterion.validationMessage?.takeIf { it.isNotBlank() }?.let { ValidationMessage(it) }
        criterion.details?.forEach { (key, value) -> CriterionDetail(key.replace('_', ' ').replaceFirstChar { it.uppercase() }, value.toString().trim('"')) }
        if (criterion.type == CUSTOM_TYPE) CustomCriterionActions(criterion, isBusy, onValidate)
    }
}

@Composable
private fun ValidationMessage(message: String) {
    Text(
        message,
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun CriterionDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label)
        Text(value, style = MonoText.small)
    }
}

@Composable
private fun CustomCriterionActions(criterion: Criterion, isBusy: Boolean, onValidate: (Long, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedActionButton("Reject", { onValidate(criterion.id, false) }, isEnabled = !isBusy && criterion.validated != FAILED)
        PrimaryButton("Accept", { onValidate(criterion.id, true) }, isEnabled = !isBusy && criterion.validated != PASSED, icon = rememberVectorPainter(Icons.Filled.Check))
    }
}

@Composable
private fun ValidationPill(validated: Int) = when (validated) {
    PASSED -> Pill("passed", PillTone.Ok)
    FAILED -> Pill("failed", PillTone.Bad)
    else -> Pill("not validated", PillTone.Neutral)
}

private const val CUSTOM_TYPE = "custom"
private const val PASSED = 1
private const val FAILED = -1

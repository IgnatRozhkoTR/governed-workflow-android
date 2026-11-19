package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Segmented control for two to four mutually exclusive options; labels stay short so it fits at 360dp. */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = SEGMENT_HEIGHT)
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .selectable(isSelected, role = Role.Tab) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    Modifier.padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Round outlined chip; the selected one inverts so it reads without relying on the accent color. */
@Composable
fun ChoiceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = CHIP_HEIGHT)
            .border(1.dp, if (isSelected) scheme.onSurface else scheme.outline, CircleShape)
            .background(if (isSelected) scheme.onSurface else scheme.surface, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = MaterialTheme.typography.labelSmall.fontWeight),
            color = if (isSelected) scheme.background else scheme.onSurface,
            maxLines = 1,
        )
        trailing?.invoke()
    }
}

/** Switch whose off state stays legible at night, where the theme outline is a faint border color. */
@Composable
fun ToggleSwitch(isChecked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, isEnabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    Switch(
        checked = isChecked,
        onCheckedChange = onChange,
        modifier = modifier,
        enabled = isEnabled,
        colors = SwitchDefaults.colors(
            uncheckedThumbColor = scheme.onSurfaceVariant,
            uncheckedTrackColor = scheme.surfaceVariant,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

private val SEGMENT_HEIGHT = 44.dp
private val CHIP_HEIGHT = 36.dp

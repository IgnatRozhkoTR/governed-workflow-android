package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** Several [ListRow]s on one card, separated by hairlines. */
@Composable
fun <T> RowGroup(items: List<T>, modifier: Modifier = Modifier, row: @Composable (T) -> Unit) {
    PanelCard(modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            row(item)
        }
    }
}

/** Tappable row with a title, a one-line subtitle and a trailing chevron; the usual way into the next screen. */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    ListRowLayout(onClick, modifier, leading, trailing) {
        Text(title, style = titleStyle)
        subtitle?.let { Text(it, style = subtitleStyle, color = subtitleColor) }
    }
}

@Composable
fun ListRowLayout(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    showsChevron: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        trailing()
        if (showsChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

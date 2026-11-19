package com.ignatrozhko.governedworkflow.ui.workspace.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.ui.common.PanelCard

/**
 * Which collapsible blocks the user opened, kept for the whole workspace visit: sections are swapped in and out of
 * composition as the user moves between them, so state saved inside a section would reset on every return.
 */
class ExpansionMemory {
    private val expanded = mutableStateMapOf<String, Boolean>()

    fun isExpanded(key: String, default: Boolean): Boolean = expanded[key] ?: default

    fun setExpanded(key: String, isExpanded: Boolean) {
        expanded[key] = isExpanded
    }
}

val LocalExpansionMemory = staticCompositionLocalOf { ExpansionMemory() }

/**
 * The one expand/collapse affordance used across the workspace screens: a tappable header with a title, optional
 * one-line subtitle and badge, and a chevron that turns as the content slides open.
 *
 * [memoryKey] must be unique within the workspace; it is how the open state survives leaving and reopening a section.
 */
@Composable
fun CollapsibleSection(
    title: String,
    memoryKey: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    horizontalPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val memory = LocalExpansionMemory.current
    val isExpanded = memory.isExpanded(memoryKey, initiallyExpanded)
    Column(modifier.fillMaxWidth()) {
        CollapsibleHeader(title, subtitle, badge, titleStyle, isExpanded, horizontalPadding) {
            memory.setExpanded(memoryKey, !isExpanded)
        }
        AnimatedVisibility(isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier.fillMaxWidth().padding(start = horizontalPadding, end = horizontalPadding, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

/** A [CollapsibleSection] on its own card, for the top-level items of a list. */
@Composable
fun CollapsibleCard(
    title: String,
    memoryKey: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    PanelCard(modifier) {
        CollapsibleSection(
            title = title,
            memoryKey = memoryKey,
            subtitle = subtitle,
            badge = badge,
            initiallyExpanded = initiallyExpanded,
            horizontalPadding = CARD_PADDING,
            content = content,
        )
    }
}

@Composable
private fun CollapsibleHeader(
    title: String,
    subtitle: String?,
    badge: (@Composable () -> Unit)?,
    titleStyle: TextStyle,
    isExpanded: Boolean,
    horizontalPadding: Dp,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(if (isExpanded) EXPANDED_ROTATION else 0f, label = "chevron")
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClickLabel = if (isExpanded) "Collapse" else "Expand", onClick = onToggle)
            .semantics { stateDescription = if (isExpanded) "Expanded" else "Collapsed" }
            .heightIn(min = MIN_HEADER_HEIGHT)
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = titleStyle,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_TITLE_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        badge?.invoke()
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, Modifier.rotate(chevronRotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val CARD_PADDING = 16.dp
private val MIN_HEADER_HEIGHT = 52.dp
private const val EXPANDED_ROTATION = 90f
private const val COLLAPSED_TITLE_LINES = 2

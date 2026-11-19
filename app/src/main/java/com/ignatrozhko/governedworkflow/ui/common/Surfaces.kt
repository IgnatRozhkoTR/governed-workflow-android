package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint

/**
 * The one card surface: an outlined panel at night, where shadows don't read, and the same outline plus a soft
 * shadow by day.
 */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val elevated = if (Checkpoint.colors.isNight) modifier else modifier.shadow(DAY_SHADOW, shape, ambientColor = DayShadow, spotColor = DayShadow)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    val body: @Composable () -> Unit = { Column(content = content) }
    if (onClick == null) {
        Surface(elevated.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surface, border = border, content = body)
    } else {
        Surface(onClick, elevated.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surface, border = border, content = body)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    icon: Painter? = null,
    height: Dp = BUTTON_HEIGHT,
) {
    Button(
        onClick,
        modifier.heightIn(min = height),
        enabled = isEnabled,
        shape = CircleShape,
        contentPadding = BUTTON_PADDING,
    ) { ButtonLabel(text, icon) }
}

@Composable
fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    icon: Painter? = null,
    height: Dp = BUTTON_HEIGHT,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(
        onClick,
        modifier.heightIn(min = height),
        enabled = isEnabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = contentColor),
        contentPadding = BUTTON_PADDING,
    ) { ButtonLabel(text, icon) }
}

/** Outlined in the surface color with red text: destructive, but never louder than the primary action. */
@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isEnabled: Boolean = true) {
    OutlinedActionButton(text, onClick, modifier, isEnabled, contentColor = MaterialTheme.colorScheme.error)
}

@Composable
private fun RowScope.ButtonLabel(text: String, icon: Painter?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

/**
 * Decision bar pinned between the content and the bottom navigation; [isAtScreenBottom] when no navigation sits below
 * it, so its surface runs under the system navigation bar.
 */
@Composable
fun ActionFooter(modifier: Modifier = Modifier, isAtScreenBottom: Boolean = false, content: @Composable RowScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(if (isAtScreenBottom) Modifier.navigationBarsPadding() else Modifier) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: Painter? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, Modifier.size(40.dp), tint = iconTint)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
    }
}

/** Small dim caption above a group of rows or a form field. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Bottom inset for scrolling content that ends above the system navigation bar. */
fun Modifier.sheetContentPadding() = navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)

private val BUTTON_HEIGHT = 44.dp
private val BUTTON_PADDING = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
private val DAY_SHADOW = 6.dp
private val DayShadow = Color(0x33141922)

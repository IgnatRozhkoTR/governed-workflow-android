package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText

/** Status meaning of a [Pill]; [Gate] is the only orange and only ever means "a gate is waiting on you". */
enum class PillTone { Gate, Ok, Warn, Bad, Info, Line, Neutral }

private class PillColors(val background: Color, val content: Color, val dot: Color)

@Composable
private fun PillTone.colors(): PillColors {
    val scheme = MaterialTheme.colorScheme
    val extra = Checkpoint.colors
    return when (this) {
        PillTone.Gate -> PillColors(extra.gateSoft, extra.onGateSoft, extra.gate)
        PillTone.Ok -> PillColors(extra.okSoft, extra.onOkSoft, extra.ok)
        PillTone.Warn -> PillColors(extra.warnSoft, extra.onWarnSoft, extra.warn)
        PillTone.Bad -> PillColors(scheme.errorContainer, scheme.onErrorContainer, scheme.error)
        PillTone.Info -> PillColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary)
        PillTone.Line -> PillColors(Color.Transparent, scheme.onSurfaceVariant, scheme.onSurfaceVariant)
        PillTone.Neutral -> PillColors(scheme.surfaceVariant, scheme.onSurfaceVariant, scheme.onSurfaceVariant)
    }
}

/** Status text in a round pill; color is never the only cue, so the text always names the state. */
@Composable
fun Pill(text: String, tone: PillTone, modifier: Modifier = Modifier, hasDot: Boolean = false) {
    val colors = tone.colors()
    val outline = if (tone == PillTone.Line) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier
    Row(
        modifier
            .heightIn(min = PILL_HEIGHT)
            .then(outline)
            .background(colors.background, CircleShape)
            .padding(horizontal = 9.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hasDot) StatusDot(colors.dot)
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.content, maxLines = 1)
    }
}

/** Mono, square-ish label for phase numbers, agent names, criteria types and file references. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .heightIn(min = PILL_HEIGHT)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MonoText.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

/** Small round counter next to a label; orange only when it counts waiting gates. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier, isGate: Boolean = false) {
    val background = if (isGate) Checkpoint.colors.gate else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (isGate) Checkpoint.colors.onGate else MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.defaultMinSize(BADGE_SIZE, BADGE_SIZE).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            count.toString(),
            Modifier.padding(horizontal = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(color, CircleShape))
}

private val PILL_HEIGHT = 22.dp
private val BADGE_SIZE = 20.dp

package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint

enum class StepState { Done, Gate, Running, Todo }

data class Step(val title: String, val detail: String?, val state: StepState)

/** Vertical rail of workflow stations: done in green, the waiting gate in orange, the running phase in blue. */
@Composable
fun PhaseStepper(steps: List<Step>, modifier: Modifier = Modifier) {
    Column(modifier) {
        steps.forEachIndexed { index, step -> StepRow(step, hasRailAbove = index > 0, hasRailBelow = index < steps.lastIndex) }
    }
}

@Composable
private fun StepRow(step: Step, hasRailAbove: Boolean, hasRailBelow: Boolean) {
    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepNode(step.state, hasRailAbove, hasRailBelow, Modifier.width(NODE_COLUMN).fillMaxHeight())
        Column(Modifier.padding(vertical = 6.dp)) {
            val isTodo = step.state == StepState.Todo
            Text(
                step.title,
                style = if (isTodo) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                color = if (isTodo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            step.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun StepNode(state: StepState, hasRailAbove: Boolean, hasRailBelow: Boolean, modifier: Modifier) {
    val doneColor = Checkpoint.colors.ok
    val pendingColor = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val (fill, halo) = when (state) {
        StepState.Done -> doneColor to null
        StepState.Gate -> Checkpoint.colors.gate to Checkpoint.colors.gateSoft
        StepState.Running -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        StepState.Todo -> surface to null
    }
    val railAbove = if (state == StepState.Todo) pendingColor else doneColor
    val railBelow = if (state == StepState.Done) doneColor else pendingColor
    Canvas(modifier) {
        val center = Offset(size.width / 2, NODE_CENTER_Y.toPx())
        val railWidth = RAIL_WIDTH.toPx()
        if (hasRailAbove) drawLine(railAbove, Offset(center.x, 0f), center, railWidth)
        if (hasRailBelow) drawLine(railBelow, center, Offset(center.x, size.height), railWidth)
        val radius = NODE_SIZE.toPx() / 2
        halo?.let { drawCircle(it, radius + HALO.toPx(), center) }
        drawCircle(fill, radius, center)
        if (state == StepState.Todo) drawCircle(pendingColor, radius - 1.dp.toPx(), center, style = Stroke(2.dp.toPx()))
    }
}

private val NODE_COLUMN = 24.dp
private val NODE_SIZE = 14.dp
private val NODE_CENTER_Y = 16.dp
private val RAIL_WIDTH = 3.dp
private val HALO = 5.dp

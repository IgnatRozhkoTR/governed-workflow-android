package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.terminal.core.StickyKey
import com.ignatrozhko.governedworkflow.terminal.core.StickyLevel
import com.ignatrozhko.governedworkflow.terminal.core.StickyModifiers
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Termux-style key rows; touches never take focus away from the terminal, so the keyboard stays up.
 * Keys share the width evenly until they would get narrower than [MIN_KEY_WIDTH]; then the rows scroll sideways.
 */
@Composable
fun ExtraKeysBar(
    rows: List<List<ExtraKey>>,
    stickyModifiers: StickyModifiers,
    onAction: (ExtraKeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 8.dp, vertical = 4.dp)) {
        val isScrolling = maxWidth / rows.maxOf { it.size } < MIN_KEY_WIDTH
        Column(if (isScrolling) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                Row(if (isScrolling) Modifier else Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        val keyModifier = if (isScrolling) Modifier.width(MIN_KEY_WIDTH) else Modifier.weight(1f)
                        ExtraKeyButton(key, stickyModifiers.levelOf(key.action), onAction, keyModifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraKeyButton(key: ExtraKey, stickyLevel: StickyLevel, onAction: (ExtraKeyAction) -> Unit, modifier: Modifier) {
    val haptics = LocalHapticFeedback.current
    val currentOnAction by rememberUpdatedState(onAction)
    var isPressed by remember { mutableStateOf(false) }
    val background = when {
        stickyLevel == StickyLevel.Locked -> MaterialTheme.colorScheme.primary
        stickyLevel == StickyLevel.Once || isPressed -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .padding(KEY_GAP)
            .background(background, RoundedCornerShape(10.dp))
            .pointerInput(key) {
                detectTapGestures(onPress = {
                    isPressed = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnAction(key.action)
                    coroutineScope {
                        val repeat = if (key.isRepeatable) launch { repeatWhileHeld { currentOnAction(key.action) } } else null
                        tryAwaitRelease()
                        repeat?.cancel()
                    }
                    isPressed = false
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (stickyLevel == StickyLevel.Locked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        if (key.iconRes != null) {
            Icon(painterResource(key.iconRes), contentDescription = key.label, tint = contentColor, modifier = Modifier.size(18.dp))
        } else {
            Text(key.label, style = MonoText.small, softWrap = false, textAlign = TextAlign.Center, maxLines = 1, color = contentColor)
        }
    }
}

private suspend fun repeatWhileHeld(action: () -> Unit) {
    delay(REPEAT_START_DELAY_MS)
    while (true) {
        action()
        delay(REPEAT_INTERVAL_MS)
    }
}

private fun StickyModifiers.levelOf(action: ExtraKeyAction): StickyLevel = when ((action as? ExtraKeyAction.Sticky)?.key) {
    StickyKey.Ctrl -> ctrl
    StickyKey.Alt -> alt
    null -> StickyLevel.Off
}

private val KEY_HEIGHT = 42.dp
private val KEY_GAP = 2.dp
private val MIN_KEY_WIDTH = 34.dp
private const val REPEAT_START_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 60L

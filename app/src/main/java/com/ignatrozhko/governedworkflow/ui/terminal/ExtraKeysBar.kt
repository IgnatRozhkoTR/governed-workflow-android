package com.ignatrozhko.governedworkflow.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ignatrozhko.governedworkflow.terminal.core.StickyKey
import com.ignatrozhko.governedworkflow.terminal.core.StickyLevel
import com.ignatrozhko.governedworkflow.terminal.core.StickyModifiers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Termux-style key rows; touches never take focus away from the terminal, so the keyboard stays up. */
@Composable
fun ExtraKeysBar(stickyModifiers: StickyModifiers, onAction: (ExtraKeyAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(2.dp)) {
        ExtraKeyLayout.rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    ExtraKeyButton(key, stickyModifiers.levelOf(key.action), onAction, Modifier.weight(1f))
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
        stickyLevel == StickyLevel.Once || isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .padding(1.dp)
            .background(background, RoundedCornerShape(6.dp))
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
        Text(
            key.label,
            fontSize = 12.sp,
            softWrap = false,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (stickyLevel == StickyLevel.Locked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
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

private val KEY_HEIGHT = 40.dp
private const val REPEAT_START_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 60L

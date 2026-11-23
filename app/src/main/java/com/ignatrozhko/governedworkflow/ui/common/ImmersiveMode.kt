package com.ignatrozhko.governedworkflow.ui.common

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Hides the status and navigation bars while [isImmersive]; a swipe from a screen edge shows them for a moment. */
@Composable
fun ImmersiveMode(isImmersive: Boolean) {
    val window = checkNotNull(LocalActivity.current) { "ImmersiveMode needs to be hosted by an activity" }.window
    DisposableEffect(window, isImmersive) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isImmersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (isImmersive) controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

package com.ignatrozhko.governedworkflow.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val CheckpointShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val LocalCheckpointColors = staticCompositionLocalOf { NightExtra }

/** The Checkpoint colors Material 3 has no slot for: gate, status, diff and terminal. */
object Checkpoint {
    val colors: CheckpointExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCheckpointColors.current
}

/** Night and day are separate token sets that follow the system setting; the system bar icons follow the active one. */
@Composable
fun GovernedWorkflowTheme(isNight: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    SystemBarAppearance(isNight)
    CompositionLocalProvider(LocalCheckpointColors provides if (isNight) NightExtra else DayExtra) {
        MaterialTheme(
            colorScheme = if (isNight) NightColors else DayColors,
            typography = CheckpointTypography,
            shapes = CheckpointShapes,
            content = content,
        )
    }
}

@Composable
private fun SystemBarAppearance(isNight: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isNight
            isAppearanceLightNavigationBars = !isNight
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

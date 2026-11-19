package com.ignatrozhko.governedworkflow.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

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

/** Night and day are separate token sets, following the system unless the user picked one; the system bars follow the active one. */
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
    LaunchedEffect(isNight) {
        val activity = view.context.findActivity() as? ComponentActivity ?: return@LaunchedEffect
        val lightScrim = DayColors.background.copy(alpha = NAVIGATION_SCRIM_ALPHA).toArgb()
        val darkScrim = NightColors.background.copy(alpha = NAVIGATION_SCRIM_ALPHA).toArgb()
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isNight },
            navigationBarStyle = SystemBarStyle.auto(lightScrim, darkScrim) { isNight },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val NAVIGATION_SCRIM_ALPHA = 0.9f

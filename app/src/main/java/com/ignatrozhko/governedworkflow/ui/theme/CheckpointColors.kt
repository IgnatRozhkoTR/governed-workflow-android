package com.ignatrozhko.governedworkflow.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object CheckpointNight {
    val background = Color(0xFF0D1117)
    val surface = Color(0xFF151B24)
    val surface2 = Color(0xFF1C2430)
    val surface3 = Color(0xFF243040)
    val border = Color(0xFF2A3442)
    val text = Color(0xFFE6EAF0)
    val textDim = Color(0xFF9BA6B5)
    val action = Color(0xFF8EA2FF)
    val onAction = Color(0xFF0B1020)
    val actionSoft = Color(0xFF18213A)
    val onActionSoft = Color(0xFFB9C5FF)
    val brand = Color(0xFF22307A)
    val gate = Color(0xFFFF8A3D)
    val gateSoft = Color(0xFF2B1B10)
    val onGateSoft = Color(0xFFFFB07A)
    val onGate = Color(0xFF141922)
    val ok = Color(0xFF7FD1A6)
    val okSoft = Color(0xFF12281E)
    val onOkSoft = Color(0xFFA9E2C3)
    val danger = Color(0xFFFF9A94)
    val dangerSoft = Color(0xFF2E1719)
    val onDangerSoft = Color(0xFFF2B3B3)
    val warn = Color(0xFFE5C07B)
    val warnSoft = Color(0xFF2A2413)
    val onWarnSoft = Color(0xFFEBD197)
    val diffAddBg = Color(0xFF12281E)
    val diffAddText = Color(0xFFA9E2C3)
    val diffAddWord = Color(0xFF1F4A35)
    val diffDelBg = Color(0xFF2E1719)
    val diffDelText = Color(0xFFF2B3B3)
    val diffDelWord = Color(0xFF5A2529)
    val terminalBg = Color(0xFF090C11)
    val terminalText = Color(0xFFD7DEE8)
}

object CheckpointDay {
    val background = Color(0xFFF3F4F1)
    val surface = Color(0xFFFFFFFF)
    val surface2 = Color(0xFFE9EBE6)
    val surface3 = Color(0xFFDDE0DA)
    val border = Color(0xFFD5D9D2)
    val text = Color(0xFF141922)
    val textDim = Color(0xFF566070)
    val action = Color(0xFF2B4BFF)
    val onAction = Color(0xFFFFFFFF)
    val actionSoft = Color(0xFFE7EBFF)
    val onActionSoft = Color(0xFF22338F)
    val brand = Color(0xFF22307A)
    val gate = Color(0xFFE85D00)
    val gateSoft = Color(0xFFFFEEDF)
    val onGateSoft = Color(0xFF8F3A00)
    val onGate = Color(0xFF141922)
    val ok = Color(0xFF1E7A4E)
    val okSoft = Color(0xFFE0F2E7)
    val onOkSoft = Color(0xFF165337)
    val danger = Color(0xFFB3261E)
    val dangerSoft = Color(0xFFFBE3E1)
    val onDangerSoft = Color(0xFF8A2320)
    val warn = Color(0xFF7A5B00)
    val warnSoft = Color(0xFFFBF1D0)
    val onWarnSoft = Color(0xFF5F4700)
    val diffAddBg = Color(0xFFE0F2E7)
    val diffAddText = Color(0xFF165337)
    val diffAddWord = Color(0xFFB5E0C6)
    val diffDelBg = Color(0xFFFBE3E1)
    val diffDelText = Color(0xFF8A2320)
    val diffDelWord = Color(0xFFF3BDB9)
    val terminalBg = Color(0xFFFBFBF9)
    val terminalText = Color(0xFF1B222D)
}

private val CheckpointScrim = Color(0xFF05080C)

val NightColors = darkColorScheme(
    primary = CheckpointNight.action,
    onPrimary = CheckpointNight.onAction,
    primaryContainer = CheckpointNight.actionSoft,
    onPrimaryContainer = CheckpointNight.onActionSoft,
    secondary = CheckpointNight.action,
    onSecondary = CheckpointNight.onAction,
    secondaryContainer = CheckpointNight.surface2,
    onSecondaryContainer = CheckpointNight.text,
    tertiary = CheckpointNight.gate,
    onTertiary = CheckpointNight.onGate,
    tertiaryContainer = CheckpointNight.gateSoft,
    onTertiaryContainer = CheckpointNight.onGateSoft,
    background = CheckpointNight.background,
    onBackground = CheckpointNight.text,
    surface = CheckpointNight.surface,
    onSurface = CheckpointNight.text,
    surfaceVariant = CheckpointNight.surface2,
    onSurfaceVariant = CheckpointNight.textDim,
    surfaceContainer = CheckpointNight.surface,
    surfaceContainerHigh = CheckpointNight.surface2,
    surfaceContainerHighest = CheckpointNight.surface3,
    outline = CheckpointNight.border,
    outlineVariant = CheckpointNight.border,
    surfaceContainerLowest = CheckpointNight.background,
    surfaceContainerLow = CheckpointNight.surface,
    surfaceBright = CheckpointNight.surface,
    surfaceDim = CheckpointNight.background,
    inverseSurface = CheckpointNight.text,
    inverseOnSurface = CheckpointNight.background,
    inversePrimary = CheckpointNight.action,
    scrim = CheckpointScrim,
    error = CheckpointNight.danger,
    errorContainer = CheckpointNight.dangerSoft,
    onError = CheckpointNight.onAction,
    onErrorContainer = CheckpointNight.onDangerSoft,
)

val DayColors = lightColorScheme(
    primary = CheckpointDay.action,
    onPrimary = CheckpointDay.onAction,
    primaryContainer = CheckpointDay.actionSoft,
    onPrimaryContainer = CheckpointDay.onActionSoft,
    secondary = CheckpointDay.action,
    onSecondary = CheckpointDay.onAction,
    secondaryContainer = CheckpointDay.surface2,
    onSecondaryContainer = CheckpointDay.text,
    tertiary = CheckpointDay.gate,
    onTertiary = CheckpointDay.onGate,
    tertiaryContainer = CheckpointDay.gateSoft,
    onTertiaryContainer = CheckpointDay.onGateSoft,
    background = CheckpointDay.background,
    onBackground = CheckpointDay.text,
    surface = CheckpointDay.surface,
    onSurface = CheckpointDay.text,
    surfaceVariant = CheckpointDay.surface2,
    onSurfaceVariant = CheckpointDay.textDim,
    surfaceContainer = CheckpointDay.surface,
    surfaceContainerHigh = CheckpointDay.surface2,
    surfaceContainerHighest = CheckpointDay.surface3,
    outline = CheckpointDay.border,
    outlineVariant = CheckpointDay.border,
    surfaceContainerLowest = CheckpointDay.background,
    surfaceContainerLow = CheckpointDay.surface,
    surfaceBright = CheckpointDay.surface,
    surfaceDim = CheckpointDay.background,
    inverseSurface = CheckpointDay.text,
    inverseOnSurface = CheckpointDay.background,
    inversePrimary = CheckpointDay.action,
    scrim = CheckpointScrim,
    error = CheckpointDay.danger,
    errorContainer = CheckpointDay.dangerSoft,
    onError = CheckpointDay.onAction,
    onErrorContainer = CheckpointDay.onDangerSoft,
)


@Immutable
data class CheckpointExtraColors(
    val isNight: Boolean,
    val gate: Color,
    val gateSoft: Color,
    val onGateSoft: Color,
    val onGate: Color,
    val ok: Color,
    val okSoft: Color,
    val onOkSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val onWarnSoft: Color,
    val diffAddBg: Color,
    val diffAddText: Color,
    val diffAddWord: Color,
    val diffDelBg: Color,
    val diffDelText: Color,
    val diffDelWord: Color,
    val terminalBg: Color,
    val terminalText: Color,
)

val NightExtra = CheckpointExtraColors(
    isNight = true,
    gate = CheckpointNight.gate,
    gateSoft = CheckpointNight.gateSoft,
    onGateSoft = CheckpointNight.onGateSoft,
    onGate = CheckpointNight.onGate,
    ok = CheckpointNight.ok,
    okSoft = CheckpointNight.okSoft,
    onOkSoft = CheckpointNight.onOkSoft,
    warn = CheckpointNight.warn,
    warnSoft = CheckpointNight.warnSoft,
    onWarnSoft = CheckpointNight.onWarnSoft,
    diffAddBg = CheckpointNight.diffAddBg,
    diffAddText = CheckpointNight.diffAddText,
    diffAddWord = CheckpointNight.diffAddWord,
    diffDelBg = CheckpointNight.diffDelBg,
    diffDelText = CheckpointNight.diffDelText,
    diffDelWord = CheckpointNight.diffDelWord,
    terminalBg = CheckpointNight.terminalBg,
    terminalText = CheckpointNight.terminalText,
)

val DayExtra = CheckpointExtraColors(
    isNight = false,
    gate = CheckpointDay.gate,
    gateSoft = CheckpointDay.gateSoft,
    onGateSoft = CheckpointDay.onGateSoft,
    onGate = CheckpointDay.onGate,
    ok = CheckpointDay.ok,
    okSoft = CheckpointDay.okSoft,
    onOkSoft = CheckpointDay.onOkSoft,
    warn = CheckpointDay.warn,
    warnSoft = CheckpointDay.warnSoft,
    onWarnSoft = CheckpointDay.onWarnSoft,
    diffAddBg = CheckpointDay.diffAddBg,
    diffAddText = CheckpointDay.diffAddText,
    diffAddWord = CheckpointDay.diffAddWord,
    diffDelBg = CheckpointDay.diffDelBg,
    diffDelText = CheckpointDay.diffDelText,
    diffDelWord = CheckpointDay.diffDelWord,
    terminalBg = CheckpointDay.terminalBg,
    terminalText = CheckpointDay.terminalText,
)

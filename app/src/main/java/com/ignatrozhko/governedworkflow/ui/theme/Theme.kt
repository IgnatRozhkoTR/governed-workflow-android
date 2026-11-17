package com.ignatrozhko.governedworkflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object PanelColors {
    val Indigo = Color(0xFF818CF8)
    val Green = Color(0xFF51CF66)
    val Amber = Color(0xFFFFD43B)
    val Red = Color(0xFFFF6B6B)
    val TerminalBackground = Color(0xFF1A1A2E)
}

private val DarkColors = darkColorScheme(
    primary = PanelColors.Indigo,
    onPrimary = Color(0xFF14143A),
    primaryContainer = Color(0xFF2E3170),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFF22D3EE),
    tertiary = PanelColors.Green,
    error = PanelColors.Red,
    background = Color(0xFF121218),
    surface = Color(0xFF121218),
    surfaceContainer = Color(0xFF1C1C26),
    surfaceContainerHigh = Color(0xFF24243A),
    surfaceContainerLow = Color(0xFF17171F),
)

@Composable
fun GovernedWorkflowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}

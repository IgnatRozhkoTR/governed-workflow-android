package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.ui.theme.PanelColors

@Composable
fun PhaseChip(phase: String) {
    Pill("Phase $phase", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
}

@Composable
fun GateBadge() {
    Pill("Awaiting approval", PanelColors.Amber, Color(0xFF2B2100))
}

@Composable
private fun Pill(text: String, background: Color, foreground: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

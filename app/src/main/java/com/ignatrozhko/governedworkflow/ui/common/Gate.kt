package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint

/** The orange pause disc that marks a waiting gate everywhere it appears. */
@Composable
fun GateIcon(modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp).background(Checkpoint.colors.gate, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_pause), contentDescription = "Gate", Modifier.size(18.dp), tint = Checkpoint.colors.onGate)
    }
}

/** The pending decision, shown first on Overview: who is waiting, for how long, and what is being asked. */
@Composable
fun GateCard(
    caption: String,
    question: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().background(Checkpoint.colors.gateSoft, MaterialTheme.shapes.extraLarge).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GateIcon()
            Text(caption, style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onGateSoft)
        }
        Text(question, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

/** A one-line reminder of the waiting gate on tabs other than Overview; tapping it goes back to the decision. */
@Composable
fun GateBar(text: String, actionLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(Checkpoint.colors.gateSoft, CircleShape)
            .clickable(onClickLabel = actionLabel, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(Checkpoint.colors.gate)
        Text(
            text,
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = Checkpoint.colors.onGateSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("$actionLabel ›", style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onGateSoft)
    }
}

/** A compact reminder, in the warn color, that the screen shows saved data because the panel cannot be reached. */
@Composable
fun OfflineBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .background(Checkpoint.colors.warnSoft, CircleShape)
            .clickable(onClickLabel = "Retry", onClick = onRetry)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(Checkpoint.colors.warn)
        Text(
            "Offline · showing saved data",
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = Checkpoint.colors.onWarnSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("Retry ›", style = MaterialTheme.typography.labelLarge, color = Checkpoint.colors.onWarnSoft)
    }
}

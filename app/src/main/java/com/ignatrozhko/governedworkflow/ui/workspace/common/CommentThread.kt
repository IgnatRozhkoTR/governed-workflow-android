package com.ignatrozhko.governedworkflow.ui.workspace.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.ui.common.LabeledField
import com.ignatrozhko.governedworkflow.ui.common.OutlinedActionButton
import com.ignatrozhko.governedworkflow.ui.common.PanelSheet
import com.ignatrozhko.governedworkflow.ui.common.PrimaryButton
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint

/** Collapsed "N comments" link that expands into the thread with resolve toggles and an add-comment action. */
@Composable
fun CommentThread(
    comments: List<Comment>,
    onAdd: (String) -> Unit,
    onResolve: (commentId: Long, isResolved: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isComposing by remember { mutableStateOf(false) }
    val openCount = comments.count { !it.resolved }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = if (comments.isEmpty()) "Comment" else "$openCount open of ${comments.size} comments"
            TextButton(
                onClick = { if (comments.isEmpty()) isComposing = true else isExpanded = !isExpanded },
                contentPadding = CompactButtonPadding,
            ) { Text(label) }
            if (comments.isNotEmpty()) TextButton(onClick = { isComposing = true }, contentPadding = CompactButtonPadding) { Text("Add") }
        }
        if (isExpanded) comments.forEach { CommentCard(it, onResolve) }
    }
    if (isComposing) {
        TextInputDialog(title = "Add comment", label = "Comment", confirmLabel = "Post", onDismiss = { isComposing = false }) {
            isComposing = false
            onAdd(it)
        }
    }
}

private val CompactButtonPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)

@Composable
fun CommentCard(comment: Comment, onResolve: (commentId: Long, isResolved: Boolean) -> Unit, onOpen: (() -> Unit)? = null) {
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).let { if (onOpen != null) it.clickable(onClick = onOpen) else it },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CommentBody(comment.author, comment.resolution?.replace('_', ' '), comment.text, isResolved = comment.resolved)
            comment.replies.forEach { reply -> CommentBody(reply.author, null, reply.text, isResolved = false) }
            OutlinedActionButton(if (comment.resolved) "Reopen" else "Resolve", { onResolve(comment.id, !comment.resolved) })
        }
    }
}

@Composable
private fun CommentBody(author: String, detail: String?, text: String, isResolved: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatar(author)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(authorName(author), style = MaterialTheme.typography.labelLarge)
                detail?.let { SecondaryText(it) }
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (isResolved) TextDecoration.LineThrough else null,
            )
        }
    }
}

private fun authorName(author: String) = when (author) {
    USER_AUTHOR -> "You"
    else -> author.replaceFirstChar { it.uppercase() }
}

/** Blue for you, amber for the automatic reviewers, neutral for the agent. */
@Composable
private fun Avatar(author: String) {
    val extra = Checkpoint.colors
    val scheme = MaterialTheme.colorScheme
    val (background, content) = when (author) {
        USER_AUTHOR -> scheme.primary to scheme.onPrimary
        REVIEWER_AUTHOR -> extra.warnSoft to extra.onWarnSoft
        else -> scheme.surfaceContainerHighest to scheme.onSurface
    }
    Box(Modifier.size(28.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Text(authorName(author).take(1), style = MaterialTheme.typography.labelMedium, color = content)
    }
}

private const val USER_AUTHOR = "user"
private const val REVIEWER_AUTHOR = "reviewer"

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    initialText: String = "",
    isTextRequired: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    PanelSheet(title, onDismiss) {
        LabeledField(label, text, { text = it }, isSingleLine = false, minLines = 3)
        Row(Modifier.fillMaxWidth()) {
            OutlinedActionButton("Cancel", onDismiss)
            Spacer(Modifier.weight(1f))
            PrimaryButton(confirmLabel, { onConfirm(text.trim()) }, isEnabled = !isTextRequired || text.isNotBlank())
        }
    }
}


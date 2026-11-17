package com.ignatrozhko.governedworkflow.ui.workspace.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
        shape = MaterialTheme.shapes.small,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SecondaryText(listOfNotNull(comment.author, comment.resolution?.replace('_', ' ')).joinToString(" · "))
            Text(
                comment.text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (comment.resolved) TextDecoration.LineThrough else null,
            )
            comment.replies.forEach { reply -> SecondaryText("↳ ${reply.author}: ${reply.text}") }
            TextButton(onClick = { onResolve(comment.id, !comment.resolved) }) {
                Text(if (comment.resolved) "Reopen" else "Resolve")
            }
        }
    }
}

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, minLines = 3) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = !isTextRequired || text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

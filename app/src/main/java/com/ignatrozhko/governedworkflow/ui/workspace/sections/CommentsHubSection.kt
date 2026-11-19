package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.common.FieldLabel
import com.ignatrozhko.governedworkflow.ui.workspace.common.CollapsibleCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding
import com.ignatrozhko.governedworkflow.ui.workspace.common.previewLine
import com.ignatrozhko.governedworkflow.ui.workspace.common.subtitleOf

/** Where an open comment lives; tapping it jumps there. */
sealed interface CommentLocation {
    data class ReviewFile(val path: String) : CommentLocation
    data object Plan : CommentLocation
    data object Research : CommentLocation

    companion object {
        fun of(comment: Comment): CommentLocation = when (comment.scope) {
            CommentTargets.REVIEW_SCOPE -> ReviewFile(comment.filePath ?: comment.target)
            CommentTargets.RESEARCH_SCOPE -> Research
            else -> Plan
        }
    }
}

@Composable
fun CommentsHubSection(
    state: WorkspaceState,
    onResolve: (Long, Boolean) -> Unit,
    onJump: (CommentLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val open = state.comments.values.flatten().filter { !it.resolved }
    if (open.isEmpty()) {
        EmptySection("No open comments.")
        return
    }
    LazyColumn(modifier, contentPadding = SectionContentPadding) {
        open.groupBy { it.scope }.toSortedMap(compareBy { CAPTION_ORDER.indexOf(it).takeIf { index -> index >= 0 } ?: CAPTION_ORDER.size }).forEach { (scope, comments) ->
            item(key = "scope:$scope") {
                FieldLabel(scope.replaceFirstChar { it.uppercase() }, Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
            }
            items(comments, key = { it.id }) { comment ->
                CollapsibleCard(
                    title = previewLine(comment.text).ifBlank { "Comment" },
                    titleStyle = MaterialTheme.typography.bodyLarge,
                    memoryKey = "comments:${comment.id}",
                    modifier = Modifier.padding(vertical = 4.dp),
                    subtitle = subtitleOf(locationLabel(comment), comment.author),
                ) {
                    CommentCard(comment, onResolve)
                    TextButton(onClick = { onJump(CommentLocation.of(comment)) }) { Text("Go to ${comment.scope}") }
                }
            }
        }
    }
}

private fun locationLabel(comment: Comment): String {
    val line = comment.lineStart?.let { ":$it" }.orEmpty()
    return (comment.filePath ?: comment.target) + line
}

private val CAPTION_ORDER = listOf("phase", "plan", "research", "review")

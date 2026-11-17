package com.ignatrozhko.governedworkflow.ui.workspace.sections

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.WorkspaceState
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.EmptySection
import com.ignatrozhko.governedworkflow.ui.workspace.common.SectionContentPadding

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
        open.groupBy { it.scope }.forEach { (scope, comments) ->
            item(key = "scope:$scope") { Text(scope.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall) }
            items(comments, key = { it.id }) { comment ->
                Text(locationLabel(comment), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                CommentCard(comment, onResolve, onOpen = { onJump(CommentLocation.of(comment)) })
            }
        }
    }
}

private fun locationLabel(comment: Comment): String {
    val line = comment.lineStart?.let { ":$it" }.orEmpty()
    return (comment.filePath ?: comment.target) + line
}

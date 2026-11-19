package com.ignatrozhko.governedworkflow.ui.changes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.data.api.Annotation
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.diff.DiffLine
import com.ignatrozhko.governedworkflow.data.diff.DiffLineKind
import com.ignatrozhko.governedworkflow.data.diff.SyntaxHighlighter
import com.ignatrozhko.governedworkflow.data.diff.SyntaxKind
import com.ignatrozhko.governedworkflow.data.diff.UnifiedDiffParser
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import com.ignatrozhko.governedworkflow.ui.theme.MonoText
import com.ignatrozhko.governedworkflow.ui.workspace.common.CommentCard
import com.ignatrozhko.governedworkflow.ui.workspace.common.ConfirmDialog
import com.ignatrozhko.governedworkflow.ui.workspace.common.MarkdownText
import com.ignatrozhko.governedworkflow.ui.workspace.common.SecondaryText
import com.ignatrozhko.governedworkflow.ui.workspace.common.TextInputDialog

/**
 * One file's unified diff with review comments and annotations placed under the new-side line they
 * belong to. Anything anchored outside the shown hunks is listed above the diff so it is never lost.
 */
@Composable
fun FileDiffView(
    file: DiffFile,
    comments: List<Comment>,
    isHidingResolved: Boolean,
    viewModel: ChangesViewModel,
    onHidingResolvedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val annotations by viewModel.annotations.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val lines = remember(file.diff) { UnifiedDiffParser.parse(file.diff) }
    val highlighter = remember(file.path) { SyntaxHighlighter.forPath(file.path) }
    val shownNumbers = remember(lines) { lines.mapNotNull { it.newNumber }.toSet() }
    var commentLine by remember { mutableStateOf<Int?>(null) }
    val openIds = comments.filter { !it.resolved }.map { it.id }
    LazyColumn(modifier.fillMaxSize()) {
        item { FileHeader(file, openIds, isBusy, isHidingResolved, onHidingResolvedChange, viewModel::resolveComments) }
        detachedNotes(comments.filter { it.lineStart !in shownNumbers }, annotations.filter { it.lineEnd !in shownNumbers }, viewModel)
        itemsIndexed(lines) { _, line ->
            DiffRow(line, highlighter) { commentLine = it }
            val number = line.newNumber.takeIf { line.kind != DiffLineKind.Removed }
            if (number != null) {
                annotations.filter { it.lineEnd == number }.forEach { AnnotationNote(it, viewModel) }
                comments.filter { it.lineStart == number }.forEach { comment ->
                    Column(Modifier.padding(start = GUTTER_WIDTH * 2, end = 8.dp)) { CommentCard(comment, viewModel::resolveComment) }
                }
            }
        }
    }
    commentLine?.let { line ->
        TextInputDialog(title = "Comment on line $line", label = "Review comment", confirmLabel = "Post", onDismiss = { commentLine = null }) {
            commentLine = null
            viewModel.addComment(NewComment(CommentTargets.REVIEW_SCOPE, file.path, it, line))
        }
    }
}

@Composable
private fun FileHeader(
    file: DiffFile,
    openCommentIds: List<Long>,
    isBusy: Boolean,
    isHidingResolved: Boolean,
    onHidingResolvedChange: (Boolean) -> Unit,
    onResolveAll: (List<Long>) -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(file.path, style = MaterialTheme.typography.titleSmall)
        SecondaryText("+${file.additions} −${file.deletions} · long-press a line to comment")
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(isHidingResolved, { onHidingResolvedChange(!isHidingResolved) }, { Text("Hide resolved") })
            TextButton(onClick = { onResolveAll(openCommentIds) }, enabled = !isBusy && openCommentIds.isNotEmpty()) {
                Text("Resolve file (${openCommentIds.size})")
            }
        }
    }
}

private fun LazyListScope.detachedNotes(comments: List<Comment>, annotations: List<Annotation>, viewModel: ChangesViewModel) {
    if (comments.isEmpty() && annotations.isEmpty()) return
    item {
        Column(Modifier.padding(horizontal = 12.dp)) {
            SecondaryText("Outside the shown changes")
            annotations.forEach { AnnotationNote(it, viewModel) }
            comments.forEach { comment ->
                comment.lineStart?.let { SecondaryText("Line $it") }
                CommentCard(comment, viewModel::resolveComment)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiffRow(line: DiffLine, highlighter: SyntaxHighlighter, onLongPressLine: (Int) -> Unit) {
    val commentable = line.newNumber.takeIf { line.kind != DiffLineKind.Removed }
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBackground(line.kind))
            .combinedClickable(enabled = commentable != null, onClick = {}, onLongClick = { commentable?.let(onLongPressLine) }),
    ) {
        if (line.kind == DiffLineKind.Hunk || line.kind == DiffLineKind.Note) {
            CodeText(AnnotatedString(line.text), Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Row
        }
        LineNumber(line.oldNumber)
        LineNumber(line.newNumber)
        CodeText(highlighted(line, highlighter, syntaxColors()), Modifier.weight(1f).padding(start = 4.dp, end = 8.dp))
    }
}

@Composable
private fun LineNumber(number: Int?) {
    Text(
        number?.toString().orEmpty(),
        Modifier.width(GUTTER_WIDTH),
        style = MonoText.small,
        textAlign = TextAlign.End,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun CodeText(text: AnnotatedString, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, modifier, style = MonoText.small, color = color)
}

private fun highlighted(line: DiffLine, highlighter: SyntaxHighlighter, colors: SyntaxColors): AnnotatedString {
    val marker = when (line.kind) {
        DiffLineKind.Added -> "+"
        DiffLineKind.Removed -> "-"
        else -> " "
    }
    return buildAnnotatedString {
        append(marker)
        append(line.text)
        highlighter.highlight(line.text).forEach { span ->
            addStyle(colors.styleFor(span.kind), span.start + marker.length, span.end + marker.length)
        }
    }
}

private class SyntaxColors(val keyword: Color, val string: Color, val comment: Color, val number: Color) {
    fun styleFor(kind: SyntaxKind) = when (kind) {
        SyntaxKind.Keyword -> SpanStyle(color = keyword)
        SyntaxKind.StringLiteral -> SpanStyle(color = string)
        SyntaxKind.Comment -> SpanStyle(color = comment, fontStyle = FontStyle.Italic)
        SyntaxKind.Number -> SpanStyle(color = number)
    }
}

@Composable
private fun syntaxColors() = SyntaxColors(
    keyword = MaterialTheme.colorScheme.primary,
    string = Checkpoint.colors.ok,
    comment = MaterialTheme.colorScheme.onSurfaceVariant,
    number = Checkpoint.colors.warn,
)

@Composable
private fun rowBackground(kind: DiffLineKind) = when (kind) {
    DiffLineKind.Added -> Checkpoint.colors.diffAddBg
    DiffLineKind.Removed -> Checkpoint.colors.diffDelBg
    DiffLineKind.Hunk -> MaterialTheme.colorScheme.surfaceVariant
    DiffLineKind.Context, DiffLineKind.Note -> Color.Transparent
}

/** Grey one-line preview, as in the web panel; tapping expands the full markdown note with Edit and Delete. */
@Composable
private fun AnnotationNote(annotation: Annotation, viewModel: ChangesViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }.padding(start = GUTTER_WIDTH * 2, end = 8.dp, top = 2.dp, bottom = 2.dp),
    ) {
        if (!isExpanded) {
            Text(
                AnnotationPreview.of(annotation.text),
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        SecondaryText("${annotation.author} · lines ${annotation.lineStart}–${annotation.lineEnd}${if (annotation.stale) " · stale" else ""}")
        MarkdownText(annotation.text)
        Row {
            TextButton(onClick = { isEditing = true }) { Text("Edit") }
            TextButton(onClick = { isDeleting = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (isEditing) {
        TextInputDialog("Edit annotation", "Annotation", "Save", initialText = annotation.text, onDismiss = { isEditing = false }) {
            isEditing = false
            viewModel.editAnnotation(annotation.id, it)
        }
    }
    if (isDeleting) {
        ConfirmDialog("Delete annotation?", "Agents and reviewers will no longer see this note.", "Delete", onDismiss = { isDeleting = false }) {
            isDeleting = false
            viewModel.deleteAnnotation(annotation.id)
        }
    }
}

private val GUTTER_WIDTH = 30.dp

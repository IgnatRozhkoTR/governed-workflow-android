package com.ignatrozhko.governedworkflow.ui.changes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.api.Annotation
import com.ignatrozhko.governedworkflow.data.api.Comment
import com.ignatrozhko.governedworkflow.data.api.CommentTargets
import com.ignatrozhko.governedworkflow.data.api.DiffFile
import com.ignatrozhko.governedworkflow.data.api.NewComment
import com.ignatrozhko.governedworkflow.data.diff.DiffLine
import com.ignatrozhko.governedworkflow.data.diff.DiffLineKind
import com.ignatrozhko.governedworkflow.data.diff.SyntaxHighlighter
import com.ignatrozhko.governedworkflow.data.diff.SyntaxKind
import com.ignatrozhko.governedworkflow.ui.common.ChoiceChip
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
    val codeScroll = rememberScrollState()
    val openIds = comments.filter { !it.resolved }.map { it.id }
    LazyColumn(modifier.fillMaxSize()) {
        item { FileHeader(openIds, isBusy, isHidingResolved, onHidingResolvedChange, viewModel::resolveComments) }
        detachedNotes(comments.filter { it.lineStart !in shownNumbers }, annotations.filter { it.lineEnd !in shownNumbers }, viewModel)
        itemsIndexed(lines) { _, line ->
            DiffRow(line, highlighter, codeScroll) { commentLine = it }
            val number = line.newNumber.takeIf { line.kind != DiffLineKind.Removed }
            if (number != null) {
                annotations.filter { it.lineEnd == number }.forEach { AnnotationNote(it, viewModel) }
                comments.filter { it.lineStart == number }.forEach { comment ->
                    Column(Modifier.padding(horizontal = 12.dp)) { CommentCard(comment, viewModel::resolveComment) }
                }
            }
        }
        item { SecondaryText("Long-press a line to comment. Code scrolls sideways, comments wrap.", Modifier.padding(16.dp)) }
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
    openCommentIds: List<Long>,
    isBusy: Boolean,
    isHidingResolved: Boolean,
    onHidingResolvedChange: (Boolean) -> Unit,
    onResolveAll: (List<Long>) -> Unit,
) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ChoiceChip("Hide resolved", isSelected = isHidingResolved, onClick = { onHidingResolvedChange(!isHidingResolved) })
        TextButton(onClick = { onResolveAll(openCommentIds) }, enabled = !isBusy && openCommentIds.isNotEmpty()) {
            Text("Resolve file (${openCommentIds.size})")
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
private fun DiffRow(line: DiffLine, highlighter: SyntaxHighlighter, codeScroll: ScrollState, onLongPressLine: (Int) -> Unit) {
    val commentable = line.newNumber.takeIf { line.kind != DiffLineKind.Removed }
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBackground(line.kind))
            .combinedClickable(enabled = commentable != null, onClick = {}, onLongClick = { commentable?.let(onLongPressLine) }),
    ) {
        if (line.kind == DiffLineKind.Hunk || line.kind == DiffLineKind.Note) {
            CodeText(AnnotatedString(line.text), Modifier.padding(horizontal = 12.dp).horizontalScroll(codeScroll), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Row
        }
        LineNumber(line.oldNumber)
        LineNumber(line.newNumber)
        CodeText(AnnotatedString(sign(line.kind)), Modifier.width(SIGN_WIDTH), color = codeColor(line.kind))
        CodeText(highlighted(line, highlighter, syntaxColors(), wordColor(line.kind)), Modifier.weight(1f).horizontalScroll(codeScroll).padding(end = 12.dp), color = codeColor(line.kind))
    }
}

private fun sign(kind: DiffLineKind) = when (kind) {
    DiffLineKind.Added -> "+"
    DiffLineKind.Removed -> "-"
    else -> ""
}

@Composable
private fun codeColor(kind: DiffLineKind) = when (kind) {
    DiffLineKind.Added -> Checkpoint.colors.diffAddText
    DiffLineKind.Removed -> Checkpoint.colors.diffDelText
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun wordColor(kind: DiffLineKind) = when (kind) {
    DiffLineKind.Added -> Checkpoint.colors.diffAddWord
    DiffLineKind.Removed -> Checkpoint.colors.diffDelWord
    else -> Color.Transparent
}

@Composable
private fun LineNumber(number: Int?) {
    Text(
        number?.toString().orEmpty(),
        Modifier.width(GUTTER_WIDTH).padding(end = 6.dp).semantics { hideFromAccessibility() },
        style = MonoText.small,
        textAlign = TextAlign.End,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

@Composable
private fun CodeText(text: AnnotatedString, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, modifier, style = MonoText.small, color = color, softWrap = false)
}

private fun highlighted(line: DiffLine, highlighter: SyntaxHighlighter, colors: SyntaxColors, wordColor: Color): AnnotatedString =
    buildAnnotatedString {
        append(line.text)
        highlighter.highlight(line.text).forEach { span -> addStyle(colors.styleFor(span.kind), span.start, span.end) }
        line.changedRange?.let { addStyle(SpanStyle(background = wordColor), it.first, it.last + 1) }
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

/** Soft-blue agent note with a flag: a two-line preview, tapping expands the full markdown note with Edit and Delete. */
@Composable
private fun AnnotationNote(annotation: Annotation, viewModel: ChangesViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_flag), contentDescription = null, Modifier.size(16.dp), tint = contentColor)
            Text(
                "Agent note${if (annotation.stale) " · stale" else ""}",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
        if (!isExpanded) {
            Text(AnnotationPreview.of(annotation.text), style = MaterialTheme.typography.bodyMedium, color = contentColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            CompositionLocalProvider(LocalContentColor provides contentColor) { MarkdownText(annotation.text) }
            SecondaryText("${annotation.author} · lines ${annotation.lineStart}–${annotation.lineEnd}")
            Row {
                TextButton(onClick = { isEditing = true }) { Text("Edit") }
                TextButton(onClick = { isDeleting = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
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

private val GUTTER_WIDTH = 36.dp
private val SIGN_WIDTH = 16.dp

package com.ignatrozhko.governedworkflow.ui.workspace.common

import android.util.LruCache
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.ui.theme.Checkpoint
import kotlin.math.roundToInt

/**
 * Sizes of diagrams already rendered this session. A lazy list disposes off-screen diagrams, and without the
 * remembered size each one would come back at placeholder height and shift the list while it re-renders.
 */
private val finishedRenders = LruCache<String, DiagramRender>(RENDER_CACHE_SIZE)

/** A titled diagram on its own card; diagrams are the one block that starts expanded, as users came to see them. */
@Composable
fun DiagramCard(title: String, source: String, memoryKey: String, modifier: Modifier = Modifier) {
    var isFullScreen by rememberSaveable(source) { mutableStateOf(false) }
    CollapsibleCard(
        title,
        memoryKey,
        modifier,
        initiallyExpanded = true,
        badge = { FullScreenButton { isFullScreen = true } },
    ) { DiagramBody(source, memoryKey) { isFullScreen = true } }
    if (isFullScreen) FullScreenDiagram(title, source) { isFullScreen = false }
}

/** A diagram embedded in other content, such as a mermaid block inside markdown. */
@Composable
fun DiagramBlock(title: String, source: String, memoryKey: String, modifier: Modifier = Modifier) {
    var isFullScreen by rememberSaveable(source) { mutableStateOf(false) }
    CollapsibleSection(
        title,
        memoryKey,
        modifier,
        initiallyExpanded = true,
        titleStyle = MaterialTheme.typography.labelLarge,
        badge = { FullScreenButton { isFullScreen = true } },
    ) { DiagramBody(source, memoryKey) { isFullScreen = true } }
    if (isFullScreen) FullScreenDiagram(title, source) { isFullScreen = false }
}

@Composable
private fun FullScreenButton(onClick: () -> Unit) {
    IconButton(onClick) { Icon(painterResource(R.drawable.ic_fullscreen), contentDescription = "Open diagram full screen", Modifier.size(20.dp)) }
}

@Composable
private fun DiagramBody(source: String, memoryKey: String, onOpen: () -> Unit) {
    val palette = rememberDiagramPalette()
    val renderKey = "$source|${Checkpoint.colors.isNight}"
    var render by remember(renderKey) { mutableStateOf(finishedRenders.get(renderKey) ?: DiagramRender.Rendering) }
    val current = render
    if (current is DiagramRender.Failed) {
        DiagramError(current.message, source)
        return
    }
    InlineDiagram(
        source = source,
        palette = palette,
        render = current,
        onResult = { result ->
            finishedRenders.put(renderKey, result)
            render = result
        },
        onOpen = onOpen,
    )
    CollapsibleSection("Show source", "$memoryKey:source", titleStyle = MaterialTheme.typography.labelLarge) { MonospaceBlock(source) }
}

@Composable
private fun rememberDiagramPalette(): DiagramPalette {
    val scheme = MaterialTheme.colorScheme
    return DiagramPalette(
        fill = scheme.surfaceVariant.toArgb(),
        border = scheme.outline.toArgb(),
        text = scheme.onSurface.toArgb(),
        dim = scheme.onSurfaceVariant.toArgb(),
        line = scheme.onSurfaceVariant.toArgb(),
        accent = scheme.primary.toArgb(),
        accentSoft = scheme.primaryContainer.toArgb(),
        accentText = scheme.onPrimaryContainer.toArgb(),
    )
}

/**
 * The diagram at its natural size. A web view wider than the screen draws wrongly, so it stays as wide as the card
 * and wide diagrams scroll inside it. A transparent layer above it takes the touches: sideways drags pan the
 * diagram, vertical drags still scroll the list, and a tap opens the zoomable full-screen view.
 */
@Composable
private fun InlineDiagram(source: String, palette: DiagramPalette, render: DiagramRender, onResult: (DiagramRender) -> Unit, onOpen: () -> Unit) {
    val currentOnResult by rememberUpdatedState(onResult)
    val context = LocalContext.current
    val webView = remember(source, palette) { DiagramWebView(context, source, palette, DiagramMode.Inline) { currentOnResult(it) } }
    DisposableEffect(webView) { onDispose { webView.destroy() } }
    val rendered = render as? DiagramRender.Rendered
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth
        Column {
            Box(Modifier.fillMaxWidth().height(rendered?.heightDp?.dp ?: PLACEHOLDER_HEIGHT)) {
                AndroidView(factory = { webView }, modifier = Modifier.matchParentSize())
                if (rendered == null) CircularProgressIndicator(Modifier.align(Alignment.Center).size(PROGRESS_SIZE))
                Spacer(Modifier.matchParentSize().diagramGestures(webView, onOpen))
            }
            if (rendered != null && rendered.widthDp.dp > cardWidth) SecondaryText("Swipe to pan. Tap or open full screen to zoom. Hold a lane title to read it in full.")
        }
    }
}

/** Sideways drags pan the diagram inside its view; anything vertical is left to the list. */
private fun Modifier.diagramGestures(webView: DiagramWebView, onOpen: () -> Unit) = this
    .pointerInput(webView) {
        detectHorizontalDragGestures { change, dragAmount ->
            change.consume()
            webView.scrollBy(-dragAmount.roundToInt(), 0)
        }
    }
    .pointerInput(onOpen) { detectTapGestures(onLongPress = { webView.showLaneTitleAt(it.x, it.y) }, onTap = { onOpen() }) }

@Composable
private fun DiagramError(message: String, source: String) {
    Text("This diagram could not be rendered: $message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    MonospaceBlock(source)
}

@Composable
private fun FullScreenDiagram(title: String, source: String, onClose: () -> Unit) {
    val palette = rememberDiagramPalette()
    var render by remember(source, palette) { mutableStateOf<DiagramRender>(DiagramRender.Rendering) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close diagram") }
                }
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context -> DiagramWebView(context, source, palette, DiagramMode.FullScreen) { render = it } },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { it.destroy() },
                    )
                    when (val current = render) {
                        DiagramRender.Rendering -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        is DiagramRender.Failed -> Column(Modifier.padding(16.dp)) { DiagramError(current.message, source) }
                        is DiagramRender.Rendered -> Unit
                    }
                }
            }
        }
    }
}

private val PLACEHOLDER_HEIGHT: Dp = 120.dp
private val PROGRESS_SIZE: Dp = 24.dp
private const val RENDER_CACHE_SIZE = 64

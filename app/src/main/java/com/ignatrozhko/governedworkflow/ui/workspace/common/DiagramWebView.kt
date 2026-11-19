package com.ignatrozhko.governedworkflow.ui.workspace.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

/** Outcome of rendering one mermaid diagram; sizes are CSS pixels, which equal dp at the page's 1:1 scale. */
sealed interface DiagramRender {
    data object Rendering : DiagramRender
    data class Rendered(val widthDp: Int, val heightDp: Int) : DiagramRender
    data class Failed(val message: String) : DiagramRender
}

/** Diagram colors taken from the active theme, so a diagram is drawn from the same tokens as the screen around it. */
data class DiagramPalette(
    val fill: Int,
    val border: Int,
    val text: Int,
    val dim: Int,
    val line: Int,
    val accent: Int,
    val accentSoft: Int,
    val accentText: Int,
) {
    fun toJson(): String = listOf(
        "fill" to fill, "border" to border, "text" to text, "dim" to dim, "line" to line,
        "accent" to accent, "accentSoft" to accentSoft, "accentText" to accentText,
    ).joinToString(",", "{", "}") { (name, color) -> "\"$name\":\"${"#%06X".format(color and RGB_MASK)}\"" }

    private companion object {
        const val RGB_MASK = 0xFFFFFF
    }
}

enum class DiagramMode(val query: String, val isZoomable: Boolean) {
    Inline("inline", isZoomable = false),
    FullScreen("fullscreen", isZoomable = true),
}

/**
 * Renders one mermaid diagram with the bundled mermaid.min.js. Diagram sources are written by agents, so the page
 * runs mermaid in strict mode and every request outside the bundled assets is refused.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class DiagramWebView(
    context: Context,
    private val source: String,
    private val palette: DiagramPalette,
    mode: DiagramMode,
    private val onResult: (DiagramRender) -> Unit,
) : WebView(context) {
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler(ASSETS_PATH, WebViewAssetLoader.AssetsPathHandler(context))
        .addPathHandler(RESOURCES_PATH, WebViewAssetLoader.ResourcesPathHandler(context))
        .build()

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        with(settings) {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            textZoom = FULL_TEXT_ZOOM
            setSupportZoom(mode.isZoomable)
            builtInZoomControls = mode.isZoomable
            displayZoomControls = false
            useWideViewPort = mode.isZoomable
        }
        webViewClient = BundledAssetsOnlyClient()
        addJavascriptInterface(Bridge(), BRIDGE_NAME)
        loadUrl("$PAGE_URL?mode=${mode.query}")
    }

    /** Shows the full title of the lane under the given point, in view pixels; the tip hides itself. */
    fun showLaneTitleAt(xPx: Float, yPx: Float) {
        val density = resources.displayMetrics.density
        evaluateJavascript("gwShowTitleAt(${xPx / density}, ${yPx / density})", null)
    }

    private inner class BundledAssetsOnlyClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
            assetLoader.shouldInterceptRequest(request.url) ?: refused()

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

        private fun refused() = WebResourceResponse("text/plain", "utf-8", FORBIDDEN, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
    }

    /** Called by diagram.js on the JavaBridge thread; results hop to the main thread. */
    private inner class Bridge {
        @JavascriptInterface
        fun source(): String = source

        @JavascriptInterface
        fun palette(): String = palette.toJson()

        @JavascriptInterface
        fun onRendered(width: Int, height: Int) {
            post { onResult(DiagramRender.Rendered(width, height)) }
        }

        @JavascriptInterface
        fun onFailed(message: String) {
            post { onResult(DiagramRender.Failed(message)) }
        }
    }

    private companion object {
        const val BRIDGE_NAME = "GwDiagram"
        const val ASSETS_PATH = "/assets/"
        const val RESOURCES_PATH = "/res/"
        const val PAGE_URL = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/mermaid/diagram.html"
        const val FULL_TEXT_ZOOM = 100
        const val FORBIDDEN = 403
    }
}

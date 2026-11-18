package com.ignatrozhko.governedworkflow.terminal.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.ignatrozhko.governedworkflow.data.settings.AppSettings
import com.ignatrozhko.governedworkflow.terminal.core.TerminalInput
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSession
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Callbacks a terminal surface raises for UI outside the emulator. */
interface TerminalHostEvents {
    fun onSelectionChanged(hasSelection: Boolean)
    fun onFontSizeChanged(size: Int)
    fun onClipboardCopy(text: String)
}

/**
 * xterm.js renders inside a WebView that never takes focus. Focus and the IME connection belong to
 * [TerminalInputField] beneath it, so keyboards talk to a raw-key editor rather than xterm's hidden textarea,
 * while the system still sees an ordinary text field.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class TerminalHostView(
    context: Context,
    private val session: TerminalSession,
    private val input: TerminalInput,
    private val initialFontSize: Int,
    private val events: TerminalHostEvents,
) : FrameLayout(context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val webView = WebView(context)
    private val inputField = TerminalInputField(context, input, ::paste)
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler(ASSETS_PATH, WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    private val outputLock = Any()
    private val pendingReplay = StringBuilder()
    private val pendingOutput = StringBuilder()
    private var isFlushScheduled = false
    private var isEmulatorReady = false
    private val replaySink: (String) -> Unit = ::enqueueReplay
    private val outputSink: (String) -> Unit = ::enqueueOutput
    private val flushOutput = Runnable(::flushPendingOutput)

    init {
        setBackgroundColor(TERMINAL_BACKGROUND)
        configureWebView()
        addView(inputField, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        webView.loadUrl(TERMINAL_PAGE_URL)
    }

    fun showKeyboard() {
        inputField.requestFocus()
        context.getSystemService(InputMethodManager::class.java).showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT)
    }

    fun toggleKeyboard() {
        val isImeVisible = ViewCompat.getRootWindowInsets(this)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (isImeVisible) {
            context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(windowToken, 0)
        } else {
            showKeyboard()
        }
    }

    /** xterm.js wraps the text in bracketed-paste markers when the program asked for them, and turns line breaks into CR. */
    fun paste(text: String) {
        webView.evaluateJavascript("gw.paste(${quote(text)})", null)
    }

    fun takeSelection(onText: (String) -> Unit) {
        webView.evaluateJavascript("gw.takeSelection()") { result ->
            onText(if (result == null || result == "null") "" else Json.decodeFromString(String.serializer(), result))
        }
    }

    fun clearSelection() {
        webView.evaluateJavascript("gw.clearSelection()", null)
    }

    /** Rotation, keyboard hiding and taps on the surrounding Compose UI can drop focus; hardware keys need it back. */
    fun ensureFocus() {
        if (!inputField.hasFocus()) inputField.requestFocus()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        post(::ensureFocus)
    }

    fun release() {
        session.detach(outputSink)
        mainHandler.removeCallbacksAndMessages(null)
        removeCallbacks(flushOutput)
        webView.removeJavascriptInterface(BRIDGE_NAME)
        webView.destroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        (hasFocus() && KeyEventMapper.dispatch(event, input)) || super.dispatchKeyEvent(event)

    @Suppress("DEPRECATION")
    private fun configureWebView() {
        webView.setBackgroundColor(TERMINAL_BACKGROUND)
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
        webView.isLongClickable = false
        webView.isHapticFeedbackEnabled = false
        webView.setOnLongClickListener { true }
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        with(webView.settings) {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = FULL_TEXT_ZOOM
        }
        webView.webViewClient = LocalAssetsClient()
        webView.addJavascriptInterface(Bridge(), BRIDGE_NAME)
    }

    private fun enqueueReplay(history: String) = enqueue(pendingReplay, history)

    private fun enqueueOutput(chunk: String) = enqueue(pendingOutput, chunk)

    private fun enqueue(buffer: StringBuilder, text: String) {
        synchronized(outputLock) {
            buffer.append(text)
            if (!isEmulatorReady || isFlushScheduled) return
            isFlushScheduled = true
        }
        postOnAnimation(flushOutput)
    }

    /**
     * Coalesces socket frames into one JavaScript call per frame; large backlogs drain over several frames.
     * Replayed history always drains before live output, which the session guarantees arrives after it.
     */
    private fun flushPendingOutput() {
        val (function, batch) = synchronized(outputLock) {
            val next = if (pendingReplay.isNotEmpty()) {
                "gw.replay" to takeBatch(pendingReplay)
            } else {
                "gw.write" to takeBatch(pendingOutput)
            }
            isFlushScheduled = pendingReplay.isNotEmpty() || pendingOutput.isNotEmpty()
            next
        }
        if (batch.isNotEmpty()) webView.evaluateJavascript("$function(${quote(batch)})", null)
        if (isFlushScheduled) postOnAnimation(flushOutput)
    }

    private fun takeBatch(buffer: StringBuilder): String {
        var length = minOf(buffer.length, MAX_WRITE_CHARS)
        if (length < buffer.length && Character.isHighSurrogate(buffer[length - 1])) length--
        val text = buffer.substring(0, length)
        buffer.delete(0, length)
        return text
    }

    private fun onEmulatorReady() {
        synchronized(outputLock) { isEmulatorReady = true }
        session.attach(onReplay = replaySink, onOutput = outputSink)
        ensureFocus()
        synchronized(outputLock) {
            if ((pendingReplay.isEmpty() && pendingOutput.isEmpty()) || isFlushScheduled) return
            isFlushScheduled = true
        }
        postOnAnimation(flushOutput)
    }

    private fun quote(text: String): String = Json.encodeToString(String.serializer(), text)

    private inner class LocalAssetsClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
    }

    /** Called by terminal.js on the WebView's JavaBridge thread; every callback hops to the main thread. */
    private inner class Bridge {
        @JavascriptInterface
        fun config(): String = buildJsonObject {
            put("fontSize", initialFontSize)
            put("minFontSize", AppSettings.MIN_TERMINAL_FONT_SIZE)
            put("maxFontSize", AppSettings.MAX_TERMINAL_FONT_SIZE)
        }.toString()

        @JavascriptInterface
        fun onReady() {
            mainHandler.post(::onEmulatorReady)
        }

        @JavascriptInterface
        fun onData(data: String) {
            input.sendRaw(data)
        }

        @JavascriptInterface
        fun onResize(columns: Int, rows: Int) {
            session.resize(columns, rows)
        }

        @JavascriptInterface
        fun onApplicationCursorChanged(enabled: Boolean) {
            session.applicationCursor = enabled
        }

        @JavascriptInterface
        fun onTap() {
            mainHandler.post(::showKeyboard)
        }

        @JavascriptInterface
        fun onLongPress() {
            mainHandler.post { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
        }

        @JavascriptInterface
        fun onSelectionChanged(hasSelection: Boolean) {
            mainHandler.post { events.onSelectionChanged(hasSelection) }
        }

        @JavascriptInterface
        fun onFontSizeChanged(size: Int) {
            mainHandler.post { events.onFontSizeChanged(size) }
        }

        @JavascriptInterface
        fun onClipboardCopy(text: String) {
            if (text.isNotEmpty()) mainHandler.post { events.onClipboardCopy(text) }
        }
    }

    private companion object {
        const val BRIDGE_NAME = "GwBridge"
        const val ASSETS_PATH = "/assets/"
        const val TERMINAL_PAGE_URL = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/terminal/index.html"
        const val MAX_WRITE_CHARS = 256 * 1024
        const val FULL_TEXT_ZOOM = 100
        val TERMINAL_BACKGROUND = Color.parseColor("#1A1A2E")
    }
}

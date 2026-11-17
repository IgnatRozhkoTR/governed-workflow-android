package com.ignatrozhko.governedworkflow.terminal.core

import com.ignatrozhko.governedworkflow.data.api.PanelException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

sealed interface TerminalConnectionState {
    data object Idle : TerminalConnectionState
    data class Connecting(val attempt: Int) : TerminalConnectionState
    data object Connected : TerminalConnectionState
    data class Retrying(val delayMs: Long, val attempt: Int, val reason: String) : TerminalConnectionState
    data class Rejected(val message: String) : TerminalConnectionState
    data object Paused : TerminalConnectionState
    data object Closed : TerminalConnectionState
}

/** Opens a WebSocket to [target] on the currently active panel URL; throws [PanelException] when offline. */
fun interface TerminalSocketOpener {
    fun open(target: TerminalTarget, listener: WebSocketListener): WebSocket
}

/**
 * One tmux attachment that outlives any view: it owns the socket, reconnects with backoff (tmux
 * redraws the whole screen on every re-attach) and keeps an output log for replay into new views.
 */
class TerminalSession(
    val target: TerminalTarget,
    private val opener: TerminalSocketOpener,
    private val scope: CoroutineScope,
    private val backoff: ReconnectBackoff = ReconnectBackoff(),
    logCapacityChars: Int = DEFAULT_LOG_CAPACITY_CHARS,
) {
    private val _state = MutableStateFlow<TerminalConnectionState>(TerminalConnectionState.Idle)
    val state: StateFlow<TerminalConnectionState> = _state.asStateFlow()

    /** DECCKM as last reported by the emulator view; arrow keys are encoded differently when set. */
    @Volatile
    var applicationCursor: Boolean = false

    private val outputLock = Any()
    private val log = OutputLog(logCapacityChars)
    private var sink: ((String) -> Unit)? = null

    @Volatile
    private var openSocket: WebSocket? = null

    @Volatile
    private var size: TerminalSize? = null

    private var loopJob: Job? = null
    private val retrySignal = Channel<Unit>(Channel.CONFLATED)

    fun connect() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { connectLoop() }
    }

    /** Drops the current socket (if any) and connects again immediately, e.g. after the panel URL changed. */
    fun reconnectNow() {
        loopJob?.cancel()
        backoff.reset()
        loopJob = scope.launch { connectLoop() }
    }

    /** Skips the remaining backoff wait when the network may have recovered. */
    fun retryWaitingNow() {
        if (_state.value is TerminalConnectionState.Retrying) retrySignal.trySend(Unit)
    }

    fun pause() {
        loopJob?.cancel()
        loopJob = null
        _state.value = TerminalConnectionState.Paused
    }

    fun resumeIfPaused() {
        if (_state.value != TerminalConnectionState.Paused) return
        backoff.reset()
        connect()
    }

    fun close() {
        loopJob?.cancel()
        loopJob = null
        _state.value = TerminalConnectionState.Closed
        synchronized(outputLock) { sink = null }
    }

    fun send(data: String): Boolean = data.isNotEmpty() && openSocket?.send(data) == true

    fun resize(columns: Int, rows: Int) {
        val newSize = TerminalSize(columns, rows)
        size = newSize
        openSocket?.send(newSize.controlMessage())
    }

    /**
     * Hands the output log to [onReplay], then streams live output to [onOutput]; one sink at a time.
     * History is kept apart because the emulator must not answer terminal queries it contains: tmux
     * stopped waiting for those replies long ago and would pass them to the shell as typed text.
     */
    fun attach(onReplay: (String) -> Unit, onOutput: (String) -> Unit) {
        synchronized(outputLock) {
            val history = log.snapshot()
            if (history.isNotEmpty()) onReplay(history)
            sink = onOutput
        }
    }

    fun detach(oldSink: (String) -> Unit) {
        synchronized(outputLock) { if (sink === oldSink) sink = null }
    }

    private suspend fun connectLoop() {
        while (true) {
            _state.value = TerminalConnectionState.Connecting(backoff.failedAttempts + 1)
            when (val end = connectOnce()) {
                is ConnectionEnd.Rejected -> {
                    _state.value = TerminalConnectionState.Rejected(end.message)
                    return
                }
                is ConnectionEnd.Dropped -> waitBeforeRetry(end.reason)
            }
        }
    }

    private suspend fun waitBeforeRetry(reason: String) {
        val delayMs = backoff.nextDelayMs()
        _state.value = TerminalConnectionState.Retrying(delayMs, backoff.failedAttempts, reason)
        retrySignal.tryReceive()
        withTimeoutOrNull(delayMs) { retrySignal.receive() }
    }

    private suspend fun connectOnce(): ConnectionEnd {
        val events = Channel<SocketEvent>(Channel.UNLIMITED)
        val listener = SessionListener(events)
        val socket = try {
            opener.open(target, listener)
        } catch (error: PanelException) {
            return ConnectionEnd.Dropped(error.message.orEmpty())
        }
        try {
            for (event in events) {
                when (event) {
                    SocketEvent.Opened -> onOpened(socket)
                    is SocketEvent.ServerError -> return ConnectionEnd.Rejected(event.message)
                    is SocketEvent.Ended -> return ConnectionEnd.Dropped(event.reason)
                }
            }
            return ConnectionEnd.Dropped("Connection closed")
        } finally {
            listener.isCurrent = false
            if (openSocket === socket) openSocket = null
            socket.close(NORMAL_CLOSURE, null)
        }
    }

    private fun onOpened(socket: WebSocket) {
        openSocket = socket
        backoff.reset()
        _state.value = TerminalConnectionState.Connected
        size?.let { socket.send(it.controlMessage()) }
    }

    private fun deliver(text: String) {
        synchronized(outputLock) {
            log.append(text)
            sink?.invoke(text)
        }
    }

    private inner class SessionListener(private val events: SendChannel<SocketEvent>) : WebSocketListener() {
        @Volatile
        var isCurrent = true
        private var isFirstFrame = true

        override fun onOpen(webSocket: WebSocket, response: Response) {
            events.trySend(SocketEvent.Opened)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!isCurrent) return
            if (isFirstFrame) {
                isFirstFrame = false
                val error = ServerFrames.errorMessage(text)
                if (error != null) {
                    events.trySend(SocketEvent.ServerError(error))
                    return
                }
            }
            deliver(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            events.trySend(SocketEvent.Ended("Closed by the panel"))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            events.trySend(SocketEvent.Ended("Closed by the panel"))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            events.trySend(SocketEvent.Ended(t.message ?: t.javaClass.simpleName))
        }
    }

    private sealed interface SocketEvent {
        data object Opened : SocketEvent
        data class ServerError(val message: String) : SocketEvent
        data class Ended(val reason: String) : SocketEvent
    }

    private sealed interface ConnectionEnd {
        data class Rejected(val message: String) : ConnectionEnd
        data class Dropped(val reason: String) : ConnectionEnd
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
        const val DEFAULT_LOG_CAPACITY_CHARS = 1_000_000
    }
}

data class TerminalSize(val columns: Int, val rows: Int) {
    /** The backend's control frame: a JSON text message `{"resize": [cols, rows]}`. */
    fun controlMessage(): String = """{"resize":[$columns,$rows]}"""
}

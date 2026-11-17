package com.ignatrozhko.governedworkflow.terminal.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class TerminalSessionTest {
    private val server = MockWebServer()
    private val client = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val target = TerminalTarget.NamedSession("qa-android-shell")

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    private fun session() = TerminalSession(
        target = target,
        opener = { _, listener -> client.newWebSocket(Request.Builder().url(server.url("/ws")).build(), listener) },
        scope = scope,
        backoff = ReconnectBackoff(initialDelayMs = 50, maxDelayMs = 100),
    )

    /** Server side of one upgrade: sends [greeting] on open and records received frames. */
    private class ServerSocket(private val greeting: String?) : WebSocketListener() {
        val received = LinkedBlockingQueue<String>()
        @Volatile lateinit var socket: WebSocket

        override fun onOpen(webSocket: WebSocket, response: Response) {
            socket = webSocket
            greeting?.let(webSocket::send)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            received.put(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
        }
    }

    private fun enqueueSocket(greeting: String?): ServerSocket =
        ServerSocket(greeting).also { server.enqueue(MockResponse().withWebSocketUpgrade(it)) }

    @Test
    fun connect_shouldStreamOutput_andSendInputAndResize() = runBlocking {
        val remote = enqueueSocket("hello\r\n")
        val session = session()
        val output = LinkedBlockingQueue<String>()
        session.attach(onReplay = { output.put(it) }, onOutput = { output.put(it) })

        session.resize(100, 30)
        session.connect()
        withTimeout(5_000) { session.state.first { it == TerminalConnectionState.Connected } }

        assertEquals("hello\r\n", output.poll(5, TimeUnit.SECONDS))
        assertEquals("""{"resize":[100,30]}""", remote.received.poll(5, TimeUnit.SECONDS))
        assertTrue(session.send("ls\r"))
        assertEquals("ls\r", remote.received.poll(5, TimeUnit.SECONDS))
        session.close()
    }

    @Test
    fun connect_shouldReconnect_whenServerDropsConnection() = runBlocking {
        val first = enqueueSocket("first\r\n")
        enqueueSocket("second\r\n")
        val session = session()
        val output = LinkedBlockingQueue<String>()
        session.attach(onReplay = { output.put(it) }, onOutput = { output.put(it) })
        session.connect()
        assertEquals("first\r\n", output.poll(5, TimeUnit.SECONDS))

        first.socket.close(1001, "panel restarted")

        assertEquals("second\r\n", output.poll(5, TimeUnit.SECONDS))
        assertEquals(TerminalConnectionState.Connected, withTimeout(5_000) { session.state.first { it == TerminalConnectionState.Connected } })
        session.close()
    }

    @Test
    fun connect_shouldStopRetrying_whenServerRejectsAttach() = runBlocking {
        enqueueSocket("""{"error": "No tmux session. Use Start or Resume first."}""")
        val session = session()

        session.connect()

        val state = withTimeout(5_000) { session.state.first { it is TerminalConnectionState.Rejected } }
        assertEquals(TerminalConnectionState.Rejected("No tmux session. Use Start or Resume first."), state)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun attach_shouldReplayHistoryApartFromLiveOutput_whenNewViewAttaches() = runBlocking {
        enqueueSocket("prompt$ ")
        val session = session()
        val firstView = LinkedBlockingQueue<String>()
        val firstSink: (String) -> Unit = { firstView.put(it) }
        session.attach(onReplay = firstSink, onOutput = firstSink)
        session.connect()
        assertEquals("prompt$ ", firstView.poll(5, TimeUnit.SECONDS))

        session.detach(firstSink)
        val replayed = LinkedBlockingQueue<String>()
        val live = LinkedBlockingQueue<String>()
        session.attach(onReplay = { replayed.put(it) }, onOutput = { live.put(it) })

        assertEquals("prompt$ ", replayed.poll(1, TimeUnit.SECONDS))
        assertTrue(live.isEmpty())
        session.close()
    }
}

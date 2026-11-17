package com.ignatrozhko.governedworkflow.ui.terminal

import com.ignatrozhko.governedworkflow.terminal.core.TerminalConnectionState
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSession
import com.ignatrozhko.governedworkflow.terminal.core.TerminalSocketOpener
import com.ignatrozhko.governedworkflow.terminal.core.TerminalTarget
import kotlinx.coroutines.CoroutineScope

/** The terminal sessions opened from one workspace screen, keyed by target; accessed from the main thread only. */
class TerminalSessionPool(private val opener: TerminalSocketOpener, private val scope: CoroutineScope) {
    private val sessions = LinkedHashMap<String, TerminalSession>()

    fun sessionFor(target: TerminalTarget): TerminalSession =
        sessions.getOrPut(target.key) { TerminalSession(target, opener, scope) }

    /** Releases sockets of sessions in use; idle, rejected and closed ones have nothing to keep alive. */
    fun pauseActive() = sessions.values.filter { it.isInUse() }.forEach(TerminalSession::pause)

    fun resumePaused() = sessions.values.forEach(TerminalSession::resumeIfPaused)

    fun reconnectActive() = sessions.values.filter { it.isInUse() }.forEach(TerminalSession::reconnectNow)

    fun retryWaiting() = sessions.values.forEach(TerminalSession::retryWaitingNow)

    fun closeAll() {
        sessions.values.forEach(TerminalSession::close)
        sessions.clear()
    }

    private fun TerminalSession.isInUse(): Boolean = when (state.value) {
        is TerminalConnectionState.Connecting,
        TerminalConnectionState.Connected,
        is TerminalConnectionState.Retrying,
        -> true
        TerminalConnectionState.Idle,
        is TerminalConnectionState.Rejected,
        TerminalConnectionState.Paused,
        TerminalConnectionState.Closed,
        -> false
    }
}

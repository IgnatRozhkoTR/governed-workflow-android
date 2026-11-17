package com.ignatrozhko.governedworkflow.gates

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ignatrozhko.governedworkflow.GovernedWorkflowApp
import com.ignatrozhko.governedworkflow.data.api.PanelException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Opt-in watcher that keeps polling every workspace while the app is in the background and raises a
 * high-priority notification when one starts waiting on a user gate.
 */
class GateWatchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tracker = GateTracker()
    private val scanner = GateScanner()
    private lateinit var notifications: GateNotifications

    override fun onCreate() {
        super.onCreate()
        notifications = GateNotifications(this).also { it.createChannels() }
        ServiceCompat.startForeground(
            this,
            GateNotifications.WATCHER_NOTIFICATION_ID,
            notifications.watcherNotification("Starting…"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        scope.launch { pollForever() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun pollForever() {
        val connectionManager = (application as GovernedWorkflowApp).container.connectionManager
        while (scope.isActive) {
            val status = try {
                val gates = scanner.scan(connectionManager.awaitApi(POLL_INTERVAL_MS))
                announce(tracker.update(gates))
                if (gates.isEmpty()) "No workspace is waiting" else "${gates.size} waiting for approval"
            } catch (error: PanelException) {
                "Panel unreachable: ${error.message}"
            }
            notifications.updateWatcher(status)
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun announce(changes: GateChanges) {
        changes.cleared.forEach(notifications::clearGate)
        changes.appeared.forEach(notifications::showGate)
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L

        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, GateWatchService::class.java))

        fun stop(context: Context) = context.stopService(Intent(context, GateWatchService::class.java))
    }
}

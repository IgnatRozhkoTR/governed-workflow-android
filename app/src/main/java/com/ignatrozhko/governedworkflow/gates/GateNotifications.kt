package com.ignatrozhko.governedworkflow.gates

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ignatrozhko.governedworkflow.MainActivity
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef

/** Channels, the persistent watcher notification and one high-priority notification per waiting gate. */
class GateNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(NotificationChannel(WATCH_CHANNEL, "Gate watcher", NotificationManager.IMPORTANCE_MIN))
        system.createNotificationChannel(NotificationChannel(GATE_CHANNEL, "Waiting for approval", NotificationManager.IMPORTANCE_HIGH))
    }

    fun watcherNotification(status: String): Notification = NotificationCompat.Builder(context, WATCH_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Watching for gates")
        .setContentText(status)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setContentIntent(openAppIntent(workspace = null))
        .build()

    fun updateWatcher(status: String) = notifyIfAllowed(WATCHER_NOTIFICATION_ID, watcherNotification(status))

    fun showGate(gate: WaitingGate) {
        val notification = NotificationCompat.Builder(context, GATE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${gate.label} waits for approval")
            .setContentText("Phase ${gate.phase} · ${gate.workspace.projectId}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(gate.workspace))
            .addAction(0, "Approve", GateActionReceiver.approveIntent(context, gate.workspace, notificationId(gate.workspace)))
            .addAction(0, "Open", openAppIntent(gate.workspace))
            .build()
        notifyIfAllowed(notificationId(gate.workspace), notification)
    }

    fun showOutcome(workspace: WorkspaceRef, message: String) {
        val notification = NotificationCompat.Builder(context, GATE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message)
            .setContentText(workspace.branch)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(workspace))
            .build()
        notifyIfAllowed(notificationId(workspace), notification)
    }

    fun clearGate(workspace: WorkspaceRef) = manager.cancel(notificationId(workspace))

    /** Without POST_NOTIFICATIONS the system drops the notification anyway; checking avoids the SecurityException. */
    private fun notifyIfAllowed(id: Int, notification: Notification) {
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(id, notification)
            } catch (error: SecurityException) {
                return
            }
        }
    }

    private fun openAppIntent(workspace: WorkspaceRef?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        workspace?.let { WorkspaceLink.put(intent, it) }
        val requestCode = workspace?.let(::notificationId) ?: WATCHER_NOTIFICATION_ID
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val WATCHER_NOTIFICATION_ID = 1
        private const val WATCH_CHANNEL = "gate_watcher"
        private const val GATE_CHANNEL = "gate_waiting"

        fun notificationId(workspace: WorkspaceRef): Int = "${workspace.projectId}/${workspace.branch}".hashCode().let {
            if (it == WATCHER_NOTIFICATION_ID) it + 1 else it
        }
    }
}

package com.ignatrozhko.governedworkflow.gates

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ignatrozhko.governedworkflow.GovernedWorkflowApp
import com.ignatrozhko.governedworkflow.data.api.PanelException
import com.ignatrozhko.governedworkflow.data.api.WorkspaceRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Approves a gate straight from its notification, with the default commit message where the gate takes one. */
class GateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workspace = WorkspaceLink.read(intent) ?: return
        val container = (context.applicationContext as GovernedWorkflowApp).container
        val notifications = GateNotifications(context)
        val pendingResult = goAsync()
        receiverScope.launch {
            val outcome = try {
                container.connectionManager.awaitApi(CONNECT_TIMEOUT_MS).approve(workspace, commitMessage = "")
                container.workspaceStateStore.refresh(workspace)
                "Approved"
            } catch (error: PanelException) {
                "Approve failed: ${error.message}"
            }
            notifications.showOutcome(workspace, outcome)
            pendingResult.finish()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val ACTION_APPROVE = "com.ignatrozhko.governedworkflow.APPROVE_GATE"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun approveIntent(context: Context, workspace: WorkspaceRef, requestCode: Int): PendingIntent {
            val intent = WorkspaceLink.put(Intent(context, GateActionReceiver::class.java).setAction(ACTION_APPROVE), workspace)
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

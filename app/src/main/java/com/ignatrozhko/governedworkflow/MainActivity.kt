package com.ignatrozhko.governedworkflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.ignatrozhko.governedworkflow.gates.GateWatchService
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.nav.AppNavHost
import com.ignatrozhko.governedworkflow.ui.theme.GovernedWorkflowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GovernedWorkflowApp).container
        container.workspaceLinks.request(intent)
        resumeGateWatchIfEnabled(container)
        setContent {
            GovernedWorkflowTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppNavHost(container)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        (application as GovernedWorkflowApp).container.workspaceLinks.request(intent)
    }

    /** A foreground service may only be started while the app is visible, so the opt-in watcher is resumed here. */
    private fun resumeGateWatchIfEnabled(container: AppContainer) {
        lifecycleScope.launch {
            if (container.settings.isGateWatchEnabled.first()) GateWatchService.start(this@MainActivity)
        }
    }
}

package com.ignatrozhko.governedworkflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ignatrozhko.governedworkflow.data.settings.ThemeMode
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
            val themeMode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = null)
            val chosenMode = themeMode ?: return@setContent
            GovernedWorkflowTheme(isNight = chosenMode.isNight()) {
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

    @Composable
    private fun ThemeMode.isNight(): Boolean = when (this) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Day -> false
        ThemeMode.Night -> true
    }

    /** A foreground service may only be started while the app is visible, so the opt-in watcher is resumed here. */
    private fun resumeGateWatchIfEnabled(container: AppContainer) {
        lifecycleScope.launch {
            if (container.settings.isGateWatchEnabled.first()) GateWatchService.start(this@MainActivity)
        }
    }
}

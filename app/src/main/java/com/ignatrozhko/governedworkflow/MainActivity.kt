package com.ignatrozhko.governedworkflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import com.ignatrozhko.governedworkflow.ui.nav.AppNavHost
import com.ignatrozhko.governedworkflow.ui.theme.GovernedWorkflowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GovernedWorkflowApp).container
        setContent {
            GovernedWorkflowTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppNavHost(container)
                }
            }
        }
    }
}

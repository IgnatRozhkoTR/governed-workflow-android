package com.ignatrozhko.governedworkflow.ui.projects

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ignatrozhko.governedworkflow.R
import com.ignatrozhko.governedworkflow.data.settings.AppSettings
import com.ignatrozhko.governedworkflow.gates.GateWatchService
import com.ignatrozhko.governedworkflow.ui.common.LocalAppContainer
import kotlinx.coroutines.launch

/** Bell in the projects bar that turns the background gate watcher on and off; asks for notification permission first. */
@Composable
fun GateWatchToggle() {
    val settings = LocalAppContainer.current.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEnabled by settings.isGateWatchEnabled.collectAsStateWithLifecycle(initialValue = false)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) scope.launch { setGateWatch(context, settings, isEnabled = true) }
    }
    IconToggleButton(checked = isEnabled, onCheckedChange = { isTurningOn ->
        if (isTurningOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { setGateWatch(context, settings, isTurningOn) }
        }
    }) {
        Icon(
            painterResource(R.drawable.ic_notification),
            contentDescription = if (isEnabled) "Stop watching gates" else "Watch gates",
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private suspend fun setGateWatch(context: Context, settings: AppSettings, isEnabled: Boolean) {
    settings.setGateWatchEnabled(isEnabled)
    if (isEnabled) GateWatchService.start(context) else GateWatchService.stop(context)
}

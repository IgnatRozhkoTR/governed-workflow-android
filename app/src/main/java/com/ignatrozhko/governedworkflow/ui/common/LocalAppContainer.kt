package com.ignatrozhko.governedworkflow.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.ignatrozhko.governedworkflow.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer is not provided") }

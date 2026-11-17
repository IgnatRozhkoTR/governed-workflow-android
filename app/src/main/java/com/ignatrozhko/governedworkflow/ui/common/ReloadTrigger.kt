package com.ignatrozhko.governedworkflow.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Monotonic counter whose changes ask a [loadWhenConnected] pipeline to load again. */
class ReloadTrigger {
    private val counter = MutableStateFlow(0)
    val reloads: StateFlow<Int> = counter.asStateFlow()

    fun reload() = counter.update { it + 1 }
}

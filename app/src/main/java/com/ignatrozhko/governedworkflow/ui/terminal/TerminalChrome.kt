package com.ignatrozhko.governedworkflow.ui.terminal

import com.ignatrozhko.governedworkflow.data.settings.ExtraKeysMode

/** How much chrome surrounds the terminal; the screen derives it from the full-screen toggle, keyboard and orientation. */
data class TerminalChrome(val isFullScreen: Boolean, val isKeyboardOpen: Boolean, val isLandscape: Boolean) {
    /** Landscape with the keyboard up leaves only a few terminal lines, so the header gives way there too. */
    private val isKeyboardLandscape: Boolean get() = isLandscape && isKeyboardOpen

    /** The title row, session chips, gate strip and the workspace tabs. */
    val isHeaderShown: Boolean get() = !isFullScreen && !isKeyboardLandscape

    val keyRows: List<List<ExtraKey>> get() = if (isKeyboardLandscape) ExtraKeyLayout.oneRow else ExtraKeyLayout.twoRows

    fun showsKeys(mode: ExtraKeysMode): Boolean = mode.isVisible(isKeyboardOpen)
}

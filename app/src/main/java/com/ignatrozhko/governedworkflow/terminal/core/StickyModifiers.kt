package com.ignatrozhko.governedworkflow.terminal.core

enum class StickyLevel { Off, Once, Locked }

enum class StickyKey { Ctrl, Alt }

/** Termux-style modifiers: one tap arms for the next key, a second tap locks, a third releases. */
data class StickyModifiers(val ctrl: StickyLevel = StickyLevel.Off, val alt: StickyLevel = StickyLevel.Off) {

    val active: Modifiers get() = Modifiers(ctrl = ctrl != StickyLevel.Off, alt = alt != StickyLevel.Off)

    fun tap(key: StickyKey): StickyModifiers = when (key) {
        StickyKey.Ctrl -> copy(ctrl = ctrl.next())
        StickyKey.Alt -> copy(alt = alt.next())
    }

    /** The state after a key press consumed the modifiers: one-shot levels release, locks stay. */
    fun afterKeyPress(): StickyModifiers = StickyModifiers(ctrl.afterUse(), alt.afterUse())

    private fun StickyLevel.next(): StickyLevel = when (this) {
        StickyLevel.Off -> StickyLevel.Once
        StickyLevel.Once -> StickyLevel.Locked
        StickyLevel.Locked -> StickyLevel.Off
    }

    private fun StickyLevel.afterUse(): StickyLevel = if (this == StickyLevel.Once) StickyLevel.Off else this
}

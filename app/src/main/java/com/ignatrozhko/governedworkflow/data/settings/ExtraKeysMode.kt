package com.ignatrozhko.governedworkflow.data.settings

/** When the terminal shows its extra key rows; [WithKeyboard] shows them only while the soft keyboard is open. */
enum class ExtraKeysMode {
    Shown,
    Hidden,
    WithKeyboard;

    fun isVisible(isKeyboardOpen: Boolean): Boolean = when (this) {
        Shown -> true
        Hidden -> false
        WithKeyboard -> isKeyboardOpen
    }

    /** The handle flips what is on screen now, so tapping it in automatic mode settles on the manual mode it shows. */
    fun toggled(isKeyboardOpen: Boolean): ExtraKeysMode = if (isVisible(isKeyboardOpen)) Hidden else Shown

    fun withKeyboardToggled(): ExtraKeysMode = if (this == WithKeyboard) Shown else WithKeyboard
}

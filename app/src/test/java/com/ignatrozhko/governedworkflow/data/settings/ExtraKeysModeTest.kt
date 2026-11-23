package com.ignatrozhko.governedworkflow.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraKeysModeTest {

    @Test
    fun isVisible_shouldFollowKeyboard_onlyInWithKeyboardMode() {
        assertTrue(ExtraKeysMode.Shown.isVisible(isKeyboardOpen = false))
        assertFalse(ExtraKeysMode.Hidden.isVisible(isKeyboardOpen = true))
        assertTrue(ExtraKeysMode.WithKeyboard.isVisible(isKeyboardOpen = true))
        assertFalse(ExtraKeysMode.WithKeyboard.isVisible(isKeyboardOpen = false))
    }

    @Test
    fun toggled_shouldSwitchBetweenShownAndHidden() {
        assertEquals(ExtraKeysMode.Hidden, ExtraKeysMode.Shown.toggled(isKeyboardOpen = false))
        assertEquals(ExtraKeysMode.Shown, ExtraKeysMode.Hidden.toggled(isKeyboardOpen = true))
    }

    @Test
    fun toggled_shouldFlipWhatIsOnScreen_whenInWithKeyboardMode() {
        assertEquals(ExtraKeysMode.Hidden, ExtraKeysMode.WithKeyboard.toggled(isKeyboardOpen = true))
        assertEquals(ExtraKeysMode.Shown, ExtraKeysMode.WithKeyboard.toggled(isKeyboardOpen = false))
    }

    @Test
    fun withKeyboardToggled_shouldEnterAutomaticMode_andLeaveItToShown() {
        assertEquals(ExtraKeysMode.WithKeyboard, ExtraKeysMode.Shown.withKeyboardToggled())
        assertEquals(ExtraKeysMode.WithKeyboard, ExtraKeysMode.Hidden.withKeyboardToggled())
        assertEquals(ExtraKeysMode.Shown, ExtraKeysMode.WithKeyboard.withKeyboardToggled())
    }
}

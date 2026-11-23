package com.ignatrozhko.governedworkflow.ui.terminal

import com.ignatrozhko.governedworkflow.data.settings.ExtraKeysMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalChromeTest {

    @Test
    fun isHeaderShown_shouldBeTrue_whenNotFullScreenInPortraitWithKeyboard() {
        assertTrue(chrome(isKeyboardOpen = true).isHeaderShown)
    }

    @Test
    fun isHeaderShown_shouldBeFalse_whenFullScreen() {
        assertFalse(chrome(isFullScreen = true).isHeaderShown)
    }

    @Test
    fun isHeaderShown_shouldBeFalse_whenLandscapeWithKeyboard() {
        assertFalse(chrome(isKeyboardOpen = true, isLandscape = true).isHeaderShown)
    }

    @Test
    fun keyRows_shouldCollapseToOneRow_onlyInLandscapeWithKeyboard() {
        assertEquals(ExtraKeyLayout.oneRow, chrome(isFullScreen = true, isKeyboardOpen = true, isLandscape = true).keyRows)
        assertEquals(ExtraKeyLayout.twoRows, chrome(isFullScreen = true, isLandscape = true).keyRows)
        assertEquals(ExtraKeyLayout.twoRows, chrome(isKeyboardOpen = true).keyRows)
    }

    @Test
    fun showsKeys_shouldFollowMode_independentlyOfFullScreen() {
        assertFalse(chrome(isFullScreen = true).showsKeys(ExtraKeysMode.Hidden))
        assertTrue(chrome(isFullScreen = true).showsKeys(ExtraKeysMode.Shown))
        assertTrue(chrome(isFullScreen = true, isKeyboardOpen = true).showsKeys(ExtraKeysMode.WithKeyboard))
        assertFalse(chrome(isFullScreen = false).showsKeys(ExtraKeysMode.WithKeyboard))
    }

    private fun chrome(isFullScreen: Boolean = false, isKeyboardOpen: Boolean = false, isLandscape: Boolean = false) =
        TerminalChrome(isFullScreen, isKeyboardOpen, isLandscape)
}
